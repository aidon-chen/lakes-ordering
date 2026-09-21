package com.siam.system.modular.package_order.service.internal;

import java.util.List;
import java.util.Map;

/**
 * 订单改单/退单 · 批次 G · 收银端 W-13（需求 M-02「接单、改单、退单，订单状态流转」）
 *
 * <p>退单**复用现成退款模型**：tb_order_refund（type/refund_way/refund_reason/refund_amount/status 7 态）
 * + tb_order_refund_goods（部分退明细），并复用与「顾客申请→商家审核」**同一套**退款执行路径
 * （微信走 wxPayService.refund，余额走加余额+账单；mvp 下 wxPayService.refund 为**模拟**，与收款一致）。
 */
public interface OrderChangeService {

    /**
     * 改单：按传入明细覆盖数量、可加新品、可删明细，重算金额并留痕。
     *
     * @param items 每项：detailId（已有明细，可空=新增）、goodsId（新增时必填）、number（数量，0 或负数=删除该明细）
     * @return beforeAmount / afterAmount / diffAmount（正=需补收，负=应退）
     */
    Map<String, Object> modify(Integer shopId, Integer operatorId, Integer orderId, List<Map<String, Object>> items);

    /**
     * 退单（商家现场发起）：整单退 / 部分退。
     * 会先落一条 tb_order_refund（type=2 已支付订单申请退款），再走退款执行器。
     *
     * @param refundWay 1=全额 2=部分
     * @param items     部分退时的明细：detailId + number
     * @return refundAmount / refundSuccess / refundStatusText
     */
    Map<String, Object> refund(Integer shopId, Integer operatorId, Integer orderId,
                              Integer refundWay, Integer reasonCode, String reasonText,
                              List<Map<String, Object>> items);

    /** 改单/退单记录（changeType 可空=全部） */
    List<Map<String, Object>> records(Integer shopId, Integer changeType, String orderNo);
}
