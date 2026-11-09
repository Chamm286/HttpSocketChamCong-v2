package com.httpchamcong.server.controller;

import com.httpchamcong.dao.FaceProfileDAO;
import com.httpchamcong.model.FaceProfile;
import com.httpchamcong.model.User;
import com.httpchamcong.server.FaceAuthStore;
import com.httpchamcong.server.FaceRecognitionService;
import com.httpchamcong.server.HttpRequestParser;
import com.httpchamcong.server.HttpResponseBuilder;
import com.httpchamcong.util.JsonUtil;
import com.httpchamcong.util.SessionManager;

/**
 * API FaceID thật:
 *   POST /api/face/challenge       -> cấp challenge một lần
 *   POST /api/face/verify          -> so khớp descriptor + liveness, cấp faceToken
 *   GET  /api/face/status          -> trạng thái đăng ký khuôn mặt
 *   POST /api/face/reset           -> xóa mẫu khuôn mặt để đăng ký lại
 *   POST /api/face/desktop/start   -> tạo phiên quét camera cho Desktop Client
 *   GET  /api/face/desktop/context -> context cho trang quét trên trình duyệt
 *   POST /api/face/desktop/verify  -> xác thực khuôn mặt từ trang quét
 *   GET  /api/face/desktop/result  -> Desktop Client poll kết quả + faceToken
 */
public class FaceController {

    private User auth(HttpRequestParser req) {
        return SessionManager.getUser(req.getBearerToken());
    }

    private String error(String message) {
        return "{\"error\":\"" + JsonUtil.escape(message) + "\"}";
    }

    private String action(String raw) {
        if (raw == null) return "";
        String a = raw.trim().toUpperCase();
        return ("CHECKIN".equals(a) || "CHECKOUT".equals(a)) ? a : "";
    }

    private boolean validImage(String image) {
        if (image == null || image.isBlank()) return false;
        if (image.length() > 600_000) return false;
        return image.startsWith("data:image/jpeg;base64,")
            || image.startsWith("data:image/png;base64,");
    }

    private boolean validFacePayload(double[] descriptor, double liveness, double detection) {
        return FaceRecognitionService.validDescriptor(descriptor)
            && liveness >= FaceAuthStore.LIVENESS_MIN
            && detection >= FaceAuthStore.DETECTION_MIN;
    }

    // ============================================================
    // STATUS
    // ============================================================
    public HttpResponseBuilder status(HttpRequestParser req) {
        User user = auth(req);
        if (user == null) return new HttpResponseBuilder().status(401).json(error("Chua dang nhap"));

        FaceProfile profile = FaceProfileDAO.find(user.getUsername());
        boolean enrolled = profile != null
            && FaceRecognitionService.validDescriptor(profile.getDescriptor());

        String body = "{"
            + "\"enrolled\":" + enrolled + ","
            + "\"threshold\":" + FaceRecognitionService.MATCH_THRESHOLD + ","
            + "\"livenessMin\":" + FaceAuthStore.LIVENESS_MIN + ","
            + "\"detectionMin\":" + FaceAuthStore.DETECTION_MIN + ","
            + "\"lastVerifiedAt\":\"" + JsonUtil.escape(profile == null ? "" : profile.getLastVerifiedAt()) + "\","
            + "\"message\":\"" + JsonUtil.escape(enrolled
                ? "Khuon mat da duoc dang ky"
                : "Lan quet dau tien se dang ky khuon mat cua ban") + "\""
            + "}";
        return new HttpResponseBuilder().status(200).json(body);
    }

    // ============================================================
    // CHALLENGE
    // ============================================================
    public HttpResponseBuilder challenge(HttpRequestParser req) {
        User user = auth(req);
        if (user == null) return new HttpResponseBuilder().status(401).json(error("Chua dang nhap"));

        String action = action(JsonUtil.getString(req.getBody(), "action"));
        if (action.isEmpty()) action = "CHECKIN";

        FaceAuthStore.Challenge challenge = FaceAuthStore.createChallenge(user.getUsername(), action);
        String body = "{"
            + "\"challengeId\":\"" + JsonUtil.escape(challenge.id) + "\","
            + "\"action\":\"" + action + "\","
            + "\"expiresIn\":" + (FaceAuthStore.TTL_MS / 1000) + ","
            + "\"livenessMin\":" + FaceAuthStore.LIVENESS_MIN + ","
            + "\"detectionMin\":" + FaceAuthStore.DETECTION_MIN
            + "}";
        return new HttpResponseBuilder().status(200).json(body);
    }

