package com.httpchamcong.model;

import com.httpchamcong.util.JsonUtil;

public class ChamCong {
    private String username;
    private String fullName;
    private String date;
    private String checkIn;
    private String checkOut;
    private Double checkinLat;
    private Double checkinLng;
    private boolean checkinFaceVerified;
    private Double checkinFaceScore;
    private Double checkinLiveness;
    private boolean checkoutFaceVerified;
    private Double checkoutFaceScore;
    private Double checkoutLiveness;
    private String note;

    public ChamCong(String username, String date, String checkIn, String checkOut) {
        this(username, "", date, checkIn, checkOut, null, null, false, null, null,
             false, null, null, "");
    }

    public ChamCong(String username, String fullName, String date, String checkIn, String checkOut) {
        this(username, fullName, date, checkIn, checkOut, null, null, false, null, null,
             false, null, null, "");
    }

    public ChamCong(String username, String fullName, String date, String checkIn, String checkOut,
                    Double checkinLat, Double checkinLng, boolean checkinFaceVerified,
                    Double checkinFaceScore, Double checkinLiveness,
                    boolean checkoutFaceVerified, Double checkoutFaceScore, Double checkoutLiveness,
                    String note) {
        this.username = username;
        this.fullName = fullName == null ? "" : fullName;
        this.date = date;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.checkinLat = checkinLat;
        this.checkinLng = checkinLng;
        this.checkinFaceVerified = checkinFaceVerified;
        this.checkinFaceScore = checkinFaceScore;
        this.checkinLiveness = checkinLiveness;
        this.checkoutFaceVerified = checkoutFaceVerified;
        this.checkoutFaceScore = checkoutFaceScore;
        this.checkoutLiveness = checkoutLiveness;
        this.note = note == null ? "" : note;
    }

    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getDate()     { return date; }
    public String getCheckIn()  { return checkIn; }
    public String getCheckOut() { return checkOut; }
    public Double getCheckinLat() { return checkinLat; }
    public Double getCheckinLng() { return checkinLng; }
    public boolean isCheckinFaceVerified() { return checkinFaceVerified; }
    public Double getCheckinFaceScore() { return checkinFaceScore; }
    public Double getCheckinLiveness() { return checkinLiveness; }
    public boolean isCheckoutFaceVerified() { return checkoutFaceVerified; }
    public Double getCheckoutFaceScore() { return checkoutFaceScore; }
    public Double getCheckoutLiveness() { return checkoutLiveness; }
    public String getNote() { return note; }
    public void setCheckOut(String checkOut) { this.checkOut = checkOut; }

    private String num(Double value) {
        return value == null ? "null" : String.valueOf(value);
    }

    public String toJson() {
        return String.format(
            "{\"username\":\"%s\",\"fullName\":\"%s\",\"date\":\"%s\"," +
            "\"checkIn\":\"%s\",\"checkOut\":\"%s\"," +
            "\"checkinLat\":%s,\"checkinLng\":%s," +
            "\"checkinFaceVerified\":%s,\"checkinFaceScore\":%s,\"checkinLiveness\":%s," +
            "\"checkoutFaceVerified\":%s,\"checkoutFaceScore\":%s,\"checkoutLiveness\":%s," +
            "\"note\":\"%s\"}",
            JsonUtil.escape(username),
            JsonUtil.escape(fullName),
            JsonUtil.escape(date),
            JsonUtil.escape(checkIn),
            JsonUtil.escape(checkOut),
            num(checkinLat),
            num(checkinLng),
            checkinFaceVerified ? "true" : "false",
            num(checkinFaceScore),
            num(checkinLiveness),
            checkoutFaceVerified ? "true" : "false",
            num(checkoutFaceScore),
            num(checkoutLiveness),
            JsonUtil.escape(note)
        );
    }

    @Override
    public String toString() {
        return username + " | " + fullName + " | " + date +
               " | " + checkIn + " -> " + (checkOut == null ? "?" : checkOut);
    }
}
