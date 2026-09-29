package com.httpchamcong.server.controller;

import com.httpchamcong.dao.UserDAO;
import com.httpchamcong.model.User;
import com.httpchamcong.server.HttpRequestParser;
import com.httpchamcong.server.HttpResponseBuilder;
import com.httpchamcong.util.JsonUtil;
import com.httpchamcong.util.SessionManager;

public class UserController {

    /** POST /api/register — Đăng ký */
    public HttpResponseBuilder register(HttpRequestParser req) {
        String username = JsonUtil.getString(req.getBody(), "username");
        String password = JsonUtil.getString(req.getBody(), "password");
        String fullName = JsonUtil.getString(req.getBody(), "fullName");

        if (username == null || password == null || fullName == null)
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"Thieu thong tin\"}");

        if (UserDAO.findByUsername(username) != null)
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"Username da ton tai\"}");

        User u = new User(username, password, fullName, "USER");
        boolean ok = UserDAO.insert(u);

        if (!ok)
            return new HttpResponseBuilder().status(500)
                .json("{\"error\":\"Khong the tao tai khoan\"}");

        return new HttpResponseBuilder().status(201)
            .json("{\"msg\":\"Dang ky thanh cong\"}");
    }

    /** POST /api/change-password */
    public HttpResponseBuilder changePassword(HttpRequestParser req) {
        String token = req.getBearerToken();
        User u = SessionManager.getUser(token);
        if (u == null)
            return new HttpResponseBuilder().status(401)
                .json("{\"error\":\"Chua dang nhap\"}");

        String oldPass = JsonUtil.getString(req.getBody(), "oldPassword");
        String newPass = JsonUtil.getString(req.getBody(), "newPassword");

        if (oldPass == null || newPass == null)
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"Thieu thong tin\"}");

        // Verify password cũ
        User check = UserDAO.findUser(u.getUsername(), oldPass);
        if (check == null)
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"Mat khau cu sai\"}");

        boolean ok = UserDAO.updatePassword(u.getUsername(), newPass);
        if (!ok)
            return new HttpResponseBuilder().status(500)
                .json("{\"error\":\"Khong the doi mat khau\"}");

        return new HttpResponseBuilder().status(200)
            .json("{\"msg\":\"Doi mat khau thanh cong\"}");
    }

    /** POST /api/profile — Lấy thông tin user */
    public HttpResponseBuilder profile(HttpRequestParser req) {
        String token = req.getBearerToken();
        User u = SessionManager.getUser(token);
        if (u == null)
            return new HttpResponseBuilder().status(401)
                .json("{\"error\":\"Chua dang nhap\"}");
        return new HttpResponseBuilder().status(200).json(u.toJson());
    }
} 
