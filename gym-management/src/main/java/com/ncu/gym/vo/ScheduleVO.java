package com.ncu.gym.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 排课视图：联表带出课程名、教练名与剩余名额
 */
@Data
public class ScheduleVO {

    private Long id;
    private Long courseId;
    private String courseName;
    private String courseType;
    private Integer difficulty;
    private Integer duration;
    private Long coachId;
    private String coachName;
    private LocalDate classDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;
    private Integer capacity;
    private Integer bookedCount;
    private Integer remain;
    private String status;
    private String remark;
}
