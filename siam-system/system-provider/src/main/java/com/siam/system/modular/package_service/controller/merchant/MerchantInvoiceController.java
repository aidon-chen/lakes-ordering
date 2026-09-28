package com.siam.system.modular.package_service.controller.merchant;

import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_service.model.param.InvoiceApplyParam;
import com.siam.system.modular.package_service.service.InvoiceApplyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 发票接口（商家侧）· 审批页配套（批次：09-27 商家端审批界面）
 *
 * 完结/审批由商家操作（顾客侧只有「申请」，没有「完结」动作）。
 * 鉴权：/rest/merchant/** 由 merchantInterceptor 统一保护。
 */
@RestController
@RequestMapping(value = "/rest/merchant/invoice")
@Transactional(rollbackFor = Exception.class)
public class MerchantInvoiceController {

    @Autowired
    private InvoiceApplyService invoiceApplyService;

    /**
     * 审批列表：全量申请记录，status 可选过滤（1 待处理 / 2 已开具 / 3 已驳回），apply_time 倒序
     */
    @PostMapping(value = "/list")
    public BasicResult list(@RequestBody(required = false) InvoiceApplyParam param) {
        Integer status = param == null ? null : param.getStatus();
        return BasicResult.success(invoiceApplyService.listForMerchant(status));
    }

    /**
     * 完结开票申请：status=2 已开具 / 3 已驳回（驳回必须填 remark）
     */
    @PostMapping(value = "/finish")
    public BasicResult finish(@RequestBody InvoiceApplyParam param) {
        invoiceApplyService.finish(param);
        return BasicResult.success();
    }
}
