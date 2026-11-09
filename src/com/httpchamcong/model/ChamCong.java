package com.httpchamcong.model;

import com.httpchamcong.util.JsonUtil;

public class ChamCong {
    private String username;
    private String fullName;
    private String date;
    private String checkIn;
    private String checkOut;

    public ChamCong(String username, String date, String checkIn, String checkOut) {
        this.username = username;
        this.fullName = "";
        this.date = date;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }

    public ChamCong(String username, String fullName, String date, String checkIn, String checkOut) {
        this.username = username;
        this.fullName = fullName;
        this.date = date;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }

    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getDate()     { return date; }
    public String getCheckIn()  { return checkIn; }
    public String getCheckOut() { return checkOut; }
    public void setCheckOut(String checkOut) { this.checkOut = checkOut; }

    public String toJson() {
        return String.format(
            "{\"username\":\"%s\",\"fullName\":\"%s\",\"date\":\"%s\",\"checkIn\":\"%s\",\"checkOut\":\"%s\"}",
            JsonUtil.escape(username),
            JsonUtil.escape(fullName),
            JsonUtil.escape(date),
            JsonUtil.escape(checkIn),
            JsonUtil.escape(checkOut)
        );
    }

    @Override
    public String toString() {
        return username + " | " + fullName + " | " + date +
               " | " + checkIn + " → " + (checkOut == null ? "?" : checkOut);
    }
}