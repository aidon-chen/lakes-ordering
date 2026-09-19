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
     * 商家完结（预留接口，本批无商家端 UI）：status=2 已开具 / 3 已驳回，写 finish_time
     */
    void finish(InvoiceApplyParam param);
}
