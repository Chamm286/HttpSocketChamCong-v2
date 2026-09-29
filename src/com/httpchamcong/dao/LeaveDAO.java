package com.httpchamcong.dao;

import com.httpchamcong.model.LeaveRequest;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LeaveDAO {

    private static LeaveRequest mapRow(ResultSet rs) throws SQLException {
        String fn = "";
        try { fn = rs.getString("full_name"); } catch (Exception e) {}
        if (fn == null) fn = "";
        return new LeaveRequest(
            rs.getInt("id"),
            rs.getString("username"),
            fn,
            rs.getDate("start_date").toString(),
            rs.getDate("end_date").toString(),
            rs.getString("reason"),
            rs.getString("status")
        );
    }

    /** Thêm đơn xin nghỉ mới */
    public static boolean insert(LeaveRequest lr) {
        String sql = "INSERT INTO leave_requests (username, start_date, end_date, reason) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, lr.getUsername());
            ps.setDate(2, java.sql.Date.valueOf(lr.getStartDate()));
            ps.setDate(3, java.sql.Date.valueOf(lr.getEndDate()));
            ps.setString(4, lr.getReason());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    /** Lấy đơn của 1 user */
    public static List<LeaveRequest> findByUser(String username) {
        List<LeaveRequest> list = new ArrayList<>();
        String sql = "SELECT l.*, u.full_name FROM leave_requests l " +
                     "LEFT JOIN users u ON l.username = u.username " +
                     "WHERE l.username = ? ORDER BY l.created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /** Lấy tất cả đơn */
    public static List<LeaveRequest> loadAll() {
        List<LeaveRequest> list = new ArrayList<>();
        String sql = "SELECT l.*, u.full_name FROM leave_requests l " +
                     "LEFT JOIN users u ON l.username = u.username " +
                     "ORDER BY l.created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /** Cập nhật trạng thái đơn (APPROVED / REJECTED) */
    public static boolean updateStatus(int id, String status) {
        String sql = "UPDATE leave_requests SET status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    /** Tìm đơn theo id */
    public static LeaveRequest findById(int id) {
        String sql = "SELECT l.*, u.full_name FROM leave_requests l " +
                     "LEFT JOIN users u ON l.username = u.username " +
                     "WHERE l.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    /** Xóa đơn theo id */
    public static boolean deleteById(int id) {
        String sql = "DELETE FROM leave_requests WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

}
