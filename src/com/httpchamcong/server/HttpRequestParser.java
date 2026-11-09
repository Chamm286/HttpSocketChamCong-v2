package com.httpchamcong.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Tự PARSE HTTP/1.1 REQUEST từ Socket InputStream.
 *
 * Format HTTP/1.1 Request (RFC 2616 / RFC 7230):
 *
 *   POST /api/login HTTP/1.1              ← Request-Line
 *   Host: localhost:2020                  ← Header bắt buộc với HTTP/1.1
 *   Authorization: Bearer xxx
 *   Content-Type: application/json
 *   Content-Length: 42
 *                                          ← CRLF trống
 *   {"username":"abc","password":"123"}   ← Body (UTF-8)
 *
 * Các điểm tuân thủ:
 *   - Request-Line đúng 3 thành phần: method SP request-target SP HTTP-version
 *   - HTTP/1.1 bắt buộc có Host, Host trùng lặp bị từ chối
 *   - Header name/value được kiểm tra, không hỗ trợ obs-fold
 *   - Content-Length tính bằng BYTE, body UTF-8 đọc đủ byte
 *   - Hỗ trợ persistent connection qua Connection: keep-alive / close
 *   - Giới hạn header 64KB, body 1MB
 */
public class HttpRequestParser {

    private static final int MAX_HEADER_BYTES = 64 * 1024;
    private static final int MAX_BODY_BYTES   = 1024 * 1024;

    private static final Pattern TOKEN =
        Pattern.compile("[!#$%&'*+.^_`|~0-9A-Za-z-]+");

    private String method;
    private String path;
    private String version;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final Map<String, String> params  = new LinkedHashMap<>();
    private String body = "";
    private String rawHead = "";
    private boolean keepAlive = true;

    /** Lỗi parse có kèm HTTP status code để server trả đúng lỗi. */
    public static class HttpParseException extends IOException {
        private final int statusCode;

        public HttpParseException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() { return statusCode; }
    }

