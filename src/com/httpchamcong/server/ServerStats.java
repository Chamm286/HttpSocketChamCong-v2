package com.httpchamcong.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thống kê server — Thread-safe + track chi tiết request
 */
public class ServerStats {
    private static final AtomicInteger activeClients = new AtomicInteger(0);
    private static final AtomicInteger totalClients  = new AtomicInteger(0);
    private static final AtomicLong    totalRequests = new AtomicLong(0);
    private static final AtomicLong    totalBytes    = new AtomicLong(0);
    private static final AtomicLong    totalDuration = new AtomicLong(0);
    private static volatile long      startTime     = 0;

    // ⭐ Track method counts
    private static final Map<String, AtomicInteger> methodCounts = new ConcurrentHashMap<>();

    // ⭐ Track status code counts (theo class: 2xx, 3xx, 4xx, 5xx + cụ thể 200/201/400/401/404/500)
    private static final int[] statusCounts = new int[6]; // [0]=200, [1]=201, [2]=400, [3]=401, [4]=404, [5]=500

    // ⭐ Recent requests (giữ 200 cái gần nhất) — dùng synchronized list
    public static class RequestInfo {
        public final String time;
        public final String method;
        public final String path;
        public final int status;
        public final long duration;
        public final int bytes;
        public final String clientIp;

        public RequestInfo(String time, String method, String path, int status,
                           long duration, int bytes, String clientIp) {
            this.time = time;
            this.method = method;
            this.path = path;
            this.status = status;
            this.duration = duration;
            this.bytes = bytes;
            this.clientIp = clientIp;
        }
    }

    private static final List<RequestInfo> recentRequests =
        Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_RECENT = 200;

    // ⭐ Live JSON: raw HTTP Response Body gần nhất (phục vụ tab Live JSON)
    public static class JsonTrace {
        public final long id;
        public final String time;
        public final String method;
        public final String path;
        public final int status;
        public final String body;

        public JsonTrace(long id, String time, String method, String path, int status, String body) {
            this.id = id;
            this.time = time;
            this.method = method;
            this.path = path;
            this.status = status;
            this.body = body;
        }
    }

    private static final List<JsonTrace> jsonTraces =
        Collections.synchronizedList(new ArrayList<>());
    private static final AtomicLong jsonTraceSeq = new AtomicLong(0);
    private static final int MAX_JSON_TRACES = 50;

    // ⭐ Requests trong giây gần nhất (dùng cho LineChart)
    private static final List<Long> requestTimestamps =
        Collections.synchronizedList(new ArrayList<>());

    public static void serverStarted() {
        startTime = System.currentTimeMillis();
    }

    public static void clientConnected() {
        activeClients.incrementAndGet();
        totalClients.incrementAndGet();
    }

    public static void clientDisconnected() {
        activeClients.decrementAndGet();
    }

    public static void requestHandled() {
        totalRequests.incrementAndGet();
    }

    /**
     * ⭐ GHI NHẬN REQUEST THẬT — gọi từ ClientHandler
     */
    public static void recordRequest(String method, String path, int status,
                                     long duration, int bytes, String clientIp) {
        // 1. Tăng counters
        totalRequests.incrementAndGet();
        totalBytes.addAndGet(bytes);
        totalDuration.addAndGet(duration);

        // 2. Track method
        methodCounts.computeIfAbsent(method, k -> new AtomicInteger(0)).incrementAndGet();

        // 3. Track status
        synchronized (statusCounts) {
            if (status == 200) statusCounts[0]++;
            else if (status == 201) statusCounts[1]++;
            else if (status == 400) statusCounts[2]++;
            else if (status == 401) statusCounts[3]++;
            else if (status == 404) statusCounts[4]++;
            else if (status >= 500) statusCounts[5]++;
        }

        // 4. Thêm vào list gần đây
        String time = java.time.LocalTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        synchronized (recentRequests) {
            recentRequests.add(0, new RequestInfo(time, method, path, status, duration, bytes, clientIp));
            if (recentRequests.size() > MAX_RECENT) {
                recentRequests.remove(recentRequests.size() - 1);
            }
        }

        // 5. Track timestamp cho LineChart
        synchronized (requestTimestamps) {
            requestTimestamps.add(System.currentTimeMillis());
            // Xóa timestamp cũ hơn 60 giây
            long cutoff = System.currentTimeMillis() - 60000;
            requestTimestamps.removeIf(ts -> ts < cutoff);
        }
    }

    /**
     * ⭐ Lấy số request trong N giây gần nhất
     */
    public static int getRequestsInLastNSeconds(int n) {
        long cutoff = System.currentTimeMillis() - (n * 1000L);
        synchronized (requestTimestamps) {
            int count = 0;
            for (Long ts : requestTimestamps) {
                if (ts >= cutoff) count++;
            }
            return count;
        }
    }

    /**
     * ⭐ Lấy snapshot của recent requests
     */
    public static List<RequestInfo> getRecentRequests() {
        synchronized (recentRequests) {
            return new ArrayList<>(recentRequests);
        }
    }

    /** Ghi lại raw JSON response body để tab Live JSON hiển thị. */
    public static void recordJsonResponse(String method, String path, int status, String body) {
        if (body == null) body = "";
        if (body.length() > 65536) body = body.substring(0, 65536) + "\n...[truncated]";
        String time = java.time.LocalTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        synchronized (jsonTraces) {
            jsonTraces.add(0, new JsonTrace(
                jsonTraceSeq.incrementAndGet(), time, method, path, status, body));
            while (jsonTraces.size() > MAX_JSON_TRACES) {
                jsonTraces.remove(jsonTraces.size() - 1);
            }
        }
    }

    public static List<JsonTrace> getJsonTraces() {
        synchronized (jsonTraces) {
            return new ArrayList<>(jsonTraces);
        }
    }

    public static void clearJsonTraces() {
        synchronized (jsonTraces) {
            jsonTraces.clear();
        }
    }

    public static int getStatusCount(int idx) {
        synchronized (statusCounts) {
            if (idx >= 0 && idx < statusCounts.length) return statusCounts[idx];
            return 0;
        }
    }

    public static int getMethodCount(String method) {
        AtomicInteger ai = methodCounts.get(method);
        return ai == null ? 0 : ai.get();
    }

    public static long getTotalBytes() { return totalBytes.get(); }

    public static double getAvgLatency() {
        long cnt = totalRequests.get();
        if (cnt == 0) return 0;
        return (double) totalDuration.get() / cnt;
    }

    public static int getActiveClients()  { return activeClients.get(); }
    public static int getTotalClients()   { return totalClients.get(); }
    public static long getTotalRequests() { return totalRequests.get(); }

    public static String getUptime() {
        if (startTime == 0) return "00:00:00";
        long sec = (System.currentTimeMillis() - startTime) / 1000;
        return String.format("%02d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60);
    }
}
