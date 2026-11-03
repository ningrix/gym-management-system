package com.ncu.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ncu.gym.common.BizException;
import com.ncu.gym.common.Result;
import com.ncu.gym.entity.CardType;
import com.ncu.gym.service.CardTypeService;
import com.ncu.gym.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会员卡类型
 */
@RestController
@RequestMapping("/api/card-types")
@RequiredArgsConstructor
public class CardTypeController {

    private final CardTypeService cardTypeService;

    @GetMapping
    public Result<List<CardType>> list(@RequestParam(required = false) Boolean onlyOnSale) {
        UserContext.require();
        QueryWrapper<CardType> qw = new QueryWrapper<>();
        if (Boolean.TRUE.equals(onlyOnSale)) {
            qw.eq("status", 1);
        }
        qw.orderByAsc("price");
        return Result.success(cardTypeService.list(qw));
    }

    @PostMapping
    public Result<CardType> create(@RequestBody CardType type) {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以维护卡种");
        }
        validate(type);
        long dup = cardTypeService.count(new QueryWrapper<CardType>().eq("type_name", type.getTypeName()));
        if (dup > 0) {
            throw new BizException("卡种名称已存在");
        }
        type.setStatus(1);
        type.setCreateTime(LocalDateTime.now());
        cardTypeService.save(type);
        return Result.success(type);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody CardType type) {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以维护卡种");
        }
        validate(type);
        CardType db = cardTypeService.getById(id);
        if (db == null) {
            throw new BizException("卡种不存在");
        }
        type.setId(id);
        type.setCardKind(db.getCardKind());
        type.setCreateTime(null);
        cardTypeService.updateById(type);
        return Result.success();
    }

    private void validate(CardType type) {
        if (type.getPrice() == null || type.getPrice().signum() <= 0) {
            throw new BizException("卡种价格必须大于 0");
        }
        if ("DURATION".equals(type.getCardKind()) && (type.getValidDays() == null || type.getValidDays() <= 0)) {
            throw new BizException("期限卡的有效天数必须大于 0");
        }
        if ("TIMES".equals(type.getCardKind()) && (type.getTotalTimes() == null || type.getTotalTimes() <= 0)) {
            throw new BizException("次卡的总次数必须大于 0");
        }
    }
}
