package com.siam.system.modular.package_service.model.param;

import lombok.Data;

/**
 * 存酒入参 · C-16
 *
 *  - 顾客列表：memberId 由会话推导，无需传
 *  - 商家录入：memberPhone, goodsName, location, bottleCount, expireDays
 *  - 取酒：storageId, takeCode, takeCount, remark（申请 / 代发起 / 核销）
 */
@Data
public class WineStorageParam {

    /* ---- 商家录入 ---- */

    /** 会员手机号（商家录入时定位会员） */
    private String memberPhone;

    /** 品名 */
    private String goodsName;

    /** 存放柜位，如「B 区 03 号柜」 */
    private String location;

    /** 存入瓶数 */
    private Integer bottleCount;

    /** 有效天数（默认 90） */
    private Integer expireDays;

    /* ---- 取酒（申请 / 代发起 / 核销） ---- */

    /** 存酒记录id（申请取酒 / 商家代发起时定位存酒） */
    private Long storageId;

    /** 取酒申请id（顾客取消申请时传入） */
    private Long applyId;

    /** 一次性取酒码（核销时传入） */
    private String takeCode;

    /** 本次取酒瓶数（申请 / 代发起） */
    private Integer takeCount;

    /** 备注（商家代发起可填） */
    private String remark;

    /* ---- 顾客查询 ---- */

    /** 存酒记录id */
    private Long id;

    private Integer pageNo;

    private Integer pageSize;
}
