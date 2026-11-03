package com.ncu.gym.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ncu.gym.entity.Waitlist;
import com.ncu.gym.mapper.WaitlistMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 候补队列：满员课程排队，名额释放后按排队顺序自动递补。
 */
@Service
public class WaitlistService extends ServiceImpl<WaitlistMapper, Waitlist> {

    public List<Waitlist> listByMember(Long memberId) {
        return list(new QueryWrapper<Waitlist>().eq("member_id", memberId).orderByDesc("id"));
    }

    public List<Waitlist> listBySchedule(Long scheduleId) {
        return list(new QueryWrapper<Waitlist>().eq("schedule_id", scheduleId)
                .eq("status", "WAITING").orderByAsc("enqueue_time"));
    }

    public void markPromoted(Long id) {
        updateStatus(id, "PROMOTED");
    }

    public void markCancelled(Long id) {
        Waitlist update = new Waitlist();
        update.setId(id);
        update.setStatus("CANCELLED");
        updateById(update);
    }

    private void updateStatus(Long id, String status) {
        Waitlist update = new Waitlist();
        update.setId(id);
        update.setStatus(status);
        updateById(update);
    }
}
