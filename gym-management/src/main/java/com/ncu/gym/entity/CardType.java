package com.ncu.gym.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("card_type")
public class CardType {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String typeName;
    private String cardKind;
    private Integer validDays;
    private Integer totalTimes;
    private BigDecimal price;
    private String description;
    private Integer status;
    private LocalDateTime createTime;
}
