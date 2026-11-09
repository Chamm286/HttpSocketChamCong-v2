package com.httpchamcong.model;

import com.httpchamcong.util.JsonUtil;

public class LeaveRequest {
    private int id;
    private String username;
    private String fullName;
    private String startDate;
    private String endDate;
    private String reason;
    private String status;
    private String leaveType;

    public LeaveRequest(String username, String startDate, String endDate, String reason) {
        this(username, startDate, endDate, reason, "NGHI_PHEP");
    }

    public LeaveRequest(String username, String startDate, String endDate,
                        String reason, String leaveType) {
        this.username = username;
        this.fullName = "";
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.status = "PENDING";
        this.leaveType = leaveType == null ? "NGHI_PHEP" : leaveType;
    }

    public LeaveRequest(int id, String username, String fullName,
                        String startDate, String endDate, String reason, String status) {
        this(id, username, fullName, startDate, endDate, reason, status, "NGHI_PHEP");
    }

    public LeaveRequest(int id, String username, String fullName,
                        String startDate, String endDate, String reason,
                        String status, String leaveType) {
        this.id = id;
        this.username = username;
        this.fullName = fullName == null ? "" : fullName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.status = status;
        this.leaveType = leaveType == null ? "NGHI_PHEP" : leaveType;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
    public String getLeaveType() { return leaveType; }

    public String toJson() {
        return String.format(
            "{\"id\":%d,\"username\":\"%s\",\"fullName\":\"%s\"," +
            "\"startDate\":\"%s\",\"endDate\":\"%s\",\"reason\":\"%s\"," +
            "\"status\":\"%s\",\"leaveType\":\"%s\"}",
            id, JsonUtil.escape(username), JsonUtil.escape(fullName),
            JsonUtil.escape(startDate), JsonUtil.escape(endDate),
            JsonUtil.escape(reason),
            JsonUtil.escape(status),
            JsonUtil.escape(leaveType)
        );
    }
}
