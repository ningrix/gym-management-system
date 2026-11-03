package com.ncu.gym.util;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录态：账号信息 + 业务主键（会员 id / 教练 id）
 */
@Data
@AllArgsConstructor
public class LoginUser {

    private Long userId;
    private String username;
    private String role;
    private String realName;
    private Long bizId;
}
