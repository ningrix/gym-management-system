package com.ncu.gym.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@Data
@TableName("waitlist")
public class Waitlist {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long scheduleId;
    private Long memberId;
    private Long memberCardId;
    private String status;
    private LocalDateTime enqueueTime;
    private LocalDateTime createTime;
}