    // ============================================================
    // VERIFY / ENROLL
    // ============================================================
    public HttpResponseBuilder verify(HttpRequestParser req) {
        User user = auth(req);
        if (user == null) return new HttpResponseBuilder().status(401).json(error("Chua dang nhap"));

        String body = req.getBody();
        String challengeId = JsonUtil.getString(body, "challengeId");
        String action = action(JsonUtil.getString(body, "action"));
        if (action.isEmpty()) action = "CHECKIN";

        double[] descriptor = JsonUtil.getDoubleArray(body, "descriptor");
        Double livenessBoxed = JsonUtil.getDouble(body, "livenessScore");
        Double detectionBoxed = JsonUtil.getDouble(body, "detectionScore");
        double liveness = livenessBoxed == null ? 0 : livenessBoxed;
        double detection = detectionBoxed == null ? 0 : detectionBoxed;
        String image = JsonUtil.getString(body, "image");

        FaceAuthStore.Challenge challenge =
            FaceAuthStore.consumeChallenge(challengeId, user.getUsername(), action);
        if (challenge == null) {
            return new HttpResponseBuilder().status(400)
                .json(error("Challenge khong hop le hoac da het han"));
        }
        if (!validFacePayload(descriptor, liveness, detection)) {
            return new HttpResponseBuilder().status(400)
                .json(error("Khuon mat chua dat nguong chat luong/liveness"));
        }
        if (!validImage(image)) {
            return new HttpResponseBuilder().status(400)
                .json(error("Anh khuon mat khong hop le hoac qua lon"));
        }

        FaceRecognitionService.Result result =
            FaceRecognitionService.verifyOrEnroll(user.getUsername(), descriptor, liveness, image);
        if (!result.verified) {
            String fail = "{"
                + "\"verified\":false,"
                + "\"matched\":false,"
                + "\"distance\":" + String.format(java.util.Locale.US, "%.6f", result.distance) + ","
                + "\"score\":" + String.format(java.util.Locale.US, "%.6f", result.score) + ","
                + "\"message\":\"" + JsonUtil.escape(result.message) + "\""
                + "}";
            return new HttpResponseBuilder().status(400).json(fail);
        }

        String token = FaceAuthStore.issueFaceToken(
            user.getUsername(), action, result.score, liveness, image);

        String ok = "{"
            + "\"verified\":true,"
            + "\"matched\":true,"
            + "\"enrolled\":" + result.enrolled + ","
            + "\"distance\":" + String.format(java.util.Locale.US, "%.6f", result.distance) + ","
            + "\"score\":" + String.format(java.util.Locale.US, "%.6f", result.score) + ","
            + "\"liveness\":" + String.format(java.util.Locale.US, "%.6f", liveness) + ","
            + "\"faceToken\":\"" + JsonUtil.escape(token) + "\","
            + "\"expiresIn\":" + (FaceAuthStore.TTL_MS / 1000) + ","
            + "\"message\":\"" + JsonUtil.escape(result.message) + "\""
            + "}";
        return new HttpResponseBuilder().status(200).json(ok);
    }

    // ============================================================
    // RESET (tự đăng ký lại khuôn mặt)
    // ============================================================
    public HttpResponseBuilder reset(HttpRequestParser req) {
        User user = auth(req);
        if (user == null) return new HttpResponseBuilder().status(401).json(error("Chua dang nhap"));
        boolean deleted = FaceProfileDAO.delete(user.getUsername());
        String body = "{\"ok\":true,\"deleted\":" + deleted
            + ",\"message\":\"" + JsonUtil.escape("Da xoa mau khuon mat. Lan quet tiep theo se dang ky lai.") + "\"}";
        return new HttpResponseBuilder().status(200).json(body);
    }

    // ============================================================
    // DESKTOP BRIDGE: Desktop Client mở trình duyệt để dùng camera thật
    // ============================================================
    public HttpResponseBuilder desktopStart(HttpRequestParser req) {
        User user = auth(req);
        if (user == null) return new HttpResponseBuilder().status(401).json(error("Chua dang nhap"));

        String action = action(JsonUtil.getString(req.getBody(), "action"));
        if (action.isEmpty()) {
            return new HttpResponseBuilder().status(400).json(error("action phai la CHECKIN hoac CHECKOUT"));
        }

        FaceAuthStore.DesktopSession session =
            FaceAuthStore.createDesktopSession(user.getUsername(), action);
        String body = "{"
            + "\"session\":\"" + JsonUtil.escape(session.id) + "\","
            + "\"action\":\"" + action + "\","
            + "\"scanUrl\":\"/face-scan.html?session=" + JsonUtil.escape(session.id) + "\","
            + "\"expiresIn\":" + (FaceAuthStore.TTL_MS / 1000)
            + "}";
        return new HttpResponseBuilder().status(200).json(body);
    }

