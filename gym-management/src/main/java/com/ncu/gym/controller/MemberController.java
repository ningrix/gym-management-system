package com.ncu.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ncu.gym.common.BizException;
import com.ncu.gym.common.Result;
import com.ncu.gym.entity.BodyRecord;
import com.ncu.gym.entity.Member;
import com.ncu.gym.service.BodyRecordService;
import com.ncu.gym.service.MemberCardService;
import com.ncu.gym.service.MemberService;
import com.ncu.gym.util.LoginUser;
import com.ncu.gym.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 会员管理
 */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberCardService memberCardService;
    private final BodyRecordService bodyRecordService;

    @GetMapping
    public Result<IPage<Member>> list(@RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Integer status,
                                      @RequestParam(defaultValue = "1") long pageNum,
                                      @RequestParam(defaultValue = "10") long pageSize) {
        LoginUser login = UserContext.require();
        Long scopeCoachId = "COACH".equals(login.getRole()) ? login.getBizId() : null;
        if ("MEMBER".equals(login.getRole())) {
            IPage<Member> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
            Member self = memberService.getById(login.getBizId());
            if (self != null) {
                self.setIdCard(maskId(self.getIdCard()));
                page.setRecords(java.util.List.of(self));
                page.setTotal(1);
            }
            return Result.success(page);
        }
        IPage<Member> page = memberService.pageMembers(keyword, status, pageNum, pageSize, scopeCoachId);
        page.getRecords().forEach(m -> m.setIdCard(maskId(m.getIdCard())));
        return Result.success(page);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        LoginUser login = UserContext.require();
        if ("MEMBER".equals(login.getRole()) && !id.equals(login.getBizId())) {
            throw new BizException(403, "只能查看本人档案");
        }
        Member member = memberService.getById(id);
        if (member == null) {
            throw new BizException("会员不存在");
        }
        member.setIdCard(UserContext.isAdmin() ? member.getIdCard() : maskId(member.getIdCard()));
        Map<String, Object> data = new HashMap<>();
        data.put("member", member);
        data.put("cards", memberCardService.listByMember(id));
        data.put("bodyRecords", bodyRecordService.list(new QueryWrapper<BodyRecord>()
                .eq("member_id", id).orderByDesc("record_date")));
        return Result.success(data);
    }

    @PostMapping
    public Result<Member> create(@RequestBody Member member) {
        UserContext.require();
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以新增会员");
        }
        return Result.success(memberService.createMember(member));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Member member) {
        LoginUser login = UserContext.require();
        if ("MEMBER".equals(login.getRole()) && !id.equals(login.getBizId())) {
            throw new BizException(403, "只能修改本人档案");
        }
        member.setId(id);
        if (!UserContext.isAdmin()) {
            member.setStatus(null);
            member.setMemberNo(null);
        }
        memberService.updateProfile(member);
        return Result.success();
    }

    private String maskId(String id) {
        if (id == null || id.length() < 8) {
            return id;
        }
        return id.substring(0, 6) + "********" + id.substring(id.length() - 4);
    }

    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以启停会员");
        }
        memberService.changeStatus(id, body.get("status"));
        return Result.success();
    }
}
