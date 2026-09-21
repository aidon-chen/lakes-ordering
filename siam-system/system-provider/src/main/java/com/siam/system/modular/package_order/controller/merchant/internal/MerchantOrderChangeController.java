package com.siam.system.modular.package_order.controller.merchant.internal;

import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_order.service.internal.OrderChangeService;
import com.siam.system.modular.package_user.auth.cache.MerchantSessionManager;
import com.siam.system.modular.package_user.entity.Merchant;
import com.siam.system.util.TokenUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单改单 / 退单 · 批次 G · 收银端 W-13
 *
 * <p>需求依据 M-02「接单、改单、退单，订单状态流转」。
 * 退单**复用现成退款模型**（tb_order_refund + _goods）与**同一套退款执行路径**：
 * 微信走 wxPayService.refund（非 prod 为模拟，与收款 MockPay 同口径）、余额走加余额+账单。
 *
 * <p>⚠️ 门店维度一律取登录会话的 shopId；订单归属校验在服务层做。
 */
@RestController
@RequestMapping(value = "/rest/merchant/orderChange")
@Transactional(rollbackFor = Exception.class)
@Api(tags = "订单改单退单接口", description = "MerchantOrderChangeController")
public class MerchantOrderChangeController {

    @Autowired
    private OrderChangeService orderChangeService;

    @Autowired
    private MerchantSessionManager merchantSessionManager;

    private Merchant loginMerchant() {
        return merchantSessionManager.getSession(TokenUtil.getToken());
    }

    /**
     * 改单：改数量 / 加新品 / 删明细，重算金额并留痕。
     * 请求体：{ orderId, items: [{ detailId?, goodsId?, goodsName?, price?, number }] }
     * （detailId 为空即新增；新增项需带 goodsName/price 快照）
     */
    @ApiOperation(value = "改单")
    @PostMapping(value = "/modify")
    public BasicResult modify(@RequestBody Map<String, Object> body) {
        Integer orderId = intOrNull(body.get("orderId"));
        List<Map<String, Object>> items = mapList(body.get("items"));
        Merchant m = loginMerchant();
        return BasicResult.success(orderChangeService.modify(m.getShopId(), m.getId(), orderId, items));
    }

    /**
     * 退单：整单退 / 部分退。
     * 请求体：{ orderId, refundWay(1全额/2部分), reasonCode, reasonText?, items:[{detailId, number}] }
     */
    @ApiOperation(value = "退单（含退款执行）")
    @PostMapping(value = "/refund")
    public BasicResult refund(@RequestBody Map<String, Object> body) {
        Integer orderId = intOrNull(body.get("orderId"));
        Integer refundWay = intOrNull(body.get("refundWay"));
        Integer reasonCode = intOrNull(body.get("reasonCode"));
        String reasonText = body.get("reasonText") == null ? null : String.valueOf(body.get("reasonText"));
        List<Map<String, Object>> items = mapList(body.get("items"));
        Merchant m = loginMerchant();
        return BasicResult.success(orderChangeService.refund(
                m.getShopId(), m.getId(), orderId, refundWay, reasonCode, reasonText, items));
    }

    /** 改单/退单记录；请求体可选：{ changeType?, orderNo? } */
    @ApiOperation(value = "改单退单记录")
    @PostMapping(value = "/record/list")
    public BasicResult recordList(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> q = body == null ? new HashMap<>() : body;
        Integer changeType = intOrNull(q.get("changeType"));
        String orderNo = q.get("orderNo") == null ? null : String.valueOf(q.get("orderNo"));
        return BasicResult.success(orderChangeService.records(loginMerchant().getShopId(), changeType, orderNo));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> mapList(Object v) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (v instanceof List) {
            for (Object o : (List<Object>) v) {
                if (o instanceof Map) {
                    out.add((Map<String, Object>) o);
                }
            }
        }
        return out;
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
}
