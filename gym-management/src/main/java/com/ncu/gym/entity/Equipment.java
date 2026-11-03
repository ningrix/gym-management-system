package com.ncu.gym.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("equipment")
public class Equipment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String equipmentNo;
    private String equipmentName;
    private String category;
    private String brand;
    private LocalDate purchaseDate;
    private BigDecimal price;
    private String location;
    private String status;
    private String faultDesc;
    private LocalDateTime reportTime;
    private LocalDateTime repairFinishTime;
    private BigDecimal repairCost;
    private String remark;
    private LocalDateTime createTime;
}
