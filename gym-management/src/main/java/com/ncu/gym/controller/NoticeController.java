package com.ncu.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ncu.gym.common.BizException;
import com.ncu.gym.common.Result;
import com.ncu.gym.entity.Notice;
import com.ncu.gym.service.NoticeService;
import com.ncu.gym.util.LoginUser;
import com.ncu.gym.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 公告管理
 */
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public Result<IPage<Notice>> list(@RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Integer status,
                                      @RequestParam(defaultValue = "1") long pageNum,
                                      @RequestParam(defaultValue = "10") long pageSize) {
        LoginUser login = UserContext.require();
        QueryWrapper<Notice> qw = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like("title", keyword);
        }
        if (UserContext.isAdmin()) {
            if (status != null) {
                qw.eq("status", status);
            }
        } else {
            qw.eq("status", 1);
            qw.and(w -> w.eq("target_role", "ALL").or().eq("target_role", login.getRole()));
        }
        qw.orderByDesc("publish_time").orderByDesc("id");
        return Result.success(noticeService.page(new Page<>(pageNum, pageSize), qw));
    }

    @GetMapping("/latest")
    public Result<List<Notice>> latest(@RequestParam(defaultValue = "5") int limit) {
        LoginUser login = UserContext.require();
        QueryWrapper<Notice> qw = new QueryWrapper<Notice>().eq("status", 1);
        if (!UserContext.isAdmin()) {
            qw.and(w -> w.eq("target_role", "ALL").or().eq("target_role", login.getRole()));
        }
        qw.orderByDesc("publish_time").last("limit " + Math.max(1, Math.min(limit, 20)));
        return Result.success(noticeService.list(qw));
    }

    @PostMapping
    public Result<Notice> create(@RequestBody Notice notice) {
        LoginUser login = UserContext.require();
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以发布公告");
        }
        if (!StringUtils.hasText(notice.getTitle())) {
            throw new BizException("公告标题不能为空");
        }
        notice.setPublisherId(login.getUserId());
        notice.setViewCount(0);
        notice.setCreateTime(LocalDateTime.now());
        if (notice.getStatus() == null) {
            notice.setStatus(1);
        }
        if (notice.getStatus() == 1) {
            notice.setPublishTime(LocalDateTime.now());
        }
        if (!StringUtils.hasText(notice.getTargetRole())) {
            notice.setTargetRole("ALL");
        }
        noticeService.save(notice);
        return Result.success(notice);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Notice notice) {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以编辑公告");
        }
        notice.setId(id);
        notice.setCreateTime(null);
        noticeService.updateById(notice);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以上下线公告");
        }
        Notice update = new Notice();
        update.setId(id);
        update.setStatus(body.get("status"));
        if (body.get("status") != null && body.get("status") == 1) {
            update.setPublishTime(LocalDateTime.now());
        }
        noticeService.updateById(update);
        return Result.success();
    }

    /** 删除公告（物理删除，仅管理员） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以删除公告");
        }
        if (noticeService.getById(id) == null) {
            throw new BizException("公告不存在");
        }
        noticeService.removeById(id);
        return Result.success();
    }
}
