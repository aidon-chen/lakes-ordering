package com.siam.system.modular.package_service.model.param;

import lombok.Data;

/**
 * 存酒入参 · C-16
 *
 *  - 顾客列表：memberId 由会话推导，无需传
 *  - 商家录入：memberPhone, goodsName, location, bottleCount, expireDays
 *  - 商家取酒：storageCode, takeCount, remark
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

    /* ---- 商家取酒 ---- */

    /** 存酒码 */
    private String storageCode;

    /** 本次取酒瓶数 */
    private Integer takeCount;

    private String remark;

    /* ---- 顾客查询 ---- */

    /** 存酒记录id */
    private Long id;

    private Integer pageNo;

    private Integer pageSize;
}