    public static HttpRequestParser parse(InputStream in) throws IOException {
        HttpRequestParser req = new HttpRequestParser();

        // ============================================================
        // BƯỚC 1: Đọc RAW header theo BYTE đến khi gặp CRLF CRLF
        // ============================================================
        ByteArrayOutputStream headerBuf = new ByteArrayOutputStream();
        int prev3 = 0, prev2 = 0, prev1 = 0, cur;
        boolean headerTerminated = false;

        while ((cur = in.read()) != -1) {
            headerBuf.write(cur);

            if (prev3 == '\r' && prev2 == '\n' && prev1 == '\r' && cur == '\n') {
                headerTerminated = true;
                break;
            }

            prev3 = prev2;
            prev2 = prev1;
            prev1 = cur;

            if (headerBuf.size() > MAX_HEADER_BYTES) {
                throw new HttpParseException(431, "Header qua lon (toi da 64KB)");
            }
        }

        if (headerBuf.size() == 0) return null; // client đóng kết nối, không phải request
        if (!headerTerminated) {
            throw new HttpParseException(400, "Request khong dung dinh dang HTTP (thieu CRLF CRLF)");
        }

        byte[] rawHeaderBytes = headerBuf.toByteArray();
        req.rawHead = new String(rawHeaderBytes, StandardCharsets.ISO_8859_1);

        String[] headerLines = req.rawHead.split("\r\n", -1);

        // ===== BƯỚC 2: REQUEST-LINE =====
        String requestLine = headerLines.length > 0 ? headerLines[0] : "";
        if (requestLine.isEmpty()) {
            throw new HttpParseException(400, "Thieu request line");
        }

        // RFC 2616: method SP request-target SP HTTP-version (đúng 1 dấu SP)
        int sp1 = requestLine.indexOf(' ');
        int sp2 = sp1 >= 0 ? requestLine.indexOf(' ', sp1 + 1) : -1;
        if (sp1 <= 0 || sp2 <= sp1 + 1 || requestLine.indexOf(' ', sp2 + 1) >= 0) {
            throw new HttpParseException(400,
                "Request line phai co 3 thanh phan: METHOD SP TARGET SP HTTP-VERSION");
        }

        req.method  = requestLine.substring(0, sp1);
        String target = requestLine.substring(sp1 + 1, sp2);
        req.version = requestLine.substring(sp2 + 1);

        if (!TOKEN.matcher(req.method).matches()) {
            throw new HttpParseException(400, "HTTP method khong hop le");
        }
        req.method = req.method.toUpperCase();

        if (!target.startsWith("/") && !"*".equals(target)) {
            throw new HttpParseException(400, "Request-target khong hop le");
        }

        if (!"HTTP/1.1".equals(req.version)) {
            throw new HttpParseException(505,
                "Chi ho tro HTTP/1.1, nhan duoc: " + req.version);
        }

        // Tách query string. Ví dụ: /api/history?from=2026-11-01&to=2026-11-09
        if (target.contains("?")) {
            String[] ps = target.split("\\?", 2);
            req.path = ps[0].isEmpty() ? "/" : ps[0];
            for (String kv : ps[1].split("&")) {
                if (kv.isEmpty()) continue;
                String[] p = kv.split("=", 2);
                if (p.length == 2) {
                    req.params.put(urlDecode(p[0]), urlDecode(p[1]));
                } else {
                    req.params.put(urlDecode(p[0]), "");
                }
            }
        } else {
            req.path = target;
        }

        // ===== BƯỚC 3: HEADERS =====
        long contentLength = 0;
        String contentLengthValue = null;
        String transferEncoding = null;
        String host = null;

        for (int i = 1; i < headerLines.length; i++) {
            String line = headerLines[i];
            if (line.isEmpty()) continue;

            // RFC 7230: obs-fold (dòng header bắt đầu bằng SP/HTAB) không được dùng
            if (line.charAt(0) == ' ' || line.charAt(0) == '\t') {
                throw new HttpParseException(400, "Header folding khong duoc ho tro");
            }

            int idx = line.indexOf(':');
            if (idx <= 0) {
                throw new HttpParseException(400, "Header khong hop le: " + line);
            }

            String k = line.substring(0, idx);
            String v = line.substring(idx + 1).trim();

            if (!TOKEN.matcher(k).matches()) {
                throw new HttpParseException(400, "Ten header khong hop le: " + k);
            }
            if (hasInvalidHeaderValueChar(v)) {
                throw new HttpParseException(400, "Gia tri header khong hop le: " + k);
            }

            // Header name không phân biệt hoa/thường. Nếu trùng key thì giữ giá trị đầu.
            boolean exists = false;
            for (String existing : req.headers.keySet()) {
                if (existing.equalsIgnoreCase(k)) { exists = true; break; }
            }
            if (!exists) req.headers.put(k, v);

            if (k.equalsIgnoreCase("Host")) {
                if (host != null) {
                    throw new HttpParseException(400, "Host header bi lap");
                }
                if (v.isEmpty() || v.indexOf(' ') >= 0 || v.indexOf('\t') >= 0) {
                    throw new HttpParseException(400, "Host header khong hop le");
                }
                host = v;
            }

            if (k.equalsIgnoreCase("Content-Length")) {
                if (contentLengthValue != null && !contentLengthValue.equals(v)) {
                    throw new HttpParseException(400, "Content-Length bi trung va khac nhau");
                }
                contentLengthValue = v;
                try {
                    contentLength = Long.parseLong(v);
                } catch (NumberFormatException e) {
                    throw new HttpParseException(400, "Content-Length khong hop le");
                }
                if (contentLength < 0) {
                    throw new HttpParseException(400, "Content-Length khong duoc am");
                }
                if (contentLength > MAX_BODY_BYTES) {
                    throw new HttpParseException(413, "Body qua lon (toi da 1MB)");
                }
            }

            if (k.equalsIgnoreCase("Transfer-Encoding")) {
                transferEncoding = v;
            }
        }

        // RFC 7230 §5.4: HTTP/1.1 bắt buộc có Host header
        if (host == null || host.isEmpty()) {
            throw new HttpParseException(400, "Thieu Host header (bat buoc voi HTTP/1.1)");
        }

        // Chưa hỗ trợ chunked request body.
        if (transferEncoding != null) {
            if (!transferEncoding.equalsIgnoreCase("identity")) {
                throw new HttpParseException(501,
                    "Transfer-Encoding chua duoc ho tro (chi ho tro Content-Length)");
            }
            // RFC 7230 §3.3.3: không nhận cả TE và CL để tránh request smuggling.
            if (contentLengthValue != null) {
                throw new HttpParseException(400,
                    "Khong duoc gui dong thoi Transfer-Encoding va Content-Length");
            }
        }

        // ============================================================
        // BƯỚC 4: BODY — đọc bằng byte[] (Content-Length tính bằng BYTES)
        // ============================================================
        if (contentLength > 0) {
            byte[] buf = new byte[(int) contentLength];
            int totalRead = 0;
            while (totalRead < contentLength) {
                int r = in.read(buf, totalRead, (int) contentLength - totalRead);
                if (r == -1) {
                    throw new HttpParseException(400, "Body khong day du theo Content-Length");
                }
                totalRead += r;
            }
            req.body = new String(buf, 0, totalRead, StandardCharsets.UTF_8);
        }

        // HTTP/1.1 mặc định keep-alive; chỉ đóng khi Connection chứa token "close".
        req.keepAlive = !hasConnectionToken(req.getHeader("Connection"), "close");

        return req;
    }

