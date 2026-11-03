package com.ncu.gym.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ncu.gym.common.BizException;
import com.ncu.gym.entity.CardRecharge;
import com.ncu.gym.entity.CardType;
import com.ncu.gym.entity.Member;
import com.ncu.gym.entity.MemberCard;
import com.ncu.gym.mapper.MemberCardMapper;
import com.ncu.gym.vo.MemberCardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberCardService extends ServiceImpl<MemberCardMapper, MemberCard> {

    private final MemberService memberService;
    private final CardTypeService cardTypeService;
    private final CardRechargeService rechargeService;

    public IPage<MemberCardVO> pageCards(String keyword, String status, long pageNum, long pageSize) {
        QueryWrapper<MemberCard> qw = new QueryWrapper<>();
        if (StringUtils.hasText(status)) {
            qw.eq("status", status);
        }
        if (StringUtils.hasText(keyword)) {
            List<Long> memberIds = memberService.list(new QueryWrapper<Member>()
                            .like("name", keyword).or().like("member_no", keyword))
                    .stream().map(Member::getId).collect(Collectors.toList());
            qw.and(w -> {
                w.like("card_no", keyword);
                if (!memberIds.isEmpty()) {
                    w.or().in("member_id", memberIds);
                }
            });
        }
        qw.orderByDesc("id");
        IPage<MemberCard> page = page(new Page<>(pageNum, pageSize), qw);
        return page.convert(card -> toVo(card));
    }

    public MemberCardVO toVo(MemberCard card) {
        MemberCardVO vo = new MemberCardVO();
        vo.setId(card.getId());
        vo.setCardNo(card.getCardNo());
        vo.setMemberId(card.getMemberId());
        vo.setCardTypeId(card.getCardTypeId());
        vo.setStartDate(card.getStartDate());
        vo.setEndDate(card.getEndDate());
        vo.setTotalTimes(card.getTotalTimes());
        vo.setRemainTimes(card.getRemainTimes());
        vo.setStatus(card.getStatus());
        vo.setFreezeReason(card.getFreezeReason());
        vo.setRenewCount(card.getRenewCount());
        Member member = memberService.getById(card.getMemberId());
        if (member != null) {
            vo.setMemberName(member.getName());
            vo.setMemberNo(member.getMemberNo());
        }
        CardType type = cardTypeService.getById(card.getCardTypeId());
        if (type != null) {
            vo.setTypeName(type.getTypeName());
            vo.setCardKind(type.getCardKind());
        }
        if (card.getEndDate() != null) {
            vo.setRemainDays(ChronoUnit.DAYS.between(LocalDate.now(), card.getEndDate()));
        }
        return vo;
    }

    /** 开卡：写入持卡记录与缴费流水，两表同事务 */
    @Transactional(rollbackFor = Exception.class)
    public MemberCard openCard(Long memberId, Long cardTypeId, String payType, BigDecimal amount, Long operatorId) {
        Member member = memberService.getById(memberId);
        if (member == null) {
            throw new BizException("会员不存在");
        }
        if (member.getStatus() == null || member.getStatus() != 1) {
            throw new BizException("该会员已停用，不能开卡");
        }
        CardType type = cardTypeService.getById(cardTypeId);
        if (type == null) {
            throw new BizException("卡种不存在");
        }
        long exist = count(new QueryWrapper<MemberCard>()
                .eq("member_id", memberId).eq("card_type_id", cardTypeId).eq("status", "ACTIVE"));
        if (exist > 0) {
            throw new BizException("该会员已持有一张未到期的同类型卡，请直接办理续费");
        }
        BigDecimal realAmount = amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 ? type.getPrice() : amount;

        MemberCard card = new MemberCard();
        card.setCardNo(nextCardNo());
        card.setMemberId(memberId);
        card.setCardTypeId(cardTypeId);
        card.setStartDate(LocalDate.now());
        if ("DURATION".equals(type.getCardKind())) {
            card.setEndDate(LocalDate.now().plusDays(type.getValidDays() - 1L));
            card.setTotalTimes(0);
            card.setRemainTimes(0);
        } else {
            card.setEndDate(null);
            card.setTotalTimes(type.getTotalTimes());
            card.setRemainTimes(type.getTotalTimes());
        }
        card.setStatus("ACTIVE");
        card.setRenewCount(0);
        card.setCreateTime(LocalDateTime.now());
        save(card);

        saveRecharge(card, type, realAmount, payType, operatorId,
                "DURATION".equals(type.getCardKind()) ? type.getValidDays() : 0,
                "TIMES".equals(type.getCardKind()) ? type.getTotalTimes() : 0);
        return card;
    }

    /** 续费：期限卡顺延到期日，次卡累加次数，并写入流水 */
    @Transactional(rollbackFor = Exception.class)
    public MemberCard renewCard(Long cardId, Long cardTypeId, String payType, BigDecimal amount, Long operatorId) {
        MemberCard card = getById(cardId);
        if (card == null) {
            throw new BizException("会员卡不存在");
        }
        if ("REFUNDED".equals(card.getStatus())) {
            throw new BizException("该卡已退卡，不能续费");
        }
        CardType type = cardTypeService.getById(cardTypeId == null ? card.getCardTypeId() : cardTypeId);
        if (type == null) {
            throw new BizException("卡种不存在");
        }
        BigDecimal realAmount = amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 ? type.getPrice() : amount;

        MemberCard update = new MemberCard();
        update.setId(cardId);
        update.setRenewCount((card.getRenewCount() == null ? 0 : card.getRenewCount()) + 1);
        int addDays = 0;
        int addTimes = 0;
        if ("DURATION".equals(type.getCardKind())) {
            LocalDate base = card.getEndDate() == null || card.getEndDate().isBefore(LocalDate.now())
                    ? LocalDate.now() : card.getEndDate();
            update.setEndDate(base.plusDays(type.getValidDays()));
            addDays = type.getValidDays();
        } else {
            update.setTotalTimes((card.getTotalTimes() == null ? 0 : card.getTotalTimes()) + type.getTotalTimes());
            update.setRemainTimes((card.getRemainTimes() == null ? 0 : card.getRemainTimes()) + type.getTotalTimes());
            addTimes = type.getTotalTimes();
        }
        if ("EXPIRED".equals(card.getStatus())) {
            update.setStatus("ACTIVE");
        }
        updateById(update);

        saveRecharge(card, type, realAmount, payType, operatorId, addDays, addTimes);
        return getById(cardId);
    }

    private void saveRecharge(MemberCard card, CardType type, BigDecimal amount, String payType,
                              Long operatorId, int addDays, int addTimes) {
        CardRecharge recharge = new CardRecharge();
        recharge.setRechargeNo("CR" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%02d", (int) (Math.random() * 90 + 10)));
        recharge.setMemberCardId(card.getId());
        recharge.setMemberId(card.getMemberId());
        recharge.setCardTypeId(type.getId());
        recharge.setAmount(amount);
        recharge.setPayType(StringUtils.hasText(payType) ? payType : "CASH");
        recharge.setAddDays(addDays);
        recharge.setAddTimes(addTimes);
        recharge.setOperatorId(operatorId);
        recharge.setCreateTime(LocalDateTime.now());
        rechargeService.save(recharge);
    }

    private String nextCardNo() {
        String prefix = "MC" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = count(new QueryWrapper<MemberCard>().likeRight("card_no", prefix)) + 1;
        String no = prefix + String.format("%03d", seq);
        while (count(new QueryWrapper<MemberCard>().eq("card_no", no)) > 0) {
            seq++;
            no = prefix + String.format("%03d", seq);
        }
        return no;
    }

    /** 冻结 / 解冻：仅允许 ACTIVE 与 FROZEN 互转 */
    public void changeStatus(Long id, String status, String reason) {
        MemberCard card = getById(id);
        if (card == null) {
            throw new BizException("会员卡不存在");
        }
        if ("EXPIRED".equals(card.getStatus())) {
            throw new BizException("已过期的会员卡不能变更状态，请先办理续费");
        }
        if ("REFUNDED".equals(card.getStatus())) {
            throw new BizException("已退卡的记录不能再变更状态");
        }
        if (!"ACTIVE".equals(status) && !"FROZEN".equals(status)) {
            throw new BizException("状态取值只能为 ACTIVE 或 FROZEN");
        }
        MemberCard update = new MemberCard();
        update.setId(id);
        update.setStatus(status);
        update.setFreezeReason("FROZEN".equals(status) ? reason : null);
        updateById(update);
    }

    /** 到期提醒：指定天数内到期以及已过期的卡 */
    public List<MemberCardVO> expiring(int days) {
        LocalDate limit = LocalDate.now().plusDays(days);
        List<MemberCard> list = list(new QueryWrapper<MemberCard>()
                .eq("status", "ACTIVE")
                .isNotNull("end_date")
                .le("end_date", limit)
                .orderByAsc("end_date"));
        return list.stream().map(this::toVo).collect(Collectors.toList());
    }

    public List<MemberCardVO> listByMember(Long memberId) {
        List<MemberCard> list = list(new QueryWrapper<MemberCard>()
                .eq("member_id", memberId).orderByDesc("id"));
        return list.stream().map(this::toVo).collect(Collectors.toList());
    }

    /** 预约时挑选可用卡：优先期限卡（不扣次），其次有余量的次卡 */
    public MemberCard pickUsableCard(Long memberId) {
        List<MemberCard> cards = list(new QueryWrapper<MemberCard>()
                .eq("member_id", memberId).eq("status", "ACTIVE"));
        if (cards.isEmpty()) {
            throw new BizException("您当前没有有效的会员卡，请先到前台办卡");
        }
        Map<Long, CardType> types = cardTypeService.list().stream()
                .collect(Collectors.toMap(CardType::getId, Function.identity()));
        List<MemberCard> durations = new ArrayList<>();
        List<MemberCard> times = new ArrayList<>();
        for (MemberCard card : cards) {
            if (card.getEndDate() != null && card.getEndDate().isBefore(LocalDate.now())) {
                continue;
            }
            CardType type = types.get(card.getCardTypeId());
            if (type == null) {
                continue;
            }
            if ("DURATION".equals(type.getCardKind())) {
                durations.add(card);
            } else if (card.getRemainTimes() != null && card.getRemainTimes() > 0) {
                times.add(card);
            }
        }
        if (!durations.isEmpty()) {
            return durations.get(0);
        }
        if (!times.isEmpty()) {
            return times.get(0);
        }
        throw new BizException("您的会员卡已过期或次数已用完，请续费后再预约");
    }
}
