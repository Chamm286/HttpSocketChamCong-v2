package com.httpchamcong.server.controller;

import com.httpchamcong.dao.ChamCongDAO;
import com.httpchamcong.dao.LeaveDAO;
import com.httpchamcong.dao.UserDAO;
import com.httpchamcong.model.ChamCong;
import com.httpchamcong.model.LeaveRequest;
import com.httpchamcong.model.User;
import com.httpchamcong.server.ClientManager;
import com.httpchamcong.server.HttpRequestParser;
import com.httpchamcong.server.HttpResponseBuilder;
import com.httpchamcong.util.SessionManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * API thống kê tổng quan cho Admin Dashboard.
 * GET /api/dashboard/stats
 */
public class DashboardController {

    private static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss");

    public HttpResponseBuilder stats(HttpRequestParser req) {
        User current = SessionManager.getUser(req.getBearerToken());
        if (current == null) {
            return new HttpResponseBuilder().status(401)
                .json("{\"error\":\"Chua dang nhap\"}");
        }
        if (!"ADMIN".equals(current.getRole())) {
            return new HttpResponseBuilder().status(403)
                .json("{\"error\":\"Khong co quyen truy cap (chi ADMIN)\"}");
        }

        String today = LocalDate.now().toString();
        List<User> users = UserDAO.loadAll();
        List<ChamCong> all = ChamCongDAO.loadAll();
        List<LeaveRequest> leaves = LeaveDAO.loadAll();

        Set<String> employeeNames = new HashSet<>();
        for (User u : users) {
            if ("USER".equals(u.getRole())) employeeNames.add(u.getUsername());
        }

        int present = 0;
        int working = 0;
        int completed = 0;
        int late = 0;
        Set<String> presentUsers = new HashSet<>();

        for (ChamCong c : all) {
            if (!today.equals(c.getDate())) continue;
            if (c.getCheckIn() == null || c.getCheckIn().isEmpty()) continue;
            if (!employeeNames.contains(c.getUsername())) continue;

            presentUsers.add(c.getUsername());
            if (c.getCheckOut() == null || c.getCheckOut().isEmpty()) working++;
            else completed++;

            if (c.getCheckIn().compareTo("08:00:00") > 0) late++;
        }
        present = presentUsers.size();

        int pendingLeaves = 0;
        for (LeaveRequest lr : leaves) {
            if ("PENDING".equals(lr.getStatus())) pendingLeaves++;
        }

        int totalEmployees = employeeNames.size();
        int absent = Math.max(0, totalEmployees - present);
        int online = ClientManager.getOnlineCount();

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"date\":\"").append(today).append("\",")
          .append("\"serverTime\":\"").append(LocalTime.now().format(TIME_FMT)).append("\",")
          .append("\"totalEmployees\":").append(totalEmployees).append(",")
          .append("\"totalUsers\":").append(users.size()).append(",")
          .append("\"present\":").append(present).append(",")
          .append("\"working\":").append(working).append(",")
          .append("\"completed\":").append(completed).append(",")
          .append("\"absent\":").append(absent).append(",")
          .append("\"late\":").append(late).append(",")
          .append("\"pendingLeaves\":").append(pendingLeaves).append(",")
          .append("\"online\":").append(online).append(",")
          .append("\"offline\":").append(Math.max(0, users.size() - online)).append(",")
          .append("\"attendanceRate\":")
          .append(totalEmployees == 0 ? 0 : Math.round(present * 100.0 / totalEmployees))
          .append("}");
        return new HttpResponseBuilder().status(200).json(sb.toString());
    }
}