    public HttpResponseBuilder desktopContext(HttpRequestParser req) {
        String id = req.getParams().get("session");
        FaceAuthStore.DesktopSession session = FaceAuthStore.getDesktopSession(id);
        if (session == null) {
            return new HttpResponseBuilder().status(404).json(error("Phien FaceID khong ton tai hoac da het han"));
        }
        long remaining = Math.max(0, FaceAuthStore.TTL_MS - (System.currentTimeMillis() - session.createdAt));
        String body = "{"
            + "\"session\":\"" + JsonUtil.escape(session.id) + "\","
            + "\"action\":\"" + JsonUtil.escape(session.action) + "\","
            + "\"status\":\"" + JsonUtil.escape(session.status) + "\","
            + "\"expiresIn\":" + (remaining / 1000)
            + "}";
        return new HttpResponseBuilder().status(200).json(body);
    }

    public HttpResponseBuilder desktopVerify(HttpRequestParser req) {
        String body = req.getBody();
        String id = JsonUtil.getString(body, "session");
        FaceAuthStore.DesktopSession session = FaceAuthStore.getDesktopSession(id);
        if (session == null) {
            return new HttpResponseBuilder().status(404).json(error("Phien FaceID khong ton tai hoac da het han"));
        }
        if (!"WAITING".equals(session.status)) {
            return new HttpResponseBuilder().status(409).json(error("Phien FaceID da duoc xu ly"));
        }

        double[] descriptor = JsonUtil.getDoubleArray(body, "descriptor");
        Double livenessBoxed = JsonUtil.getDouble(body, "livenessScore");
        Double detectionBoxed = JsonUtil.getDouble(body, "detectionScore");
        double liveness = livenessBoxed == null ? 0 : livenessBoxed;
        double detection = detectionBoxed == null ? 0 : detectionBoxed;
        String image = JsonUtil.getString(body, "image");

        if (!validFacePayload(descriptor, liveness, detection)) {
            FaceAuthStore.errorDesktopSession(id, "Khuon mat chua dat nguong chat luong/liveness");
            return new HttpResponseBuilder().status(400)
                .json(error("Khuon mat chua dat nguong chat luong/liveness"));
        }
        if (!validImage(image)) {
            FaceAuthStore.errorDesktopSession(id, "Anh khuon mat khong hop le");
            return new HttpResponseBuilder().status(400).json(error("Anh khuon mat khong hop le"));
        }

        // Challenge nội bộ của phiên desktop chỉ dùng một lần.
        FaceAuthStore.Challenge challenge =
            FaceAuthStore.consumeChallenge(session.challengeId, session.username, session.action);
        if (challenge == null) {
            FaceAuthStore.errorDesktopSession(id, "Challenge da het han");
            return new HttpResponseBuilder().status(400).json(error("Challenge da het han"));
        }

        FaceRecognitionService.Result result =
            FaceRecognitionService.verifyOrEnroll(session.username, descriptor, liveness, image);
        if (!result.verified) {
            FaceAuthStore.errorDesktopSession(id, result.message);
            String fail = "{"
                + "\"verified\":false,"
                + "\"distance\":" + String.format(java.util.Locale.US, "%.6f", result.distance) + ","
                + "\"message\":\"" + JsonUtil.escape(result.message) + "\""
                + "}";
            return new HttpResponseBuilder().status(400).json(fail);
        }

        String token = FaceAuthStore.issueFaceToken(
            session.username, session.action, result.score, liveness, image);
        FaceAuthStore.completeDesktopSession(id, token, result.score, result.distance, result.message);

        String ok = "{"
            + "\"verified\":true,"
            + "\"enrolled\":" + result.enrolled + ","
            + "\"score\":" + String.format(java.util.Locale.US, "%.6f", result.score) + ","
            + "\"distance\":" + String.format(java.util.Locale.US, "%.6f", result.distance) + ","
            + "\"message\":\"" + JsonUtil.escape(result.message) + "\""
            + "}";
        return new HttpResponseBuilder().status(200).json(ok);
    }

    public HttpResponseBuilder desktopResult(HttpRequestParser req) {
        User user = auth(req);
        if (user == null) return new HttpResponseBuilder().status(401).json(error("Chua dang nhap"));

        String id = req.getParams().get("session");
        FaceAuthStore.DesktopSession session = FaceAuthStore.getDesktopSession(id);
        if (session == null || !session.username.equals(user.getUsername())) {
            return new HttpResponseBuilder().status(404)
                .json(error("Phien FaceID khong ton tai hoac khong thuoc nguoi dung"));
        }

        String body = "{"
            + "\"session\":\"" + JsonUtil.escape(session.id) + "\","
            + "\"status\":\"" + JsonUtil.escape(session.status) + "\","
            + "\"message\":\"" + JsonUtil.escape(session.message) + "\","
            + "\"score\":" + String.format(java.util.Locale.US, "%.6f", session.score) + ","
            + "\"distance\":" + String.format(java.util.Locale.US, "%.6f", session.distance) + ","
            + "\"faceToken\":\"" + JsonUtil.escape(session.faceToken == null ? "" : session.faceToken) + "\""
            + "}";
        return new HttpResponseBuilder().status(200).json(body);
    }
}
