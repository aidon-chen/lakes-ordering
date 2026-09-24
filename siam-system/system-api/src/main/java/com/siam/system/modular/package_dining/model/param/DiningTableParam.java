package com.siam.system.modular.package_dining.model.param;

import com.siam.system.modular.package_dining.entity.DiningTable;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

/**
 * 台位参数（open/change/clear 与 A-09 档案管理共用）
 */
@Data
@ApiModel(value = "台位参数")
public class DiningTableParam extends DiningTable {

    /** 开台就餐人数 */
    private Integer memberNum;

    /** 转台目标台id */
    private Long targetTableId;

    /** 操作员id（本期可选） */
    private Integer operatorId;

    /** 批量操作台位id集合（A-09 批量生成桌码） */
    private List<Long> ids;

    /**
     * 确认收款 · 外带自提订单号
     * 与 {@code id}（台位id）二选一：堂食传台位id（收全桌未付单），外带传订单号
     */
    private String orderNo;

    /**
     * 确认收款 · 收款方式
     * 1=微信收款码，4=到店支付(现金) —— 对齐 A-07 营收看板的 payment_mode 口径
     */
    private Integer payMode;

    /**
     * 扫码收款 · 顾客微信付款码（被扫）
     * 扫码枪读入或摄像头识别；⚠️ 属敏感凭据，**不得写入操作日志**（A-08 已脱敏处理）
     */
    private String authCode;

    private Integer pageNo = 1;

    private Integer pageSize = 20;
}
