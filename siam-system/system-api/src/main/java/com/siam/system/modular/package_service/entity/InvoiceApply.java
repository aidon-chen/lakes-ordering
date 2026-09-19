package com.siam.system.modular.package_service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 发票申请表（tb_invoice_apply）· 对应原型 C-15（表单 + 申请记录同页）
 *
 * 注意：本表属本批次新增，执行 update.sql §6 前库中不存在。
 * 批次 5 决策：§6 追加 UNIQUE KEY uk_order(order_id)，一张订单只能开一次发票。
 * amount = 订单实付（tb_order.actualPrice），order_no 为下单时快照。
 */
@Data
@TableName("tb_invoice_apply")
public class InvoiceApply {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会员id */
    private Integer memberId;

    /** 订单id */
    private Integer orderId;

    /** 订单编号（快照，列表展示用，前端不回查订单） */
    private String orderNo;

    /** 开票金额 = 订单实付 */
    private BigDecimal amount;

    /** 抬头类型 1=个人 2=企业 */
    private Integer titleType;

    /** 发票抬头 */
    private String title;

    /** 税号（titleType=2 必填） */
    private String taxNo;

    /** 接收邮箱 */
    private String email;

    /** 状态 1=待处理 2=已开具 3=已驳回 */
    private Integer status;

    /** 驳回原因/备注 */
    private String remark;

    /** 申请时间 */
    private Date applyTime;

    /** 处理完成时间 */
    private Date finishTime;

    private Date createTime;

    private Date updateTime;
}
