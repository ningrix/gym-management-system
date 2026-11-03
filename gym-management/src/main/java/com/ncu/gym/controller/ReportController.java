package com.ncu.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ncu.gym.common.BizException;
import com.ncu.gym.common.Result;
import com.ncu.gym.entity.BodyRecord;
import com.ncu.gym.entity.CourseSchedule;
import com.ncu.gym.entity.Reservation;
import com.ncu.gym.service.BodyRecordService;
import com.ncu.gym.service.CourseScheduleService;
import com.ncu.gym.service.ReportService;
import com.ncu.gym.service.ReservationService;
import com.ncu.gym.util.LoginUser;
import com.ncu.gym.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计报表与工作台数据
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final CourseScheduleService scheduleService;
    private final ReservationService reservationService;
    private final BodyRecordService bodyRecordService;
    private final com.ncu.gym.service.CoachService coachService;
    private final com.ncu.gym.service.CourseService courseService;

    private void requireAdmin(String what) {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以" + what);
        }
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        UserContext.require();
        requireAdmin("查看全馆报表");
        return Result.success(reportService.overview());
    }

    @GetMapping("/member-growth")
    public Result<List<Map<String, Object>>> memberGrowth(@RequestParam(defaultValue = "6") int months) {
        UserContext.require();
        requireAdmin("查看会员统计");
        return Result.success(reportService.memberGrowth(months));
    }

    @GetMapping("/course-hot")
    public Result<List<Map<String, Object>>> courseHot(@RequestParam(defaultValue = "5") int limit) {
        UserContext.require();
        requireAdmin("查看课程统计");
        return Result.success(reportService.courseHot(limit));
    }

    @GetMapping("/coach-workload")
    public Result<List<Map<String, Object>>> coachWorkload(@RequestParam(required = false) String month) {
        UserContext.require();
        requireAdmin("查看教练课时统计");
        return Result.success(reportService.coachWorkload(month));
    }

    @GetMapping("/revenue")
    public Result<List<Map<String, Object>>> revenue(@RequestParam(defaultValue = "6") int months) {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以查看营收统计");
        }
        return Result.success(reportService.revenue(months));
    }

    /** 会员工作台：我的会员卡、我近期的预约与最近一次体测 */
    /** 教练课时结算单：当月已结束课程的逐节明细与合计 */
    @GetMapping("/coach-settlement")
    public Result<Map<String, Object>> coachSettlement(@RequestParam(required = false) String month,
                                                       @RequestParam(required = false) Long coachId) {
        LoginUser login = UserContext.require();
        Long coachScope = "COACH".equals(login.getRole()) ? login.getBizId()
                : ("ADMIN".equals(login.getRole()) ? coachId : null);
        if (coachScope == null) {
            throw new BizException(400, "请指定教练");
        }
        com.ncu.gym.entity.Coach coach = coachService.getById(coachScope);
        if (coach == null) {
            throw new BizException("教练不存在");
        }
        java.time.YearMonth ym = month == null || month.isBlank()
                ? java.time.YearMonth.now() : java.time.YearMonth.parse(month);
        List<Map<String, Object>> rows = new java.util.ArrayList<>();
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        var schedules = scheduleService.list(new QueryWrapper<CourseSchedule>()
                .eq("coach_id", coachScope).ne("status", "CANCELLED")
                .orderByAsc("class_date").orderByAsc("start_time"));
        for (CourseSchedule s : schedules) {
            if (!java.time.YearMonth.from(s.getClassDate()).equals(ym)) continue;
            if (java.time.LocalDateTime.of(s.getClassDate(), s.getEndTime()).isAfter(java.time.LocalDateTime.now())) continue;
            java.math.BigDecimal fee = coach.getHourlyWage() == null ? java.math.BigDecimal.ZERO : coach.getHourlyWage();
            rows.add(new java.util.LinkedHashMap<>(java.util.Map.of(
                    "classDate", s.getClassDate().toString(),
                    "start", s.getStartTime().toString(),
                    "end", s.getEndTime().toString(),
                    "fee", fee)));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("coachName", coach.getName());
        data.put("month", ym.toString());
        data.put("rows", rows);
        data.put("totalFee", total.add(java.math.BigDecimal.valueOf(rows.size() * coach.getHourlyWage().doubleValue())));
        data.put("lessonCount", rows.size());
        return Result.success(data);
    }

    @GetMapping("/member-home")
    public Result<Map<String, Object>> memberHome() {
        LoginUser login = UserContext.require();
        if (!"MEMBER".equals(login.getRole()) || login.getBizId() == null) {
            throw new BizException(403, "该接口仅供会员使用");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.putAll(reportService.memberOverview(login.getBizId()));
        List<BodyRecord> records = bodyRecordService.list(new QueryWrapper<BodyRecord>()
                .eq("member_id", login.getBizId()).orderByDesc("record_date"));
        data.put("latestBody", records.isEmpty() ? null : records.get(0));
        data.put("bodyRecords", records);
        return Result.success(data);
    }

    /** 教练工作台：今日课表、待签到人数与本月课时 */
    @GetMapping("/coach-home")
    public Result<Map<String, Object>> coachHome() {
        LoginUser login = UserContext.require();
        if (!"COACH".equals(login.getRole()) || login.getBizId() == null) {
            throw new BizException(403, "该接口仅供教练使用");
        }
        LocalDate today = LocalDate.now();
        List<CourseSchedule> todayList = scheduleService.list(new QueryWrapper<CourseSchedule>()
                .eq("coach_id", login.getBizId()).eq("class_date", today)
                .ne("status", "CANCELLED").orderByAsc("start_time"));
        long pendingCheckIn = reservationService.count(new QueryWrapper<Reservation>()
                .eq("status", "BOOKED")
                .inSql("schedule_id", "select id from course_schedule where coach_id = " + login.getBizId()
                        + " and class_date = '" + today + "'"));
        List<Map<String, Object>> workload = reportService.coachWorkload(null);
        Map<String, Object> mine = workload.stream()
                .filter(m -> login.getRealName().equals(m.get("coachName")))
                .findFirst().orElse(Map.of());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("todaySchedules", scheduleService.toVoList(todayList));
        data.put("pendingCheckIn", pendingCheckIn);
        data.put("monthLesson", mine.getOrDefault("lessonCount", 0));
        data.put("monthMember", mine.getOrDefault("memberCount", 0));
        return Result.success(data);
    }
}
