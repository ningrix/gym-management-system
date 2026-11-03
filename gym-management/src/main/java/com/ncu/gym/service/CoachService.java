package com.ncu.gym.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ncu.gym.common.BizException;
import com.ncu.gym.entity.Coach;
import com.ncu.gym.entity.CourseSchedule;
import com.ncu.gym.entity.SysUser;
import com.ncu.gym.mapper.CoachMapper;
import com.ncu.gym.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CoachService extends ServiceImpl<CoachMapper, Coach> {

    private final SysUserService userService;
    private final CourseScheduleService scheduleService;

    public IPage<Coach> pageCoaches(String keyword, String specialty, Integer status, long pageNum, long pageSize) {
        QueryWrapper<Coach> qw = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like("name", keyword).or().like("phone", keyword).or().like("coach_no", keyword));
        }
        if (StringUtils.hasText(specialty)) {
            qw.eq("specialty", specialty);
        }
        if (status != null) {
            qw.eq("status", status);
        }
        qw.orderByDesc("id");
        return page(new Page<>(pageNum, pageSize), qw);
    }

    /** 新增教练：档案与登录账号一并创建 */
    @Transactional(rollbackFor = Exception.class)
    public Coach createCoach(Coach coach) {
        if (!StringUtils.hasText(coach.getName()) || !StringUtils.hasText(coach.getPhone())) {
            throw new BizException("教练姓名与手机号不能为空");
        }
        long dup = count(new QueryWrapper<Coach>().eq("phone", coach.getPhone()));
        if (dup > 0) {
            throw new BizException("该手机号已登记为教练");
        }
        long next = count() + 1;
        String coachNo = "C" + String.format("%03d", next);
        while (count(new QueryWrapper<Coach>().eq("coach_no", coachNo)) > 0) {
            next++;
            coachNo = "C" + String.format("%03d", next);
        }
        String username = "coach" + String.format("%02d", next);
        while (userService.count(new QueryWrapper<SysUser>().eq("username", username)) > 0) {
            next++;
            username = "coach" + String.format("%02d", next);
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(PasswordUtil.encode("gym2026"));
        user.setRole("COACH");
        user.setRealName(coach.getName());
        user.setPhone(coach.getPhone());
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        userService.save(user);

        coach.setUserId(user.getId());
        coach.setCoachNo(coachNo);
        coach.setStatus(1);
        coach.setHireDate(coach.getHireDate() == null ? LocalDate.now() : coach.getHireDate());
        coach.setCreateTime(LocalDateTime.now());
        save(coach);
        return coach;
    }

    public void updateProfile(Coach coach) {
        Coach db = getById(coach.getId());
        if (db == null) {
            throw new BizException("教练不存在");
        }
        long dup = count(new QueryWrapper<Coach>().eq("phone", coach.getPhone()).ne("id", coach.getId()));
        if (dup > 0) {
            throw new BizException("该手机号已被其他教练使用");
        }
        coach.setUserId(db.getUserId());
        coach.setCoachNo(db.getCoachNo());
        updateById(coach);

        SysUser user = new SysUser();
        user.setId(db.getUserId());
        user.setRealName(coach.getName());
        user.setPhone(coach.getPhone());
        userService.updateById(user);
    }

    /** 停用教练前检查是否还有未结束的排课 */
    public void changeStatus(Long id, Integer status) {
        Coach coach = getById(id);
        if (coach == null) {
            throw new BizException("教练不存在");
        }
        if (status == 0) {
            long pending = scheduleService.count(new QueryWrapper<CourseSchedule>()
                    .eq("coach_id", id)
                    .in("status", "WAITING", "ONGOING"));
            if (pending > 0) {
                throw new BizException("该教练名下还有 " + pending + " 节未结束的课程，请先处理后再停用");
            }
        }
        Coach update = new Coach();
        update.setId(id);
        update.setStatus(status);
        updateById(update);

        SysUser user = new SysUser();
        user.setId(coach.getUserId());
        user.setStatus(status);
        userService.updateById(user);
    }
}
