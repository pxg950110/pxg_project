package com.maidc.common.security.desensitize;

/**
 * 数据脱敏工具类
 * 提供常见敏感字段的脱敏方法
 */
public final class DesensitizeUtils {

    private DesensitizeUtils() {}

    /**
     * 姓名脱敏: 张三 → 张* 或 欧阳修 → 欧阳*
     */
    public static String name(String name) {
        if (name == null || name.length() <= 1) {
            return name;
        }
        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }
        return name.charAt(0) + "*" + name.substring(2);
    }

    /**
     * 身份证号脱敏: 110101199001011234 → 110101****1234
     */
    public static String idCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 6) + "****" + idCard.substring(idCard.length() - 4);
    }

    /**
     * 手机号脱敏: 13800138000 → 138****8000
     */
    public static String phone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 邮箱脱敏: admin@example.com → a***@example.com
     */
    public static String email(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        if (local.length() <= 1) {
            return local + "@" + parts[1];
        }
        return local.charAt(0) + "***@" + parts[1];
    }

    /**
     * 地址脱敏: 北京市朝阳区xxx路 → 北京市朝阳区***
     */
    public static String address(String address) {
        if (address == null || address.length() <= 6) {
            return address;
        }
        return address.substring(0, 6) + "***";
    }

    /**
     * 银行卡号脱敏: 6222021234567890 → 6222****7890
     */
    public static String bankCard(String cardNo) {
        if (cardNo == null || cardNo.length() < 8) {
            return cardNo;
        }
        return cardNo.substring(0, 4) + "****" + cardNo.substring(cardNo.length() - 4);
    }

    /**
     * 通用脱敏: 保留前n后m位
     */
    public static String mask(String value, int keepPrefix, int keepSuffix) {
        if (value == null) {
            return null;
        }
        if (value.length() <= keepPrefix + keepSuffix) {
            return value;
        }
        String prefix = value.substring(0, keepPrefix);
        String suffix = keepSuffix > 0 ? value.substring(value.length() - keepSuffix) : "";
        int maskLen = value.length() - keepPrefix - keepSuffix;
        return prefix + "*".repeat(maskLen) + suffix;
    }
}
