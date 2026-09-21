package com.siam.system.modular.package_goods.service.internal;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.system.modular.package_goods.entity.internal.PrintTask;
import com.siam.system.modular.package_goods.entity.internal.PrintTemplate;

import java.util.List;
import java.util.Map;

/** 打印模板与打印任务服务 · 批次 G · W-21 / W-25 */
public interface PrintService {

    /* ---- 模板（W-21） ---- */

    List<PrintTemplate> listTemplates(Integer shopId, Integer scene);

    PrintTemplate saveTemplate(Integer shopId, PrintTemplate t);

    /** 设为该 scene 的默认模板（同 shop + scene 下互斥） */
    void setDefaultTemplate(Integer shopId, Integer id);

    void deleteTemplate(Integer shopId, Integer id);

    /* ---- 任务（W-25 队列 / W-21 补打记录） ---- */

    /**
     * 任务分页查询。
     *
     * @param onlyPendingOrFail true 时只取「待打印/失败」（= W-25 的待重试队列）
     * @param orderNo           按单号模糊
     */
    Page<Map<String, Object>> listTasks(Integer shopId, Integer status, String orderNo,
                                        Boolean onlyPendingOrFail, Boolean isReprint, int pageNo, int pageSize);

    /** 队列指标：待重试 / 今日失败 / 今日成功 */
    Map<String, Object> taskSummary(Integer shopId);

    /** 立即重试：清空失败态、重置为待打印并置 nextRetryTime=now，返回是否成功 */
    boolean retryTask(Integer shopId, Integer taskId);

    /** 忽略任务（人工放弃） */
    boolean ignoreTask(Integer shopId, Integer taskId);

    /** 新建补打任务（W-21 补打记录会多一条 isReprint=true 的流水） */
    PrintTask createReprint(Integer shopId, Integer operatorId, Integer sourceTaskId);
}
