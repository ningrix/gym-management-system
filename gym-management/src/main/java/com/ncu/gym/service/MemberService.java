package com.ncu.gym.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ncu.gym.common.BizException;
import com.ncu.gym.entity.Member;
import com.ncu.gym.entity.SysUser;
import com.ncu.gym.mapper.MemberMapper;
import com.ncu.gym.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MemberService extends ServiceImpl<MemberMapper, Member> {

    private final SysUserService userService;
    private final com.ncu.gym.mapper.MemberCardMapper memberCardMapper;
    private final com.ncu.gym.mapper.ReservationMapper reservationMapper;

    /** 分页查询：教练只能看到自己课程下的会员 */
    public IPage<Member> pageMembers(String keyword, Integer status, long pageNum, long pageSize, Long scopeCoachId) {
        QueryWrapper<Member> qw = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like("name", keyword).or().like("phone", keyword).or().like("member_no", keyword));
        }
        if (status != null) {
            qw.eq("status", status);
        }
        if (scopeCoachId != null) {
            qw.inSql("id", "select distinct r.member_id from reservation r "
                    + "join course_schedule s on r.schedule_id = s.id where s.coach_id = " + scopeCoachId);
        }
        qw.orderByDesc("id");
        return page(new Page<>(pageNum, pageSize), qw);
    }

    /** 新增会员：同时开账号、生成会员编号，两表要么都成功要么都不落库 */
    @Transactional(rollbackFor = Exception.class)
    public Member createMember(Member member) {
        if (!StringUtils.hasText(member.getName())) {
            throw new BizException("会员姓名不能为空");
        }
        if (!StringUtils.hasText(member.getPhone())) {
            throw new BizException("手机号不能为空");
        }
        long exists = count(new QueryWrapper<Member>().eq("phone", member.getPhone()));
        if (exists > 0) {
            throw new BizException("该手机号已登记为会员");
        }

        long next = count() + 1;
        String memberNo = "M" + LocalDate.now().getYear() + String.format("%03d", next);
        while (count(new QueryWrapper<Member>().eq("member_no", memberNo)) > 0) {
            next++;
            memberNo = "M" + LocalDate.now().getYear() + String.format("%03d", next);
        }

        String username = "member" + String.format("%02d", next);
        while (userService.count(new QueryWrapper<SysUser>().eq("username", username)) > 0) {
            next++;
            username = "member" + String.format("%02d", next);
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(PasswordUtil.encode("gym2026"));
        user.setRole("MEMBER");
        user.setRealName(member.getName());
        user.setPhone(member.getPhone());
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        userService.save(user);

        member.setUserId(user.getId());
        member.setMemberNo(memberNo);
        member.setStatus(1);
        member.setJoinDate(member.getJoinDate() == null ? LocalDate.now() : member.getJoinDate());
        member.setCreateTime(LocalDateTime.now());
        save(member);
        return member;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Member member) {
        Member db = getById(member.getId());
        if (db == null) {
            throw new BizException("会员不存在");
        }
        long dup = count(new QueryWrapper<Member>().eq("phone", member.getPhone()).ne("id", member.getId()));
        if (dup > 0) {
            throw new BizException("该手机号已被其他会员使用");
        }
        member.setUserId(db.getUserId());
        member.setMemberNo(db.getMemberNo());
        updateById(member);

        SysUser user = new SysUser();
        user.setId(db.getUserId());
        user.setRealName(member.getName());
        user.setPhone(member.getPhone());
        userService.updateById(user);
    }

    public void changeStatus(Long id, Integer status) {
        Member member = getById(id);
        if (member == null) {
            throw new BizException("会员不存在");
        }
        // 停用前校验：仍持有有效会员卡或未结束预约时不允许直接停用，避免卡与账号状态脱节
        if (status != null && status == 0) {
            long activeCards = memberCardMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.ncu.gym.entity.MemberCard>()
                            .eq("member_id", id).eq("status", "ACTIVE"));
            if (activeCards > 0) {
                throw new BizException("该会员仍有 " + activeCards + " 张有效会员卡，请先冻结或退卡后再停用");
            }
            long activeRes = reservationMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.ncu.gym.entity.Reservation>()
                            .eq("member_id", id).in("status", "BOOKED", "CHECKED_IN"));
            if (activeRes > 0) {
                throw new BizException("该会员还有 " + activeRes + " 条未完成的预约，请先处理后再停用");
            }
        }
        Member update = new Member();
        update.setId(id);
        update.setStatus(status);
        updateById(update);

        SysUser user = new SysUser();
        user.setId(member.getUserId());
        user.setStatus(status);
        userService.updateById(user);
    }
}
