package com.httpchamcong.client;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * HTTP Client — tự build HTTP/1.1 request thủ công và gửi qua Socket TCP.
 *
 * ═══════════════════════════════════════════════════════════════
 *  📌 CẤU HÌNH KẾT NỐI — 2 TRƯỜNG HỢP
 * ═══════════════════════════════════════════════════════════════
 *
 *  🅰️ CHẠY TRÊN 1 MÁY (Local):
 *     mặc định HOST = "localhost"
 *
 *  🅱️ CHẠY TRÊN 2 MÁY (LAN):
 *     java -Dchamcong.server.host=192.168.1.100 ...
 *     hoặc set biến môi trường CHAMCONG_SERVER_HOST
 */
public class SocketHttpClient {

    private static final String HOST = getConfig("chamcong.server.host", "CHAMCONG_SERVER_HOST", "localhost");
    private static final int PORT = Integer.getInteger("chamcong.server.port", 2020);
    private static final int CONNECT_TIMEOUT_MS = 5000;   // 5 giây
    private static final int READ_TIMEOUT_MS = 15000;     // 15 giây

    private static String getConfig(String prop, String env, String defaultValue) {
        String v = System.getProperty(prop);
        if (v == null || v.isEmpty()) v = System.getenv(env);
        return (v == null || v.isEmpty()) ? defaultValue : v;
    }

    private String token = null;

    public void setToken(String t) { this.token = t; }
    public String getToken() { return token; }

    /** Trả về thông tin server để hiển thị trên UI */
    public static String getServerInfo() {
        return HOST + ":" + PORT;
    }

    /** Base URL cho Web Client / trang quét FaceID. */
    public static String getBaseUrl() {
        return "http://" + HOST + ":" + PORT;
    }

    public String get(String path) throws IOException {
        return request("GET", path, null);
    }

    public String post(String path, String jsonBody) throws IOException {
        return request("POST", path, jsonBody);
    }

    public String delete(String path) throws IOException {
        return request("DELETE", path, null);
    }

    /**
     * Gửi HTTP/1.1 request thủ công qua Socket.
     * Vì Content-Length tính bằng BYTES, không phải CHARS.
     * Với UTF-8 nhiều bytes (tiếng Việt), 1 char = 2-3 bytes
     * → Nếu đọc char[contentLength] sẽ block chờ đủ chars → timeout 15s.
     */
    private String request(String method, String path, String body) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(HOST, PORT), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // ============ BUILD HEADER ============
            StringBuilder header = new StringBuilder();
            header.append(method).append(" ").append(path).append(" HTTP/1.1\r\n");
            header.append("Host: ").append(HOST).append(":").append(PORT).append("\r\n");
            header.append("User-Agent: JavaChamCongClient/1.0\r\n");
            header.append("Accept: application/json\r\n");

            if (token != null) {
                header.append("Authorization: Bearer ").append(token).append("\r\n");
            }

            byte[] bodyBytes = null;
            if (body != null) {
                bodyBytes = body.getBytes(StandardCharsets.UTF_8);
                header.append("Content-Type: application/json; charset=UTF-8\r\n");
                header.append("Content-Length: ").append(bodyBytes.length).append("\r\n");
            }
            header.append("Connection: close\r\n");
            header.append("\r\n");

            // ============ GỬI HEADER TRƯỚC ============
            out.write(header.toString().getBytes(StandardCharsets.UTF_8));
            out.flush();

            // ============ GỬI BODY SAU (NẾU CÓ) ============
            if (bodyBytes != null && bodyBytes.length > 0) {
                out.write(bodyBytes);
                out.flush();
            }

            // ============================================================
            // ⭐ ĐỌC HTTP RESPONSE — FIX BUG 15s (dùng BYTE[], không CHAR[])
            // ============================================================

            // ─── Bước 1: Đọc RAW headers (byte-by-byte) đến khi gặp \r\n\r\n ───
            ByteArrayOutputStream headerBuf = new ByteArrayOutputStream();
            int prev3 = 0, prev2 = 0, prev1 = 0, cur;
            while ((cur = in.read()) != -1) {
                headerBuf.write(cur);
                // Check CRLF CRLF
                if (prev3 == '\r' && prev2 == '\n' && prev1 == '\r' && cur == '\n') {
                    break;
                }
                prev3 = prev2;
                prev2 = prev1;
                prev1 = cur;
                // Safety: header tối đa 64KB
                if (headerBuf.size() > 65536) break;
            }

            String headerStr = new String(headerBuf.toByteArray(), StandardCharsets.UTF_8);
            String[] headerLines = headerStr.split("\r\n");

            // ─── Bước 2: Parse status line + Content-Length ───
            String statusLine = headerLines.length > 0 ? headerLines[0] : "";
            System.out.println("<<< " + statusLine);

            if (statusLine.isEmpty()) return "";

            int contentLength = -1;
            for (int i = 1; i < headerLines.length; i++) {
                String lower = headerLines[i].toLowerCase();
                if (lower.startsWith("content-length:")) {
                    try {
                        contentLength = Integer.parseInt(headerLines[i].split(":", 2)[1].trim());
                    } catch (Exception e) {
                        contentLength = -1;
                    }
                }
            }

            // ─── Bước 3: Đọc body bằng BYTE[] (Content-Length tính bằng bytes) ───
            ByteArrayOutputStream bodyBuf = new ByteArrayOutputStream();
            if (contentLength > 0) {
                byte[] buf = new byte[contentLength];
                int totalRead = 0;
                while (totalRead < contentLength) {
                    int r = in.read(buf, totalRead, contentLength - totalRead);
                    if (r == -1) break;
                    totalRead += r;
                }
                bodyBuf.write(buf, 0, totalRead);
            } else if (contentLength == 0) {
                return "";
            } else {
                // Fallback: đọc đến EOF
                byte[] buf = new byte[4096];
                int r;
                while ((r = in.read(buf)) != -1) {
                    bodyBuf.write(buf, 0, r);
                }
            }

            // ─── Bước 4: Decode UTF-8 ───
            return new String(bodyBuf.toByteArray(), StandardCharsets.UTF_8);

        } finally {
            try { socket.close(); } catch (Exception e) { }
        }
    }
}