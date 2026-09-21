package com.siam.system.modular.package_service.model.vo;

import lombok.Data;

/**
 * 存酒记录展示对象 · C-16 / C-01 存酒区
 *
 * 状态四态口径（批次 5 决策 D-H，临期阈值 30 天）：
 *  - status=2                                     → 已取完（灰）
 *  - status=3 或 expire_date < today              → 已过期（灰）
 *  - expire_date - today <= 30 天                 → 将过期（黄，C-16 页措辞为「临期」）
 *  - 其余                                         → 存续中 / 可取（绿）
 */
@Data
public class WineStorageVo {

    private Long id;

    /** 品名 */
    private String goodsName;

    /** 存入瓶数 */
    private Integer bottleCount;

    /** 剩余瓶数 */
    private Integer remainCount;

    /** 存放柜位 */
    private String location;

    /** 存酒码（批次F · W-19 临期提醒列表需要展示） */
    private String storageCode;

    /** 会员姓名（**商家侧 W-18 专用**；顾客侧不填充，保持为 null） */
    private String memberName;

    /** 会员手机号（商家侧 W-18 专用，**已脱敏** 如 138****8601） */
    private String memberPhone;

    /** 会员id（商家侧 W-18 专用） */
    private Integer memberId;

    /** 门店名 */
    private String shopName;

    /** 状态码：1 存续中 / 2 已取完 / 3 已过期 / 4 将过期（由服务端计算，非表字段） */
    private Integer statusCode;

    /** 状态文案：存续中 / 将过期 / 已过期 / 已取完 */
    private String statusText;

    /** 存入日期 yyyy-MM-dd */
    private String storageDate;

    /** 有效期截止 yyyy-MM-dd */
    private String expireDate;

    /** 展示用一行信息：B 区 03 号柜 · 存入 08-20（无柜位则只留「存入 MM-dd」） */
    private String summaryLine;

    /** 展示用有效期行：有效期至 2026-11-18 */
    private String expireLine;

    /** 该存酒当前是否有「有效待核销」取酒申请（顾客端据此禁用「申请取酒」） */
    private Boolean pendingApply;
}
