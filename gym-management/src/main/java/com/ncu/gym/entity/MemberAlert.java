package com.ncu.gym.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@Data
@TableName("member_alert")
public class MemberAlert {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memberId;
    private String alertType;
    private String content;
    private String status;
    private String handleNote;
    private LocalDateTime createTime;
    private LocalDateTime handleTime;
}
