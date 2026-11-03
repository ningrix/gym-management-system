package com.ncu.gym.controller;

import com.ncu.gym.common.BizException;
import com.ncu.gym.common.Result;
import com.ncu.gym.entity.CourseSchedule;
import com.ncu.gym.entity.MemberCard;
import com.ncu.gym.entity.Waitlist;
import com.ncu.gym.service.CourseScheduleService;
import com.ncu.gym.service.MemberCardService;
import com.ncu.gym.service.WaitlistService;
import com.ncu.gym.util.LoginUser;
import com.ncu.gym.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 候补队列：满员课程排队，名额释放后按顺序递补。
 */
@RestController
@RequestMapping("/api/reservations/waitlist")
@RequiredArgsConstructor
public class WaitlistController {

    private final WaitlistService waitlistService;
    private final CourseScheduleService scheduleService;
    private final MemberCardService memberCardService;

    /** 排候补：课程满员时可进入队列，顺序递补 */
    @PostMapping("/{scheduleId}")
    public Result<Waitlist> join(@PathVariable Long scheduleId) {
        LoginUser login = UserContext.require();
        if (!"MEMBER".equals(login.getRole()) || login.getBizId() == null) {
            throw new BizException(403, "只有会员可以排候补");
        }
        CourseSchedule schedule = scheduleService.getById(scheduleId);
        if (schedule == null) {
            throw new BizException("课程不存在");
        }
        if (!"WAITING".equals(schedule.getStatus())) {
            throw new BizException("仅待开课的课程可以候补");
        }
        long dup = waitlistService.count(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Waitlist>()
                .eq("schedule_id", scheduleId).eq("member_id", login.getBizId())
                .eq("status", "WAITING"));
        if (dup > 0) {
            throw new BizException("您已在本课程候补队列中");
        }
        MemberCard card = memberCardService.pickUsableCard(login.getBizId());

        Waitlist w = new Waitlist();
        w.setScheduleId(scheduleId);
        w.setMemberId(login.getBizId());
        w.setMemberCardId(card.getId());
        w.setStatus("WAITING");
        w.setEnqueueTime(LocalDateTime.now());
        w.setCreateTime(LocalDateTime.now());
        waitlistService.save(w);
        return Result.success(w);
    }

    @DeleteMapping("/{scheduleId}")
    public Result<Void> leave(@PathVariable Long scheduleId) {
        LoginUser login = UserContext.require();
        Waitlist w = waitlistService.getOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Waitlist>()
                .eq("schedule_id", scheduleId).eq("member_id", login.getBizId())
                .eq("status", "WAITING"));
        if (w == null) {
            throw new BizException("您不在该课程的候补队列中");
        }
        waitlistService.markCancelled(w.getId());
        return Result.success();
    }

    @GetMapping("/my")
    public Result<List<Waitlist>> my() {
        LoginUser login = UserContext.require();
        if (!"MEMBER".equals(login.getRole())) {
            throw new BizException(403, "该接口仅供会员使用");
        }
        return Result.success(waitlistService.listByMember(login.getBizId()));
    }
}
