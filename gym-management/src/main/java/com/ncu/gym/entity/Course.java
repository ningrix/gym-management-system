package com.ncu.gym.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("course")
public class Course {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String courseName;
    private String courseType;
    private Integer difficulty;
    private Integer duration;
    private Integer capacity;
    private BigDecimal price;
    private Long coachId;
    private String description;
    private Integer status;
    private LocalDateTime createTime;
}
