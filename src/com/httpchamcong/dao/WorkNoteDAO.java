package com.httpchamcong.dao;

import com.httpchamcong.model.WorkNote;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WorkNoteDAO {

    private static WorkNote mapRow(ResultSet rs, boolean withFullName) throws SQLException {
        WorkNote n = new WorkNote();
        n.setId(rs.getInt("id"));
        n.setUsername(rs.getString("username"));
        n.setNoteDate(rs.getDate("note_date").toString());
        n.setContent(rs.getString("content"));
        n.setPlan(rs.getString("plan"));
        Timestamp ts = rs.getTimestamp("created_at");
        n.setCreatedAt(ts != null ? ts.toString() : "");
        if (withFullName) {
            try {
                String fn = rs.getString("full_name");
                n.setFullName(fn != null ? fn : n.getUsername());
            } catch (Exception e) { n.setFullName(n.getUsername()); }
        }
        return n;
    }

    public static boolean saveOrUpdate(WorkNote note) {
        String sql = "INSERT INTO work_notes (username, note_date, content, plan) VALUES (?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE content = VALUES(content), plan = VALUES(plan)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, note.getUsername());
            ps.setDate(2, java.sql.Date.valueOf(note.getNoteDate()));
            ps.setString(3, note.getContent());
            ps.setString(4, note.getPlan() != null ? note.getPlan() : "");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[WorkNoteDAO] Loi save: " + e.getMessage());
            return false;
        }
    }

    public static WorkNote findByUserAndDate(String username, String date) {
        String sql = "SELECT * FROM work_notes WHERE username = ? AND note_date = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setDate(2, java.sql.Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs, false);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public static List<WorkNote> findByUser(String username) {
        List<WorkNote> list = new ArrayList<>();
        String sql = "SELECT * FROM work_notes WHERE username = ? ORDER BY note_date DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs, false));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static List<WorkNote> loadAll() {
        List<WorkNote> list = new ArrayList<>();
        String sql = "SELECT wn.*, u.full_name FROM work_notes wn " +
                     "LEFT JOIN users u ON u.username = wn.username " +
                     "ORDER BY wn.note_date DESC, wn.username";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs, true));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static WorkNote findById(int id) {
        String sql = "SELECT wn.*, u.full_name FROM work_notes wn " +
                     "LEFT JOIN users u ON u.username = wn.username " +
                     "WHERE wn.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs, true);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public static boolean deleteById(int id) {
        String sql = "DELETE FROM work_notes WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }
}
