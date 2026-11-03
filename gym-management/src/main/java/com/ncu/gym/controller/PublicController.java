package com.ncu.gym.controller;

import com.ncu.gym.common.Result;
import com.ncu.gym.entity.CardRecharge;
import com.ncu.gym.service.CardRechargeService;
import com.ncu.gym.service.CoachService;
import com.ncu.gym.service.CourseScheduleService;
import com.ncu.gym.service.CourseService;
import com.ncu.gym.service.EquipmentService;
import com.ncu.gym.service.MemberService;
import com.ncu.gym.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 公开数据接口：供系统介绍首页等未登录页面读取运行数据。
 * 只返回聚合计数与营收，不包含任何会员或教练的个人信息。
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final MemberService memberService;
    private final CoachService coachService;
    private final CourseService courseService;
    private final CourseScheduleService scheduleService;
    private final ReservationService reservationService;
    private final EquipmentService equipmentService;
    private final CardRechargeService rechargeService;

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        YearMonth month = YearMonth.now();
        BigDecimal monthRevenue = rechargeService.list().stream()
                .filter(r -> r.getCreateTime() != null && YearMonth.from(r.getCreateTime()).equals(month))
                .map(CardRecharge::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("members", memberService.count());
        data.put("coaches", coachService.count());
        data.put("courses", courseService.count());
        data.put("schedules", scheduleService.count());
        data.put("reservations", reservationService.count());
        data.put("equipments", equipmentService.count());
        data.put("monthRevenue", monthRevenue);
        data.put("serverTime", LocalDateTime.now().toString());
        return Result.success(data);
    }
}
