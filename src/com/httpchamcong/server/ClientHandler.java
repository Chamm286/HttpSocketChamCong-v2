package com.httpchamcong.server;

import com.httpchamcong.model.User;
import com.httpchamcong.util.JsonUtil;
import com.httpchamcong.util.SessionManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Xử lý một kết nối TCP.
 *
 * HTTP/1.1 persistent connection (keep-alive):
 *   - Một Socket có thể nhận nhiều request liên tiếp.
 *   - Sau mỗi response, server đọc tiếp request kế tiếp trên cùng kết nối.
 *   - Chỉ đóng khi client gửi Connection: close, khi idle timeout, hoặc khi có lỗi.
 */
public class ClientHandler implements Runnable {

    /** Thời gian giữ kết nối rảnh trước khi đóng. */
    private static final int IDLE_TIMEOUT_MS = 30_000;

    /** Giới hạn số request trên một kết nối để tránh giữ thread vô hạn. */
    private static final int MAX_REQUESTS_PER_CONNECTION = 100;

    private final Socket socket;
    private final Router router;
    private final HttpSocketServer server;

    public ClientHandler(Socket socket, Router router, HttpSocketServer server) {
        this.socket = socket;
        this.router = router;
        this.server = server;
    }

    @Override
    public void run() {
        String clientInfo = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        String clientIp = socket.getInetAddress().getHostAddress();
        String threadName = Thread.currentThread().getName();

        server.logExternal("[TCP] Accepted connection from " + clientInfo + " (OSI Layer 4 - Transport/TCP)");
        server.logExternal("[" + threadName + "] + Client: " + clientInfo);

        try (Socket s = socket) {
            s.setSoTimeout(IDLE_TIMEOUT_MS);
            s.setTcpNoDelay(true);

            InputStream in = s.getInputStream();
            OutputStream out = s.getOutputStream();

            int requestCount = 0;

            while (server.isRunning() && requestCount < MAX_REQUESTS_PER_CONNECTION) {
                HttpRequestParser req;

server.logExternal("[HTTP] Parsing Request from " + clientInfo + " (OSI Layer 7 - Application/HTTP)");
                try {
                    req = HttpRequestParser.parse(in);
                } catch (HttpRequestParser.HttpParseException pe) {
                    // Parse lỗi: trả response lỗi rồi đóng kết nối.
                    long start = System.currentTimeMillis();
                    HttpResponseBuilder err = new HttpResponseBuilder()
                        .status(pe.getStatusCode())
                        .connection(false)
                        .json("{\"error\":\"" + JsonUtil.escape(pe.getMessage()) + "\"}");

                    err.writeTo(out);
                    ServerStats.recordRequest("?", "?", pe.getStatusCode(),
                        System.currentTimeMillis() - start, err.getBodyLength(), clientIp);
                    server.logExternal("[HTTP] Parse error " + pe.getStatusCode() + ": " + pe.getMessage());
                    return;
                } catch (SocketTimeoutException ste) {
                    // Client giữ kết nối nhưng không gửi request tiếp theo.
                    if (requestCount > 0) {
                        server.logExternal("[" + threadName + "]   keep-alive idle timeout");
                    }
                    return;
                }

                if (req == null) return; // client đóng kết nối sạch

                requestCount++;
                long start = System.currentTimeMillis();
                String method = req.getMethod();
                String path = req.getPath();

                server.logExternal("[" + threadName + "]   " + req
                    + "  (keep-alive: " + req.isKeepAlive() + ")");

                // Cập nhật ClientManager nếu có token.
                String token = req.getBearerToken();
                if (token != null) {
                    User u = SessionManager.getUser(token);
                    if (u != null) {
                        ClientManager.addOrUpdate(
                            u.getUsername(),
                            socket.getInetAddress().getHostAddress(),
                            socket.getPort()
                        );
                    }
                }

                boolean keepAlive = req.isKeepAlive()
                    && requestCount < MAX_REQUESTS_PER_CONNECTION;

                HttpResponseBuilder res;
                try {
                    res = router.handle(req);
                } catch (Exception e) {
                    server.logExternal("[!] Loi route " + method + " " + path + ": " + e.getMessage());
                    res = new HttpResponseBuilder()
                        .status(500)
                        .json("{\"error\":\"Loi server noi bo\"}");
                    keepAlive = false;
                }

                // Cho Network Console lấy đúng request thô của check-in/check-out.
                addRequestTrace(req, res);

                // Tab Live JSON của Server Dashboard: lưu raw response body JSON.
                captureJsonResponse(method, path, res);

                // Quyết định Connection theo request, không hard-code close.
                res.connection(keepAlive);
                if (keepAlive) {
                    res.header("Keep-Alive",
                        "timeout=" + (IDLE_TIMEOUT_MS / 1000)
                        + ", max=" + (MAX_REQUESTS_PER_CONNECTION - requestCount));
                } else {
                    res.removeHeader("Keep-Alive");
                }

if (keepAlive) {
                    server.logExternal("[TCP] Keeping connection alive (HTTP/1.1 keep-alive)");
                } else {
                    server.logExternal("[TCP] Closing connection (Connection: close)");
                }

                int statusCode = res.getStatusCode();
                int responseBytes = res.getBodyLength();

                // RFC 7231: HEAD giống GET nhưng không gửi body.
                res.writeTo(out, "HEAD".equals(method));

                long duration = System.currentTimeMillis() - start;
                ServerStats.recordRequest(method, path, statusCode, duration, responseBytes, clientIp);
                server.logExternal("[REQUEST] " + method + " " + path + " -> "
                    + statusCode + " (" + duration + "ms, bytes=" + responseBytes + ")");

                if (!keepAlive) break;
            }
        } catch (SocketTimeoutException ste) {
            server.logExternal("[KEEP-ALIVE] idle timeout: " + clientInfo);
        } catch (IOException e) {
            // Client reset/đóng socket giữa chừng — không phải lỗi server.
            server.logExternal("[CLIENT] Dong ket noi " + clientInfo + ": " + e.getMessage());
        } catch (Exception e) {
            server.logExternal("[!] Loi client " + clientInfo + ": " + e.getMessage());
        } finally {
            ServerStats.clientDisconnected();
            server.logExternal("[" + threadName + "] - Dong: " + clientInfo);
        }
    }

