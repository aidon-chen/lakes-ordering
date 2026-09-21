package com.siam.system.modular.package_order.service_impl.internal;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.siam.package_common.exception.StoneCustomerException;
import com.siam.system.modular.package_order.controller.member.WxPayService;
import com.siam.system.modular.package_order.entity.Order;
import com.siam.system.modular.package_order.entity.OrderDetail;
import com.siam.system.modular.package_order.entity.OrderRefund;
import com.siam.system.modular.package_order.entity.OrderRefundGoods;
import com.siam.system.modular.package_order.entity.internal.OrderChangeLog;
import com.siam.system.modular.package_order.mapper.internal.OrderChangeLogMapper;
import com.siam.system.modular.package_order.service.OrderDetailService;
import com.siam.system.modular.package_order.service.OrderRefundGoodsService;
import com.siam.system.modular.package_order.service.OrderRefundService;
import com.siam.system.modular.package_order.service.OrderService;
import com.siam.system.modular.package_order.service.internal.OrderChangeService;
import com.siam.system.modular.package_user.entity.Member;
import com.siam.system.modular.package_user.entity.MemberBillingRecord;
import com.siam.system.modular.package_user.service.MemberBillingRecordService;
import com.siam.system.modular.package_user.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单改单/退单实现 · 批次 G · W-13
 *
 * <p><b>退款执行与收款同构</b>：微信支付路径调 {@link WxPayService#refund}
 * ——该方法在**非 prod profile 下直接返回 true（模拟成功）**，与收款走 MockPay 是同一套演示口径；
 * 余额支付路径则复用与 {@code auditAfterSalesOrder} 完全相同的「加余额 + 写账单」逻辑。
 * 商户号/证书齐备后无需改本类，只需配 {@code wxpay.*} 环境变量并把 prod 开关放开。
 *
 * <p><b>改单不影响已支付金额</b>（画板口径）：只调整商品明细与应收，差额仅**记录**为
 * 「待补收 / 待退」，不触发真实收退款；真实补收/退差额属批次 H。
 */
@Service
public class OrderChangeServiceImpl implements OrderChangeService {

    /* 订单状态（tb_order.status 注释口径） */
    private static final int ORDER_STATUS_WAIT_PAY = 1;
    private static final int ORDER_STATUS_DONE = 6;
    private static final int ORDER_STATUS_AFTER_SALE_DONE = 9;
    private static final int ORDER_STATUS_CANCELED_PAID = 11;

    /* tb_order_refund.status */
    private static final int REFUND_STATUS_WAIT_MERCHANT = 2;
    private static final int REFUND_STATUS_SUCCESS = 7;
    /* tb_order_refund.refund_way */
    private static final int REFUND_WAY_ALL = 1;
    private static final int REFUND_WAY_PART = 2;
    /* tb_order_refund.type：2=已支付订单申请退款 */
    private static final int REFUND_TYPE_PAID_APPLY = 2;
    /* tb_order_refund.refund_account：1=微信 */
    private static final int REFUND_ACCOUNT_WECHAT = 1;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private OrderRefundService orderRefundService;

    @Autowired
    private OrderRefundGoodsService orderRefundGoodsService;

    @Autowired
    private OrderChangeLogMapper orderChangeLogMapper;

    @Autowired
    private WxPayService wxPayService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberBillingRecordService memberBillingRecordService;

    /* ==================== 改单 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> modify(Integer shopId, Integer operatorId, Integer orderId, List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            throw new StoneCustomerException("改单明细不能为空");
        }
        Order dbOrder = requireOrder(shopId, orderId);
        if (dbOrder.getStatus() != null && (dbOrder.getStatus() == ORDER_STATUS_DONE
                || dbOrder.getStatus() == ORDER_STATUS_AFTER_SALE_DONE
                || dbOrder.getStatus() == ORDER_STATUS_CANCELED_PAID)) {
            throw new StoneCustomerException("该订单已结束，不可改单");
        }
        if (dbOrder.getIsPayment() != null && dbOrder.getIsPayment() && dbOrder.getStatus() == null) {
            throw new StoneCustomerException("订单状态异常，不可改单");
        }

        BigDecimal beforeAmount = nz(dbOrder.getActualPrice());
        BigDecimal beforeGoodsTotal = nz(dbOrder.getGoodsTotalPrice());

        List<OrderDetail> before = orderDetailService.selectByOrderId(orderId);
        StringBuilder snap = new StringBuilder("before=");
        for (OrderDetail d : before) {
            snap.append(d.getGoodsName()).append('x').append(d.getNumber()).append(';');
        }
        snap.append(" | after=");

        // 逐项应用：detailId 存在则改数量（<=0 删除）；不存在则新增（品名/单价由前端带快照，与项目「明细即快照」一致）
        for (Map<String, Object> it : items) {
            Integer detailId = intOrNull(it.get("detailId"));
            Integer number = intOrNull(it.get("number"));
            if (number == null) {
                continue;
            }
            if (detailId != null) {
                if (number <= 0) {
                    orderDetailService.deleteByPrimaryKey(detailId);
                    continue;
                }
                OrderDetail db = orderDetailService.selectByPrimaryKey(detailId);
                if (db == null || !orderId.equals(db.getOrderId())) {
                    throw new StoneCustomerException("明细不存在或不属于该订单");
                }
                OrderDetail update = new OrderDetail();
                update.setId(detailId);
                update.setNumber(number);
                update.setSubtotal(nz(db.getPrice()).multiply(BigDecimal.valueOf(number)));
                orderDetailService.updateByPrimaryKeySelective(update);
            } else {
                if (number <= 0) {
                    continue;
                }
                OrderDetail add = new OrderDetail();
                add.setOrderId(orderId);
                add.setGoodsId(intOrNull(it.get("goodsId")));
                add.setGoodsName(str(it.get("goodsName")));
                add.setMainImage(str(it.get("mainImage")));
                add.setSpecList(str(it.get("specList")));
                add.setPrice(dec(it.get("price")));
                add.setNumber(number);
                add.setSubtotal(dec(it.get("price")).multiply(BigDecimal.valueOf(number)));
                orderDetailService.insertSelective(add);
            }
        }

        List<OrderDetail> after = orderDetailService.selectByOrderId(orderId);
        int newQty = 0;
        BigDecimal newGoodsTotal = BigDecimal.ZERO;
        for (OrderDetail d : after) {
            newQty += d.getNumber() == null ? 0 : d.getNumber();
            newGoodsTotal = newGoodsTotal.add(nz(d.getSubtotal()));
            snap.append(d.getGoodsName()).append('x').append(d.getNumber()).append(';');
        }
        if (newQty <= 0) {
            throw new StoneCustomerException("改单后不能一件商品都不剩，如需清空请走退单");
        }

        // 应收按「商品差额」平移：保留原有包装费/配送费/优惠不变，避免误算活动价
        BigDecimal afterAmount = beforeAmount.add(newGoodsTotal.subtract(beforeGoodsTotal));

        Order updateOrder = new Order();
        updateOrder.setId(orderId);
        updateOrder.setGoodsTotalQuantity(newQty);
        updateOrder.setGoodsTotalPrice(newGoodsTotal);
        updateOrder.setActualPrice(afterAmount);
        updateOrder.setUpdateTime(new Date());
        orderService.updateById(updateOrder);

        writeLog(shopId, dbOrder, OrderChangeLog.TYPE_MODIFY, beforeAmount, afterAmount,
                afterAmount.subtract(beforeAmount), null, null, null, null, snap.toString(), operatorId);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("beforeAmount", beforeAmount);
        out.put("afterAmount", afterAmount);
        out.put("diffAmount", afterAmount.subtract(beforeAmount));
        return out;
    }

    /* ==================== 退单 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> refund(Integer shopId, Integer operatorId, Integer orderId,
                                      Integer refundWay, Integer reasonCode, String reasonText,
                                      List<Map<String, Object>> items) {
        Order dbOrder = requireOrder(shopId, orderId);
        if (dbOrder.getIsPayment() == null || !dbOrder.getIsPayment()) {
            throw new StoneCustomerException("该订单尚未支付，无需退款");
        }
        if (dbOrder.getStatus() != null && (dbOrder.getStatus() == ORDER_STATUS_AFTER_SALE_DONE
                || dbOrder.getStatus() == ORDER_STATUS_CANCELED_PAID)) {
            throw new StoneCustomerException("该订单已退款或已取消，不可重复退款");
        }
        if (orderRefundService.selectByOrderId(orderId) != null) {
            throw new StoneCustomerException("该订单已有退款记录，请勿重复发起");
        }
        int way = (refundWay == null) ? REFUND_WAY_ALL : refundWay;

        // 退款额：整单退=实付全额；部分退=选中明细的商品小计之和（不含配送费/包装费，与画板示例一致）
        BigDecimal refundAmount;
        int refundQty = 0;
        List<OrderDetail> details = orderDetailService.selectByOrderId(orderId);
        List<OrderDetail> picked = new ArrayList<>();
        if (way == REFUND_WAY_ALL) {
            refundAmount = nz(dbOrder.getActualPrice());
            picked.addAll(details);
            for (OrderDetail d : details) {
                refundQty += d.getNumber() == null ? 0 : d.getNumber();
            }
        } else {
            if (items == null || items.isEmpty()) {
                throw new StoneCustomerException("部分退单请勾选要退的明细");
            }
            refundAmount = BigDecimal.ZERO;
            for (Map<String, Object> it : items) {
                Integer detailId = intOrNull(it.get("detailId"));
                Integer number = intOrNull(it.get("number"));
                if (detailId == null || number == null || number <= 0) {
                    continue;
                }
                OrderDetail d = orderDetailService.selectByPrimaryKey(detailId);
                if (d == null || !orderId.equals(d.getOrderId())) {
                    throw new StoneCustomerException("明细不存在或不属于该订单");
                }
                if (d.getNumber() != null && number > d.getNumber()) {
                    throw new StoneCustomerException("退款数量不能超过该明细的数量");
                }
                refundAmount = refundAmount.add(nz(d.getPrice()).multiply(BigDecimal.valueOf(number)));
                refundQty += number;
                OrderDetail pick = new OrderDetail();
                pick.setId(detailId);
                pick.setGoodsId(d.getGoodsId());
                pick.setGoodsName(d.getGoodsName());
                pick.setMainImage(d.getMainImage());
                pick.setSpecList(d.getSpecList());
                pick.setPrice(d.getPrice());
                pick.setNumber(number);
                pick.setSubtotal(nz(d.getPrice()).multiply(BigDecimal.valueOf(number)));
                picked.add(pick);
            }
            if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new StoneCustomerException("部分退单的退款金额必须大于 0");
            }
        }

        // 落退款记录（复用现成模型）
        OrderRefund refund = new OrderRefund();
        refund.setOrderId(orderId);
        refund.setType(REFUND_TYPE_PAID_APPLY);
        refund.setRefundWay(way);
        refund.setRefundReason(reasonCode);
        refund.setRefundReasonDescription(reasonText);
        refund.setRefundAmount(refundAmount);
        refund.setRefundAccount(REFUND_ACCOUNT_WECHAT);
        refund.setStatus(REFUND_STATUS_WAIT_MERCHANT);
        refund.setGoodsTotalQuantity(refundQty);
        refund.setGoodsTotalPrice(refundAmount);
        refund.setCreateTime(new Date());
        orderRefundService.insertSelective(refund);

        for (OrderDetail d : picked) {
            OrderRefundGoods g = new OrderRefundGoods();
            g.setOrderRefundId(refund.getId());
            g.setOrderDetailId(d.getId());
            g.setGoodsId(d.getGoodsId());
            g.setGoodsName(d.getGoodsName());
            g.setMainImage(d.getMainImage());
            g.setSpecList(d.getSpecList());
            g.setPrice(d.getPrice());
            g.setNumber(d.getNumber());
            g.setSubtotal(d.getSubtotal());
            orderRefundGoodsService.insertSelective(g);
        }

        // ★ 退款执行：与「顾客申请→商家审核」同一套路径
        boolean refundSuccess;
        Integer paymentMode = dbOrder.getPaymentMode();
        if (paymentMode != null && paymentMode == 2) {
            // 余额支付：加回余额 + 写账单（逻辑与 auditAfterSalesOrder 一致）
            Member orderMember = memberService.selectByPrimaryKey(dbOrder.getMemberId());
            if (orderMember == null) {
                throw new StoneCustomerException("下单会员不存在，无法退回余额");
            }
            Member updateMember = new Member();
            updateMember.setId(orderMember.getId());
            updateMember.setBalance(nz(orderMember.getBalance()).add(refundAmount));
            updateMember.setTotalConsumeBalance(nz(orderMember.getTotalConsumeBalance()).subtract(refundAmount));
            memberService.updateByPrimaryKeySelective(updateMember);

            MemberBillingRecord record = new MemberBillingRecord();
            record.setMemberId(orderMember.getId());
            record.setOperateType(MemberBillingRecord.OPERATE_TYPE_ADD);
            record.setCoinType(MemberBillingRecord.COIN_TYPE_BALANCE);
            record.setNumber(refundAmount);
            record.setMessage("商家退单-余额退回");
            record.setCreateTime(new Date());
            memberBillingRecordService.insertSelective(record);
            refundSuccess = true;
        } else {
            // 微信支付：走 wxPayService.refund（非 prod profile 下为**模拟成功**，与收款 MockPay 同口径）
            refundSuccess = wxPayService.refund(dbOrder.getOrderNo(), nz(dbOrder.getActualPrice()), refundAmount);
        }
        if (!refundSuccess) {
            throw new StoneCustomerException("退款失败，请稍后重试或联系平台");
        }

        OrderRefund updateRefund = new OrderRefund();
        updateRefund.setId(refund.getId());
        updateRefund.setStatus(REFUND_STATUS_SUCCESS);
        updateRefund.setUpdateTime(new Date());
        orderRefundService.updateByPrimaryKeySelective(updateRefund);

        // 整单退 → 订单置「售后处理完成」；部分退 → 订单继续营业，仅记退款
        if (way == REFUND_WAY_ALL) {
            Order updateOrder = new Order();
            updateOrder.setId(orderId);
            updateOrder.setStatus(ORDER_STATUS_AFTER_SALE_DONE);
            updateOrder.setUpdateTime(new Date());
            orderService.updateById(updateOrder);
        }

        String snapshot = "refundWay=" + (way == REFUND_WAY_ALL ? "整单退" : "部分退")
                + ";items=" + picked.size() + ";qty=" + refundQty;
        writeLog(shopId, dbOrder, OrderChangeLog.TYPE_REFUND, nz(dbOrder.getActualPrice()),
                way == REFUND_WAY_ALL ? BigDecimal.ZERO : nz(dbOrder.getActualPrice()).subtract(refundAmount),
                refundAmount.negate(), refundAmount, way, reasonCode, reasonText, snapshot, operatorId);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("refundAmount", refundAmount);
        out.put("refundSuccess", true);
        out.put("refundId", refund.getId());
        out.put("orderStatus", way == REFUND_WAY_ALL ? ORDER_STATUS_AFTER_SALE_DONE : dbOrder.getStatus());
        return out;
    }

    /* ==================== 记录 ==================== */

    @Override
    public List<Map<String, Object>> records(Integer shopId, Integer changeType, String orderNo) {
        QueryWrapper<OrderChangeLog> qw = new QueryWrapper<>();
        qw.eq("shop_id", shopId);
        if (changeType != null) {
            qw.eq("change_type", changeType);
        }
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            qw.like("order_no", orderNo.trim());
        }
        qw.orderByDesc("id");
        List<OrderChangeLog> rows = orderChangeLogMapper.selectList(qw);
        List<Map<String, Object>> out = new ArrayList<>();
        for (OrderChangeLog r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("orderId", r.getOrderId());
            m.put("orderNo", r.getOrderNo());
            m.put("changeType", r.getChangeType());
            m.put("beforeAmount", r.getBeforeAmount());
            m.put("afterAmount", r.getAfterAmount());
            m.put("diffAmount", r.getDiffAmount());
            m.put("refundAmount", r.getRefundAmount());
            m.put("refundWay", r.getRefundWay());
            m.put("reasonCode", r.getReasonCode());
            m.put("reasonText", r.getReasonText());
            m.put("detailSnapshot", r.getDetailSnapshot());
            m.put("operatorId", r.getOperatorId());
            m.put("createTime", r.getCreateTime());
            out.add(m);
        }
        return out;
    }

    /* ==================== 私有 ==================== */

    private Order requireOrder(Integer shopId, Integer orderId) {
        if (orderId == null) {
            throw new StoneCustomerException("订单id不能为空");
        }
        Order dbOrder = orderService.getById(orderId);
        if (dbOrder == null) {
            throw new StoneCustomerException("该订单不存在");
        }
        if (!shopId.equals(dbOrder.getShopId())) {
            throw new StoneCustomerException("您没有权限操作该订单");
        }
        return dbOrder;
    }

    private void writeLog(Integer shopId, Order order, int type, BigDecimal before, BigDecimal after,
                          BigDecimal diff, BigDecimal refundAmount, Integer refundWay,
                          Integer reasonCode, String reasonText, String snapshot, Integer operatorId) {
        OrderChangeLog log = new OrderChangeLog();
        log.setShopId(shopId);
        log.setOrderId(order.getId());
        log.setOrderNo(order.getOrderNo());
        log.setChangeType(type);
        log.setBeforeAmount(before);
        log.setAfterAmount(after);
        log.setDiffAmount(diff);
        log.setRefundAmount(refundAmount);
        log.setRefundWay(refundWay);
        log.setReasonCode(reasonCode);
        log.setReasonText(reasonText);
        log.setDetailSnapshot(snapshot);
        log.setOperatorId(operatorId);
        log.setCreateTime(new Date());
        orderChangeLogMapper.insert(log);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static Integer intOrNull(Object v) {
        if (v == null || String.valueOf(v).isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal dec(Object v) {
        if (v == null || String.valueOf(v).isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
