package com.httpchamcong.dao;

import com.httpchamcong.model.ChamCong;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ChamCongDAO {

    private static String safeString(ResultSet rs, String column) {
        try {
            String v = rs.getString(column);
            return v == null ? "" : v;
        } catch (SQLException e) {
            return "";
        }
    }

    private static Double safeDouble(ResultSet rs, String column) {
        try {
            double v = rs.getDouble(column);
            return rs.wasNull() ? null : v;
        } catch (SQLException e) {
            return null;
        }
    }

    private static boolean safeBoolean(ResultSet rs, String column) {
        try {
            return rs.getBoolean(column);
        } catch (SQLException e) {
            return false;
        }
    }

    private static ChamCong mapRow(ResultSet rs) throws SQLException {
        Time inT  = rs.getTime("check_in");
        Time outT = rs.getTime("check_out");
        return new ChamCong(
            rs.getString("username"),
            safeString(rs, "full_name"),
            rs.getDate("work_date").toString(),
            inT  == null ? null : inT.toString(),
            outT == null ? null : outT.toString(),
            safeDouble(rs, "checkin_lat"),
            safeDouble(rs, "checkin_lng"),
            safeBoolean(rs, "checkin_face_verified"),
            safeString(rs, "note")
        );
    }

    public static List<ChamCong> loadAll() {
        List<ChamCong> list = new ArrayList<>();
        String sql = "SELECT c.*, u.full_name FROM chamcong c " +
                     "LEFT JOIN users u ON c.username = u.username " +
                     "ORDER BY c.work_date DESC, c.username";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static List<ChamCong> findByUser(String username) {
        List<ChamCong> list = new ArrayList<>();
        String sql = "SELECT c.*, u.full_name FROM chamcong c " +
                     "LEFT JOIN users u ON c.username = u.username " +
                     "WHERE c.username = ? ORDER BY c.work_date DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static ChamCong findToday(String username) {
        String sql = "SELECT c.*, u.full_name FROM chamcong c " +
                     "LEFT JOIN users u ON c.username = u.username " +
                     "WHERE c.username = ? AND c.work_date = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setDate(2, java.sql.Date.valueOf(LocalDate.now()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public static boolean checkIn(String username, String time) {
        return checkIn(username, time, null, null, false, "");
    }

    public static boolean checkIn(String username, String time, Double lat, Double lng,
                                  boolean faceVerified, String note) {
        if (findToday(username) != null) return false;
        String sql = "INSERT INTO chamcong " +
                     "(username, work_date, check_in, checkin_lat, checkin_lng, " +
                     "checkin_face_verified, note) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setDate(2, java.sql.Date.valueOf(LocalDate.now()));
            ps.setTime(3, java.sql.Time.valueOf(time));
            if (lat == null) ps.setNull(4, Types.DOUBLE); else ps.setDouble(4, lat);
            if (lng == null) ps.setNull(5, Types.DOUBLE); else ps.setDouble(5, lng);
            ps.setBoolean(6, faceVerified);
            ps.setString(7, note == null ? "" : note);
            return ps.executeUpdate() > 0;
        } catch (SQLIntegrityConstraintViolationException duplicate) {
            // Hai request check-in đồng thời: khóa chính (username, work_date) chặn request sau.
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean checkOut(String username, String time) {
        ChamCong today = findToday(username);
        if (today == null || today.getCheckOut() != null) return false;
        String sql = "UPDATE chamcong SET check_out = ? " +
                     "WHERE username = ? AND work_date = ? AND check_out IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTime(1, java.sql.Time.valueOf(time));
            ps.setString(2, username);
            ps.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static int count() {
        String sql = "SELECT COUNT(*) FROM chamcong";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public static void main(String[] args) {
        System.out.println("=== TEST ChamCongDAO ===");
        System.out.println("\n1. Tong ban ghi: " + count());
        System.out.println("\n2. 5 ban ghi gan nhat:");
        List<ChamCong> list = loadAll();
        for (int i = 0; i < Math.min(5, list.size()); i++) {
            System.out.println("   - " + list.get(i));
        }
    }
}
