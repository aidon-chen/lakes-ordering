package com.siam.system.modular.package_dining.controller.merchant;

import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_dining.entity.TableType;
import com.siam.system.modular.package_dining.service.TableManageService;
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
 * 台位类型管理接口（A-09：散台/卡座/包间，低消仅展示不校验）
 * 鉴权：/rest/merchant/** 由 merchant 拦截器统一保护
 */
@RestController
@RequestMapping(value = "/rest/merchant/tableType")
@Transactional(rollbackFor = Exception.class)
public class MerchantTableTypeController {

    @Autowired
    private TableManageService tableManageService;

    @Autowired
    private MerchantSessionManager merchantSessionManager;

    private Integer shopId() {
        Merchant loginMerchant = merchantSessionManager.getSession(TokenUtil.getToken());
        return loginMerchant.getShopId();
    }

    @PostMapping(value = "/list")
    public BasicResult list() {
        return BasicResult.success(tableManageService.listType(shopId()));
    }

    @PostMapping(value = "/add")
    public BasicResult add(@RequestBody TableType type) {
        tableManageService.addType(shopId(), type);
        return BasicResult.success();
    }

    @PostMapping(value = "/update")
    public BasicResult update(@RequestBody TableType type) {
        tableManageService.updateType(shopId(), type);
        return BasicResult.success();
    }

    @PostMapping(value = "/delete")
    public BasicResult delete(@RequestBody TableType type) {
        tableManageService.deleteType(shopId(), type.getId());
        return BasicResult.success();
    }
}
