package com.httpchamcong.util;

public class JsonUtil {

    /** Escape một chuỗi để chèn an toàn vào JSON (dấu ", \, newline...). */
    public static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String getString(String json, String key) {
        if (json == null || key == null) return null;

        String pattern = "\"" + key + "\"";
        int i = json.indexOf(pattern);
        if (i < 0) return null;

        int colon = json.indexOf(':', i + pattern.length());
        if (colon < 0) return null;

        int start = json.indexOf('"', colon + 1);
        if (start < 0) return null;

        int end = json.indexOf('"', start + 1);
        if (end < 0) return null;

        return json.substring(start + 1, end);
    }

    public static void main(String[] args) {
        String json = "{\"username\":\"chamnee\",\"password\":\"Chamnee@2025\"}";
        System.out.println("username = " + getString(json, "username"));
        System.out.println("password = " + getString(json, "password"));
    }
} 
