package com.httpchamcong.model;

public class FaceProfile {
    private final String username;
    private final double[] descriptor;
    private final String imageData;
    private final double livenessScore;
    private final String enrolledAt;
    private final String lastVerifiedAt;

    public FaceProfile(String username, double[] descriptor, String imageData,
                       double livenessScore, String enrolledAt, String lastVerifiedAt) {
        this.username = username;
        this.descriptor = descriptor;
        this.imageData = imageData == null ? "" : imageData;
        this.livenessScore = livenessScore;
        this.enrolledAt = enrolledAt == null ? "" : enrolledAt;
        this.lastVerifiedAt = lastVerifiedAt == null ? "" : lastVerifiedAt;
    }

    public String getUsername() { return username; }
    public double[] getDescriptor() { return descriptor; }
    public String getImageData() { return imageData; }
    public double getLivenessScore() { return livenessScore; }
    public String getEnrolledAt() { return enrolledAt; }
    public String getLastVerifiedAt() { return lastVerifiedAt; }
}
