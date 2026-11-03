package com.ncu.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ncu.gym.common.BizException;
import com.ncu.gym.common.Result;
import com.ncu.gym.entity.Coach;
import com.ncu.gym.entity.Member;
import com.ncu.gym.entity.SysUser;
import com.ncu.gym.service.CoachService;
import com.ncu.gym.service.MemberService;
import com.ncu.gym.service.SysUserService;
import com.ncu.gym.util.LoginUser;
import com.ncu.gym.util.PasswordUtil;
import com.ncu.gym.util.TokenStore;
import com.ncu.gym.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录认证与个人中心
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService userService;
    private final MemberService memberService;
    private final CoachService coachService;
    private final TokenStore tokenStore;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) {
            throw new BizException("请输入账号与密码");
        }
        SysUser user = userService.getOne(new QueryWrapper<SysUser>().eq("username", username.trim()));
        if (user == null || !PasswordUtil.matches(password, user.getPassword())) {
            throw new BizException(401, "账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(403, "该账号已被停用，请联系管理员");
        }
        Long bizId = resolveBizId(user);
        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getRole(), user.getRealName(), bizId);
        String token = tokenStore.issue(loginUser);

        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setLastLoginTime(LocalDateTime.now());
        userService.updateById(update);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("role", user.getRole());
        data.put("realName", user.getRealName());
        data.put("bizId", bizId);
        return Result.success(data);
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "X-Token", required = false) String token) {
        tokenStore.remove(token);
        return Result.success();
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        LoginUser login = UserContext.require();
        Map<String, Object> data = new HashMap<>();
        data.put("userId", login.getUserId());
        data.put("username", login.getUsername());
        data.put("role", login.getRole());
        data.put("realName", login.getRealName());
        data.put("bizId", login.getBizId());
        if ("MEMBER".equals(login.getRole()) && login.getBizId() != null) {
            data.put("profile", memberService.getById(login.getBizId()));
        } else if ("COACH".equals(login.getRole()) && login.getBizId() != null) {
            data.put("profile", coachService.getById(login.getBizId()));
        }
        return Result.success(data);
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body) {
        LoginUser login = UserContext.require();
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.length() < 6) {
            throw new BizException("新密码长度不能少于 6 位");
        }
        SysUser user = userService.getById(login.getUserId());
        if (!PasswordUtil.matches(oldPassword, user.getPassword())) {
            throw new BizException("原密码不正确");
        }
        if (PasswordUtil.matches(newPassword, user.getPassword())) {
            throw new BizException("新密码不能与原密码相同");
        }
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPassword(PasswordUtil.encode(newPassword));
        userService.updateById(update);
        return Result.success();
    }

    private Long resolveBizId(SysUser user) {
        if ("MEMBER".equals(user.getRole())) {
            Member member = memberService.getOne(new QueryWrapper<Member>().eq("user_id", user.getId()));
            return member == null ? null : member.getId();
        }
        if ("COACH".equals(user.getRole())) {
            Coach coach = coachService.getOne(new QueryWrapper<Coach>().eq("user_id", user.getId()));
            return coach == null ? null : coach.getId();
        }
        return null;
    }
}
