package com.siam.system.modular.package_service.service;

import com.siam.system.modular.package_service.entity.InvoiceApply;
import com.siam.system.modular.package_service.model.param.InvoiceApplyParam;

import java.util.List;

/**
 * 发票申请服务 · C-15
 */
public interface InvoiceApplyService {

    /**
     * 会员提交开票申请（同一事务：插 tb_invoice_apply + 回写 tb_order.is_invoice/invoice_id）
     *
     * 校验链：订单存在 → 归属当前会员 → status=6(已完成) → is_invoice=false
     *        → title 必填 → titleType=2 时 taxNo 必填 → email 格式
     */
    void apply(Integer memberId, InvoiceApplyParam param);

    /**
     * 会员的申请记录（apply_time 倒序；C-15 同页「申请记录」卡片）
     */
    List<InvoiceApply> listByMember(Integer memberId, Integer status);

    /**
     * 商家审批列表（全量，status 可选过滤 1 待处理 / 2 已开具 / 3 已驳回；apply_time 倒序）
     *
     * 注：tb_invoice_apply 无 shop_id 列——唯一门店期（湖里Lakes）不做门店过滤，
     * 多店场景需先加列再补过滤（铁律：加列本地库+云库同步）。
     */
    List<InvoiceApply> listForMerchant(Integer status);

    /**
     * 商家完结（发票审批页）：status=2 已开具 / 3 已驳回，写 finish_time
     */
    void finish(InvoiceApplyParam param);
}
