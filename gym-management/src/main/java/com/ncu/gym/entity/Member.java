package com.ncu.gym.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("member")
public class Member {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String memberNo;
    private String name;
    private Integer gender;
    private LocalDate birthday;
    private String phone;
    private String idCard;
    private BigDecimal height;
    private LocalDate joinDate;
    private String source;
    private String remark;
    private Integer status;
    private LocalDateTime createTime;
}
