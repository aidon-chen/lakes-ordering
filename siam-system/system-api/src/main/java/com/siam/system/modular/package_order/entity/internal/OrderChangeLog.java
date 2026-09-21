package com.siam.system.modular.package_order.entity.internal;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单改单/退单留痕 · 批次 G · 收银端 W-13
 *
 * <p>为什么单建一张表：退款本身有现成模型（tb_order_refund / _goods / _process）可复用，
 * 但**「改单」工程里没有任何记录**（改了谁、改了什么、差额多少事后无从追溯）→ 本表补齐。
 */
@Data
@TableName("tb_order_change_log")
public class OrderChangeLog {

    /** 变更类型：改单 */
    public static final int TYPE_MODIFY = 1;
    /** 变更类型：退单 */
    public static final int TYPE_REFUND = 2;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer shopId;

    private Integer orderId;

    private String orderNo;

    /** 1=改单 2=退单，见 TYPE_* */
    private Integer changeType;

    private BigDecimal beforeAmount;

    private BigDecimal afterAmount;

    /** 差额：正=需补收，负=应退，0=无差额 */
    private BigDecimal diffAmount;

    private BigDecimal refundAmount;

    /** 1=全额 2=部分（退单用） */
    private Integer refundWay;

    /** 原因码（退单复用 tb_order_refund.refund_reason 枚举） */
    private Integer reasonCode;

    private String reasonText;

    /** 变更前后明细快照（JSON） */
    private String detailSnapshot;

    /** 操作人（商家id） */
    private Integer operatorId;

    private Date createTime;
}
