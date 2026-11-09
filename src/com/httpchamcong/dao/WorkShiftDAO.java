package com.httpchamcong.dao;

import com.httpchamcong.model.WorkShift;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WorkShiftDAO {

    private static WorkShift mapRow(ResultSet rs) throws SQLException {
        String fn = "";
        try { fn = rs.getString("full_name"); } catch (Exception e) {}
        if (fn == null) fn = "";
        return new WorkShift(
            rs.getInt("id"),
            rs.getString("username"),
            fn,
            rs.getDate("shift_date").toString(),
            rs.getString("shift_type"),
            rs.getString("note")
        );
    }

    public static boolean insert(WorkShift ws) {
        String sql = "INSERT INTO work_shifts (username, shift_date, shift_type, note) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ws.getUsername());
            ps.setDate(2, java.sql.Date.valueOf(ws.getShiftDate()));
            ps.setString(3, ws.getShiftType());
            ps.setString(4, ws.getNote());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[WorkShiftDAO] Lỗi insert: " + e.getMessage());
            return false;
        }
    }

    public static boolean exists(String username, String date, String shiftType) {
        String sql = "SELECT COUNT(*) FROM work_shifts WHERE username = ? AND shift_date = ? AND shift_type = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setDate(2, java.sql.Date.valueOf(date));
            ps.setString(3, shiftType);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static List<WorkShift> findByUser(String username) {
        List<WorkShift> list = new ArrayList<>();
        String sql = "SELECT w.*, u.full_name FROM work_shifts w " +
                     "LEFT JOIN users u ON w.username = u.username " +
                     "WHERE w.username = ? ORDER BY w.shift_date DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static List<WorkShift> loadAll() {
        List<WorkShift> list = new ArrayList<>();
        String sql = "SELECT w.*, u.full_name FROM work_shifts w " +
                     "LEFT JOIN users u ON w.username = u.username " +
                     "ORDER BY w.shift_date DESC, w.username";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static WorkShift findById(int id) {
        String sql = "SELECT w.*, u.full_name FROM work_shifts w " +
                     "LEFT JOIN users u ON w.username = u.username " +
                     "WHERE w.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public static boolean deleteById(int id) {
        String sql = "DELETE FROM work_shifts WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }
}