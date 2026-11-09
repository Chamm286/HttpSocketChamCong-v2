package com.httpchamcong.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Kết nối MySQL.
 *
 * Có thể cấu hình bằng system property hoặc biến môi trường:
 *   -Dchamcong.db.url=jdbc:mysql://localhost:3306/chamcong_db
 *   -Dchamcong.db.user=root
 *   -Dchamcong.db.password=
 *
 * Nếu không cấu hình, tự thử XAMPP port 3307 trước, sau đó 3306
 * (XAMPP mặc định có thể dùng 1 trong 2 port tùy máy).
 */
public class DBConnection {

    private static final String PARAMS =
        "?useUnicode=true"
        + "&characterEncoding=UTF-8"
        + "&serverTimezone=Asia/Ho_Chi_Minh"
        + "&useSSL=false"
        + "&allowPublicKeyRetrieval=true"
        + "&connectTimeout=3000"
        + "&socketTimeout=10000";

    private static final String[] DEFAULT_URLS = {
        "jdbc:mysql://localhost:3307/chamcong_db" + PARAMS,
        "jdbc:mysql://localhost:3306/chamcong_db" + PARAMS
    };

    private static final String USER = getConfig("chamcong.db.user", "CHAMCONG_DB_USER", "root");
    private static final String PASSWORD = getConfig("chamcong.db.password", "CHAMCONG_DB_PASSWORD", "");

    private static volatile String workingUrl = null;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Không tìm thấy MySQL Driver!");
            e.printStackTrace();
        }
    }

    private static String getConfig(String prop, String env, String defaultValue) {
        String v = System.getProperty(prop);
        if (v == null || v.isEmpty()) v = System.getenv(env);
        return (v == null) ? defaultValue : v;
    }

    public static Connection getConnection() throws SQLException {
        String customUrl = getConfig("chamcong.db.url", "CHAMCONG_DB_URL", null);
        if (customUrl != null && !customUrl.isEmpty()) {
            return DriverManager.getConnection(customUrl, USER, PASSWORD);
        }

        String cached = workingUrl;
        if (cached != null) {
            try {
                return DriverManager.getConnection(cached, USER, PASSWORD);
            } catch (SQLException e) {
                workingUrl = null; // thử lại các URL mặc định
            }
        }

        SQLException last = null;
        for (String url : DEFAULT_URLS) {
            try {
                Connection conn = DriverManager.getConnection(url, USER, PASSWORD);
                workingUrl = url;
                return conn;
            } catch (SQLException e) {
                last = e;
            }
        }

        throw last != null ? last : new SQLException("Không thể kết nối MySQL");
    }

    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("  TEST KẾT NỐI MYSQL");
        System.out.println("===========================================");
        System.out.println("User: " + USER);
        System.out.println("Thử lần lượt: 3307 → 3306 (hoặc chamcong.db.url)");
        System.out.println();

        try (Connection conn = getConnection()) {
            System.out.println("✅ Kết nối MySQL thành công!");
            System.out.println("   URL:      " + workingUrl);
            System.out.println("   Database: " + conn.getCatalog());
            System.out.println("   Driver:   " + conn.getMetaData().getDriverName());
            System.out.println("   Version:  " + conn.getMetaData().getDatabaseProductVersion());
        } catch (SQLException e) {
            System.err.println("❌ Lỗi: " + e.getMessage());
            System.err.println("   Kiểm tra XAMPP đã Start MySQL chưa, và đã import database/chamcong_db.sql chưa.");
        }
    }
}
