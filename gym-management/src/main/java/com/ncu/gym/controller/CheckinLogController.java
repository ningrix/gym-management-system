package com.ncu.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ncu.gym.common.BizException;
import com.ncu.gym.common.Result;
import com.ncu.gym.entity.CheckinLog;
import com.ncu.gym.entity.Member;
import com.ncu.gym.service.CheckinLogService;
import com.ncu.gym.service.MemberService;
import com.ncu.gym.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 到店记录：区分团课签到（COURSE）与自助训练（SELF），是到店率口径的唯一数据源。
 */
@RestController
@RequestMapping("/api/checkin-logs")
@RequiredArgsConstructor
public class CheckinLogController {

    private final CheckinLogService checkinLogService;
    private final MemberService memberService;

    /** 前台代录自助入场 */
    @PostMapping
    public Result<CheckinLog> create(@RequestBody Map<String, Object> body) {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以登记到店记录");
        }
        Long memberId = body.get("memberId") == null ? null : Long.valueOf(body.get("memberId").toString());
        Member member = memberService.getById(memberId);
        if (member == null) {
            throw new BizException("会员不存在");
        }
        CheckinLog log = new CheckinLog();
        log.setMemberId(memberId);
        log.setCheckinType("SELF");
        log.setCreateTime(LocalDateTime.now());
        checkinLogService.save(log);
        return Result.success(log);
    }

    @GetMapping("/my")
    public Result<List<CheckinLog>> my() {
        var login = com.ncu.gym.util.UserContext.require();
        if (!"MEMBER".equals(login.getRole()) && !"COACH".equals(login.getRole())) {
            throw new BizException(403, "该接口面向会员与教练");
        }
        return Result.success(checkinLogService.list(new QueryWrapper<CheckinLog>()
                .eq("member_id", login.getBizId()).orderByDesc("create_time")));
    }

    /** 管理端到店记录（含会员信息） */
    @GetMapping
    public Result<IPage<Map<String, Object>>> list(@RequestParam(defaultValue = "1") long pageNum,
                                                   @RequestParam(defaultValue = "10") long pageSize) {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以查看到店记录");
        }
        IPage<CheckinLog> page = checkinLogService.page(new Page<>(pageNum, pageSize),
                new QueryWrapper<CheckinLog>().orderByDesc("create_time"));
        Page<Map<String, Object>> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(log -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", log.getId());
            row.put("checkinType", log.getCheckinType());
            row.put("createTime", log.getCreateTime());
            Member m = memberService.getById(log.getMemberId());
            row.put("memberName", m == null ? "-" : m.getName());
            row.put("memberNo", m == null ? "-" : m.getMemberNo());
            return row;
        }).collect(java.util.stream.Collectors.toList()));
        return Result.success(result);
    }
}
