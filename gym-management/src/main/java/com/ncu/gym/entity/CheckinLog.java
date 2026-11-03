package com.ncu.gym.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@Data
@TableName("checkin_log")
public class CheckinLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memberId;
    private String checkinType;
    private Long refId;
    private LocalDateTime createTime;
}
