package com.httpchamcong.server;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kho challenge / token một lần cho FaceID.
 *
 * Luồng:
 *  1. Client xin challenge ngắn hạn.
 *  2. Client quét camera, gửi descriptor + liveness + challenge.
 *  3. Server xác thực và cấp faceToken dùng một lần.
 *  4. /api/checkin hoặc /api/checkout tiêu thụ faceToken, không tin cờ faceVerified từ client.
 */
public class FaceAuthStore {

    public static final long TTL_MS = 120_000;
    public static final double LIVENESS_MIN = 0.60;
    public static final double DETECTION_MIN = 0.50;

    public static class Challenge {
        public final String id;
        public final String username;
        public final String action;
        public final long createdAt;

        Challenge(String id, String username, String action) {
            this.id = id;
            this.username = username;
            this.action = action;
            this.createdAt = System.currentTimeMillis();
        }
    }

    public static class Proof {
        public final String username;
        public final String action;
        public final double score;
        public final double liveness;
        public final String image;
        public final long createdAt;

        Proof(String username, String action, double score, double liveness, String image) {
            this.username = username;
            this.action = action;
            this.score = score;
            this.liveness = liveness;
            this.image = image == null ? "" : image;
            this.createdAt = System.currentTimeMillis();
        }
    }

    public static class DesktopSession {
        public final String id;
        public final String username;
        public final String action;
        public final String challengeId;
        public final long createdAt;
        public volatile String status = "WAITING";
        public volatile String message = "";
        public volatile double score = 0;
        public volatile double distance = 1;
        public volatile String faceToken = null;

        DesktopSession(String id, String username, String action, String challengeId) {
            this.id = id;
            this.username = username;
            this.action = action;
            this.challengeId = challengeId;
            this.createdAt = System.currentTimeMillis();
        }
    }

    private static final Map<String, Challenge> challenges = new ConcurrentHashMap<>();
    private static final Map<String, Proof> tokens = new ConcurrentHashMap<>();
    private static final Map<String, DesktopSession> desktopSessions = new ConcurrentHashMap<>();

    private static boolean expired(long createdAt) {
        return System.currentTimeMillis() - createdAt > TTL_MS;
    }

    private static void evictExpired() {
        challenges.entrySet().removeIf(e -> expired(e.getValue().createdAt));
        tokens.entrySet().removeIf(e -> expired(e.getValue().createdAt));
        desktopSessions.entrySet().removeIf(e -> expired(e.getValue().createdAt));
    }

    public static Challenge createChallenge(String username, String action) {
        evictExpired();
        String id = UUID.randomUUID().toString();
        Challenge challenge = new Challenge(id, username, action);
        challenges.put(id, challenge);
        return challenge;
    }

    public static Challenge consumeChallenge(String id, String username, String action) {
        evictExpired();
        if (id == null) return null;
        Challenge challenge = challenges.remove(id);
        if (challenge == null || expired(challenge.createdAt)) return null;
        if (!challenge.username.equals(username)) return null;
        if (action != null && !challenge.action.equals(action)) return null;
        return challenge;
    }

    /** Trả về token id và lưu Proof tương ứng. */
    public static String issueFaceToken(String username, String action, double score,
                                        double liveness, String image) {
        evictExpired();
        String token = UUID.randomUUID().toString();
        tokens.put(token, new Proof(username, action, score, liveness, image));
        return token;
    }

    public static Proof consumeToken(String username, String action, String token) {
        evictExpired();
        if (token == null || token.isBlank()) return null;
        Proof proof = tokens.remove(token);
        if (proof == null || expired(proof.createdAt)) return null;
        if (!proof.username.equals(username)) return null;
        if (action != null && !proof.action.equals(action)) return null;
        return proof;
    }

    public static DesktopSession createDesktopSession(String username, String action) {
        evictExpired();
        Challenge challenge = createChallenge(username, action);
        String id = UUID.randomUUID().toString();
        DesktopSession session = new DesktopSession(id, username, action, challenge.id);
        desktopSessions.put(id, session);
        return session;
    }

    public static DesktopSession getDesktopSession(String id) {
        evictExpired();
        if (id == null) return null;
        DesktopSession session = desktopSessions.get(id);
        if (session == null || expired(session.createdAt)) return null;
        return session;
    }

    public static void errorDesktopSession(String id, String message) {
        DesktopSession session = getDesktopSession(id);
        if (session == null) return;
        session.status = "ERROR";
        session.message = message == null ? "FaceID that bai" : message;
    }

    public static void completeDesktopSession(String id, String faceToken, double score,
                                              double distance, String message) {
        DesktopSession session = getDesktopSession(id);
        if (session == null) return;
        session.score = score;
        session.distance = distance;
        if (faceToken == null || faceToken.isBlank()) {
            session.status = "ERROR";
            session.message = "FaceID khong tao duoc token";
            return;
        }
        session.status = "DONE";
        session.faceToken = faceToken;
        session.message = message == null ? "" : message;
    }
}
