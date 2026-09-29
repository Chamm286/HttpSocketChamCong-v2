package com.httpchamcong.server.controller;

import com.httpchamcong.dao.ChamCongDAO;
import com.httpchamcong.dao.UserDAO;
import com.httpchamcong.util.JsonUtil;
import com.httpchamcong.model.ChamCong;
import com.httpchamcong.model.User;
import com.httpchamcong.server.HttpRequestParser;
import com.httpchamcong.server.HttpResponseBuilder;
import com.httpchamcong.util.SessionManager;

import java.util.List;

public class AdminController {

    public HttpResponseBuilder getAll(HttpRequestParser req) {
        String token = req.getBearerToken();
        User u = SessionManager.getUser(token);

        if (u == null) {
            return new HttpResponseBuilder().status(401)
                .json("{\"error\":\"Chua dang nhap\"}");
        }

        if (!"ADMIN".equals(u.getRole())) {
            return new HttpResponseBuilder().status(403)
                .json("{\"error\":\"Khong co quyen truy cap\"}");
        }

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
        String token = req.getBearerToken();
        User u = SessionManager.getUser(token);
        if (u == null || !"ADMIN".equals(u.getRole()))
            return new HttpResponseBuilder().status(403)
                .json("{\"error\":\"Khong co quyen\"}");

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
        String token = req.getBearerToken();
        User u = SessionManager.getUser(token);
        if (u == null || !"ADMIN".equals(u.getRole()))
            return new HttpResponseBuilder().status(403).json("{\"error\":\"Khong co quyen\"}");

        String username = JsonUtil.getString(req.getBody(), "username");
        String password = JsonUtil.getString(req.getBody(), "password");
        String fullName = JsonUtil.getString(req.getBody(), "fullName");
        String role = JsonUtil.getString(req.getBody(), "role");
        if (role == null) role = "USER";

        if (username == null || password == null || fullName == null)
            return new HttpResponseBuilder().status(400).json("{\"error\":\"Thieu thong tin\"}");

        if (UserDAO.findByUsername(username) != null)
            return new HttpResponseBuilder().status(400).json("{\"error\":\"Username da ton tai\"}");

        User nu = new User(username, password, fullName, role);
        return UserDAO.insert(nu)
            ? new HttpResponseBuilder().status(201).json("{\"msg\":\"Da them nhan vien\"}")
            : new HttpResponseBuilder().status(500).json("{\"error\":\"Khong the them\"}");
    }

    /** POST /api/admin/user/update */
    public HttpResponseBuilder updateUser(HttpRequestParser req) {
        String token = req.getBearerToken();
        User u = SessionManager.getUser(token);
        if (u == null || !"ADMIN".equals(u.getRole()))
            return new HttpResponseBuilder().status(403).json("{\"error\":\"Khong co quyen\"}");

        String username = JsonUtil.getString(req.getBody(), "username");
        String fullName = JsonUtil.getString(req.getBody(), "fullName");
        String role = JsonUtil.getString(req.getBody(), "role");

        if (username == null || fullName == null)
            return new HttpResponseBuilder().status(400).json("{\"error\":\"Thieu thong tin\"}");

        User upd = new User(username, "", fullName, role == null ? "USER" : role);
        return UserDAO.update(upd)
            ? new HttpResponseBuilder().status(200).json("{\"msg\":\"Da cap nhat\"}")
            : new HttpResponseBuilder().status(500).json("{\"error\":\"Khong the cap nhat\"}");
    }

    /** POST /api/admin/user/delete */
    public HttpResponseBuilder deleteUser(HttpRequestParser req) {
        String token = req.getBearerToken();
        User u = SessionManager.getUser(token);
        if (u == null || !"ADMIN".equals(u.getRole()))
            return new HttpResponseBuilder().status(403).json("{\"error\":\"Khong co quyen\"}");

        String username = JsonUtil.getString(req.getBody(), "username");
        if (username == null)
            return new HttpResponseBuilder().status(400).json("{\"error\":\"Thieu username\"}");

        if (username.equals(u.getUsername()))
            return new HttpResponseBuilder().status(400).json("{\"error\":\"Khong the tu xoa\"}");

        return UserDAO.delete(username)
            ? new HttpResponseBuilder().status(200).json("{\"msg\":\"Da xoa\"}")
            : new HttpResponseBuilder().status(500).json("{\"error\":\"Khong the xoa\"}");
    }

        /** ⭐ PATCH /api/admin/users/{username} */
    public HttpResponseBuilder patchUser(HttpRequestParser req, String username) {
        String token = req.getBearerToken();
        User u = SessionManager.getUser(token);
        if (u == null || !"ADMIN".equals(u.getRole()))
            return new HttpResponseBuilder().status(403).json("{\"error\":\"Khong co quyen\"}");

        String fullName = JsonUtil.getString(req.getBody(), "fullName");
        String role = JsonUtil.getString(req.getBody(), "role");

        if (fullName == null && role == null)
            return new HttpResponseBuilder().status(400).json("{\"error\":\"Khong co du lieu cap nhat\"}");

        User existing = UserDAO.findByUsername(username);
        if (existing == null)
            return new HttpResponseBuilder().status(404)
                .json("{\"error\":\"Khong tim thay user: " + username + "\"}");

        String newFullName = fullName != null ? fullName : existing.getFullName();
        String newRole = role != null ? role : existing.getRole();

        User upd = new User(username, "", newFullName, newRole);
        boolean ok = UserDAO.update(upd);
        return ok
            ? new HttpResponseBuilder().status(200).json("{\"msg\":\"Da cap nhat\"}")
            : new HttpResponseBuilder().status(500).json("{\"error\":\"Khong the cap nhat\"}");
    }

    /** ⭐ DELETE /api/admin/users/{username} */
    public HttpResponseBuilder deleteUserByPath(HttpRequestParser req, String username) {
        String token = req.getBearerToken();
        User u = SessionManager.getUser(token);
        if (u == null || !"ADMIN".equals(u.getRole()))
            return new HttpResponseBuilder().status(403).json("{\"error\":\"Khong co quyen\"}");

        if (username.equals(u.getUsername()))
            return new HttpResponseBuilder().status(400).json("{\"error\":\"Khong the tu xoa\"}");

        boolean ok = UserDAO.delete(username);
        return ok
            ? new HttpResponseBuilder().status(204).body("")
            : new HttpResponseBuilder().status(404).json("{\"error\":\"Khong the xoa\"}");
    }
}