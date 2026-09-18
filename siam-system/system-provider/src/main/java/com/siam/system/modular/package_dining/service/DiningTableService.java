package com.siam.system.modular.package_dining.service;

import java.util.Map;

/**
 * 台位作业服务（开台/清台/转台）
 */
public interface DiningTableService {

    /**
     * 桌台图数据：全量台位（含类型/区域名/进行中会话聚合）+ 区域列表
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
     * 清台：拦未结账订单 → 会话结台 → 释放台位
     */
    void clearTable(Integer shopId, Long tableId);
}
