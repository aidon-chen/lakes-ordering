package com.siam.system.modular.package_service.controller.merchant;

import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_log.annoation.Log;
import com.siam.system.modular.package_log.aop.LogContext;
import com.siam.system.modular.package_service.entity.WineStorage;
import com.siam.system.modular.package_service.model.param.WineStorageParam;
import com.siam.system.modular.package_service.service.WineStorageService;
import com.siam.system.modular.package_user.auth.cache.MerchantSessionManager;
import com.siam.system.modular.package_user.entity.Merchant;
import com.siam.system.util.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 存酒接口（商家侧）· 批次 5 决策 D-F：**预留接口，本批无商家端 UI**
 *
 * 存酒「录入」与「取酒扣瓶」都是商家动作 —— 顾客侧（C-16/C-01 存酒区）只能查看。
 * 原型 4 块画板均为顾客侧界面，没有商家端板子，故本批只落接口；
 * 商家端界面（收银端/管理后台）后续批次接入时直接调。
 *
 * 鉴权：/rest/merchant/** 由 merchantInterceptor 统一保护。
 */
@RestController
@RequestMapping(value = "/rest/merchant/wineStorage")
@Transactional(rollbackFor = Exception.class)
public class MerchantWineStorageController {

    @Autowired
    private WineStorageService wineStorageService;

    @Autowired
    private MerchantSessionManager merchantSessionManager;

    private Merchant loginMerchant() {
        return merchantSessionManager.getSession(TokenUtil.getToken());
    }

    /**
     * 存酒列表（W-18 存酒列表与取酒核销）
     *
     * <p>按登录商家所在门店限定；支持「存酒码」模糊、「顾客手机号」精确筛选。
     * 四态口径与顾客侧 C-16 完全一致（存续中 / 将过期 / 已过期 / 已取完），
     * 并额外填充会员姓名与**脱敏**手机号供列表「顾客」列展示。
     */
    @PostMapping(value = "/list")
    public BasicResult list(@RequestBody(required = false) WineStorageParam param) {
        return BasicResult.success(wineStorageService.listByShop(loginMerchant().getShopId(), param));
    }

    /**
     * 录入存酒：手机号定位会员 → 生成唯一 4 位存酒码 → 有效期默认 90 天
     */
    @Log(module = Log.MODULE_WINE_STORAGE, moduleName = "存酒", action = "录入存酒")
    @PostMapping(value = "/create")
    public BasicResult create(@RequestBody WineStorageParam param) {
        WineStorage created = wineStorageService.create(param, loginMerchant().getShopId());
        //A-08 操作日志：录入无变更前值，变更后值记本次录入内容
        Map<String, Object> after = new HashMap<>();
        after.put("memberPhone", param.getMemberPhone());
        after.put("goodsName", param.getGoodsName());
        after.put("location", param.getLocation());
        after.put("bottleCount", param.getBottleCount());
        after.put("expireDays", param.getExpireDays());
        LogContext.setAfter(after);

        // 回给前端创建结果（W-17 凭证预览要展示存酒码）——仅 id/存酒码/有效期，不外泄其它字段
        Map<String, Object> data = new HashMap<>();
        data.put("id", created.getId());
        data.put("storageCode", created.getStorageCode());
        data.put("expireDate", created.getExpireDate());
        return BasicResult.success(data);
    }

    /**
     * 取酒（原子扣减，防超扣；扣到 0 自动置已取完，并写取酒记录）
     */
    @Log(module = Log.MODULE_WINE_STORAGE, moduleName = "存酒", action = "取酒")
    @PostMapping(value = "/take")
    public BasicResult take(@RequestBody WineStorageParam param) {
        //A-08 操作日志：取酒为扣减动作，变更前值记存酒码与本次取酒瓶数（扣减后的剩余瓶数由存酒记录自行维护）
        Map<String, Object> before = new HashMap<>();
        before.put("storageCode", param.getStorageCode());
        before.put("takeCount", param.getTakeCount());
        LogContext.setBefore(before);

        wineStorageService.take(param, loginMerchant().getId());
        return BasicResult.success();
    }
}
