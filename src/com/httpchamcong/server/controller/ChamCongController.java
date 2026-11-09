package com.httpchamcong.server.controller;

import com.httpchamcong.dao.ChamCongDAO;
import com.httpchamcong.model.ChamCong;
import com.httpchamcong.model.User;
import com.httpchamcong.server.FaceAuthStore;
import com.httpchamcong.server.HttpRequestParser;
import com.httpchamcong.server.HttpResponseBuilder;
import com.httpchamcong.util.JsonUtil;
import com.httpchamcong.util.SessionManager;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ChamCongController {

    private static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss");

    private User auth(HttpRequestParser req) {
        String token = req.getBearerToken();
        return SessionManager.getUser(token);
    }

    // ============ TODAY STATUS ============
    /**
     * GET /api/attendance/today
     * Trả về trạng thái chấm công hôm nay của user đang đăng nhập.
     * status: NOT_CHECKED_IN | WORKING | COMPLETED
     */
    public HttpResponseBuilder today(HttpRequestParser req) {
        User u = auth(req);
        if (u == null)
            return new HttpResponseBuilder().status(401)
                .json("{\"error\":\"Chua dang nhap\"}");

        ChamCong c = ChamCongDAO.findToday(u.getUsername());
        return new HttpResponseBuilder().status(200)
            .json(attendanceJson(c, null, LocalTime.now()));
    }

    // ============ CHECK-IN ============
    /**
     * POST /api/checkin
     * - Nếu hôm nay chưa có bản ghi: INSERT giờ hiện tại (do server quyết định).
     * - Nếu đã có bản ghi: 400 Bad Request.
     */
    public HttpResponseBuilder checkIn(HttpRequestParser req) {
        User u = auth(req);
        if (u == null)
            return new HttpResponseBuilder().status(401)
                .json("{\"error\":\"Chua dang nhap\"}");

        // GPS lấy từ trình duyệt; FaceID chỉ được chấp nhận qua faceToken do
        // /api/face/verify cấp. Server không tin cờ faceVerified do client gửi.
        Double lat = JsonUtil.getDouble(req.getBody(), "lat");
        if (lat == null) lat = JsonUtil.getDouble(req.getBody(), "checkinLat");
        Double lng = JsonUtil.getDouble(req.getBody(), "lng");
        if (lng == null) lng = JsonUtil.getDouble(req.getBody(), "checkinLng");
        String note = JsonUtil.getString(req.getBody(), "note");
        String faceToken = JsonUtil.getString(req.getBody(), "faceToken");

        FaceAuthStore.Proof proof =
            FaceAuthStore.consumeToken(u.getUsername(), "CHECKIN", faceToken);
        if (proof == null) {
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"Can xac thuc FaceID truoc khi check-in\"}");
        }

        if (lat != null && (lat < -90 || lat > 90))
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"Vi do GPS khong hop le\"}");
        if (lng != null && (lng < -180 || lng > 180))
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"Kinh do GPS khong hop le\"}");

        // Giờ do server ghi nhận, client không gửi giờ lên.
        String time = LocalTime.now().format(TIME_FMT);
        if (!ChamCongDAO.checkIn(u.getUsername(), time, lat, lng, true,
                proof.score, proof.liveness, note)) {
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"Hom nay da check-in roi\"}");
        }

        ChamCong saved = ChamCongDAO.findToday(u.getUsername());
        if (saved == null) {
            return new HttpResponseBuilder().status(500)
                .json("{\"error\":\"Khong doc duoc ban ghi vua tao\"}");
        }

        return new HttpResponseBuilder().status(200)
            .json(attendanceJson(saved, "Check-in thanh cong", LocalTime.now()));
    }

    // ============ CHECK-OUT ============
    /**
     * POST /api/checkout
     * - Phải đã check-in và check_out đang NULL.
     * - UPDATE giờ ra, sau đó tính tổng thời gian làm từ dữ liệu DB.
     * - Nếu chưa check-in hoặc đã check-out: 400 Bad Request.
     */
    public HttpResponseBuilder checkOut(HttpRequestParser req) {
        User u = auth(req);
        if (u == null)
            return new HttpResponseBuilder().status(401)
                .json("{\"error\":\"Chua dang nhap\"}");

        String faceToken = JsonUtil.getString(req.getBody(), "faceToken");
        FaceAuthStore.Proof proof =
            FaceAuthStore.consumeToken(u.getUsername(), "CHECKOUT", faceToken);
        if (proof == null) {
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"Can xac thuc FaceID truoc khi check-out\"}");
        }

        String time = LocalTime.now().format(TIME_FMT);
        if (!ChamCongDAO.checkOut(u.getUsername(), time, true,
                proof.score, proof.liveness)) {
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"Chua check-in hoac da check-out\"}");
        }

        ChamCong saved = ChamCongDAO.findToday(u.getUsername());
        if (saved == null || saved.getCheckOut() == null) {
            return new HttpResponseBuilder().status(500)
                .json("{\"error\":\"Khong doc duoc ban ghi sau khi check-out\"}");
        }

        return new HttpResponseBuilder().status(200)
            .json(attendanceJson(saved, "Check-out thanh cong", LocalTime.now()));
    }

    /**
     * Build JSON chấm công từ đúng dữ liệu DB.
     * Không hardcode giờ vào/ra; workedMinutes được tính từ checkIn/checkOut.
     */
    private String attendanceJson(ChamCong c, String message, LocalTime serverTime) {
        String date = c != null && c.getDate() != null
            ? c.getDate()
            : LocalDate.now().toString();
        String checkIn = c == null || c.getCheckIn() == null ? "" : c.getCheckIn();
        String checkOut = c == null || c.getCheckOut() == null ? "" : c.getCheckOut();

        String status;
        if (checkIn.isEmpty()) {
            status = "NOT_CHECKED_IN";
        } else if (checkOut.isEmpty()) {
            status = "WORKING";
        } else {
            status = "COMPLETED";
        }

        long workedMinutes = workedMinutes(c);
        String now = (serverTime == null ? LocalTime.now() : serverTime).format(TIME_FMT);

        StringBuilder sb = new StringBuilder("{");
        if (message != null) {
            sb.append("\"msg\":\"").append(JsonUtil.escape(message)).append("\",");
        }
        sb.append("\"time\":\"").append(JsonUtil.escape(now)).append("\",")
          .append("\"date\":\"").append(JsonUtil.escape(date)).append("\",")
          .append("\"checkIn\":\"").append(JsonUtil.escape(checkIn)).append("\",")
          .append("\"checkOut\":\"").append(JsonUtil.escape(checkOut)).append("\",")
          .append("\"checkinLat\":").append(c == null || c.getCheckinLat() == null ? "null" : c.getCheckinLat()).append(",")
          .append("\"checkinLng\":").append(c == null || c.getCheckinLng() == null ? "null" : c.getCheckinLng()).append(",")
          .append("\"checkinFaceVerified\":").append(c != null && c.isCheckinFaceVerified()).append(",")
          .append("\"checkinFaceScore\":").append(c == null || c.getCheckinFaceScore() == null ? "null" : c.getCheckinFaceScore()).append(",")
          .append("\"checkinLiveness\":").append(c == null || c.getCheckinLiveness() == null ? "null" : c.getCheckinLiveness()).append(",")
          .append("\"checkoutFaceVerified\":").append(c != null && c.isCheckoutFaceVerified()).append(",")
          .append("\"checkoutFaceScore\":").append(c == null || c.getCheckoutFaceScore() == null ? "null" : c.getCheckoutFaceScore()).append(",")
          .append("\"checkoutLiveness\":").append(c == null || c.getCheckoutLiveness() == null ? "null" : c.getCheckoutLiveness()).append(",")
          .append("\"note\":\"").append(JsonUtil.escape(c == null ? "" : c.getNote())).append("\",")
          .append("\"status\":\"").append(status).append("\",")
          .append("\"workedMinutes\":").append(workedMinutes).append(",")
          .append("\"serverTime\":\"").append(JsonUtil.escape(now)).append("\"}");
        return sb.toString();
    }

    private LocalTime parseTime(String t) {
        if (t == null || t.isEmpty()) return null;
        String s = t.length() >= 8 ? t.substring(0, 8) : t;
        try { return LocalTime.parse(s, TIME_FMT); }
        catch (Exception e) { return null; }
    }

    private long workedMinutes(ChamCong c) {
        if (c == null) return 0;
        LocalTime start = parseTime(c.getCheckIn());
        LocalTime end = c.getCheckOut() != null
            ? parseTime(c.getCheckOut())
            : LocalTime.now();
        if (start == null || end == null) return 0;

        long mins = Duration.between(start, end).toMinutes();
        // Ca đêm: giờ ra nhỏ hơn giờ vào → cộng thêm 24h.
        if (mins < 0) mins += 24 * 60;
        return Math.max(0, mins);
    }

    // ============ HISTORY + FILTER + PAGINATION ============
    /**
     * GET /api/history?from=2026-09-01&to=2026-09-30&user=nhanvien1&page=1&limit=20
     */
    public HttpResponseBuilder history(HttpRequestParser req) {
        User u = auth(req);
        if (u == null)
            return new HttpResponseBuilder().status(401)
                .json("{\"error\":\"Chua dang nhap\"}");

        // PARSE QUERY STRING
        String from = req.getParams().get("from");
        String to   = req.getParams().get("to");
        String userFilter = req.getParams().get("user");

        int page = 1, limit = 20;
        try {
            if (req.getParams().get("page") != null)
                page = Integer.parseInt(req.getParams().get("page"));
            if (req.getParams().get("limit") != null)
                limit = Integer.parseInt(req.getParams().get("limit"));
        } catch (Exception e) { /* dùng default */ }

        if (page < 1) page = 1;
        if (limit < 1) limit = 10;
        if (limit > 100) limit = 100;

        // Bước 1: Lấy data theo filter
        List<ChamCong> list;
        if ("ADMIN".equals(u.getRole())) {
            if (userFilter != null && !userFilter.isEmpty()) {
                list = ChamCongDAO.findByUser(userFilter);
            } else {
                list = ChamCongDAO.loadAll();
            }
        } else {
            list = ChamCongDAO.findByUser(u.getUsername());
        }

        // Bước 2: Filter theo ngày
        List<ChamCong> filtered = new ArrayList<>();
        for (ChamCong c : list) {
            if (from != null && !from.isEmpty() && c.getDate().compareTo(from) < 0) continue;
            if (to != null && !to.isEmpty() && c.getDate().compareTo(to) > 0) continue;
            filtered.add(c);
        }

        // Bước 3: Pagination
        int total = filtered.size();
        int totalPages = (int) Math.ceil((double) total / limit);
        int fromIdx = (page - 1) * limit;
        int toIdx = Math.min(fromIdx + limit, total);

        List<ChamCong> pageData;
        if (fromIdx >= total) {
            pageData = new ArrayList<>();
        } else {
            pageData = filtered.subList(fromIdx, toIdx);
        }

        // Bước 4: Build JSON
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < pageData.size(); i++) {
            sb.append(pageData.get(i).toJson());
            if (i < pageData.size() - 1) sb.append(",");
        }
        sb.append("]");

        return new HttpResponseBuilder().status(200).json(
            "{\"page\":" + page +
            ",\"limit\":" + limit +
            ",\"total\":" + total +
            ",\"totalPages\":" + totalPages +
            ",\"data\":" + sb + "}"
        );
    }

    // ============ EXPORT CSV ============
    /**
     * GET /api/export/csv?from=2026-09-01&to=2026-09-30
     * Response: text/csv + Content-Disposition: attachment
     */
    public HttpResponseBuilder exportCsv(HttpRequestParser req) {
        User u = auth(req);
        if (u == null)
            return new HttpResponseBuilder().status(401)
                .json("{\"error\":\"Chua dang nhap\"}");

        String from = req.getParams().get("from");
        String to   = req.getParams().get("to");

        List<ChamCong> list;
        if ("ADMIN".equals(u.getRole())) {
            list = ChamCongDAO.loadAll();
        } else {
            list = ChamCongDAO.findByUser(u.getUsername());
        }

        // Build CSV
        StringBuilder csv = new StringBuilder();
        csv.append("Username,Ho ten,Ngay,Gio vao,Gio ra\n");
        for (ChamCong c : list) {
            if (from != null && !from.isEmpty() && c.getDate().compareTo(from) < 0) continue;
            if (to != null && !to.isEmpty() && c.getDate().compareTo(to) > 0) continue;

            csv.append(escapeCsv(c.getUsername())).append(",")
               .append(escapeCsv(c.getFullName())).append(",")
               .append(c.getDate()).append(",")
               .append(c.getCheckIn() == null ? "" : c.getCheckIn()).append(",")
               .append(c.getCheckOut() == null ? "" : c.getCheckOut())
               .append("\n");
        }

        String filename = "chamcong_" + LocalDate.now() + ".csv";

        // HTTP/1.1: Content-Type + Content-Disposition
        return new HttpResponseBuilder()
            .status(200)
            .header("Content-Type", "text/csv; charset=UTF-8")
            .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
            .body(csv.toString());
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
