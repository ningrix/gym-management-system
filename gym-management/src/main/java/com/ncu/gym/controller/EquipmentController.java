package com.ncu.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ncu.gym.common.BizException;
import com.ncu.gym.common.Result;
import com.ncu.gym.entity.Equipment;
import com.ncu.gym.service.EquipmentService;
import com.ncu.gym.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 器材台账与状态流转
 */
@RestController
@RequestMapping("/api/equipments")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String category,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "12") long pageSize) {
        UserContext.require();
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以查看器材台账");
        }
        QueryWrapper<Equipment> qw = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like("equipment_name", keyword).or().like("equipment_no", keyword)
                    .or().like("brand", keyword));
        }
        if (StringUtils.hasText(category)) {
            qw.eq("category", category);
        }
        if (StringUtils.hasText(status)) {
            qw.eq("status", status);
        }
        qw.orderByAsc("id");
        IPage<Equipment> page = equipmentService.page(new Page<>(pageNum, pageSize), qw);

        List<Equipment> all = equipmentService.list();
        Map<String, Long> byStatus = new HashMap<>();
        Map<String, Long> byCategory = new HashMap<>();
        for (Equipment e : all) {
            byStatus.merge(e.getStatus(), 1L, Long::sum);
            byCategory.merge(e.getCategory(), 1L, Long::sum);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("page", page);
        data.put("byStatus", byStatus);
        data.put("byCategory", byCategory);
        data.put("total", all.size());
        return Result.success(data);
    }

    @PostMapping
    public Result<Equipment> create(@RequestBody Equipment equipment) {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以登记器材");
        }
        if (!StringUtils.hasText(equipment.getEquipmentNo()) || !StringUtils.hasText(equipment.getEquipmentName())) {
            throw new BizException("器材编号与名称不能为空");
        }
        long dup = equipmentService.count(new QueryWrapper<Equipment>()
                .eq("equipment_no", equipment.getEquipmentNo()));
        if (dup > 0) {
            throw new BizException("器材编号已存在");
        }
        equipment.setStatus("NORMAL");
        equipment.setCreateTime(LocalDateTime.now());
        equipmentService.save(equipment);
        return Result.success(equipment);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Equipment equipment) {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以修改器材信息");
        }
        Equipment db = equipmentService.getById(id);
        if (db == null) {
            throw new BizException("器材不存在");
        }
        equipment.setId(id);
        equipment.setStatus(null);
        equipment.setCreateTime(null);
        equipmentService.updateById(equipment);
        return Result.success();
    }

    /** 状态流转：正常 → 报修 → 归位 / 报废 */
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以变更器材状态");
        }
        Equipment db = equipmentService.getById(id);
        if (db == null) {
            throw new BizException("器材不存在");
        }
        String status = (String) body.get("status");
        if ("SCRAPPED".equals(db.getStatus())) {
            throw new BizException("已报废的器材不能再次变更状态");
        }
        Equipment update = new Equipment();
        update.setId(id);
        update.setStatus(status);
        if ("REPAIR".equals(status)) {
            String fault = (String) body.get("faultDesc");
            if (!StringUtils.hasText(fault)) {
                throw new BizException("报修必须填写故障描述");
            }
            update.setFaultDesc(fault);
            update.setReportTime(LocalDateTime.now());
        } else if ("NORMAL".equals(status)) {
            if (body.get("repairCost") != null) {
                update.setRepairCost(new java.math.BigDecimal(body.get("repairCost").toString()));
            }
            update.setRepairFinishTime(LocalDateTime.now());
        }
        equipmentService.updateById(update);
        return Result.success();
    }

    /** 删除器材：维修中不可删，避免状态丢失 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以删除器材");
        }
        var db = equipmentService.getById(id);
        if (db == null) {
            throw new BizException("器材不存在");
        }
        if ("REPAIR".equals(db.getStatus())) {
            throw new BizException("维修中的器材请先完成维修或报废后再删除");
        }
        equipmentService.removeById(id);
        return Result.success();
    }
}
