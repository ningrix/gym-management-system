package com.ncu.gym.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 预约视图：联表带出会员、课程、教练与上课时间
 */
@Data
public class ReservationVO {

    private Long id;
    private String reserveNo;
    private Long scheduleId;
    private Long memberId;
    private String memberName;
    private String memberNo;
    private String courseName;
    private String coachName;
    private LocalDate classDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;
    private String status;
    private LocalDateTime reserveTime;
    private LocalDateTime checkInTime;
    private String remark;
    private Integer positionNo;
}
