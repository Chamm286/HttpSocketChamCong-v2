package com.httpchamcong.dao;

import com.httpchamcong.model.FaceProfile;

import java.sql.*;
import java.util.StringJoiner;

public class FaceProfileDAO {

    public static FaceProfile find(String username) {
        String sql = "SELECT * FROM face_profiles WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean save(String username, double[] descriptor, String imageData,
                               double livenessScore) {
        String sql = "INSERT INTO face_profiles " +
                     "(username, descriptor, image_data, liveness_score, last_verified_at) " +
                     "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                     "ON DUPLICATE KEY UPDATE descriptor = VALUES(descriptor), " +
                     "image_data = VALUES(image_data), liveness_score = VALUES(liveness_score), " +
                     "last_verified_at = CURRENT_TIMESTAMP";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, join(descriptor));
            ps.setString(3, imageData == null ? "" : imageData);
            ps.setDouble(4, livenessScore);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean touchVerified(String username) {
        String sql = "UPDATE face_profiles SET last_verified_at = CURRENT_TIMESTAMP WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean delete(String username) {
        String sql = "DELETE FROM face_profiles WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static FaceProfile map(ResultSet rs) throws SQLException {
        return new FaceProfile(
            rs.getString("username"),
            parse(rs.getString("descriptor")),
            rs.getString("image_data"),
            rs.getDouble("liveness_score"),
            String.valueOf(rs.getTimestamp("enrolled_at")),
            rs.getTimestamp("last_verified_at") == null ? "" : String.valueOf(rs.getTimestamp("last_verified_at"))
        );
    }

    private static String join(double[] values) {
        StringJoiner joiner = new StringJoiner(",");
        for (double value : values) joiner.add(Double.toString(value));
        return joiner.toString();
    }

    private static double[] parse(String text) {
        if (text == null || text.isBlank()) return new double[0];
        String[] parts = text.split(",");
        double[] values = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                values[i] = Double.parseDouble(parts[i].trim());
            } catch (NumberFormatException e) {
                return new double[0];
            }
        }
        return values;
    }
}
