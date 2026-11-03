package com.ncu.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ncu.gym.common.BizException;
import com.ncu.gym.common.Result;
import com.ncu.gym.entity.BodyRecord;
import com.ncu.gym.entity.Member;
import com.ncu.gym.service.BodyRecordService;
import com.ncu.gym.service.MemberService;
import com.ncu.gym.util.LoginUser;
import com.ncu.gym.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 体测记录
 */
@RestController
@RequestMapping("/api/body-records")
@RequiredArgsConstructor
public class BodyRecordController {

    private final BodyRecordService bodyRecordService;
    private final MemberService memberService;

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LoginUser login = UserContext.require();
        QueryWrapper<BodyRecord> qw = new QueryWrapper<>();
        if ("MEMBER".equals(login.getRole())) {
            qw.eq("member_id", login.getBizId());
        } else if (memberId != null) {
            qw.eq("member_id", memberId);
        } else if ("COACH".equals(login.getRole())) {
            qw.inSql("member_id", "select distinct r.member_id from reservation r "
                    + "join course_schedule s on r.schedule_id = s.id where s.coach_id = " + login.getBizId());
        }
        if (startDate != null) {
            qw.ge("record_date", startDate);
        }
        if (endDate != null) {
            qw.le("record_date", endDate);
        }
        qw.orderByAsc("record_date");
        List<BodyRecord> list = bodyRecordService.list(qw);
        Map<Long, String> names = new HashMap<>();
        for (BodyRecord r : list) {
            if (!names.containsKey(r.getMemberId())) {
                Member m = memberService.getById(r.getMemberId());
                names.put(r.getMemberId(), m == null ? "" : m.getName() + "（" + m.getMemberNo() + "）");
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("records", list);
        data.put("memberNames", names);
        return Result.success(data);
    }

    @PostMapping
    public Result<BodyRecord> create(@RequestBody BodyRecord record) {
        LoginUser login = UserContext.require();
        if ("MEMBER".equals(login.getRole())) {
            throw new BizException(403, "体测数据由教练或管理员录入");
        }
        if (record.getMemberId() == null || record.getHeight() == null || record.getWeight() == null) {
            throw new BizException("会员、身高与体重不能为空");
        }
        Member member = memberService.getById(record.getMemberId());
        if (member == null) {
            throw new BizException("会员不存在");
        }
        if (record.getHeight().compareTo(new BigDecimal("100")) < 0
                || record.getHeight().compareTo(new BigDecimal("250")) > 0) {
            throw new BizException("身高应在 100-250 厘米之间");
        }
        if (record.getWeight().compareTo(new BigDecimal("30")) < 0
                || record.getWeight().compareTo(new BigDecimal("200")) > 0) {
            throw new BizException("体重应在 30-200 公斤之间");
        }
        LocalDate date = record.getRecordDate() == null ? LocalDate.now() : record.getRecordDate();
        long dup = bodyRecordService.count(new QueryWrapper<BodyRecord>()
                .eq("member_id", record.getMemberId()).eq("record_date", date));
        if (dup > 0) {
            throw new BizException("该会员当天已有体测记录，请勿重复录入");
        }
        BigDecimal heightM = record.getHeight().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal bmi = record.getWeight().divide(heightM.multiply(heightM), 1, RoundingMode.HALF_UP);
        record.setRecordDate(date);
        record.setBmi(bmi);
        if ("COACH".equals(login.getRole())) {
            record.setCoachId(login.getBizId());
        }
        record.setCreateTime(LocalDateTime.now());
        bodyRecordService.save(record);
        return Result.success(record);
    }


    /** 修改体测记录：允许更正身高体重等测量值，BMI 随之重算 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody BodyRecord input) {
        LoginUser login = UserContext.require();
        BodyRecord db = bodyRecordService.getById(id);
        if (db == null) {
            throw new BizException("体测记录不存在");
        }
        if ("MEMBER".equals(login.getRole()) && !db.getMemberId().equals(login.getBizId())) {
            throw new BizException(403, "只能修改本人的体测记录");
        }
        if ("COACH".equals(login.getRole()) && db.getCoachId() != null
                && !db.getCoachId().equals(login.getBizId())) {
            throw new BizException(403, "只能修改本人录入的体测记录");
        }
        BodyRecord update = new BodyRecord();
        update.setId(id);
        update.setHeight(input.getHeight());
        update.setWeight(input.getWeight());
        update.setBodyFat(input.getBodyFat());
        update.setMuscle(input.getMuscle());
        update.setWaist(input.getWaist());
        update.setHeartRate(input.getHeartRate());
        update.setRemark(input.getRemark());
        java.math.BigDecimal h = input.getHeight() != null ? input.getHeight() : db.getHeight();
        java.math.BigDecimal w = input.getWeight() != null ? input.getWeight() : db.getWeight();
        if (h != null && w != null && h.compareTo(java.math.BigDecimal.ZERO) > 0) {
            update.setBmi(w.multiply(java.math.BigDecimal.valueOf(10000))
                    .divide(h.multiply(h), 1, java.math.RoundingMode.HALF_UP));
        }
        bodyRecordService.updateById(update);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以删除体测记录");
        }
        bodyRecordService.removeById(id);
        return Result.success();
    }
}
