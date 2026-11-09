package com.httpchamcong.server.controller;

import com.httpchamcong.dao.ChamCongDAO;
import com.httpchamcong.dao.UserDAO;
import com.httpchamcong.server.ClientManager;
import com.httpchamcong.server.HttpRequestParser;
import com.httpchamcong.server.HttpResponseBuilder;
import com.httpchamcong.model.ChamCong;
import com.httpchamcong.model.User;
import com.httpchamcong.util.JsonUtil;
import com.httpchamcong.util.SessionManager;
import com.httpchamcong.util.ValidationUtil;

import java.util.List;
import java.util.Map;

public class AdminController {

    private User requireAdmin(HttpRequestParser req) {
        User u = SessionManager.getUser(req.getBearerToken());
        return u != null && "ADMIN".equals(u.getRole()) ? u : null;
    }

    private HttpResponseBuilder forbidden() {
        return new HttpResponseBuilder().status(403)
            .json("{\"error\":\"Khong co quyen truy cap\"}");
    }

    public HttpResponseBuilder getAll(HttpRequestParser req) {
        if (requireAdmin(req) == null) return forbidden();

        List<ChamCong> list = ChamCongDAO.loadAll();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i).toJson());
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");

        return new HttpResponseBuilder().status(200).json(
            "{\"total\":" + list.size() + ",\"data\":" + sb + "}"
        );
    }

    /** GET /api/admin/users — Danh sách users */
    public HttpResponseBuilder listUsers(HttpRequestParser req) {
        if (requireAdmin(req) == null) return forbidden();

        List<User> list = UserDAO.loadAll();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i).toJson());
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return new HttpResponseBuilder().status(200)
            .json("{\"data\":" + sb + "}");
    }

    /** POST /api/admin/user/add */
    public HttpResponseBuilder addUser(HttpRequestParser req) {
        if (requireAdmin(req) == null) return forbidden();

        String username = JsonUtil.getString(req.getBody(), "username");
        String password = JsonUtil.getString(req.getBody(), "password");
        String fullName = JsonUtil.getString(req.getBody(), "fullName");
        String role = JsonUtil.getString(req.getBody(), "role");
        String department = JsonUtil.getString(req.getBody(), "department");
        String phone = JsonUtil.getString(req.getBody(), "phone");
        String avatarUrl = JsonUtil.getString(req.getBody(), "avatarUrl");
        if (role == null) role = "USER";

        if (username == null || password == null || fullName == null)
            return new HttpResponseBuilder().status(400).json("{\"error\":\"Thieu thong tin\"}");

        if (!ValidationUtil.isValidRole(role))
            return new HttpResponseBuilder().status(400).json("{\"error\":\"role phai la ADMIN hoac USER\"}");

        if (UserDAO.findByUsername(username) != null)
            return new HttpResponseBuilder().status(400).json("{\"error\":\"Username da ton tai\"}");

        User nu = new User(username, password, fullName, role, department, phone, avatarUrl);
        return UserDAO.insert(nu)
            ? new HttpResponseBuilder().status(201).json("{\"msg\":\"Da them nhan vien\"}")
            : new HttpResponseBuilder().status(500).json("{\"error\":\"Khong the them\"}");
    }

    /** POST /api/admin/user/update */
    public HttpResponseBuilder updateUser(HttpRequestParser req) {
        if (requireAdmin(req) == null) return forbidden();

        String username = JsonUtil.getString(req.getBody(), "username");
        String fullName = JsonUtil.getString(req.getBody(), "fullName");
        String role = JsonUtil.getString(req.getBody(), "role");
        String department = JsonUtil.getString(req.getBody(), "department");
        String phone = JsonUtil.getString(req.getBody(), "phone");
        String avatarUrl = JsonUtil.getString(req.getBody(), "avatarUrl");

        if (username == null || fullName == null)
            return new HttpResponseBuilder().status(400).json("{\"error\":\"Thieu thong tin\"}");

        if (role != null && !ValidationUtil.isValidRole(role))
            return new HttpResponseBuilder().status(400).json("{\"error\":\"role phai la ADMIN hoac USER\"}");

        User existing = UserDAO.findByUsername(username);
        if (existing == null)
            return new HttpResponseBuilder().status(404).json("{\"error\":\"Khong tim thay user\"}");

        User upd = new User(
            username, "",
            fullName,
            role == null ? existing.getRole() : role,
            department == null ? existing.getDepartment() : department,
            phone == null ? existing.getPhone() : phone,
            avatarUrl == null ? existing.getAvatarUrl() : avatarUrl
        );
        return UserDAO.update(upd)
            ? new HttpResponseBuilder().status(200).json("{\"msg\":\"Da cap nhat\"}")
            : new HttpResponseBuilder().status(500).json("{\"error\":\"Khong the cap nhat\"}");
    }

    /** POST /api/admin/user/delete */
    public HttpResponseBuilder deleteUser(HttpRequestParser req) {
        User admin = requireAdmin(req);
        if (admin == null) return forbidden();

        String username = JsonUtil.getString(req.getBody(), "username");
        if (username == null)
            return new HttpResponseBuilder().status(400).json("{\"error\":\"Thieu username\"}");

        if (username.equals(admin.getUsername()))
            return new HttpResponseBuilder().status(400).json("{\"error\":\"Khong the tu xoa\"}");

        return UserDAO.delete(username)
            ? new HttpResponseBuilder().status(200).json("{\"msg\":\"Da xoa\"}")
            : new HttpResponseBuilder().status(500).json("{\"error\":\"Khong the xoa\"}");
    }

    /** PATCH /api/admin/users/{username} */
    public HttpResponseBuilder patchUser(HttpRequestParser req, String username) {
        if (requireAdmin(req) == null) return forbidden();

        String fullName = JsonUtil.getString(req.getBody(), "fullName");
        String role = JsonUtil.getString(req.getBody(), "role");
        String department = JsonUtil.getString(req.getBody(), "department");
        String phone = JsonUtil.getString(req.getBody(), "phone");
        String avatarUrl = JsonUtil.getString(req.getBody(), "avatarUrl");

        if (fullName == null && role == null && department == null
                && phone == null && avatarUrl == null)
            return new HttpResponseBuilder().status(400).json("{\"error\":\"Khong co du lieu cap nhat\"}");

        if (role != null && !ValidationUtil.isValidRole(role))
            return new HttpResponseBuilder().status(400).json("{\"error\":\"role phai la ADMIN hoac USER\"}");

        User existing = UserDAO.findByUsername(username);
        if (existing == null)
            return new HttpResponseBuilder().status(404)
                .json("{\"error\":\"Khong tim thay user: " + JsonUtil.escape(username) + "\"}");

        User upd = new User(
            username, "",
            fullName == null ? existing.getFullName() : fullName,
            role == null ? existing.getRole() : role,
            department == null ? existing.getDepartment() : department,
            phone == null ? existing.getPhone() : phone,
            avatarUrl == null ? existing.getAvatarUrl() : avatarUrl
        );
        boolean ok = UserDAO.update(upd);
        return ok
            ? new HttpResponseBuilder().status(200).json("{\"msg\":\"Da cap nhat\"}")
            : new HttpResponseBuilder().status(500).json("{\"error\":\"Khong the cap nhat\"}");
    }

    /** DELETE /api/admin/users/{username} */
    public HttpResponseBuilder deleteUserByPath(HttpRequestParser req, String username) {
        User admin = requireAdmin(req);
        if (admin == null) return forbidden();

        if (username.equals(admin.getUsername()))
            return new HttpResponseBuilder().status(400).json("{\"error\":\"Khong the tu xoa\"}");

        boolean ok = UserDAO.delete(username);
        return ok
            ? new HttpResponseBuilder().status(204).body("")
            : new HttpResponseBuilder().status(404).json("{\"error\":\"Khong the xoa\"}");
    }

    /**
     * GET /api/admin/clients — giám sát Online/Offline.
     * Trả về toàn bộ nhân viên, kèm trạng thái hoạt động gần nhất.
     */
    public HttpResponseBuilder monitor(HttpRequestParser req) {
        if (requireAdmin(req) == null) return forbidden();

        List<User> users = UserDAO.loadAll();
        Map<String, ClientManager.ClientInfo> clients = ClientManager.getAll();

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            ClientManager.ClientInfo c = clients.get(u.getUsername());
            String status = c == null ? "OFFLINE" : c.getStatus();
            String ip = c == null ? "" : c.getIp();
            int port = c == null ? 0 : c.getPort();
            int requests = c == null ? 0 : c.getRequestCount();
            String loginTime = c == null ? "" : c.getLoginTimeStr();
            String lastActivity = c == null ? "" : c.getLastActivityStr();

            sb.append("{\"username\":\"").append(JsonUtil.escape(u.getUsername())).append("\",")
              .append("\"fullName\":\"").append(JsonUtil.escape(u.getFullName())).append("\",")
              .append("\"role\":\"").append(JsonUtil.escape(u.getRole())).append("\",")
              .append("\"department\":\"").append(JsonUtil.escape(u.getDepartment())).append("\",")
              .append("\"status\":\"").append(JsonUtil.escape(status)).append("\",")
              .append("\"ip\":\"").append(JsonUtil.escape(ip)).append("\",")
              .append("\"port\":").append(port).append(",")
              .append("\"requestCount\":").append(requests).append(",")
              .append("\"loginTime\":\"").append(JsonUtil.escape(loginTime)).append("\",")
              .append("\"lastActivity\":\"").append(JsonUtil.escape(lastActivity)).append("\"}");

            if (i < users.size() - 1) sb.append(",");
        }
        sb.append("]");

        return new HttpResponseBuilder().status(200)
            .json("{\"total\":" + users.size() + ",\"data\":" + sb + "}");
    }
}
