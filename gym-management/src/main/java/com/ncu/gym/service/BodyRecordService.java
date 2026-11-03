package com.ncu.gym.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ncu.gym.entity.BodyRecord;
import com.ncu.gym.mapper.BodyRecordMapper;
import org.springframework.stereotype.Service;

@Service
public class BodyRecordService extends ServiceImpl<BodyRecordMapper, BodyRecord> {
}
