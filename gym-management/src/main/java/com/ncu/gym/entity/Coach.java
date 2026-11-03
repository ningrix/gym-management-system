package com.ncu.gym.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("coach")
public class Coach {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String coachNo;
    private String name;
    private Integer gender;
    private String phone;
    private String specialty;
    private String coachLevel;
    private LocalDate hireDate;
    private BigDecimal hourlyWage;
    private String intro;
    private Integer status;
    private LocalDateTime createTime;
}
