package com.httpchamcong.model;

public class WorkNote {
    private int id;
    private String username;
    private String noteDate;
    private String content;
    private String plan;
    private String createdAt;
    private String fullName;

    public WorkNote() {}

    public WorkNote(String username, String noteDate, String content, String plan) {
        this.username = username;
        this.noteDate = noteDate;
        this.content = content;
        this.plan = plan;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNoteDate() { return noteDate; }
    public void setNoteDate(String noteDate) { this.noteDate = noteDate; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String toJson() {
        return "{"
            + "\"id\":" + id + ","
            + "\"username\":\"" + escape(username) + "\","
            + "\"fullName\":\"" + escape(fullName != null ? fullName : username) + "\","
            + "\"noteDate\":\"" + escape(noteDate) + "\","
            + "\"content\":\"" + escape(content) + "\","
            + "\"plan\":\"" + escape(plan != null ? plan : "") + "\","
            + "\"createdAt\":\"" + escape(createdAt != null ? createdAt : "") + "\""
            + "}";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
