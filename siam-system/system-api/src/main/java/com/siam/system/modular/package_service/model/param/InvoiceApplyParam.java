package com.siam.system.modular.package_service.model.param;

import lombok.Data;

/**
 * 发票申请入参 · C-15
 *
 * 同一个 param 承担三种用途（apply / list / merchant finish），沿用工程既有惯例：
 *  - 顾客申请：orderId, titleType, title, taxNo, email
 *  - 顾客列表：status（可选）, pageNo, pageSize
 *  - 商家完结：id, status(2 已开具 / 3 已驳回), remark
 */
@Data
public class InvoiceApplyParam {

    /* ---- 顾客申请 ---- */

    /** 关联订单id */
    private Integer orderId;

    /** 抬头类型 1=个人 2=企业 */
    private Integer titleType;

    /** 发票抬头 */
    private String title;

    /** 税号（titleType=2 必填） */
    private String taxNo;

    /** 接收邮箱 */
    private String email;

    /* ---- 列表 / 商家完结 ---- */

    /** 申请记录id */
    private Long id;

    /** 状态过滤：申请=默认 1 待处理；完结=2 已开具 / 3 已驳回 */
    private Integer status;

    /** 驳回原因/备注 */
    private String remark;

    private Integer pageNo;

    private Integer pageSize;
}
