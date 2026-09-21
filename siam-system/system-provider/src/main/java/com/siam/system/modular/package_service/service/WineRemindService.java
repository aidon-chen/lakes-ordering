package com.siam.system.modular.package_service.service;

import java.util.List;
import java.util.Map;

/**
 * 存酒临期提醒服务 · 批次 F · W-19
 *
 * <p>口径（与画板一致）：
 * <ul>
 *   <li>「临期」= 0 ≤ 剩余天数 ≤ 提前天数（默认 7，存于门店设置 wineRemindAheadDays）</li>
 *   <li>「过期」= 剩余天数 &lt; 0</li>
 *   <li>「通知状态」= 该存酒记录最近一次弹窗记录的时间；没有则为「待提示」</li>
 * </ul>
 * 只统计**存续中**的存酒（已取完/已销毁的不提醒）。
 */
public interface WineRemindService {

    /**
     * 收银端 W-19 任务表：存续中 + 有效期非空的存酒，按剩余天数升序（最紧急在前）。
     * 每行补：customerName / customerPhone(脱敏) / daysRemaining / lastNotifyTime / notified
     */
    List<Map<String, Object>> listForMerchant(Integer shopId, Integer aheadDays);

    /** 收银端 W-19 指标行：临期数 / 过期数 / 已通知数 */
    Map<String, Object> summary(Integer shopId, Integer aheadDays);

    /** 顾客端弹窗后回报（幂等：同一天同一存酒只记一条） */
    int ack(Integer memberId, List<Integer> storageIds);

    /** 顾客端「我的存酒」弹窗数据源：该会员的临期+过期存酒（含剩余天数与品名） */
    List<Map<String, Object>> listExpiringForMember(Integer memberId, Integer aheadDays);

    /** 读某门店的「到期前 N 天」设置（读不到用默认 7）—— 收银端 W-19 页也用它 */
    Integer aheadDaysForShop(Integer shopId);

    /** 顾客端便捷版：自动按该会员存酒所属门店读「提前天数」设置（读不到用默认 7） */
    List<Map<String, Object>> listExpiringForMemberAuto(Integer memberId);
}
