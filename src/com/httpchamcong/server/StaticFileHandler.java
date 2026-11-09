package com.httpchamcong.server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Phục vụ Web client (HTML/CSS/JS) bằng chính HTTP server tự viết trên Socket.
 *
 * Không dùng web server/framework nào — chỉ đọc file từ thư mục `web/`
 * và trả về đúng HTTP/1.1 response.
 *
 * Thư mục web có thể cấu hình:
 *   -Dchamcong.web.dir=duong/dan/web
 *   hoặc biến môi trường CHAMCONG_WEB_DIR
 */
public final class StaticFileHandler {

    private static final Map<String, String> MIME = new HashMap<>();

    static {
        MIME.put("html", "text/html; charset=UTF-8");
        MIME.put("htm",  "text/html; charset=UTF-8");
        MIME.put("css",  "text/css; charset=UTF-8");
        MIME.put("js",   "application/javascript; charset=UTF-8");
        MIME.put("mjs",  "application/javascript; charset=UTF-8");
        MIME.put("json", "application/json; charset=UTF-8");
        MIME.put("svg",  "image/svg+xml; charset=UTF-8");
        MIME.put("txt",  "text/plain; charset=UTF-8");
        MIME.put("ico",  "image/x-icon");
        MIME.put("png",  "image/png");
        MIME.put("jpg",  "image/jpeg");
        MIME.put("jpeg", "image/jpeg");
        MIME.put("webp", "image/webp");
        MIME.put("woff2", "font/woff2");
    }

    private static volatile Path webRoot;

    private StaticFileHandler() {}

    /**
     * Trả response nếu path là file tĩnh tồn tại, ngược lại trả null
     * để Router tiếp tục xử lý (404 JSON).
     */
    public static HttpResponseBuilder serve(String path, String method) {
        if (!"GET".equals(method)) {
            return new HttpResponseBuilder()
                .status(405)
                .header("Allow", "GET, HEAD")
                .json("{\"error\":\"Method khong duoc phep cho file tinh\"}");
        }

        // Chặn path traversal và ký tự lạ
        if (path == null || path.indexOf('\0') >= 0
                || path.contains("..") || path.contains("\\")) {
            return notFound();
        }

        if ("/".equals(path) || path.isEmpty()) path = "/index.html";
        if ("/favicon.ico".equals(path)) path = "/favicon.svg";

        String rel = path.startsWith("/") ? path.substring(1) : path;

        Path root = root();
        if (root == null) {
            return new HttpResponseBuilder().status(503).json(
                "{\"error\":\"Chua tim thay thu muc web/. Hay chay server tu thu muc goc project.\"}");
        }

        Path file = root.resolve(rel).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            return notFound();
        }

        try {
            String ext = extension(file.getFileName().toString());
            String mime = MIME.getOrDefault(ext, "application/octet-stream");

            HttpResponseBuilder res = new HttpResponseBuilder()
                .status(200)
                .header("Content-Type", mime)
                .header("Cache-Control", "no-cache")
                .header("X-Content-Type-Options", "nosniff");

            if (mime.startsWith("text/") || mime.contains("javascript") || mime.contains("json")
                    || mime.contains("svg")) {
                res.body(Files.readString(file, StandardCharsets.UTF_8));
            } else {
                res.bodyBytes(Files.readAllBytes(file), mime);
            }
            return res;
        } catch (IOException e) {
            return new HttpResponseBuilder().status(500)
                .json("{\"error\":\"Khong doc duoc file tinh\"}");
        }
    }

    private static HttpResponseBuilder notFound() {
        return new HttpResponseBuilder().status(404)
            .json("{\"error\":\"Khong tim thay file\"}");
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }

    /** Tìm thư mục web/ từ cấu hình, thư mục hiện tại hoặc thư mục cha. */
    private static Path root() {
        if (webRoot != null) return webRoot;

        synchronized (StaticFileHandler.class) {
            if (webRoot != null) return webRoot;

            String configured = System.getProperty("chamcong.web.dir");
            if (configured == null || configured.isBlank()) {
                configured = System.getenv("CHAMCONG_WEB_DIR");
            }

            if (configured != null && !configured.isBlank()) {
                Path p = Paths.get(configured).toAbsolutePath().normalize();
                if (Files.isDirectory(p)) {
                    webRoot = p;
                    return webRoot;
                }
            }

            Path cwd = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
            Path[] candidates = new Path[] {
                cwd.resolve("web"),
                cwd.resolve("..").resolve("web").normalize(),
                cwd.resolve("..").resolve("..").resolve("web").normalize()
            };
            for (Path p : candidates) {
                if (Files.isDirectory(p)) {
                    webRoot = p;
                    return webRoot;
                }
            }
            return null;
        }
    }

    /** Cho biết web root đang dùng (để log lúc khởi động). */
    public static String describeRoot() {
        Path p = root();
        return p == null ? "(khong tim thay web/)" : p.toString();
    }
}
