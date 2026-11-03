package com.ncu.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ncu.gym.common.BizException;
import com.ncu.gym.common.Result;
import com.ncu.gym.entity.Course;
import com.ncu.gym.entity.CourseSchedule;
import com.ncu.gym.service.CourseScheduleService;
import com.ncu.gym.service.CourseService;
import com.ncu.gym.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 课程管理
 */
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final CourseScheduleService scheduleService;

    @GetMapping
    public Result<IPage<Course>> list(@RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String courseType,
                                      @RequestParam(required = false) Integer status,
                                      @RequestParam(defaultValue = "1") long pageNum,
                                      @RequestParam(defaultValue = "10") long pageSize) {
        UserContext.require();
        QueryWrapper<Course> qw = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like("course_name", keyword);
        }
        if (StringUtils.hasText(courseType)) {
            qw.eq("course_type", courseType);
        }
        if (status != null) {
            qw.eq("status", status);
        } else if (UserContext.isMember()) {
            qw.eq("status", 1);
        }
        qw.orderByAsc("id");
        return Result.success(courseService.page(new Page<>(pageNum, pageSize), qw));
    }

    @GetMapping("/options")
    public Result<List<Course>> options() {
        UserContext.require();
        return Result.success(courseService.list(new QueryWrapper<Course>().eq("status", 1).orderByAsc("id")));
    }

    @PostMapping
    public Result<Course> create(@RequestBody Course course) {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以维护课程");
        }
        long dup = courseService.count(new QueryWrapper<Course>()
                .eq("course_name", course.getCourseName()).eq("course_type", course.getCourseType()));
        if (dup > 0) {
            throw new BizException("同类型下已存在同名课程");
        }
        course.setStatus(1);
        course.setCreateTime(LocalDateTime.now());
        courseService.save(course);
        return Result.success(course);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Course course) {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以维护课程");
        }
        Course db = courseService.getById(id);
        if (db == null) {
            throw new BizException("课程不存在");
        }
        if (course.getCapacity() != null) {
            List<CourseSchedule> pending = scheduleService.list(new QueryWrapper<CourseSchedule>()
                    .eq("course_id", id).in("status", "WAITING", "ONGOING"));
            for (CourseSchedule s : pending) {
                if (course.getCapacity() < s.getBookedCount()) {
                    throw new BizException("容量不能小于已排课时的预约人数（" + s.getBookedCount() + " 人）");
                }
            }
        }
        course.setId(id);
        course.setCreateTime(null);
        courseService.updateById(course);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以上下架课程");
        }
        Course update = new Course();
        update.setId(id);
        update.setStatus(body.get("status"));
        courseService.updateById(update);
        return Result.success();
    }

    /** 删除课程：若已被排课引用则拒绝，保证历史数据完整 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以删除课程");
        }
        var db = courseService.getById(id);
        if (db == null) {
            throw new BizException("课程不存在");
        }
        long used = scheduleService.count(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.ncu.gym.entity.CourseSchedule>()
                .eq("course_id", id));
        if (used > 0) {
            throw new BizException("该课程已被 " + used + " 节课引用，无法删除，可改为停用");
        }
        courseService.removeById(id);
        return Result.success();
    }
}
