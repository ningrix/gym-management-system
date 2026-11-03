package com.ncu.gym.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ncu.gym.entity.CardRecharge;
import com.ncu.gym.entity.Coach;
import com.ncu.gym.entity.Course;
import com.ncu.gym.entity.CourseSchedule;
import com.ncu.gym.entity.Member;
import com.ncu.gym.entity.MemberCard;
import com.ncu.gym.entity.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统计报表：所有聚合在 Java 侧完成，避免依赖数据库方言的日期函数
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final MemberService memberService;
    private final CoachService coachService;
    private final CourseService courseService;
    private final CourseScheduleService scheduleService;
    private final MemberCardService memberCardService;
    private final CardRechargeService rechargeService;
    private final ReservationService reservationService;

    public Map<String, Object> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("memberTotal", memberService.count());
        data.put("activeCardTotal", memberCardService.count(
                new QueryWrapper<MemberCard>().eq("status", "ACTIVE")));
        List<CourseSchedule> today = scheduleService.list(new QueryWrapper<CourseSchedule>()
                .eq("class_date", LocalDate.now()).ne("status", "CANCELLED"));
        data.put("todayClassTotal", today.size());
        data.put("monthRevenue", monthRevenue());
        data.put("memberGrowth", memberGrowth(6));
        data.put("courseHot", courseHot(5));
        return data;
    }

    private BigDecimal monthRevenue() {
        YearMonth now = YearMonth.now();
        return rechargeService.list().stream()
                .filter(r -> YearMonth.from(r.getCreateTime()).equals(now))
                .map(CardRecharge::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 近 N 个月会员新增与累计 */
    public List<Map<String, Object>> memberGrowth(int months) {
        List<Member> members = memberService.list();
        List<Map<String, Object>> result = new ArrayList<>();
        YearMonth start = YearMonth.now().minusMonths(months - 1L);
        for (int i = 0; i < months; i++) {
            YearMonth ym = start.plusMonths(i);
            long newCount = members.stream()
                    .filter(m -> m.getJoinDate() != null && YearMonth.from(m.getJoinDate()).equals(ym))
                    .count();
            long total = members.stream()
                    .filter(m -> m.getJoinDate() != null && YearMonth.from(m.getJoinDate()).compareTo(ym) <= 0)
                    .count();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", ym.getYear() + "-" + String.format("%02d", ym.getMonthValue()));
            row.put("newCount", newCount);
            row.put("totalCount", total);
            result.add(row);
        }
        return result;
    }

    /** 课程热门度：预约人次 Top N 与平均满员率 */
    public List<Map<String, Object>> courseHot(int limit) {
        List<Reservation> reservations = reservationService.list(new QueryWrapper<Reservation>()
                .ne("status", "CANCELLED"));
        Map<Long, Long> countByCourse = new HashMap<>();
        Map<Long, CourseSchedule> scheduleMap = scheduleService.list().stream()
                .collect(Collectors.toMap(CourseSchedule::getId, s -> s));
        for (Reservation r : reservations) {
            CourseSchedule s = scheduleMap.get(r.getScheduleId());
            if (s != null) {
                countByCourse.merge(s.getCourseId(), 1L, Long::sum);
            }
        }
        Map<Long, Course> courses = courseService.list().stream()
                .collect(Collectors.toMap(Course::getId, c -> c));
        List<Map<String, Object>> result = new ArrayList<>();
        countByCourse.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .forEach(e -> {
                    Course c = courses.get(e.getKey());
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("courseName", c == null ? "-" : c.getCourseName());
                    row.put("reserveCount", e.getValue());
                    List<CourseSchedule> schedules = scheduleService.list(
                            new QueryWrapper<CourseSchedule>().eq("course_id", e.getKey()));
                    double rate = 0;
                    if (!schedules.isEmpty()) {
                        rate = schedules.stream()
                                .mapToDouble(s -> s.getCapacity() == 0 ? 0 : (double) s.getBookedCount() / s.getCapacity())
                                .average().orElse(0);
                    }
                    row.put("fullRate", Math.round(rate * 1000) / 10.0);
                    result.add(row);
                });
        return result;
    }

    /** 教练课时统计：当月已结束课程的节数与服务人数（按时间判断，不依赖状态字段） */
    public List<Map<String, Object>> coachWorkload(String month) {
        YearMonth ym = month == null || month.isBlank() ? YearMonth.now() : YearMonth.parse(month);
        LocalDateTime now = LocalDateTime.now();
        List<CourseSchedule> schedules = scheduleService.list().stream()
                .filter(s -> !"CANCELLED".equals(s.getStatus()))
                .filter(s -> !LocalDateTime.of(s.getClassDate(), s.getEndTime()).isAfter(now))
                .collect(Collectors.toList());
        List<Reservation> reservations = reservationService.list();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Coach coach : coachService.list(new QueryWrapper<Coach>().orderByAsc("id"))) {
            List<CourseSchedule> mine = schedules.stream()
                    .filter(s -> s.getCoachId().equals(coach.getId()))
                    .filter(s -> YearMonth.from(s.getClassDate()).equals(ym))
                    .collect(Collectors.toList());
            long memberCount = mine.stream()
                    .flatMap(s -> reservations.stream().filter(r -> r.getScheduleId().equals(s.getId())))
                    .map(Reservation::getMemberId)
                    .distinct().count();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("coachName", coach.getName());
            row.put("specialty", coach.getSpecialty());
            row.put("lessonCount", mine.size());
            row.put("memberCount", memberCount);
            result.add(row);
        }
        return result;
    }

    /** 近 N 个月会员卡营收 */
    public List<Map<String, Object>> revenue(int months) {
        List<CardRecharge> recharges = rechargeService.list();
        List<Map<String, Object>> result = new ArrayList<>();
        YearMonth start = YearMonth.now().minusMonths(months - 1L);
        for (int i = 0; i < months; i++) {
            YearMonth ym = start.plusMonths(i);
            List<CardRecharge> monthList = recharges.stream()
                    .filter(r -> r.getCreateTime() != null && YearMonth.from(r.getCreateTime()).equals(ym))
                    .collect(Collectors.toList());
            BigDecimal amount = monthList.stream().map(CardRecharge::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", ym.getYear() + "-" + String.format("%02d", ym.getMonthValue()));
            row.put("amount", amount);
            row.put("orderCount", monthList.size());
            result.add(row);
        }
        return result;
    }

    /** 会员端概览：我的卡、我的预约与最近体测 */
    public Map<String, Object> memberOverview(Long memberId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cards", memberCardService.listByMember(memberId));
        data.put("reservations", reservationService.listByMember(memberId, null).stream()
                .limit(5).collect(Collectors.toList()));
        data.put("latestNotice", null);
        return data;
    }
}
