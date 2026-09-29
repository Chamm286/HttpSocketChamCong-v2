package com.httpchamcong.server.controller;

import com.httpchamcong.dao.ChamCongDAO;
import com.httpchamcong.model.ChamCong;
import com.httpchamcong.model.User;
import com.httpchamcong.server.HttpRequestParser;
import com.httpchamcong.server.HttpResponseBuilder;
import com.httpchamcong.util.SessionManager;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ChamCongController {

    private User auth(HttpRequestParser req) {
        String token = req.getBearerToken();
        return SessionManager.getUser(token);
    }

    // ============ CHECK-IN ============
    public HttpResponseBuilder checkIn(HttpRequestParser req) {
        User u = auth(req);
        if (u == null)
            return new HttpResponseBuilder().status(401)
                .json("{\"error\":\"Chua dang nhap\"}");

        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        boolean ok = ChamCongDAO.checkIn(u.getUsername(), time);

        if (!ok)
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"Hom nay da check-in roi\"}");

        return new HttpResponseBuilder().status(201)  // ⭐ 201 Created
            .json("{\"msg\":\"Check-in thanh cong\",\"time\":\"" + time + "\"}");
    }

    // ============ CHECK-OUT ============
    public HttpResponseBuilder checkOut(HttpRequestParser req) {
        User u = auth(req);
        if (u == null)
            return new HttpResponseBuilder().status(401)
                .json("{\"error\":\"Chua dang nhap\"}");

        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        boolean ok = ChamCongDAO.checkOut(u.getUsername(), time);

        if (!ok)
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"Chua check-in hoac da check-out\"}");

        return new HttpResponseBuilder().status(200)
            .json("{\"msg\":\"Check-out thanh cong\",\"time\":\"" + time + "\"}");
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

        // ⭐ PARSE QUERY STRING
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

        String filename = "chamcong_" + java.time.LocalDate.now() + ".csv";

        // ⭐ HTTP/1.1: Content-Type + Content-Disposition
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