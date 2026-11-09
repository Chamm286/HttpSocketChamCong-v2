package com.httpchamcong.server.controller;

import com.httpchamcong.dao.LeaveDAO;
import com.httpchamcong.model.LeaveRequest;
import com.httpchamcong.model.User;
import com.httpchamcong.server.HttpRequestParser;
import com.httpchamcong.server.HttpResponseBuilder;
import com.httpchamcong.util.JsonUtil;
import com.httpchamcong.util.SessionManager;
import com.httpchamcong.util.ValidationUtil;

import java.time.LocalDate;
import java.util.List;

public class LeaveController {

    /** POST /api/leaves */
    public HttpResponseBuilder request(HttpRequestParser req) {
        String token = req.getBearerToken();
        User u = SessionManager.getUser(token);
        if (u == null)
            return new HttpResponseBuilder().status(401)
                .json("{\"error\":\"Chua dang nhap\"}");

        String startDate = JsonUtil.getString(req.getBody(), "startDate");
        String endDate = JsonUtil.getString(req.getBody(), "endDate");
        String reason = JsonUtil.getString(req.getBody(), "reason");

        if (startDate == null || endDate == null)
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"Thieu ngay bat dau/ket thuc\"}");

        if (!ValidationUtil.isIsoDate(startDate) || !ValidationUtil.isIsoDate(endDate))
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"Ngay phai theo dinh dang yyyy-MM-dd\"}");

        if (LocalDate.parse(startDate).isAfter(LocalDate.parse(endDate)))
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"Ngay ket thuc phai sau hoac bang ngay bat dau\"}");

        LeaveRequest lr = new LeaveRequest(u.getUsername(), startDate, endDate, reason);
        boolean ok = LeaveDAO.insert(lr);

        if (!ok)
            return new HttpResponseBuilder().status(500)
                .json("{\"error\":\"Khong the gui don\"}");

        return new HttpResponseBuilder().status(201)
            .json("{\"msg\":\"Da gui don xin nghi\"}");
    }

    /** GET /api/leave-list — đơn của mình */
    public HttpResponseBuilder list(HttpRequestParser req) {
        String token = req.getBearerToken();
        User u = SessionManager.getUser(token);
        if (u == null)
            return new HttpResponseBuilder().status(401)
                .json("{\"error\":\"Chua dang nhap\"}");

        List<LeaveRequest> list = LeaveDAO.findByUser(u.getUsername());
        return new HttpResponseBuilder().status(200)
            .json("{\"data\":" + toJsonArray(list) + "}");
    }

    /** GET /api/admin/leaves — tất cả */
    public HttpResponseBuilder all(HttpRequestParser req) {
        String token = req.getBearerToken();
        User u = SessionManager.getUser(token);
        if (u == null || !"ADMIN".equals(u.getRole()))
            return new HttpResponseBuilder().status(403)
                .json("{\"error\":\"Khong co quyen\"}");

        List<LeaveRequest> list = LeaveDAO.loadAll();
        return new HttpResponseBuilder().status(200)
            .json("{\"data\":" + toJsonArray(list) + "}");
    }

    /** POST /api/admin/leave/update */
    public HttpResponseBuilder updateStatus(HttpRequestParser req) {
        String token = req.getBearerToken();
        User u = SessionManager.getUser(token);
        if (u == null || !"ADMIN".equals(u.getRole()))
            return new HttpResponseBuilder().status(403)
                .json("{\"error\":\"Khong co quyen\"}");

        String idStr = extractInt(req.getBody(), "id");
        String status = JsonUtil.getString(req.getBody(), "status");

        if (idStr == null || status == null)
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"Thieu id hoac status\"}");

        if (!ValidationUtil.isValidLeaveStatus(status))
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"status phai la APPROVED hoac REJECTED\"}");

        int id;
        try { id = Integer.parseInt(idStr.trim()); }
        catch (Exception e) {
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"id khong hop le\"}");
        }

        boolean ok = LeaveDAO.updateStatus(id, status);
        return ok
            ? new HttpResponseBuilder().status(200).json("{\"msg\":\"Da cap nhat\"}")
            : new HttpResponseBuilder().status(500).json("{\"error\":\"Khong the cap nhat\"}");
    }

    /** ⭐ GET /api/leaves/{id} */
    public HttpResponseBuilder getById(HttpRequestParser req, String idStr) {
        String token = req.getBearerToken();
        User u = SessionManager.getUser(token);
        if (u == null)
            return new HttpResponseBuilder().status(401)
                .json("{\"error\":\"Chua dang nhap\"}");

        int id;
        try { id = Integer.parseInt(idStr); }
        catch (Exception e) {
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"id khong hop le\"}");
        }

        LeaveRequest lr = LeaveDAO.findById(id);
        if (lr == null)
            return new HttpResponseBuilder().status(404)
                .json("{\"error\":\"Khong tim thay don id=" + id + "\"}");

        if (!"ADMIN".equals(u.getRole()) && !lr.getUsername().equals(u.getUsername()))
            return new HttpResponseBuilder().status(403)
                .json("{\"error\":\"Khong co quyen xem don nay\"}");

        return new HttpResponseBuilder().status(200).json(lr.toJson());
    }

    /** ⭐ DELETE /api/leaves/{id} */
    public HttpResponseBuilder delete(HttpRequestParser req, String idStr) {
        String token = req.getBearerToken();
        User u = SessionManager.getUser(token);
        if (u == null)
            return new HttpResponseBuilder().status(401)
                .json("{\"error\":\"Chua dang nhap\"}");

        int id;
        try { id = Integer.parseInt(idStr); }
        catch (Exception e) {
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"id khong hop le\"}");
        }

        LeaveRequest lr = LeaveDAO.findById(id);
        if (lr == null)
            return new HttpResponseBuilder().status(404)
                .json("{\"error\":\"Khong tim thay don id=" + id + "\"}");

        if (!lr.getUsername().equals(u.getUsername()))
            return new HttpResponseBuilder().status(403)
                .json("{\"error\":\"Chi duoc huy don cua minh\"}");

        if (!"PENDING".equals(lr.getStatus()))
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"Chi duoc huy don dang cho duyet\"}");

        boolean ok = LeaveDAO.deleteById(id);
        if (!ok)
            return new HttpResponseBuilder().status(500)
                .json("{\"error\":\"Khong the huy don\"}");

        return new HttpResponseBuilder().status(204).body("");
    }

    private String toJsonArray(List<LeaveRequest> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i).toJson());
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String extractInt(String json, String key) {
        if (json == null) return null;
        String pattern = "\"" + key + "\"";
        int i = json.indexOf(pattern);
        if (i < 0) return null;
        int colon = json.indexOf(':', i + pattern.length());
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\t')) start++;
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        if (end == start) return null;
        return json.substring(start, end);
    }
}