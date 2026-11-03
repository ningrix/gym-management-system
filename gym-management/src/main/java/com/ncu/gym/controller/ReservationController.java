package com.ncu.gym.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ncu.gym.common.BizException;
import com.ncu.gym.common.Result;
import com.ncu.gym.entity.Reservation;
import com.ncu.gym.service.ReservationService;
import com.ncu.gym.util.LoginUser;
import com.ncu.gym.util.UserContext;
import com.ncu.gym.vo.ReservationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 课程预约与签到
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /** 会员预约课程：卡有效性、重复预约、满员、开课时间逐项校验 */
    @PostMapping
    public Result<Reservation> create(@RequestBody Map<String, Object> body) {
        LoginUser login = UserContext.require();
        if (!"MEMBER".equals(login.getRole()) || login.getBizId() == null) {
            throw new BizException(403, "只有会员可以预约课程");
        }
        Object scheduleId = body.get("scheduleId");
        if (scheduleId == null) {
            throw new BizException("请选择要预约的课程");
        }
        return Result.success(reservationService.reserve(Long.valueOf(scheduleId.toString()), login.getBizId()));
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        LoginUser login = UserContext.require();
        reservationService.cancel(id, login.getBizId(), UserContext.isAdmin());
        return Result.success();
    }

    @PutMapping("/{id}/checkin")
    public Result<Void> checkIn(@PathVariable Long id) {
        LoginUser login = UserContext.require();
        if ("MEMBER".equals(login.getRole())) {
            throw new BizException(403, "会员不能自行签到，请由教练或前台核销");
        }
        reservationService.checkIn(id, "COACH".equals(login.getRole()) ? login.getBizId() : null,
                UserContext.isAdmin());
        return Result.success();
    }

    @GetMapping
    public Result<IPage<ReservationVO>> list(@RequestParam(required = false) Long memberId,
                                             @RequestParam(required = false) Long scheduleId,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize) {
        LoginUser login = UserContext.require();
        Long scopeMemberId = null;
        Long scopeCoachId = null;
        if ("MEMBER".equals(login.getRole())) {
            scopeMemberId = login.getBizId();
        } else if ("COACH".equals(login.getRole())) {
            scopeCoachId = login.getBizId();
        } else if (memberId != null) {
            scopeMemberId = memberId;
        }
        return Result.success(reservationService.pageReservations(scopeMemberId, scheduleId, status,
                pageNum, pageSize, scopeCoachId));
    }

    @GetMapping("/my")
    public Result<List<ReservationVO>> my(@RequestParam(required = false) String status) {
        LoginUser login = UserContext.require();
        if (!"MEMBER".equals(login.getRole()) || login.getBizId() == null) {
            throw new BizException(403, "该接口仅供会员使用");
        }
        return Result.success(reservationService.listByMember(login.getBizId(), status));
    }
}
