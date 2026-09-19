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
 * 发票接口（商家侧）· 批次 5 决策 D-F/D-F：**预留接口，本批无商家端 UI**
 *
 * 取消/驳回开票由商家操作（顾客侧只有「申请」，没有「完结」动作）。
 * 商家端界面（收银端/管理后台）后续批次接入时直接调本接口。
 *
 * 鉴权：/rest/merchant/** 由 merchantInterceptor 统一保护。
 */
@RestController
@RequestMapping(value = "/rest/merchant/invoice")
@Transactional(rollbackFor = Exception.class)
public class MerchantInvoiceController {

    @Autowired
    private InvoiceApplyService invoiceApplyService;

    /**
     * 完结开票申请：status=2 已开具 / 3 已驳回（驳回必须填 remark）
     */
    @PostMapping(value = "/finish")
    public BasicResult finish(@RequestBody InvoiceApplyParam param) {
        invoiceApplyService.finish(param);
        return BasicResult.success();
    }
}
