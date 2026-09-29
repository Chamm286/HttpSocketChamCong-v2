package com.httpchamcong.server;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Tự PARSE HTTP/1.1 REQUEST từ Socket InputStream.
 *
 * ⭐ FIX BUG: Đọc body bằng byte[] thay vì char[] (giống Client).
 * Vì Content-Length tính bằng BYTES, không phải CHARS.
 * Với UTF-8 nhiều bytes (tiếng Việt), 1 char = 2-3 bytes
 * → Đọc char[contentLength] sẽ block chờ đủ chars → timeout 15s.
 *
 * Format HTTP/1.1 Request:
 *   POST /api/login HTTP/1.1              ← Request line
 *   Host: localhost:8080                  ← Headers
 *   Authorization: Bearer xxx
 *   Content-Length: 42
 *                                          ← Dòng trống (CRLF)
 *   {"username":"abc","password":"123"}   ← Body
 */
public class HttpRequestParser {

    private String method;
    private String path;
    private String version;
    private Map<String, String> headers = new HashMap<>();
    private Map<String, String> params  = new HashMap<>();
    private String body = "";

    public static HttpRequestParser parse(InputStream in) throws IOException {
        HttpRequestParser req = new HttpRequestParser();

        // ============================================================
        // ⭐ BƯỚC 1: Đọc RAW header (byte-by-byte đến \r\n\r\n)
        // ============================================================
        ByteArrayOutputStream headerBuf = new ByteArrayOutputStream();
        int prev3 = 0, prev2 = 0, prev1 = 0, cur;
        while ((cur = in.read()) != -1) {
            headerBuf.write(cur);
            if (prev3 == '\r' && prev2 == '\n' && prev1 == '\r' && cur == '\n') {
                break;
            }
            prev3 = prev2;
            prev2 = prev1;
            prev1 = cur;
            if (headerBuf.size() > 65536) break;  // safety max 64KB
        }

        String headerStr = new String(headerBuf.toByteArray(), StandardCharsets.UTF_8);
        String[] headerLines = headerStr.split("\r\n");

        // ===== BƯỚC 2: REQUEST LINE =====
        String requestLine = headerLines.length > 0 ? headerLines[0] : "";
        if (requestLine.isEmpty()) return null;

        String[] parts = requestLine.split(" ");
        if (parts.length < 3) return null;

        req.method  = parts[0];
        String fullPath = parts[1];
        req.version = parts[2];

        // Tách query string
        if (fullPath.contains("?")) {
            String[] ps = fullPath.split("\\?", 2);
            req.path = ps[0];
            for (String kv : ps[1].split("&")) {
                String[] p = kv.split("=", 2);
                if (p.length == 2) {
                    req.params.put(urlDecode(p[0]), urlDecode(p[1]));
                }
            }
        } else {
            req.path = fullPath;
        }

        // ===== BƯỚC 3: HEADERS =====
        int contentLength = 0;
        for (int i = 1; i < headerLines.length; i++) {
            String line = headerLines[i];
            if (line.isEmpty()) continue;
            int idx = line.indexOf(':');
            if (idx > 0) {
                String k = line.substring(0, idx).trim();
                String v = line.substring(idx + 1).trim();
                req.headers.put(k, v);

                if (k.equalsIgnoreCase("Content-Length")) {
                    try { contentLength = Integer.parseInt(v); }
                    catch (Exception e) { contentLength = 0; }
                }
            }
        }

        // ============================================================
        // ⭐ BƯỚC 4: BODY — FIX dùng byte[] thay vì char[]
        // ============================================================
        if (contentLength > 0) {
            byte[] buf = new byte[contentLength];
            int totalRead = 0;
            while (totalRead < contentLength) {
                int r = in.read(buf, totalRead, contentLength - totalRead);
                if (r == -1) break;
                totalRead += r;
            }
            req.body = new String(buf, 0, totalRead, StandardCharsets.UTF_8);
        }

        return req;
    }

    private static String urlDecode(String s) {
        try { return URLDecoder.decode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }

    // ============ GETTERS ============
    public String getMethod() { return method; }
    public String getPath()   { return path; }
    public String getVersion(){ return version; }
    public Map<String, String> getHeaders() { return headers; }
    public Map<String, String> getParams()  { return params; }
    public String getBody()   { return body == null ? "" : body; }

    /** Lấy token từ header Authorization: Bearer xxx */
    public String getBearerToken() {
        String auth = headers.get("Authorization");
        if (auth == null) return null;
        if (auth.startsWith("Bearer ")) return auth.substring(7).trim();
        return auth.trim();
    }

    @Override
    public String toString() {
        return method + " " + path + " " + version;
    }
}