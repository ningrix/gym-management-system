package com.ncu.gym.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("notice")
public class Notice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String content;
    private Long publisherId;
    private String targetRole;
    private Integer status;
    private LocalDateTime publishTime;
    private Integer viewCount;
    private LocalDateTime createTime;
}
