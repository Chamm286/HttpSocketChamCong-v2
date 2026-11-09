package com.httpchamcong.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Cac ham kiem tra du lieu dau vao dung chung cho controller. */
public final class ValidationUtil {

    private ValidationUtil() {}

    /** Kiem tra chuoi ngay theo dinh dang ISO yyyy-MM-dd. */
    public static boolean isIsoDate(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            LocalDate.parse(value);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /** Chi cho phep 2 vai tro co trong database. */
    public static boolean isValidRole(String role) {
        return "ADMIN".equals(role) || "USER".equals(role);
    }

    /** Trang thai don nghi chi duoc phep duyet hoac tu choi. */
    public static boolean isValidLeaveStatus(String status) {
        return "APPROVED".equals(status) || "REJECTED".equals(status);
    }
}