    private static boolean hasInvalidHeaderValueChar(String v) {
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (c == '\r' || c == '\n' || c == 0x7f) return true;
            if (c < 0x20 && c != '\t') return true;
        }
        return false;
    }

    private static boolean hasConnectionToken(String value, String token) {
        if (value == null || value.isEmpty()) return false;
        for (String part : value.split(",")) {
            if (part.trim().equalsIgnoreCase(token)) return true;
        }
        return false;
    }

    private static String urlDecode(String s) {
        try { return URLDecoder.decode(s, StandardCharsets.UTF_8.name()); }
        catch (Exception e) { return s; }
    }

    // ============ GETTERS ============
    public String getMethod() { return method; }
    public String getPath()   { return path; }
    public String getVersion(){ return version; }
    public Map<String, String> getHeaders() { return headers; }
    public Map<String, String> getParams()  { return params; }
    public String getBody()   { return body == null ? "" : body; }

    /** HTTP/1.1 mặc định persistent; Connection: close sẽ tắt keep-alive. */
    public boolean isKeepAlive() { return keepAlive; }

    /** Lấy header theo tên, không phân biệt hoa/thường (RFC 7230). */
    public String getHeader(String name) {
        if (name == null) return null;
        for (Map.Entry<String, String> e : headers.entrySet()) {
            if (e.getKey().equalsIgnoreCase(name)) return e.getValue();
        }
        return null;
    }

    /** Lấy token từ Authorization: Bearer xxx, fallback X-Session-Token. */
    public String getBearerToken() {
        String auth = getHeader("Authorization");
        if (auth != null && !auth.isBlank()) {
            if (auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
                return auth.substring(7).trim();
            }
            return auth.trim();
        }
        String session = getHeader("X-Session-Token");
        return session == null ? null : session.trim();
    }

    /** Request head thô (request-line + headers + CRLF trống) để trace/debug. */
    public String getRawRequestHead() { return rawHead; }

    /** Toàn bộ request thô: head + body (body đã decode UTF-8). */
    public String getRawRequest() { return rawHead + getBody(); }

    /** Request thô đã che Authorization/Cookie để trả về Network Console. */
    public String getRawRequestRedacted() {
        String head = rawHead
            .replaceAll("(?im)^(Authorization|Proxy-Authorization|Cookie):[^\\r\\n]*",
                        "$1: <redacted>");
        return head + getBody();
    }

    @Override
    public String toString() {
        return method + " " + path + " " + version;
    }
}
