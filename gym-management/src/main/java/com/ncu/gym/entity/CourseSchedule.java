package com.ncu.gym.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Data;

@Data
@TableName("course_schedule")
public class CourseSchedule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;
    private Long coachId;
    private LocalDate classDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;
    private Integer capacity;
    private Integer bookedCount;
    private String status;
    private String remark;
    private LocalDateTime createTime;

    private Integer positionTotal;
}
