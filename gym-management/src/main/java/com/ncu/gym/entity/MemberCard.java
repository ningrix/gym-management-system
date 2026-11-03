package com.ncu.gym.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("member_card")
public class MemberCard {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String cardNo;
    private Long memberId;
    private Long cardTypeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalTimes;
    private Integer remainTimes;
    private String status;
    private String freezeReason;
    private Integer renewCount;
    private LocalDateTime createTime;
}
