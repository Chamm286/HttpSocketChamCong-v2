package com.httpchamcong.server.controller;

import com.httpchamcong.server.HttpRequestParser;
import com.httpchamcong.server.HttpResponseBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Dịch vụ tải file qua HTTP/1.1:
 *   GET /files/{filename}
 *
 * Trả về file từ thư mục web/assets với Content-Disposition: attachment.
 */
public class FileController {

    public HttpResponseBuilder download(HttpRequestParser req, String fileName) {
        if (!"GET".equalsIgnoreCase(req.getMethod()) && !"HEAD".equalsIgnoreCase(req.getMethod())) {
            return new HttpResponseBuilder().status(405)
                .header("Allow", "GET, HEAD")
                .json("{\"error\":\"Method khong duoc phep. Chi chap nhan: GET, HEAD\"}");
        }

        if (fileName == null || fileName.isBlank() || fileName.contains("..")
                || fileName.contains("/") || fileName.contains("\\\\")) {
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"Ten file khong hop le\"}");
        }

        String webDir = System.getProperty("chamcong.web.dir");
        if (webDir == null || webDir.isBlank()) {
            webDir = System.getenv("CHAMCONG_WEB_DIR");
        }
        if (webDir == null || webDir.isBlank()) {
            webDir = "web";
        }

        Path root = Paths.get(webDir).toAbsolutePath().normalize().resolve("assets");
        Path file = root.resolve(fileName).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            return new HttpResponseBuilder().status(404)
                .json("{\"error\":\"File khong ton tai: " + com.httpchamcong.util.JsonUtil.escape(fileName) + "\"}");
        }

        String contentType = guessContentType(fileName);
        try {
            byte[] bytes = Files.readAllBytes(file);
            return new HttpResponseBuilder()
                .status(200)
                .header("Content-Type", contentType)
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .bodyBytes(bytes, contentType);
        } catch (Exception e) {
            return new HttpResponseBuilder().status(500)
                .json("{\"error\":\"Khong doc duoc file: " + com.httpchamcong.util.JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }

    private String guessContentType(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".txt")) return "text/plain; charset=UTF-8";
        return "application/octet-stream";
    }
}
