package com.siam.system.modular.package_order.controller.merchant.internal;

import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_order.entity.internal.PointsMallOrder;
import com.siam.system.modular.package_order.model.param.internal.PointsMallOrderParam;
import com.siam.system.modular.package_order.service.internal.PointsMallOrderService;
import com.siam.system.modular.package_user.auth.cache.MerchantSessionManager;
import com.siam.system.modular.package_user.entity.Merchant;
import com.siam.system.util.TokenUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 商城订单-商家端接口（自提核销 / 待核销列表 / 核销记录）
 *
 * 鉴权：/rest/merchant/** 由 merchant 拦截器统一保护，商家身份取自登录会话。
 * 调用方：①管理端 A-06-4 商城订单页（经单店桥接会话）；②收银端 W-15 自提核销 + W-16 核销记录。
 * 状态机：待自提(4) → 已完成(6)，写 verify_time + verifier_id(商家 id)；幂等（已完成直接返回成功）。
 *
 * ⚠️ 查询端点一律以**登录商家的 shopId** 限定门店，不接受前端传入的 shopId，避免跨店读取。
 */
@RestController
@RequestMapping(value = "/rest/merchant/pointsMall/order")
@Transactional(rollbackFor = Exception.class)
@Api(tags = "商家端商城订单模块相关接口", description = "MerchantPointsMallOrderController")
public class MerchantPointsMallOrderController {

    @Resource(name = "pointsMallOrderServiceImpl")
    private PointsMallOrderService orderService;

    @Autowired
    private MerchantSessionManager merchantSessionManager;

    private Merchant loginMerchant() {
        return merchantSessionManager.getSession(TokenUtil.getToken());
    }

    @ApiOperation(value = "自提核销（待自提 → 已完成）")
    @PostMapping(value = "/confirmPickup")
    public BasicResult confirmPickup(@RequestBody @Validated(value = {}) PointsMallOrderParam param) {
        orderService.confirmPickupByMerchant(param);
        return BasicResult.success();
    }

    /**
     * 商城订单列表（收银端 W-15「待核销订单」用：status 传 4）
     *
     * <p>支持筛选：status（4=待自提）、pickupCode（自提码，模糊）、keyWords（手机号/订单号）。
     * ⚠️ 商家端视图，**前端传的 shopId 会被会话里的 shopId 覆盖**。
     */
    @ApiOperation(value = "商城订单列表（商家端 · 待核销等）")
    @PostMapping(value = "/list")
    public BasicResult list(@RequestBody(required = false) PointsMallOrderParam param) {
        PointsMallOrderParam query = param == null ? new PointsMallOrderParam() : param;
        query.setShopId(loginMerchant().getShopId());
        return BasicResult.success(orderService.listForMerchant(query));
    }

    /**
     * 核销记录（收银端 W-16）
     *
     * <p>固定取「已完成 + verify_time 非空」，即真正被核销过的单；
     * 支持 pickupCode / verifierId / 核销时间区间（startVerifyTime、endVerifyTime）筛选。
     * 返回行已补 verifierName（核销人姓名）。
     */
    @ApiOperation(value = "核销记录列表（商家端）")
    @PostMapping(value = "/verifyRecord/list")
    public BasicResult verifyRecordList(@RequestBody(required = false) PointsMallOrderParam param) {
        PointsMallOrderParam query = param == null ? new PointsMallOrderParam() : param;
        query.setShopId(loginMerchant().getShopId());
        // 商城自提单核销后状态为「已完成(6)」；verify_time 非空才是真正核销过的
        query.setStatus(PointsMallOrder.STATUS_OF_COMPLETED);
        query.setOnlyVerified(true);
        return BasicResult.success(orderService.listForMerchant(query));
    }
}
