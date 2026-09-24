package com.siam.system.modular.package_points.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.system.modular.package_points.entity.MemberPointsLog;

import java.math.BigDecimal;

/**
 * 会员积分服务 · 统一入账/红冲/查询入口。
 * 业务侧只调本服务，禁止直插流水表（保证 balance_after 快照一致性）。
 */
public interface MemberPointsService {

    /** 会员累计积分（红冲已抵扣）；无流水返回 0 */
    Integer getTotalPoints(Integer memberId);

    /**
     * 固定分值入账（活动/后台调整场景，分值由调用方从配置表读取）
     *
     * @param bizType 见 MemberPointsLog.BIZ_TYPE_*
     */
    void earn(Integer memberId, Integer bizType, String bizId, Integer points, String remark);

    /**
     * 按金额×比例入账（消费/充值场景）。比例从 tb_member_points_setting 读取（rateKey），
     * 比例缺失默认 1；换算后分值为 0 则跳过不入账。
     */
    void earnByAmount(Integer memberId, Integer bizType, String bizId, BigDecimal amount, String rateKey);

    /**
     * 退款红冲：将该业务单号名下全部正向流水的总额以负数冲回。
     * 幂等：同 bizId 已存在红冲记录时跳过。
     */
    void redeemByBiz(Integer bizType, String bizId, String remark);

    /** 后台手工调分（正加负减），biz_type=5 */
    void adjust(Integer memberId, Integer points, String remark);

    /** 会员积分流水分页（时间倒序） */
    Page<MemberPointsLog> getLogPage(Integer memberId, Integer pageNo, Integer pageSize);
}
