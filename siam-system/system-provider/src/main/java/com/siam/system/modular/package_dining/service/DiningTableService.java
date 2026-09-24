package com.siam.system.modular.package_dining.service;

import java.util.Map;

/**
 * 台位作业服务（开台/结账/清台/转台）
 */
public interface DiningTableService {

    /**
     * 桌台图数据：全量台位（含类型/区域名/会话聚合与待清台标志）+ 区域列表
     */
    Map<String, Object> list(Integer shopId);

    /**
     * 开台：校验人数 → 原子抢台 → 建会话
     * @return {sessionId, tableId}
     */
    Map<String, Object> openTable(Integer shopId, Long tableId, Integer memberNum);

    /**
     * 转台：源台会话迁移到目标空闲台，订单快照列迁移
     */
    void changeTable(Integer shopId, Long tableId, Long targetTableId);

    /**
     * 结账（批次 C 2026-09-19 新增）：拦未结账订单 → **会话结台**，**台位保持占用**
     * → 台位进入「待清台」，由 {@link #clearTable} 真正释放
     */
    void settleTable(Integer shopId, Long tableId);

    /**
     * 确认收款（2026-09-24 新增）：把这一桌（或这一单）的钱标记为已收
     *
     * <p>口径＝收银台「点单即收款」（先付后吃，用户 2026-09-24 拍板）：
     * 点菜与收款一次完成，订单置已付 + status 1→2（待处理=下厨），
     * **台位与会话保持不变**（顾客仍在用餐）；顾客离店由 {@link #clearTable}
     * 一步完成「结台 + 放台」，因此本流程不依赖 {@link #settleTable}→「待清台」动线。
     *
     * <p>⚠️ 只写支付三字段（is_payment / payment_mode / payment_success_time），
     * **不调 orderService.paymentNotify** —— 收银台订单的 memberId 被复用为商家 id，
     * 走支付回调会给商家发积分/查不到会员报错。真实微信收款属批次 H。
     *
     * @param tableId 堂食台位id（收该桌全部未付订单，含漏收与后续加酒）
     * @param orderNo 外带自提订单号（与 tableId 二选一）
     * @param payMode 收款方式：1=微信收款码，4=到店支付(现金)
     * @return {paidCount 收款笔数, paidAmount 收款金额}；已收过则 paidCount=0（幂等）
     */
    Map<String, Object> confirmPayment(Integer shopId, Long tableId, String orderNo, Integer payMode);

    /**
     * 扫码收款（付款码支付 · 被扫）：扫顾客微信付款码 → 调微信 /pay/micropay 真扣款
     *
     * <p>与 {@link #confirmPayment}（现金＝人工确认记账）的分工，正好对应画板结算栏
     * 的两档支付方式：微信收款码 → 本方法（真扣款）；现金 → confirmPayment（记账）。
     *
     * <p>结果三态：
     * <ul>
     *   <li>{@code status=SUCCESS} —— 已扣款并落库，返回 paidAmount / transactionId</li>
     *   <li>{@code status=USERPAYING} —— 顾客仍在输密码，**未改动数据库**，提示收银员稍后再扫一次
     *       （微信对「同单号同参数」幂等，重扫安全）</li>
     *   <li>失败 —— 直接抛业务异常，文案取微信 {@code err_code_des}（如「付款码已过期」「余额不足」）</li>
     * </ul>
     *
     * <p>⚠️ 不在本方法上开事务：内部含 HTTP 调用与 USERPAYING 轮询（最长数秒），
     * 持事务会白占连接池；落库只有一条原子 UPDATE，无需事务。
     *
     * @param tableId  堂食台位id（取该桌**最早一笔**未付堂食单收款；与 orderNo 二选一）
     * @param orderNo  指定订单号（本次刚下的单 / 外带单）
     * @param authCode 顾客付款码（扫码枪或摄像头读入）
     * @param clientIp 发起支付的机器 IP（微信必填 spbill_create_ip）
     */
    Map<String, Object> scanPay(Integer shopId, Long tableId, String orderNo, String authCode, String clientIp);

    /**
     * 清台：拦未结账订单 → 会话结台（若仍进行中）→ 释放台位
     *
     * <p>兼容「待清台」：若会话已被 {@link #settleTable} 置为已结台，则本次仅释放台位。
     */
    void clearTable(Integer shopId, Long tableId);
}
