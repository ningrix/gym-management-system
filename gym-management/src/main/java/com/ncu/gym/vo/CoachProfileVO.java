package com.ncu.gym.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 教练风采展示用的只读视图：刻意不包含手机号、课时费、用户 ID 等内部字段。
 */
@Data
public class CoachProfileVO {

    private Long id;

    private String name;

    private String specialty;

    private String coachLevel;

    private LocalDate hireDate;

    /** 由入职日期推算的教龄（年） */
    private Integer yearsOfService;

    private String intro;
}
