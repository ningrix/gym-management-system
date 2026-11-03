package com.ncu.gym.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ncu.gym.entity.CheckinLog;
import com.ncu.gym.entity.CourseSchedule;
import com.ncu.gym.entity.Member;
import com.ncu.gym.entity.MemberAlert;
import com.ncu.gym.entity.MemberCard;
import com.ncu.gym.entity.Reservation;
import com.ncu.gym.mapper.CheckinLogMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务：预约超时标记、排课状态回写、到期与流失双预警生成。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SweepService {

    private final CourseScheduleService scheduleService;
    private final ReservationService reservationService;
    private final MemberCardService memberCardService;
    private final CheckinLogService checkinLogService;
    private final MemberService memberService;
    private final MemberAlertService memberAlertService;

    /** 课程结束 30 分钟后：未签到的预约记 NO_SHOW，排课状态回写 FINISHED */
    @PostConstruct
    public void bootstrap() {
        sweepNoShow();
        generateAlerts();
    }

    @Scheduled(initialDelay = 20_000, fixedDelay = 600_000)
    public void sweepNoShow() {
        LocalDateTime line = LocalDateTime.now().minusMinutes(30);
        List<CourseSchedule> overdue = scheduleService.list(new QueryWrapper<CourseSchedule>()
                .ne("status", "CANCELLED")
                .apply("CONCAT(class_date, ' ', end_time) < {0}", line.toLocalDate() + " " + line.toLocalTime()));
        int marked = 0;
        for (CourseSchedule s : overdue) {
            marked += reservationService.markNoShowBySchedule(s.getId());
            if (!"FINISHED".equals(s.getStatus())) {
                CourseSchedule update = new CourseSchedule();
                update.setId(s.getId());
                update.setStatus("FINISHED");
                scheduleService.updateById(update);
            }
        }
        if (!overdue.isEmpty()) {
            log.info("[sweep] 过期排课 {} 节，标记 NO_SHOW {} 条", overdue.size(), marked);
        }
    }

    /** 每天 02:00 生成到期与流失双预警（存在未处理的同类型预警时不重复生成） */
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Shanghai")
    public void generateAlerts() {
        int due = dueAlerts();
        int churn = churnAlerts();
        log.info("[alerts] 到期预警 {} 条，流失预警 {} 条", due, churn);
    }


    private int dueAlerts() {
        LocalDate limit = LocalDate.now().plusDays(7);
        int count = 0;
        for (MemberCard card : memberCardService.list(new QueryWrapper<MemberCard>()
                .eq("status", "ACTIVE").isNotNull("end_date").le("end_date", limit))) {
            Member member = memberService.getById(card.getMemberId());
            if (member == null || member.getStatus() == null || member.getStatus() != 1) {
                continue;
            }
            if (existsOpen(card.getMemberId(), "DUE")) {
                continue;
            }
            MemberAlert alert = new MemberAlert();
            alert.setMemberId(member.getId());
            alert.setAlertType("DUE");
            alert.setContent("会员卡 " + card.getCardNo() + " 将于 " + card.getEndDate() + " 到期，请及时跟进续费");
            count += saveAlert(alert);
        }
        return count;
    }

    private int churnAlerts() {
        LocalDateTime line = LocalDateTime.now().minusDays(14);
        int count = 0;
        for (Member member : memberService.list(new QueryWrapper<Member>().eq("status", 1))) {
            if (member.getJoinDate() != null && member.getJoinDate().isAfter(LocalDate.now().minusDays(30))) {
                continue;
            }
            boolean visited = checkinLogService.count(new QueryWrapper<CheckinLog>()
                    .eq("member_id", member.getId()).ge("create_time", line)) > 0
                    || reservationService.count(new QueryWrapper<Reservation>()
                    .eq("member_id", member.getId()).ge("reserve_time", line)) > 0;
            if (visited || existsOpen(member.getId(), "CHURN")) {
                continue;
            }
            MemberAlert alert = new MemberAlert();
            alert.setMemberId(member.getId());
            alert.setAlertType("CHURN");
            alert.setContent("连续 14 天未到店，存在流失风险，请安排回访");
            count += saveAlert(alert);
        }
        return count;
    }

    private boolean existsOpen(Long memberId, String type) {
        return memberAlertService.count(new QueryWrapper<MemberAlert>()
                .eq("member_id", memberId).eq("alert_type", type).eq("status", "OPEN")) > 0;
    }

    private int saveAlert(MemberAlert alert) {
        alert.setStatus("OPEN");
        alert.setCreateTime(LocalDateTime.now());
        memberAlertService.save(alert);
        return 1;
    }
}
