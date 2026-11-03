package com.ncu.gym.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ncu.gym.common.BizException;
import com.ncu.gym.common.Result;
import com.ncu.gym.entity.Coach;
import com.ncu.gym.service.CoachService;
import com.ncu.gym.util.UserContext;
import com.ncu.gym.vo.CoachProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 教练管理
 */
@RestController
@RequestMapping("/api/coaches")
@RequiredArgsConstructor
public class CoachController {

    private final CoachService coachService;

    @GetMapping
    public Result<IPage<Coach>> list(@RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String specialty,
                                     @RequestParam(required = false) Integer status,
                                     @RequestParam(defaultValue = "1") long pageNum,
                                     @RequestParam(defaultValue = "10") long pageSize) {
        UserContext.require();
        return Result.success(coachService.pageCoaches(keyword, specialty, status, pageNum, pageSize));
    }


    /** 教练风采：只返回可公开展示的字段，任何登录角色可见 */
    @GetMapping("/showcase")
    public Result<List<CoachProfileVO>> showcase() {
        UserContext.require();
        List<CoachProfileVO> list = new ArrayList<>();
        for (Coach coach : coachService.list(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Coach>()
                .eq("status", 1).orderByAsc("id"))) {
            CoachProfileVO vo = new CoachProfileVO();
            vo.setId(coach.getId());
            vo.setName(coach.getName());
            vo.setSpecialty(coach.getSpecialty());
            vo.setCoachLevel(coach.getCoachLevel());
            vo.setHireDate(coach.getHireDate());
            vo.setIntro(coach.getIntro());
            if (coach.getHireDate() != null) {
                vo.setYearsOfService(Math.max(0, Period.between(coach.getHireDate(), LocalDate.now()).getYears()));
            }
            list.add(vo);
        }
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result<Coach> detail(@PathVariable Long id) {
        UserContext.require();
        return Result.success(coachService.getById(id));
    }

    @PostMapping
    public Result<Coach> create(@RequestBody Coach coach) {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以新增教练");
        }
        return Result.success(coachService.createCoach(coach));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Coach coach) {
        UserContext.require();
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以修改教练档案");
        }
        coach.setId(id);
        coachService.updateProfile(coach);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以启停教练");
        }
        coachService.changeStatus(id, body.get("status"));
        return Result.success();
    }
}
