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
     * 清台：拦未结账订单 → 会话结台（若仍进行中）→ 释放台位
     *
     * <p>兼容「待清台」：若会话已被 {@link #settleTable} 置为已结台，则本次仅释放台位。
     */
    void clearTable(Integer shopId, Long tableId);
}