    /** Lưu response JSON thô để Server Dashboard hiển thị ở tab Live JSON. */
    private void captureJsonResponse(String method, String path, HttpResponseBuilder res) {
        try {
            String contentType = res.getHeaders().get("Content-Type");
            if (contentType != null && contentType.toLowerCase().contains("application/json")) {
                ServerStats.recordJsonResponse(method, path, res.getStatusCode(), res.getBody());
            }
        } catch (Exception ignored) {
            // Không để lỗi capture ảnh hưởng response chính.
        }
    }

    /**
     * Gắn request thô (đã che token) vào header để Web Network Console hiển thị.
     * Chỉ gắn cho check-in / check-out — đúng mục tiêu chứng minh giao thức.
     */
    private void addRequestTrace(HttpRequestParser req, HttpResponseBuilder res) {
        String path = req.getPath();
        if (!"/api/checkin".equals(path) && !"/api/checkout".equals(path)) return;

        try {
            String raw = req.getRawRequestRedacted();
            if (raw.length() > 4096) raw = raw.substring(0, 4096) + "\r\n...[truncated]";
            String encoded = Base64.getEncoder().encodeToString(
                raw.getBytes(StandardCharsets.UTF_8));
            res.header("X-Request-Trace", encoded);
        } catch (Exception ignored) {
            // Trace chỉ phục vụ demo, không được làm hỏng request chính.
        }
    }
}
