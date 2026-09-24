package com.siam.system.modular.package_user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.system.modular.package_user.entity.MemberBillingRecord;
import com.siam.system.modular.package_user.model.dto.MemberBillingRecordDto;
import com.siam.system.modular.package_user.model.example.MemberBillingRecordExample;
import com.siam.system.modular.package_user.model.param.MemberBillingRecordParam;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 *  暹罗
 */
public interface MemberBillingRecordService {

    void deleteByPrimaryKey(Integer id);

    void insertSelective(MemberBillingRecord memberBillingRecord);

    MemberBillingRecord selectByPrimaryKey(MemberBillingRecordParam param);

    void updateByPrimaryKeySelective(MemberBillingRecord memberBillingRecord);

    List<MemberBillingRecord> selectByExample(MemberBillingRecordExample example);

    Page getListByPage(MemberBillingRecordParam param);

    /**
     * 管理端：会员资金流水列表
     *
     * <p>⚠️ 与 {@link #getListByPage} 的区别 —— 那个方法是**会员端**专用：内部会用
     * 会员会话（{@code memberSessionManager}）覆盖 memberId，管理端没有会员会话，
     * 直接复用会抛 NullPointerException（2026-09-24 实测踩到）。
     * 本方法不碰会话，直接按入参里的 memberId（可空 = 查全量）过滤。
     */
    Page getListByPageForAdmin(MemberBillingRecordParam param);

    /**
     * 查询用户某段时间内的货币增减数额
     */
    BigDecimal selectSumNumber(MemberBillingRecordDto memberBillingRecordDto, Date startTime, Date endTime);

    void settledReward();
}