package com.ncu.gym.controller;

import com.ncu.gym.common.BizException;
import com.ncu.gym.common.Result;
import com.ncu.gym.entity.CourseSchedule;
import com.ncu.gym.service.CourseScheduleService;
import com.ncu.gym.service.ReservationService;
import com.ncu.gym.util.LoginUser;
import com.ncu.gym.util.UserContext;
import com.ncu.gym.vo.ReservationVO;
import com.ncu.gym.vo.ScheduleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 排课与课表
 */
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final CourseScheduleService scheduleService;
    private final ReservationService reservationService;

    @GetMapping
    public Result<List<ScheduleVO>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long coachId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String status) {
        LoginUser login = UserContext.require();
        Long scopeCoachId = "COACH".equals(login.getRole()) ? login.getBizId() : null;
        return Result.success(scheduleService.listSchedules(startDate, endDate, coachId, courseId, status, scopeCoachId));
    }

    @GetMapping("/my")
    public Result<List<ScheduleVO>> my(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LoginUser login = UserContext.require();
        if (!"COACH".equals(login.getRole())) {
            throw new BizException(403, "该接口仅供教练使用");
        }
        return Result.success(scheduleService.listSchedules(startDate, endDate, null, null, null, login.getBizId()));
    }

    @GetMapping("/{id}")
    public Result<ScheduleVO> detail(@PathVariable Long id) {
        UserContext.require();
        return Result.success(scheduleService.detail(id));
    }

    @PostMapping
    public Result<CourseSchedule> create(@RequestBody CourseSchedule schedule) {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以排课");
        }
        return Result.success(scheduleService.createSchedule(schedule));
    }

    @PutMapping("/{id}")
    public Result<CourseSchedule> update(@PathVariable Long id, @RequestBody CourseSchedule schedule) {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以调整排课");
        }
        schedule.setId(id);
        return Result.success(scheduleService.updateSchedule(schedule));
    }

    /** 取消排课：批量取消该节课的预约并归零已约人数 */
    @PutMapping("/{id}/cancel")
    public Result<Map<String, Object>> cancel(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以取消排课");
        }
        String reason = body == null ? null : body.get("reason");
        int affected = scheduleService.cancelSchedule(id, reason);
        return Result.success(Map.of("cancelledReservations", affected));
    }

    @GetMapping("/{id}/reservations")
    public Result<List<ReservationVO>> reservations(@PathVariable Long id) {
        LoginUser login = UserContext.require();
        ScheduleVO schedule = scheduleService.detail(id);
        if ("COACH".equals(login.getRole()) && !schedule.getCoachId().equals(login.getBizId())) {
            throw new BizException(403, "只能查看本人课程的预约名单");
        }
        return Result.success(reservationService.listBySchedule(id));
    }
}
