package com.ncu.gym.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("body_record")
public class BodyRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memberId;
    private LocalDate recordDate;
    private BigDecimal height;
    private BigDecimal weight;
    private BigDecimal bmi;
    private BigDecimal bodyFat;
    private BigDecimal muscle;
    private BigDecimal waist;
    private Integer heartRate;
    private Long coachId;
    private String remark;
    private LocalDateTime createTime;
}
