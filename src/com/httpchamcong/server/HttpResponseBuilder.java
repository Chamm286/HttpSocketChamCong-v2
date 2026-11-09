package com.httpchamcong.server;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Tự BUILD HTTP/1.1 RESPONSE để ghi ra Socket.
 *
 * Format HTTP/1.1 Response (RFC 2616 / RFC 7230):
 *
 *   HTTP/1.1 200 OK                      ← Status-Line
 *   Server: JavaChamCongServer/1.1       ← Headers
 *   Date: Sat, 07 Nov 2026 10:00:00 GMT  ← RFC 1123 IMF-fixdate
 *   Content-Type: application/json; charset=UTF-8
 *   Content-Length: 45                   ← số BYTE UTF-8, không phải số ký tự
 *   Connection: keep-alive
 *                                        ← CRLF trống
 *   {"msg":"Check-in thành công"}        ← Body
 *
 * Lưu ý:
 *   - 204/304/1xx: KHÔNG body, KHÔNG Content-Length
 *   - HEAD: gửi header như GET + Content-Length của GET, nhưng KHÔNG gửi body
 *   - Connection do ClientHandler quyết định theo request (keep-alive/close)
 */
public class HttpResponseBuilder {

    private static final DateTimeFormatter HTTP_DATE =
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH)
                         .withZone(ZoneOffset.UTC);

    private int statusCode = 200;
    private String statusText = "OK";
    private final Map<String, String> headers = new LinkedHashMap<>();
    private String body = "";
    private byte[] rawBody = null;

    public HttpResponseBuilder() {
        setHeader("Server", "JavaChamCongServer/1.1");
        setHeader("Date", HTTP_DATE.format(Instant.now()));
    }

    public HttpResponseBuilder status(int code) {
        this.statusCode = code;
        switch (code) {
            case 100: statusText = "Continue"; break;
            case 200: statusText = "OK"; break;
            case 201: statusText = "Created"; break;
            case 202: statusText = "Accepted"; break;
            case 204: statusText = "No Content"; break;
            case 301: statusText = "Moved Permanently"; break;
            case 304: statusText = "Not Modified"; break;
            case 400: statusText = "Bad Request"; break;
            case 401: statusText = "Unauthorized"; break;
            case 403: statusText = "Forbidden"; break;
            case 404: statusText = "Not Found"; break;
            case 405: statusText = "Method Not Allowed"; break;
            case 408: statusText = "Request Timeout"; break;
            case 409: statusText = "Conflict"; break;
            case 411: statusText = "Length Required"; break;
            case 413: statusText = "Payload Too Large"; break;
            case 414: statusText = "URI Too Long"; break;
            case 415: statusText = "Unsupported Media Type"; break;
            case 422: statusText = "Unprocessable Entity"; break;
            case 429: statusText = "Too Many Requests"; break;
            case 431: statusText = "Request Header Fields Too Large"; break;
            case 500: statusText = "Internal Server Error"; break;
            case 501: statusText = "Not Implemented"; break;
            case 503: statusText = "Service Unavailable"; break;
            case 505: statusText = "HTTP Version Not Supported"; break;
            default:  statusText = "Unknown";
        }
        return this;
    }

    /** Thêm/ghi đè header theo tên không phân biệt hoa/thường. */
    public HttpResponseBuilder header(String k, String v) {
        setHeader(k, v);
        return this;
    }

    /** Xóa header theo tên không phân biệt hoa/thường. */
    public HttpResponseBuilder removeHeader(String k) {
        if (k == null) return this;
        headers.keySet().removeIf(existing -> existing.equalsIgnoreCase(k));
        return this;
    }

    private void setHeader(String k, String v) {
        if (k == null || k.isBlank()) return;
        headers.keySet().removeIf(existing -> existing.equalsIgnoreCase(k));
        if (v == null || v.isEmpty()) return;
        // Không cho CR/LF đi vào header để tránh header injection.
        String safeValue = v.replace("\r", "").replace("\n", "");
        headers.put(k, safeValue);
    }

    public HttpResponseBuilder connection(boolean keepAlive) {
        return header("Connection", keepAlive ? "keep-alive" : "close");
    }

    public HttpResponseBuilder body(String b) {
        this.body = b == null ? "" : b;
        this.rawBody = null;
        return this;
    }

    /** Body nhị phân (ảnh, font...) — ghi nguyên byte, không encode lại. */
    public HttpResponseBuilder bodyBytes(byte[] b, String contentType) {
        this.rawBody = b == null ? new byte[0] : b;
        this.body = "";
        if (contentType != null) setHeader("Content-Type", contentType);
        return this;
    }

    public HttpResponseBuilder json(String b) {
        setHeader("Content-Type", "application/json; charset=UTF-8");
        return body(b);
    }

    /** Nội dung body dưới dạng byte (UTF-8 cho text, nguyên bản cho binary). */
    public byte[] getBodyBytes() {
        return rawBody != null ? rawBody : body.getBytes(StandardCharsets.UTF_8);
    }

    /** Content-Length phải là số BYTE UTF-8, không phải số ký tự Java. */
    public int getBodyLength() {
        return getBodyBytes().length;
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    /** Ghi response ra Socket theo đúng chuẩn HTTP/1.1. */
    public void writeTo(OutputStream out) throws IOException {
        writeTo(out, false);
    }

    /**
     * @param head true nếu request gốc là HEAD → gửi header nhưng không gửi body.
     */
    public void writeTo(OutputStream out, boolean head) throws IOException {
        byte[] bodyBytes = getBodyBytes();

        boolean bodyForbidden =
            (statusCode >= 100 && statusCode < 200) || statusCode == 204 || statusCode == 304;

        if (!bodyForbidden) {
            setHeader("Content-Length", String.valueOf(bodyBytes.length));
        } else {
            removeHeader("Content-Length");
            removeHeader("Content-Type");
        }

        StringBuilder sb = new StringBuilder();

        // ===== STATUS-LINE =====
        sb.append("HTTP/1.1 ").append(statusCode).append(" ")
          .append(statusText).append("\r\n");

        // ===== HEADERS =====
        for (Map.Entry<String, String> e : headers.entrySet()) {
            sb.append(e.getKey()).append(": ").append(e.getValue()).append("\r\n");
        }

        // ===== CRLF TRỐNG TRƯỚC BODY =====
        sb.append("\r\n");

        // RFC 2616: header dùng ISO-8859-1; body UTF-8 theo Content-Length byte.
        out.write(sb.toString().getBytes(StandardCharsets.ISO_8859_1));
        if (!head && !bodyForbidden && bodyBytes.length > 0) {
            out.write(bodyBytes);
        }
        out.flush();
    }

    public int getStatusCode() { return statusCode; }
    public String getStatusText() { return statusText; }
    public String getBody() { return body; }
}
