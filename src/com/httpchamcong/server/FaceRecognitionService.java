package com.httpchamcong.server;

import com.httpchamcong.dao.FaceProfileDAO;
import com.httpchamcong.model.FaceProfile;

/**
 * So khớp descriptor 128 chiều do face-api.js trích xuất từ camera.
 * Lần đầu chưa có profile: tự đăng ký mẫu sinh trắc học.
 * Các lần sau: so khoảng cách Euclidean với mẫu đã lưu.
 */
public class FaceRecognitionService {

    public static final double MATCH_THRESHOLD = 0.58;
    public static final int DESCRIPTOR_SIZE = 128;

    public static class Result {
        public final boolean verified;
        public final boolean enrolled;
        public final boolean matched;
        public final double distance;
        public final double score;
        public final String message;

        public Result(boolean verified, boolean enrolled, boolean matched,
                      double distance, double score, String message) {
            this.verified = verified;
            this.enrolled = enrolled;
            this.matched = matched;
            this.distance = distance;
            this.score = score;
            this.message = message;
        }

        public static Result fail(String message) {
            return new Result(false, false, false, 1.0, 0.0, message);
        }
    }

    public static Result verifyOrEnroll(String username, double[] descriptor,
                                        double liveness, String imageData) {
        if (username == null || username.isBlank()) {
            return Result.fail("Khong xac dinh duoc nguoi dung");
        }
        if (!validDescriptor(descriptor)) {
            return Result.fail("Du lieu khuon mat khong hop le (can 128 chieu)");
        }

        FaceProfile profile = FaceProfileDAO.find(username);
        if (profile == null || !validDescriptor(profile.getDescriptor())) {
            boolean saved = FaceProfileDAO.save(username, descriptor, imageData, liveness);
            if (!saved) return Result.fail("Khong luu duoc mau khuon mat");
            return new Result(true, true, true, 0.0, 1.0,
                "Da dang ky khuon mat lan dau. Tu lan sau he thong se doi chieu.");
        }

        double distance = euclidean(descriptor, profile.getDescriptor());
        double score = Math.max(0.0, Math.min(1.0, 1.0 - distance));
        if (distance > MATCH_THRESHOLD) {
            return new Result(false, false, false, distance, score,
                "Khuon mat khong khop voi mau da dang ky");
        }

        FaceProfileDAO.touchVerified(username);
        return new Result(true, false, true, distance, score,
            "Xac thuc khuon mat thanh cong");
    }

    public static boolean validDescriptor(double[] descriptor) {
        if (descriptor == null || descriptor.length != DESCRIPTOR_SIZE) return false;
        for (double v : descriptor) {
            if (Double.isNaN(v) || Double.isInfinite(v) || Math.abs(v) > 3.0) return false;
        }
        return true;
    }

    private static double euclidean(double[] a, double[] b) {
        int n = Math.min(a.length, b.length);
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            double d = a[i] - b[i];
            sum += d * d;
        }
        return Math.sqrt(sum);
    }
}
