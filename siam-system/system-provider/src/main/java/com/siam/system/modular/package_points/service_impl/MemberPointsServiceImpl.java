package com.siam.system.modular.package_points.service_impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.package_common.exception.StoneCustomerException;
import com.siam.system.modular.package_points.entity.MemberPointsLog;
import com.siam.system.modular.package_points.mapper.MemberPointsLogMapper;
import com.siam.system.modular.package_points.service.MemberPointsService;
import com.siam.system.modular.package_points.service.MemberPointsSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

@Service
public class MemberPointsServiceImpl implements MemberPointsService {

    @Autowired
    private MemberPointsLogMapper memberPointsLogMapper;

    @Autowired
    private MemberPointsSettingService memberPointsSettingService;

    @Override
    public Integer getTotalPoints(Integer memberId) {
        Integer sum = memberPointsLogMapper.sumPointsByMemberId(memberId);
        return sum == null ? 0 : sum;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void earn(Integer memberId, Integer bizType, String bizId, Integer points, String remark) {
        if (memberId == null || points == null || points == 0) {
            return; // 0 分值与空会员直接跳过，业务侧无需判空
        }
        Integer current = getTotalPoints(memberId);
        MemberPointsLog log = new MemberPointsLog();
        log.setMemberId(memberId);
        log.setBizType(bizType);
        log.setBizId(bizId);
        log.setChangePoints(points);
        log.setBalanceAfter(current + points);
        log.setRemark(remark);
        log.setCreateTime(new Date());
        memberPointsLogMapper.insert(log);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void earnByAmount(Integer memberId, Integer bizType, String bizId, BigDecimal amount, String rateKey) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Integer rate = memberPointsSettingService.getInt(rateKey, 1);
        if (rate == null || rate <= 0) {
            rate = 1;
        }
        // floor(金额 × 比例)，向下取整保证不超发
        int points = amount.multiply(BigDecimal.valueOf(rate))
                .setScale(0, RoundingMode.DOWN).intValue();
        earn(memberId, bizType, bizId, points, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void redeemByBiz(Integer bizType, String bizId, String remark) {
        if (bizId == null) {
            return;
        }
        // 幂等：该业务单已红冲过则跳过
        Integer redeemed = memberPointsLogMapper.selectCount(
                new QueryWrapper<MemberPointsLog>()
                        .eq("biz_type", MemberPointsLog.BIZ_TYPE_REDEEM)
                        .eq("biz_id", bizId));
        if (redeemed != null && redeemed > 0) {
            return;
        }
        // 汇总该业务单名下全部正向流水
        List<MemberPointsLog> earnedList = memberPointsLogMapper.selectList(
                new QueryWrapper<MemberPointsLog>()
                        .eq("biz_type", bizType)
                        .eq("biz_id", bizId)
                        .gt("change_points", 0));
        int total = earnedList.stream().mapToInt(MemberPointsLog::getChangePoints).sum();
        if (total <= 0) {
            return; // 本来就没给过分，无需红冲
        }
        Integer memberId = earnedList.get(0).getMemberId();
        earn(memberId, MemberPointsLog.BIZ_TYPE_REDEEM, bizId, -total, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjust(Integer memberId, Integer points, String remark) {
        if (points == null || points == 0) {
            throw new StoneCustomerException("调整分值不能为0");
        }
        earn(memberId, MemberPointsLog.BIZ_TYPE_ADJUST, null, points,
                remark == null ? "后台手工调整" : remark);
    }

    @Override
    public Page<MemberPointsLog> getLogPage(Integer memberId, Integer pageNo, Integer pageSize) {
        Page<MemberPointsLog> page = new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 20 : pageSize);
        return memberPointsLogMapper.selectPage(page,
                new QueryWrapper<MemberPointsLog>()
                        .eq(memberId != null, "member_id", memberId)
                        .orderByDesc("create_time")
                        .orderByDesc("id"));
    }
}
