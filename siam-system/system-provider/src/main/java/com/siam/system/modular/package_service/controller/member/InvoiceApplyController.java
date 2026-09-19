package com.siam.system.modular.package_service.controller.member;

import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_service.model.param.InvoiceApplyParam;
import com.siam.system.modular.package_service.service.InvoiceApplyService;
import com.siam.system.modular.package_user.auth.cache.MemberSessionManager;
import com.siam.system.modular.package_user.entity.Member;
import com.siam.system.util.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 发票接口（会员侧）· C-15
 *
 * 鉴权：/rest/member/** 由 memberInterceptor 统一保护，需登录。
 * apply 走事务（插申请 + 回写订单开票标记必须同生共死）。
 */
@RestController
@RequestMapping(value = "/rest/member/invoice")
@Transactional(rollbackFor = Exception.class)
public class InvoiceApplyController {

    @Autowired
    private InvoiceApplyService invoiceApplyService;

    @Autowired
    private MemberSessionManager memberSessionManager;

    private Member loginMember() {
        return memberSessionManager.getSession(TokenUtil.getToken());
    }

    /**
     * 提交开票申请（C-15 表单；仅已完成且未开票的订单可申请）
     */
    @PostMapping(value = "/apply")
    public BasicResult apply(@RequestBody InvoiceApplyParam param) {
        invoiceApplyService.apply(loginMember().getId(), param);
        return BasicResult.success();
    }

    /**
     * 申请记录（C-15 同页「申请记录」卡片，apply_time 倒序）
     */
    @PostMapping(value = "/list")
    public BasicResult list(@RequestBody(required = false) InvoiceApplyParam param) {
        Integer status = param == null ? null : param.getStatus();
        return BasicResult.success(invoiceApplyService.listByMember(loginMember().getId(), status));
    }
}
