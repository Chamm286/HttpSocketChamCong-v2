package com.httpchamcong.model;

import com.httpchamcong.util.JsonUtil;

public class User {
    private String username;
    private String password;
    private String fullName;
    private String role;
    private String department;
    private String phone;
    private String avatarUrl;

    public User(String username, String password, String fullName, String role) {
        this(username, password, fullName, role, "", "", "");
    }

    public User(String username, String password, String fullName, String role,
                String department, String phone, String avatarUrl) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.department = department == null ? "" : department;
        this.phone = phone == null ? "" : phone;
        this.avatarUrl = avatarUrl == null ? "" : avatarUrl;
    }

    public String getUsername()   { return username; }
    public String getPassword()   { return password; }
    public String getFullName()   { return fullName; }
    public String getRole()       { return role; }
    public String getDepartment() { return department; }
    public String getPhone()      { return phone; }
    public String getAvatarUrl()  { return avatarUrl; }

    public String toJson() {
        return String.format(
            "{\"username\":\"%s\",\"fullName\":\"%s\",\"role\":\"%s\"," +
            "\"department\":\"%s\",\"phone\":\"%s\",\"avatarUrl\":\"%s\"}",
            JsonUtil.escape(username),
            JsonUtil.escape(fullName),
            JsonUtil.escape(role),
            JsonUtil.escape(department),
            JsonUtil.escape(phone),
            JsonUtil.escape(avatarUrl)
        );
    }

    @Override
    public String toString() {
        return "User{" + username + ", " + fullName + ", " + role +
               ", " + department + ", " + phone + "}";
    }
}
