package com.ncu.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ncu.gym.common.BizException;
import com.ncu.gym.common.Result;
import com.ncu.gym.entity.Member;
import com.ncu.gym.entity.MemberAlert;
import com.ncu.gym.service.MemberAlertService;
import com.ncu.gym.service.MemberService;
import com.ncu.gym.util.LoginUser;
import com.ncu.gym.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 会员预警（到期 / 流失）：管理端统一查看与回填处理结果，会员查看本人预警。
 */
@RestController
@RequestMapping("/api/member-alerts")
@RequiredArgsConstructor
public class MemberAlertController {

    private final MemberAlertService alertService;
    private final MemberService memberService;

    @GetMapping
    public Result<IPage<Map<String, Object>>> list(@RequestParam(required = false) String alertType,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(defaultValue = "1") long pageNum,
                                                   @RequestParam(defaultValue = "10") long pageSize) {
        LoginUser login = UserContext.require();
        QueryWrapper<MemberAlert> qw = new QueryWrapper<>();
        if ("ADMIN".equals(login.getRole())) {
            if (alertType != null && !alertType.isBlank()) {
                qw.eq("alert_type", alertType);
            }
            if (status != null && !status.isBlank()) {
                qw.eq("status", status);
            }
        } else {
            qw.eq("member_id", login.getBizId());
            qw.eq("status", "OPEN");
        }
        qw.orderByDesc("create_time");
        IPage<MemberAlert> page = alertService.page(new Page<>(pageNum, pageSize), qw);
        Page<Map<String, Object>> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(a -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", a.getId());
            row.put("memberId", a.getMemberId());
            row.put("alertType", a.getAlertType());
            row.put("content", a.getContent());
            row.put("status", a.getStatus());
            row.put("createTime", a.getCreateTime());
            Member m = memberService.getById(a.getMemberId());
            row.put("memberName", m == null ? "-" : m.getName());
            row.put("memberNo", m == null ? "-" : m.getMemberNo());
            row.put("memberPhone", m == null ? "-" : m.getPhone());
            return row;
        }).collect(java.util.stream.Collectors.toList()));
        return Result.success(result);
    }

    @PutMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以处理预警");
        }
        MemberAlert alert = alertService.getById(id);
        if (alert == null) {
            throw new BizException("预警不存在");
        }
        MemberAlert update = new MemberAlert();
        update.setId(id);
        update.setStatus("HANDLED");
        update.setHandleNote(body.getOrDefault("note", "已处理"));
        update.setHandleTime(LocalDateTime.now());
        alertService.updateById(update);
        return Result.success();
    }
}
