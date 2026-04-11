package com.maidc.common.security.password;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * 密码策略校验器
 * 等保三级密码复杂度要求：
 * - 至少8位
 * - 包含大小写字母
 * - 包含数字
 * - 包含特殊字符
 */
@Slf4j
public final class PasswordPolicyValidator {

    private PasswordPolicyValidator() {}

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;

    private static final Pattern UPPER_CASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWER_CASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SPECIAL = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

    /**
     * 校验密码是否符合策略要求
     * @throws BusinessException 如果不符合要求
     */
    public static void validate(String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        if (password.length() < MIN_LENGTH) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        if (password.length() > MAX_LENGTH) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        if (!UPPER_CASE.matcher(password).find()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        if (!LOWER_CASE.matcher(password).find()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        if (!DIGIT.matcher(password).find()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        if (!SPECIAL.matcher(password).find()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }

    /**
     * 检查密码强度等级 (1-4)
     */
    public static int getStrength(String password) {
        if (password == null) return 0;
        int score = 0;
        if (password.length() >= 8) score++;
        if (UPPER_CASE.matcher(password).find() && LOWER_CASE.matcher(password).find()) score++;
        if (DIGIT.matcher(password).find()) score++;
        if (SPECIAL.matcher(password).find()) score++;
        return score;
    }
}
