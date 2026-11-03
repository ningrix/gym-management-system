package com.ncu.gym.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ncu.gym.entity.CardType;
import com.ncu.gym.mapper.CardTypeMapper;
import org.springframework.stereotype.Service;

@Service
public class CardTypeService extends ServiceImpl<CardTypeMapper, CardType> {
}
