package com.ncu.gym.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ncu.gym.common.BizException;
import com.ncu.gym.entity.Coach;
import com.ncu.gym.entity.Course;
import com.ncu.gym.entity.CourseSchedule;
import com.ncu.gym.entity.Reservation;
import com.ncu.gym.mapper.CoachMapper;
import com.ncu.gym.mapper.CourseScheduleMapper;
import com.ncu.gym.mapper.ReservationMapper;
import com.ncu.gym.vo.ScheduleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseScheduleService extends ServiceImpl<CourseScheduleMapper, CourseSchedule> {

    private final CourseService courseService;
    private final CoachMapper coachMapper;
    private final ReservationMapper reservationMapper;

    /** 按条件查询课表，返回带课程名与教练名的视图对象 */
    public List<ScheduleVO> listSchedules(LocalDate startDate, LocalDate endDate, Long coachId,
                                          Long courseId, String status, Long scopeCoachId) {
        QueryWrapper<CourseSchedule> qw = new QueryWrapper<>();
        if (startDate != null) {
            qw.ge("class_date", startDate);
        }
        if (endDate != null) {
            qw.le("class_date", endDate);
        }
        if (coachId != null) {
            qw.eq("coach_id", coachId);
        }
        if (courseId != null) {
            qw.eq("course_id", courseId);
        }
        if (StringUtils.hasText(status)) {
            qw.eq("status", status);
        }
        if (scopeCoachId != null) {
            qw.eq("coach_id", scopeCoachId);
        }
        qw.orderByAsc("class_date", "start_time");
        List<CourseSchedule> list = list(qw);
        return toVoList(list);
    }

    /**
     * 按当前时间推导排课的实际状态：
     * 已取消的保持原样，其余按“未开始 / 进行中 / 已结束”实时计算，
     * 避免种子数据或长期未维护的记录把状态停留在 WAITING/ONGOING。
     */
    public String effectiveStatus(CourseSchedule s) {
        if ("CANCELLED".equals(s.getStatus())) {
            return "CANCELLED";
        }
        LocalDateTime start = LocalDateTime.of(s.getClassDate(), s.getStartTime());
        LocalDateTime end = LocalDateTime.of(s.getClassDate(), s.getEndTime());
        LocalDateTime now = LocalDateTime.now();
        if (!now.isBefore(end)) {
            return "FINISHED";
        }
        if (!now.isBefore(start)) {
            return "ONGOING";
        }
        return "WAITING";
    }

    public List<ScheduleVO> toVoList(List<CourseSchedule> list) {
        if (list.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Course> courses = courseService.list().stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));
        Map<Long, Coach> coaches = coachMapper.selectList(null).stream()
                .collect(Collectors.toMap(Coach::getId, Function.identity()));
        List<ScheduleVO> result = new ArrayList<>(list.size());
        for (CourseSchedule s : list) {
            ScheduleVO vo = new ScheduleVO();
            vo.setId(s.getId());
            vo.setCourseId(s.getCourseId());
            vo.setCoachId(s.getCoachId());
            vo.setClassDate(s.getClassDate());
            vo.setStartTime(s.getStartTime());
            vo.setEndTime(s.getEndTime());
            vo.setRoom(s.getRoom());
            vo.setCapacity(s.getCapacity());
            vo.setBookedCount(s.getBookedCount());
            vo.setRemain(Math.max(0, s.getCapacity() - s.getBookedCount()));
            vo.setStatus(effectiveStatus(s));
            vo.setRemark(s.getRemark());
            Course c = courses.get(s.getCourseId());
            if (c != null) {
                vo.setCourseName(c.getCourseName());
                vo.setCourseType(c.getCourseType());
                vo.setDifficulty(c.getDifficulty());
                vo.setDuration(c.getDuration());
            }
            Coach coach = coaches.get(s.getCoachId());
            if (coach != null) {
                vo.setCoachName(coach.getName());
            }
            result.add(vo);
        }
        result.sort(Comparator.comparing(ScheduleVO::getClassDate).thenComparing(ScheduleVO::getStartTime));
        return result;
    }

    public ScheduleVO detail(Long id) {
        CourseSchedule s = getById(id);
        if (s == null) {
            throw new BizException("排课不存在或已被删除");
        }
        return toVoList(List.of(s)).get(0);
    }

    /** 新增排课：教练冲突、教室冲突、课程状态三重校验 */
    @Transactional(rollbackFor = Exception.class)
    public CourseSchedule createSchedule(CourseSchedule schedule) {
        if (schedule.getCourseId() == null || schedule.getCoachId() == null
                || schedule.getClassDate() == null || schedule.getStartTime() == null || schedule.getEndTime() == null) {
            throw new BizException("课程、教练、日期与时间不能为空");
        }
        if (!schedule.getEndTime().isAfter(schedule.getStartTime())) {
            throw new BizException("结束时间必须晚于开始时间");
        }
        if (schedule.getClassDate().isBefore(LocalDate.now())) {
            throw new BizException("不能为已过去的日期排课");
        }
        Course course = courseService.getById(schedule.getCourseId());
        if (course == null || course.getStatus() == null || course.getStatus() != 1) {
            throw new BizException("课程不存在或已下架");
        }
        Coach coach = coachMapper.selectById(schedule.getCoachId());
        if (coach == null || coach.getStatus() == null || coach.getStatus() != 1) {
            throw new BizException("教练不存在或已离职");
        }
        if (schedule.getCapacity() == null || schedule.getCapacity() < 1) {
            schedule.setCapacity(course.getCapacity());
        }

        List<CourseSchedule> sameDay = list(new QueryWrapper<CourseSchedule>()
                .eq("class_date", schedule.getClassDate())
                .ne("status", "CANCELLED"));
        for (CourseSchedule s : sameDay) {
            boolean overlap = schedule.getStartTime().isBefore(s.getEndTime())
                    && schedule.getEndTime().isAfter(s.getStartTime());
            if (!overlap) {
                continue;
            }
            if (s.getCoachId().equals(schedule.getCoachId())) {
                throw new BizException("该教练在此时间段已有排课，请更换时间");
            }
            if (s.getRoom() != null && s.getRoom().equals(schedule.getRoom())) {
                throw new BizException("该教室在此时间段已被占用，请更换教室");
            }
        }

        schedule.setBookedCount(0);
        schedule.setStatus("WAITING");
        schedule.setCreateTime(LocalDateTime.now());
        save(schedule);
        return schedule;
    }

    @Transactional(rollbackFor = Exception.class)
    public CourseSchedule updateSchedule(CourseSchedule schedule) {
        CourseSchedule db = getById(schedule.getId());
        if (db == null) {
            throw new BizException("排课不存在");
        }
        if (schedule.getCapacity() != null && schedule.getCapacity() < db.getBookedCount()) {
            throw new BizException("容量不能小于已预约人数（" + db.getBookedCount() + " 人）");
        }
        schedule.setBookedCount(db.getBookedCount());
        updateById(schedule);
        return getById(schedule.getId());
    }

    /** 取消排课：排课置为已取消，名下预约批量取消，已预约人数归零 */
    @Transactional(rollbackFor = Exception.class)
    public int cancelSchedule(Long id, String reason) {
        CourseSchedule db = getById(id);
        if (db == null) {
            throw new BizException("排课不存在");
        }
        if ("FINISHED".equals(effectiveStatus(db))) {
            throw new BizException("课程已结束，无法取消");
        }
        CourseSchedule update = new CourseSchedule();
        update.setId(id);
        update.setStatus("CANCELLED");
        update.setBookedCount(0);
        update.setRemark(StringUtils.hasText(reason) ? reason : "课程取消");
        updateById(update);

        List<Reservation> reservations = reservationMapper.selectList(new QueryWrapper<Reservation>()
                .eq("schedule_id", id).in("status", "BOOKED", "CHECKED_IN"));
        for (Reservation r : reservations) {
            Reservation cancel = new Reservation();
            cancel.setId(r.getId());
            cancel.setStatus("CANCELLED");
            cancel.setCancelTime(LocalDateTime.now());
            cancel.setRemark("课程取消");
            reservationMapper.updateById(cancel);
        }
        return reservations.size();
    }
}
