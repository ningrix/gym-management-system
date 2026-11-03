package com.ncu.gym.util;

import com.ncu.gym.common.BizException;

public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static LoginUser require() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BizException(401, "登录状态已失效，请重新登录");
        }
        return user;
    }

    public static boolean isAdmin() {
        LoginUser u = HOLDER.get();
        return u != null && "ADMIN".equals(u.getRole());
    }

    public static boolean isMember() {
        LoginUser u = HOLDER.get();
        return u != null && "MEMBER".equals(u.getRole());
    }

    public static boolean isCoach() {
        LoginUser u = HOLDER.get();
        return u != null && "COACH".equals(u.getRole());
    }

    public static void clear() {
        HOLDER.remove();
    }
}
