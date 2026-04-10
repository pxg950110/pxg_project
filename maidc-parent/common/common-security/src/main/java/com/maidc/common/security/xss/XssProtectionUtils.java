package com.maidc.common.security.xss;

import java.util.regex.Pattern;

/**
 * XSS 防护工具
 * 提供输入净化和 HTML 转义功能
 */
public final class XssProtectionUtils {

    private XssProtectionUtils() {}

    private static final Pattern[] XSS_PATTERNS = {
            Pattern.compile("<script[^>]*>[\\s\\S]*?</script>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("javascript\\s*:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<iframe[^>]*>[\\s\\S]*?</iframe>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<object[^>]*>[\\s\\S]*?</object>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<embed[^>]*>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("eval\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("vbscript\\s*:", Pattern.CASE_INSENSITIVE),
    };

    /**
     * 检测输入是否包含潜在的 XSS 攻击向量
     * @return true 如果检测到可疑内容
     */
    public static boolean containsXss(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * HTML 转义，防止 XSS 注入
     */
    public static String escapeHtml(String input) {
        if (input == null) {
            return null;
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;");
    }

    /**
     * 净化输入：移除危险标签和事件处理器
     */
    public static String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        String result = input;
        for (Pattern pattern : XSS_PATTERNS) {
            result = pattern.matcher(result).replaceAll("");
        }
        return result.trim();
    }
}
