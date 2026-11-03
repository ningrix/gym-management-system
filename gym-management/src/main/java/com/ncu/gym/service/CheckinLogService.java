package com.ncu.gym.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ncu.gym.entity.CheckinLog;
import com.ncu.gym.mapper.CheckinLogMapper;
import org.springframework.stereotype.Service;

@Service
public class CheckinLogService extends ServiceImpl<CheckinLogMapper, CheckinLog> {
}
