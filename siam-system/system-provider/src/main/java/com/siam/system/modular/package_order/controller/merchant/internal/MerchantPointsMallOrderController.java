package com.siam.system.modular.package_order.controller.merchant.internal;

import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_order.model.param.internal.PointsMallOrderParam;
import com.siam.system.modular.package_order.service.internal.PointsMallOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 商城订单-商家端接口（自提核销）
 *
 * 鉴权：/rest/merchant/** 由 merchant 拦截器统一保护，商家身份取自登录会话。
 * 调用方：①管理端 A-06-4 商城订单页（经单店桥接会话）；②收银端 收银-M-03（后续批次，扫码+手输双通道）。
 * 状态机：待自提(4) → 已完成(6)，写 verify_time + verifier_id(商家 id)；幂等（已完成直接返回成功）。
 */
@RestController
@RequestMapping(value = "/rest/merchant/pointsMall/order")
@Transactional(rollbackFor = Exception.class)
@Api(tags = "商家端商城订单模块相关接口", description = "MerchantPointsMallOrderController")
public class MerchantPointsMallOrderController {

    @Resource(name = "pointsMallOrderServiceImpl")
    private PointsMallOrderService orderService;

    @ApiOperation(value = "自提核销（待自提 → 已完成）")
    @PostMapping(value = "/confirmPickup")
    public BasicResult confirmPickup(@RequestBody @Validated(value = {}) PointsMallOrderParam param) {
        orderService.confirmPickupByMerchant(param);
        return BasicResult.success();
    }
}