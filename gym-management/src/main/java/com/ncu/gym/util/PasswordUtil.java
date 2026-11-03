package com.ncu.gym.util;

import com.ncu.gym.common.BizException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 口令摘要工具：SHA-256 加盐（盐值固定为 gym@，与演示数据保持一致）
 */
public final class PasswordUtil {

    private static final String SALT = "gym@";

    private PasswordUtil() {
    }

    public static String encode(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest((SALT + raw).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new BizException(500, "口令摘要计算失败");
        }
    }

    public static boolean matches(String raw, String encoded) {
        return encoded != null && encoded.equalsIgnoreCase(encode(raw));
    }
}
