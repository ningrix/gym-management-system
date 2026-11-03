package com.ncu.gym.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("card_recharge")
public class CardRecharge {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String rechargeNo;
    private Long memberCardId;
    private Long memberId;
    private Long cardTypeId;
    private BigDecimal amount;
    private String payType;
    private Integer addDays;
    private Integer addTimes;
    private Long operatorId;
    private String remark;
    private LocalDateTime createTime;
}
