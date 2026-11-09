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

    /**
     * Lấy giá trị thô (raw) của một key: chuỗi, số, boolean, null.
     * Dùng cho các parser nhỏ gọn của đồ án, không phải JSON parser đầy đủ.
     */
    public static String getRaw(String json, String key) {
        if (json == null || key == null) return null;

        String pattern = "\"" + key + "\"";
        int i = json.indexOf(pattern);
        if (i < 0) return null;

        int colon = json.indexOf(':', i + pattern.length());
        if (colon < 0) return null;

        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length()) return null;

        char first = json.charAt(start);
        if (first == '"') {
            StringBuilder sb = new StringBuilder();
            for (int p = start + 1; p < json.length(); p++) {
                char c = json.charAt(p);
                if (c == '\\' && p + 1 < json.length()) {
                    char n = json.charAt(++p);
                    switch (n) {
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        default: sb.append(n);
                    }
                } else if (c == '"') {
                    return sb.toString();
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) break;
            end++;
        }
        return json.substring(start, end);
    }

    public static Double getDouble(String json, String key) {
        String raw = getRaw(json, key);
        if (raw == null || raw.isEmpty() || "null".equalsIgnoreCase(raw)) return null;
        try { return Double.valueOf(raw); }
        catch (NumberFormatException e) { return null; }
    }

    public static Integer getInt(String json, String key) {
        String raw = getRaw(json, key);
        if (raw == null || raw.isEmpty() || "null".equalsIgnoreCase(raw)) return null;
        try { return Integer.valueOf(raw); }
        catch (NumberFormatException e) {
            try { return (int) Double.parseDouble(raw); }
            catch (NumberFormatException ex) { return null; }
        }
    }

    public static Boolean getBoolean(String json, String key) {
        String raw = getRaw(json, key);
        if (raw == null || raw.isEmpty() || "null".equalsIgnoreCase(raw)) return null;
        if ("true".equalsIgnoreCase(raw) || "1".equals(raw)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(raw) || "0".equals(raw)) return Boolean.FALSE;
        return null;
    }

    /** Parse mảng số JSON, ví dụ: "descriptor":[0.12,-0.4,...]. */
    public static double[] getDoubleArray(String json, String key) {
        if (json == null || key == null) return null;
        String pattern = "\"" + key + "\"";
        int i = json.indexOf(pattern);
        if (i < 0) return null;
        int colon = json.indexOf(':', i + pattern.length());
        if (colon < 0) return null;
        int start = json.indexOf('[', colon + 1);
        if (start < 0) return null;
        int end = json.indexOf(']', start + 1);
        if (end < 0) return null;

        String body = json.substring(start + 1, end).trim();
        if (body.isEmpty()) return new double[0];
        String[] parts = body.split(",");
        double[] out = new double[parts.length];
        for (int p = 0; p < parts.length; p++) {
            try {
                out[p] = Double.parseDouble(parts[p].trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return out;
    }

    public static void main(String[] args) {
        String json = "{\"username\":\"chamnee\",\"password\":\"Chamnee@2025\"}";
        System.out.println("username = " + getString(json, "username"));
        System.out.println("password = " + getString(json, "password"));
    }
} 
