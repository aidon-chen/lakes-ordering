package com.siam.system.modular.package_service.model.vo;

import lombok.Data;

/**
 * 取酒申请展示对象 · 方案 v1.1
 *
 * <p>顾客侧：只填 id/品名/瓶数/取酒码/状态/时间；商家侧额外填会员昵称 + 脱敏手机。
 */
@Data
public class WineTakeApplyVo {

    private Long id;

    /** 存酒记录id */
    private Long storageId;

    /** 会员id（商家侧用于展示，顾客侧可忽略） */
    private Integer memberId;

    /** 品名（取自存酒记录） */
    private String goodsName;

    /** 存放柜位（取自存酒记录） */
    private String location;

    /** 申请取酒瓶数 */
    private Integer takeCount;

    /** 一次性取酒码（6 位；仅待核销时对顾客展示） */
    private String takeCode;

    /** 状态码：1待核销 2已核销 3已取消 4已过期 */
    private Integer statusCode;

    /** 状态文案：待核销 / 已核销 / 已取消 / 已过期 */
    private String statusText;

    /** 来源 1顾客自助 2商家代发起 */
    private Integer applySource;

    /** 来源文案：顾客自助 / 商家代发起 */
    private String applySourceText;

    /** 申请时间 yyyy-MM-dd HH:mm */
    private String applyTime;

    /** 取酒码有效期 yyyy-MM-dd HH:mm */
    private String expireTime;

    /** 核销时间 yyyy-MM-dd HH:mm */
    private String confirmTime;

    /** 会员昵称（商家侧专用；顾客侧不填） */
    private String memberName;

    /** 会员手机号（商家侧专用，**已脱敏** 如 138****8601） */
    private String memberPhone;
}
