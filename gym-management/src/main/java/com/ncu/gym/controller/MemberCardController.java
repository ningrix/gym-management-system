package com.ncu.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ncu.gym.common.BizException;
import com.ncu.gym.common.Result;
import com.ncu.gym.entity.CardRecharge;
import com.ncu.gym.entity.MemberCard;
import com.ncu.gym.service.CardRechargeService;
import com.ncu.gym.service.MemberCardService;
import com.ncu.gym.util.LoginUser;
import com.ncu.gym.util.UserContext;
import com.ncu.gym.vo.MemberCardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会员卡：开卡、续费、冻结、到期提醒
 */
@RestController
@RequestMapping("/api/member-cards")
@RequiredArgsConstructor
public class MemberCardController {

    private final MemberCardService memberCardService;
    private final CardRechargeService rechargeService;

    @GetMapping
    public Result<IPage<MemberCardVO>> list(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize) {
        UserContext.require();
        return Result.success(memberCardService.pageCards(keyword, status, pageNum, pageSize));
    }

    /** 开卡：建卡与缴费流水同一事务 */
    @PostMapping("/open")
    public Result<MemberCard> open(@RequestBody Map<String, Object> body) {
        LoginUser login = UserContext.require();
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以办理开卡");
        }
        Long memberId = toLong(body.get("memberId"));
        Long cardTypeId = toLong(body.get("cardTypeId"));
        BigDecimal amount = body.get("amount") == null ? null : new BigDecimal(body.get("amount").toString());
        return Result.success(memberCardService.openCard(memberId, cardTypeId,
                (String) body.get("payType"), amount, login.getUserId()));
    }

    /** 续费：期限卡顺延、次卡加次，与流水同事务 */
    @PostMapping("/{id}/renew")
    public Result<MemberCard> renew(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        LoginUser login = UserContext.require();
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以办理续费");
        }
        Long cardTypeId = body.get("cardTypeId") == null ? null : toLong(body.get("cardTypeId"));
        BigDecimal amount = body.get("amount") == null ? null : new BigDecimal(body.get("amount").toString());
        return Result.success(memberCardService.renewCard(id, cardTypeId,
                (String) body.get("payType"), amount, login.getUserId()));
    }

    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有管理员可以冻结或解冻会员卡");
        }
        memberCardService.changeStatus(id, body.get("status"), body.get("freezeReason"));
        return Result.success();
    }

    @GetMapping("/expiring")
    public Result<List<MemberCardVO>> expiring(@RequestParam(defaultValue = "7") int days) {
        UserContext.require();
        return Result.success(memberCardService.expiring(days));
    }

    @GetMapping("/my")
    public Result<Map<String, Object>> my() {
        LoginUser login = UserContext.require();
        if (!"MEMBER".equals(login.getRole()) || login.getBizId() == null) {
            throw new BizException(403, "该接口仅供会员使用");
        }
        List<MemberCardVO> cards = memberCardService.listByMember(login.getBizId());
        List<CardRecharge> recharges = rechargeService.list(new QueryWrapper<CardRecharge>()
                .eq("member_id", login.getBizId()).orderByDesc("id"));
        Map<String, Object> data = new HashMap<>();
        data.put("cards", cards);
        data.put("recharges", recharges);
        return Result.success(data);
    }

    @GetMapping("/{id}/recharges")
    public Result<List<CardRecharge>> recharges(@PathVariable Long id) {
        UserContext.require();
        return Result.success(rechargeService.list(new QueryWrapper<CardRecharge>()
                .eq("member_card_id", id).orderByDesc("id")));
    }

    private Long toLong(Object value) {
        if (value == null) {
            throw new BizException("缺少必要参数");
        }
        return Long.valueOf(value.toString());
    }
}
