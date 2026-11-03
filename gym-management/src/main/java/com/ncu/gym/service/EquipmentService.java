package com.ncu.gym.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ncu.gym.entity.Equipment;
import com.ncu.gym.mapper.EquipmentMapper;
import org.springframework.stereotype.Service;

@Service
public class EquipmentService extends ServiceImpl<EquipmentMapper, Equipment> {
}
