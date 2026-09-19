package com.siam.system.modular.package_dining.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 桌台图聚合行（list 接口：台位 + 类型/区域名 + 会话摘要）
 *
 * <p>2026-09-19 批次 C 扩展：会话不再只取「进行中」——
 * 为派生「待清台」态（台位仍占用但会话已结台），本 VO 需要下发会话状态与结台时间。
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

    /* ########## 会话摘要（对齐前端 SessionBrief） ########## */

    private Long sessionId;

    private Integer memberNum;

    private Long openMinutes;

    private BigDecimal goodsTotalPrice;

    private Integer addedCount;

    /* ########## 2026-09-19 新增：待清台派生所需 ########## */

    /** 会话状态 1=进行中 2=已结台 3=已失效；无会话时为 null */
    private Integer sessionStatus;

    /** 结台时间（sessionStatus=2 时有值） */
    private Date closeTime;

    /**
     * 待清台：台位仍占用(status=2) 且 最近一次会话已结台(sessionStatus=2)
     * 由后端算好下发，避免前端重复推导口径
     */
    private Boolean awaitingClean;
}

