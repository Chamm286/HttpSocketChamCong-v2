package com.httpchamcong.model;

public class WorkShift {
    private int id;
    private String username;
    private String fullName;
    private String shiftDate;
    private String shiftType;
    private String note;

    public WorkShift(String username, String shiftDate, String shiftType, String note) {
        this.username = username;
        this.fullName = "";
        this.shiftDate = shiftDate;
        this.shiftType = shiftType;
        this.note = note;
    }

    public WorkShift(int id, String username, String fullName,
                     String shiftDate, String shiftType, String note) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.shiftDate = shiftDate;
        this.shiftType = shiftType;
        this.note = note;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getShiftDate() { return shiftDate; }
    public String getShiftType() { return shiftType; }
    public String getNote() { return note; }

    public String toJson() {
        return String.format(
            "{\"id\":%d,\"username\":\"%s\",\"fullName\":\"%s\"," +
            "\"shiftDate\":\"%s\",\"shiftType\":\"%s\",\"note\":\"%s\"}",
            id, username,
            fullName == null ? "" : fullName,
            shiftDate, shiftType,
            note == null ? "" : note.replace("\"", "'")
        );
    }
}