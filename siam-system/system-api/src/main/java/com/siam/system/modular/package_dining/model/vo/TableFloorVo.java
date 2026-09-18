package com.siam.system.modular.package_dining.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 桌台图聚合行（list 接口：台位 + 类型/区域名 + 进行中会话摘要）
 */
@Data
public class TableFloorVo {

    private Long id;

    private String tableNo;

    private String tableName;

    /** 台位类型名 散台/卡座/包间 */
    private String typeName;

    /** 区域名 A区散台/K区卡座/V区包间 */
    private String zone;

    private Integer capacity;

    /** 状态 1=空闲 2=占用 3=停用 */
    private Integer status;

    /** 低消(仅展示，0=无) */
    private BigDecimal minSpend;

    /* ########## 占用时的进行中会话摘要（对齐前端 SessionBrief） ########## */

    private Long sessionId;

    private Integer memberNum;

    private Long openMinutes;

    private BigDecimal goodsTotalPrice;

    private Integer addedCount;
}
