package com.siam.system.modular.package_dining.controller.merchant;

import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_dining.model.param.DiningTableParam;
import com.siam.system.modular.package_dining.service.DiningTableService;
import com.siam.system.modular.package_user.auth.cache.MerchantSessionManager;
import com.siam.system.modular.package_user.entity.Merchant;
import com.siam.system.util.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 收银台台位作业接口（开台/清台/转台）
 * 鉴权：/rest/merchant/** 由 merchant 拦截器统一保护
 * 台位档案（增删改/桌码）归 A-09 在商家中心做，本期不提供
 */
@RestController
@RequestMapping(value = "/rest/merchant/diningTable")
@Transactional(rollbackFor = Exception.class)
public class MerchantDiningTableController {

    @Autowired
    private DiningTableService diningTableService;

    @Autowired
    private MerchantSessionManager merchantSessionManager;

    private Integer shopId() {
        Merchant loginMerchant = merchantSessionManager.getSession(TokenUtil.getToken());
        return loginMerchant.getShopId();
    }

    /**
     * 桌台图数据：全量台位 + 区域列表（不分页）
     */
    @PostMapping(value = "/list")
    public BasicResult list(@RequestBody(required = false) DiningTableParam param) {
        return BasicResult.success(diningTableService.list(shopId()));
    }

    /**
     * 开台
     */
    @PostMapping(value = "/open")
    public BasicResult open(@RequestBody DiningTableParam param) {
        return BasicResult.success(diningTableService.openTable(shopId(), param.getId(), param.getMemberNum()));
    }

    /**
     * 转台
     */
    @PostMapping(value = "/change")
    public BasicResult change(@RequestBody DiningTableParam param) {
        diningTableService.changeTable(shopId(), param.getId(), param.getTargetTableId());
        return BasicResult.success();
    }

    /**
     * 清台
     */
    @PostMapping(value = "/clear")
    public BasicResult clear(@RequestBody DiningTableParam param) {
        diningTableService.clearTable(shopId(), param.getId());
        return BasicResult.success();
    }
}
