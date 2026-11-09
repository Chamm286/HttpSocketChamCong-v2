package com.httpchamcong.dao;

import com.httpchamcong.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    /** Tìm user theo username + password (đăng nhập) */
    public static User findUser(String username, String password) {
        User u = findByUsername(username);
        if (u == null) {
            System.out.println("[AUTH] User khong ton tai: " + username);
            return null;
        }
        if (!u.getPassword().equals(password)) {
            System.out.println("[AUTH] Sai mat khau cho user: " + username);
            return null;
        }
        return u;
    }

    private static String safeString(ResultSet rs, String column) {
        try {
            String v = rs.getString(column);
            return v == null ? "" : v;
        } catch (SQLException e) {
            return "";
        }
    }

    private static User mapUser(ResultSet rs) throws SQLException {
        return new User(
            rs.getString("username"),
            rs.getString("password"),
            rs.getString("full_name"),
            rs.getString("role"),
            safeString(rs, "department"),
            safeString(rs, "phone"),
            safeString(rs, "avatar_url")
        );
    }

    /** Tìm user theo username */
    public static User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapUser(rs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    /** Lấy tất cả users */
    public static List<User> loadAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY username";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapUser(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /** Thêm user mới */
    public static boolean insert(User u) {
        String sql = "INSERT INTO users " +
                     "(username, password, full_name, role, department, phone, avatar_url) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPassword());
            ps.setString(3, u.getFullName());
            ps.setString(4, u.getRole());
            ps.setString(5, u.getDepartment());
            ps.setString(6, u.getPhone());
            ps.setString(7, u.getAvatarUrl());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    /** Cập nhật mật khẩu */
    public static boolean updatePassword(String username, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    /** Cập nhật đầy đủ thông tin user */
    public static boolean update(User u) {
        String sql = "UPDATE users SET full_name = ?, role = ?, " +
                     "department = ?, phone = ?, avatar_url = ? WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getFullName());
            ps.setString(2, u.getRole());
            ps.setString(3, u.getDepartment());
            ps.setString(4, u.getPhone());
            ps.setString(5, u.getAvatarUrl());
            ps.setString(6, u.getUsername());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    /** Xóa user */
    public static boolean delete(String username) {
        String sql = "DELETE FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }
}
