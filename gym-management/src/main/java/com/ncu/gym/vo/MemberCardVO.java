package com.ncu.gym.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 会员卡视图：联表带出会员姓名与卡种名称
 */
@Data
public class MemberCardVO {

    private Long id;
    private String cardNo;
    private Long memberId;
    private String memberName;
    private String memberNo;
    private Long cardTypeId;
    private String typeName;
    private String cardKind;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalTimes;
    private Integer remainTimes;
    private String status;
    private String freezeReason;
    private Integer renewCount;
    private Long remainDays;
}
