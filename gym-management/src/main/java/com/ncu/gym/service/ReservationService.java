package com.ncu.gym.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ncu.gym.common.BizException;
import com.ncu.gym.entity.CardType;
import com.ncu.gym.entity.CheckinLog;
import com.ncu.gym.entity.Coach;
import com.ncu.gym.entity.Course;
import com.ncu.gym.entity.CourseSchedule;
import com.ncu.gym.entity.Member;
import com.ncu.gym.entity.MemberCard;
import com.ncu.gym.entity.Reservation;
import com.ncu.gym.mapper.ReservationMapper;
import com.ncu.gym.vo.ReservationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService extends ServiceImpl<ReservationMapper, Reservation> {

    private final CourseScheduleService scheduleService;
    private final MemberService memberService;
    private final MemberCardService memberCardService;
    private final CardTypeService cardTypeService;
    private final CourseService courseService;
    private final CoachService coachService;
    private final WaitlistService waitlistService;
    private final CheckinLogService checkinLogService;

    /** 预约课程：依次校验课程状态、会员卡、重复预约、名额与开课时间 */
    @Transactional(rollbackFor = Exception.class)
    public Reservation reserve(Long scheduleId, Long memberId) {
        CourseSchedule schedule = scheduleService.getById(scheduleId);
        if (schedule == null) {
            throw new BizException("课程不存在");
        }
        if ("CANCELLED".equals(schedule.getStatus())) {
            throw new BizException("该课程已取消，无法预约");
        }
        if ("FINISHED".equals(schedule.getStatus())) {
            throw new BizException("该课程已结束，无法预约");
        }
        LocalDateTime start = LocalDateTime.of(schedule.getClassDate(), schedule.getStartTime());
        if (start.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BizException("距开课不足 2 小时，已停止预约");
        }
        long dup = count(new QueryWrapper<Reservation>()
                .eq("schedule_id", scheduleId).eq("member_id", memberId)
                .ne("status", "CANCELLED"));
        if (dup > 0) {
            throw new BizException("您已预约该课程，请勿重复预约");
        }
        long queued = waitlistService.count(new QueryWrapper<com.ncu.gym.entity.Waitlist>()
                .eq("schedule_id", scheduleId).eq("member_id", memberId)
                .eq("status", "WAITING"));
        if (queued > 0) {
            throw new BizException("您已在本课程的候补队列中，递补成功后会自动转为预约");
        }
        // 旷课熔断：本周 NO_SHOW 达 2 次，本周仅可保有 1 个有效预约
        LocalDate weekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        long noShowWeek = count(new QueryWrapper<Reservation>()
                .eq("member_id", memberId).eq("status", "NO_SHOW")
                .ge("reserve_time", weekStart.atStartOfDay()));
        if (noShowWeek >= 2) {
            long activeWeek = count(new QueryWrapper<Reservation>()
                    .eq("member_id", memberId).eq("status", "BOOKED")
                    .ge("reserve_time", weekStart.atStartOfDay()));
            if (activeWeek >= 1) {
                throw new BizException("本周已旷课 2 次，本周限约 1 节课程，下周自动恢复");
            }
        }
        MemberCard card = memberCardService.pickUsableCard(memberId);
        CardType cardType = cardTypeService.getById(card.getCardTypeId());
        if (cardType != null && "TIMES".equals(cardType.getCardKind())) {
            long occupied = count(new QueryWrapper<Reservation>()
                    .eq("member_card_id", card.getId())
                    .in("status", "BOOKED", "CHECKED_IN"));
            int remain = card.getRemainTimes() == null ? 0 : card.getRemainTimes();
            if (occupied >= remain) {
                throw new BizException("次卡剩余次数不足（剩余 " + remain + " 次，已占用 " + occupied + " 次），请续费或选择其他课程");
            }
        }

        // 名额占用使用条件更新，避免并发下超额
        boolean locked = scheduleService.update(null, new UpdateWrapper<CourseSchedule>()
                .setSql("booked_count = booked_count + 1")
                .eq("id", scheduleId)
                .apply("booked_count < capacity"));
        if (!locked) {
            throw new BizException("该课程名额已满，请选择其他时段");
        }

        LocalDateTime now = LocalDateTime.now();
        String no = nextReserveNo();
        // 取消过的记录仍占用唯一索引 (schedule_id, member_id)，这里复用原行而不是新增
        Reservation cancelled = getOne(new QueryWrapper<Reservation>()
                .eq("schedule_id", scheduleId).eq("member_id", memberId)
                .eq("status", "CANCELLED").orderByDesc("id").last("limit 1"));
        Integer position = assignPosition(schedule, null);
        if (cancelled != null) {
            update(null, new UpdateWrapper<Reservation>()
                    .set("reserve_no", no)
                    .set("member_card_id", card.getId())
                    .set("status", "BOOKED")
                    .set("reserve_time", now)
                    .set("check_in_time", null)
                    .set("cancel_time", null)
                    .set("position_no", position)
                    .eq("id", cancelled.getId()));
            return getById(cancelled.getId());
        }

        Reservation reservation = new Reservation();
        reservation.setReserveNo(no);
        reservation.setScheduleId(scheduleId);
        reservation.setMemberId(memberId);
        reservation.setMemberCardId(card.getId());
        reservation.setStatus("BOOKED");
        reservation.setReserveTime(now);
        reservation.setCreateTime(now);
        reservation.setPositionNo(position);
        save(reservation);
        return reservation;
    }

    /** 取消预约：仅本人、仅已预约状态、距开课 2 小时以上 */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, Long memberId, boolean admin) {
        Reservation reservation = getById(id);
        if (reservation == null) {
            throw new BizException("预约记录不存在");
        }
        if (!admin && !reservation.getMemberId().equals(memberId)) {
            throw new BizException(403, "只能取消本人的预约");
        }
        if (!"BOOKED".equals(reservation.getStatus())) {
            throw new BizException("当前状态不允许取消");
        }
        CourseSchedule schedule = scheduleService.getById(reservation.getScheduleId());
        String cancelType = "FREE";
        if (schedule != null) {
            LocalDateTime start = LocalDateTime.of(schedule.getClassDate(), schedule.getStartTime());
            long hours = java.time.Duration.between(LocalDateTime.now(), start).toHours();
            if (start.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new BizException("距开课不足 2 小时，不能取消预约");
            }
            if (hours <= 6) {
                // 2–6 小时取消计入当月“临时取消”额度（每月 3 次），超出则拒绝
                LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
                long quotaUsed = count(new QueryWrapper<Reservation>()
                        .eq("member_id", reservation.getMemberId())
                        .eq("cancel_type", "QUOTA")
                        .ge("cancel_time", monthStart));
                if (quotaUsed >= 3) {
                    throw new BizException("本月临时取消额度已用完（3 次），本次取消将计入违约，请到前台办理");
                }
                cancelType = "QUOTA";
            }
        }
        Reservation update = new Reservation();
        update.setId(id);
        update.setStatus("CANCELLED");
        update.setCancelTime(LocalDateTime.now());
        update.setCancelType(cancelType);
        update.setPositionNo(null);
        updateById(update);

        scheduleService.update(null, new UpdateWrapper<CourseSchedule>()
                .setSql("booked_count = case when booked_count > 0 then booked_count - 1 else 0 end")
                .eq("id", reservation.getScheduleId()));

        // 释放名额后自动递补候补第一名
        promoteWaitlist(reservation.getScheduleId());
    }

    /** 签到：管理员或本节课教练在课程当天核销，次卡同时扣减次数 */
    @Transactional(rollbackFor = Exception.class)
    public void checkIn(Long id, Long operatorCoachId, boolean admin) {
        Reservation reservation = getById(id);
        if (reservation == null) {
            throw new BizException("预约记录不存在");
        }
        if (!"BOOKED".equals(reservation.getStatus())) {
            throw new BizException("只有“已预约”状态的记录可以签到");
        }
        CourseSchedule schedule = scheduleService.getById(reservation.getScheduleId());
        if (schedule == null) {
            throw new BizException("课程不存在");
        }
        if (!admin && (operatorCoachId == null || !operatorCoachId.equals(schedule.getCoachId()))) {
            throw new BizException(403, "只能为本人的课程签到");
        }
        if (!LocalDate.now().equals(schedule.getClassDate())) {
            throw new BizException("仅课程当天可签到");
        }
        Reservation update = new Reservation();
        update.setId(id);
        update.setStatus("CHECKED_IN");
        update.setCheckInTime(LocalDateTime.now());
        updateById(update);

        CheckinLog log = new CheckinLog();
        log.setMemberId(reservation.getMemberId());
        log.setCheckinType("COURSE");
        log.setRefId(reservation.getScheduleId());
        log.setCreateTime(LocalDateTime.now());
        checkinLogService.save(log);

        if (reservation.getMemberCardId() != null) {
            MemberCard card = memberCardService.getById(reservation.getMemberCardId());
            if (card != null) {
                CardType type = cardTypeService.getById(card.getCardTypeId());
                if (type != null && "TIMES".equals(type.getCardKind())) {
                    MemberCard deduct = new MemberCard();
                    deduct.setId(card.getId());
                    deduct.setRemainTimes(Math.max(0, (card.getRemainTimes() == null ? 0 : card.getRemainTimes()) - 1));
                    memberCardService.updateById(deduct);
                }
            }
        }
    }

    public IPage<ReservationVO> pageReservations(Long memberId, Long scheduleId, String status,
                                                 long pageNum, long pageSize, Long scopeCoachId) {
        QueryWrapper<Reservation> qw = new QueryWrapper<>();
        if (memberId != null) {
            qw.eq("member_id", memberId);
        }
        if (scheduleId != null) {
            qw.eq("schedule_id", scheduleId);
        }
        if (StringUtils.hasText(status)) {
            qw.eq("status", status);
        }
        if (scopeCoachId != null) {
            qw.inSql("schedule_id", "select id from course_schedule where coach_id = " + scopeCoachId);
        }
        qw.orderByDesc("id");
        IPage<Reservation> page = page(new Page<>(pageNum, pageSize), qw);
        return page.convert(this::toVo);
    }

    public List<ReservationVO> listByMember(Long memberId, String status) {
        QueryWrapper<Reservation> qw = new QueryWrapper<Reservation>().eq("member_id", memberId);
        if (StringUtils.hasText(status)) {
            qw.eq("status", status);
        }
        qw.orderByDesc("id");
        return list(qw).stream().map(this::toVo).collect(Collectors.toList());
    }

    public List<ReservationVO> listBySchedule(Long scheduleId) {
        List<Reservation> list = list(new QueryWrapper<Reservation>()
                .eq("schedule_id", scheduleId).orderByAsc("id"));
        return list.stream().map(this::toVo).collect(Collectors.toList());
    }

    public ReservationVO toVo(Reservation r) {
        ReservationVO vo = new ReservationVO();
        vo.setId(r.getId());
        vo.setReserveNo(r.getReserveNo());
        vo.setScheduleId(r.getScheduleId());
        vo.setMemberId(r.getMemberId());
        vo.setStatus(r.getStatus());
        vo.setReserveTime(r.getReserveTime());
        vo.setCheckInTime(r.getCheckInTime());
        vo.setRemark(r.getRemark());
        vo.setPositionNo(r.getPositionNo());
        Member member = memberService.getById(r.getMemberId());
        if (member != null) {
            vo.setMemberName(member.getName());
            vo.setMemberNo(member.getMemberNo());
        }
        CourseSchedule schedule = scheduleService.getById(r.getScheduleId());
        if (schedule != null) {
            vo.setClassDate(schedule.getClassDate());
            vo.setStartTime(schedule.getStartTime());
            vo.setEndTime(schedule.getEndTime());
            vo.setRoom(schedule.getRoom());
            Course course = courseService.getById(schedule.getCourseId());
            if (course != null) {
                vo.setCourseName(course.getCourseName());
            }
            Coach coach = coachService.getById(schedule.getCoachId());
            if (coach != null) {
                vo.setCoachName(coach.getName());
            }
        }
        return vo;
    }

    private String nextReserveNo() {
        String prefix = "RS" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = count(new QueryWrapper<Reservation>().likeRight("reserve_no", prefix)) + 1;
        String no = prefix + String.format("%03d", seq);
        while (count(new QueryWrapper<Reservation>().eq("reserve_no", no)) > 0) {
            seq++;
            no = prefix + String.format("%03d", seq);
        }
        return no;
    }

    /** 团课选位：在排课的位置总数内自动分配最小空闲位置号（未启用选位时返回 null） */
    private Integer assignPosition(CourseSchedule schedule, Long excludeReservationId) {
        if (schedule.getPositionTotal() == null || schedule.getPositionTotal() <= 0) {
            return null;
        }
        java.util.Set<Integer> used = new java.util.HashSet<>();
        for (Reservation r : list(new QueryWrapper<Reservation>()
                .eq("schedule_id", schedule.getId())
                .in("status", "BOOKED", "CHECKED_IN")
                .isNotNull("position_no"))) {
            if (r.getPositionNo() != null && !r.getId().equals(excludeReservationId)) {
                used.add(r.getPositionNo());
            }
        }
        for (int i = 1; i <= schedule.getPositionTotal(); i++) {
            if (!used.contains(i)) {
                return i;
            }
        }
        return null;
    }

    /** 候补递补：按排队顺序尝试把候补转为正式预约，遇到业务校验不通过的顺延下一个 */
    private void promoteWaitlist(Long scheduleId) {
        CourseSchedule schedule = scheduleService.getById(scheduleId);
        if (schedule == null || schedule.getBookedCount() >= schedule.getCapacity()) {
            return;
        }
        List<com.ncu.gym.entity.Waitlist> queue = waitlistService.list(new QueryWrapper<com.ncu.gym.entity.Waitlist>()
                .eq("schedule_id", scheduleId).eq("status", "WAITING")
                .orderByAsc("enqueue_time"));
        for (com.ncu.gym.entity.Waitlist w : queue) {
            try {
                reserve(scheduleId, w.getMemberId());
                waitlistService.markPromoted(w.getId());
                return;
            } catch (BizException e) {
                waitlistService.markCancelled(w.getId());
            }
        }
    }


    /** 定时任务：把已结束课程中仍未签到的预约标记为未到场 */
    public int markNoShowBySchedule(Long scheduleId) {
        List<Reservation> list = list(new QueryWrapper<Reservation>()
                .eq("schedule_id", scheduleId).eq("status", "BOOKED"));
        for (Reservation r : list) {
            Reservation update = new Reservation();
            update.setId(r.getId());
            update.setStatus("NO_SHOW");
            updateById(update);
        }
        return list.size();
    }
}
