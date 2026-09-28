package com.siam.system.modular.package_service.service_impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.siam.package_common.exception.StoneCustomerException;
import com.siam.system.modular.package_order.entity.Order;
import com.siam.system.modular.package_order.mapper.OrderMapper;
import com.siam.system.modular.package_service.entity.InvoiceApply;
import com.siam.system.modular.package_service.mapper.InvoiceApplyMapper;
import com.siam.system.modular.package_service.model.param.InvoiceApplyParam;
import com.siam.system.modular.package_service.service.InvoiceApplyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 发票申请服务实现 · C-15
 *
 * 关键约束：一张订单只能开一次发票 —— 服务层事务内复查 is_invoice=0，
 * DB 层由 update.sql §6 的 UNIQUE KEY uk_order(order_id) 兜底（并发兜底）。
 * 金额取 tb_order.actualPrice（订单实付），order_no 为快照。
 */
@Service
public class InvoiceApplyServiceImpl implements InvoiceApplyService {

    /** 订单已完成（tb_order.status 语义：6=已完成） */
    private static final int ORDER_STATUS_FINISHED = 6;

    /** 申请状态：1 待处理 2 已开具 3 已驳回 */
    private static final int STATUS_PENDING = 1;
    private static final int STATUS_ISSUED = 2;
    private static final int STATUS_REJECTED = 3;

    private static final int TITLE_TYPE_PERSONAL = 1;
    private static final int TITLE_TYPE_COMPANY = 2;

    @Autowired
    private InvoiceApplyMapper invoiceApplyMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Override
    public void apply(Integer memberId, InvoiceApplyParam param) {
        if (param.getOrderId() == null) {
            throw new StoneCustomerException("订单id不能为空");
        }
        Order order = orderMapper.selectById(param.getOrderId());
        if (order == null) {
            throw new StoneCustomerException("订单不存在");
        }
        if (!Objects.equals(order.getMemberId(), memberId)) {
            throw new StoneCustomerException("无权操作该订单");
        }
        if (order.getStatus() == null || order.getStatus() != ORDER_STATUS_FINISHED) {
            throw new StoneCustomerException("仅已完成订单可申请开票");
        }
        if (Boolean.TRUE.equals(order.getIsInvoice())) {
            throw new StoneCustomerException("该订单已申请开票");
        }
        if (param.getTitleType() == null) {
            param.setTitleType(TITLE_TYPE_PERSONAL);
        }
        if (param.getTitle() == null || param.getTitle().trim().isEmpty()) {
            throw new StoneCustomerException("请填写发票抬头");
        }
        if (param.getTitleType() == TITLE_TYPE_COMPANY
                && (param.getTaxNo() == null || param.getTaxNo().trim().isEmpty())) {
            throw new StoneCustomerException("企业抬头需填写纳税人识别号");
        }
        if (param.getEmail() == null || !param.getEmail().trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new StoneCustomerException("请填写正确的接收邮箱");
        }
        // 并发/重复点击兜底：同一订单已存在申请记录即拒绝（uk_order 为 DB 层第二道防线）
        Integer existed = invoiceApplyMapper.selectCount(
                new QueryWrapper<InvoiceApply>().eq("order_id", order.getId()));
        if (existed != null && existed > 0) {
            throw new StoneCustomerException("该订单已申请开票");
        }

        Date now = new Date();
        InvoiceApply apply = new InvoiceApply();
        apply.setMemberId(memberId);
        apply.setOrderId(order.getId());
        apply.setOrderNo(order.getOrderNo());
        apply.setAmount(order.getActualPrice());
        apply.setTitleType(param.getTitleType());
        apply.setTitle(param.getTitle().trim());
        apply.setTaxNo(param.getTitleType() == TITLE_TYPE_COMPANY ? param.getTaxNo().trim() : null);
        apply.setEmail(param.getEmail().trim());
        apply.setStatus(STATUS_PENDING);
        apply.setApplyTime(now);
        apply.setCreateTime(now);
        invoiceApplyMapper.insert(apply);

        // 回写订单开票标记（同一事务，Controller 层 @Transactional）
        Order upd = new Order();
        upd.setId(order.getId());
        upd.setIsInvoice(true);
        upd.setInvoiceId(apply.getId().intValue());
        orderMapper.updateById(upd);
    }

    @Override
    public List<InvoiceApply> listByMember(Integer memberId, Integer status) {
        QueryWrapper<InvoiceApply> qw = new QueryWrapper<>();
        qw.eq("member_id", memberId);
        if (status != null) {
            qw.eq("status", status);
        }
        qw.orderByDesc("apply_time").orderByDesc("id");
        return invoiceApplyMapper.selectList(qw);
    }

    @Override
    public List<InvoiceApply> listForMerchant(Integer status) {
        QueryWrapper<InvoiceApply> qw = new QueryWrapper<>();
        if (status != null) {
            qw.eq("status", status);
        }
        qw.orderByDesc("apply_time").orderByDesc("id");
        return invoiceApplyMapper.selectList(qw);
    }

    @Override
    public void finish(InvoiceApplyParam param) {
        if (param.getId() == null) {
            throw new StoneCustomerException("申请记录id不能为空");
        }
        Integer status = param.getStatus();
        if (status == null || (status != STATUS_ISSUED && status != STATUS_REJECTED)) {
            throw new StoneCustomerException("状态仅支持 2=已开具 / 3=已驳回");
        }
        InvoiceApply db = invoiceApplyMapper.selectById(param.getId());
        if (db == null) {
            throw new StoneCustomerException("申请记录不存在");
        }
        if (status == STATUS_REJECTED && (param.getRemark() == null || param.getRemark().trim().isEmpty())) {
            throw new StoneCustomerException("驳回必须填写原因");
        }

        InvoiceApply upd = new InvoiceApply();
        upd.setId(db.getId());
        upd.setStatus(status);
        upd.setRemark(param.getRemark());
        upd.setFinishTime(new Date());
        invoiceApplyMapper.updateById(upd);
    }
}
