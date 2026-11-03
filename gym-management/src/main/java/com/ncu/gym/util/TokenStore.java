package com.ncu.gym.util;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 令牌存储：登录成功后下发 UUID 令牌，服务端内存保存登录态
 */
@Component
public class TokenStore {

    private final Map<String, LoginUser> tokens = new ConcurrentHashMap<>();

    public String issue(LoginUser user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, user);
        return token;
    }

    public LoginUser get(String token) {
        return token == null ? null : tokens.get(token);
    }

    public void remove(String token) {
        if (token != null) {
            tokens.remove(token);
        }
    }
}
