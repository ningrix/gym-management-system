package com.ncu.gym.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("reservation")
public class Reservation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String reserveNo;
    private Long scheduleId;
    private Long memberId;
    private Long memberCardId;
    private String status;
    private LocalDateTime reserveTime;
    private LocalDateTime checkInTime;
    private LocalDateTime cancelTime;
    private String remark;
    private LocalDateTime createTime;

    private Integer positionNo;
    private String cancelType;
}
