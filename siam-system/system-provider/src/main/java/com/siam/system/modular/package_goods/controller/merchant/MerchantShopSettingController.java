package com.siam.system.modular.package_goods.controller.merchant;

import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_log.annoation.Log;
import com.siam.system.modular.package_log.aop.LogContext;
import com.siam.system.modular.package_goods.entity.internal.ShopSetting;
import com.siam.system.modular.package_goods.service.internal.ShopSettingService;
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

import java.util.Map;

/**
 * 门店营业设置 · 批次 G · 收银端 W-23
 *
 * <p>⚠️ 门店维度：一律用**登录商家的 shopId**，不接受前端传入，避免跨店读写。
 *
 * <p>⚠️ 与画板的两处差异（已核实，非偷懒）：
 * <ol>
 *   <li>画板的「低消校验」开关**不实现** —— 需求书 V7 变更一明令取消最低消费校验，
 *       做成可用开关等于把已撤销的功能又装回去（同批次 C 的 W-08 裁决）。</li>
 *   <li>画板的「云打印」开关可以存，但它只代表**意愿**；真正的云打印下发受云打印平台账号阻塞（P-01）。</li>
 * </ol>
 */
@RestController
@RequestMapping(value = "/rest/merchant/shopSetting")
@Transactional(rollbackFor = Exception.class)
@Api(tags = "门店营业设置接口", description = "MerchantShopSettingController")
public class MerchantShopSettingController {

    @Autowired
    private ShopSettingService shopSettingService;

    @Autowired
    private MerchantSessionManager merchantSessionManager;

    private Merchant loginMerchant() {
        return merchantSessionManager.getSession(TokenUtil.getToken());
    }

    /** 读出门店全部设置（键值对）；前端负责按类型解析与填默认值 */
    @ApiOperation(value = "读出门店营业设置")
    @PostMapping(value = "/list")
    public BasicResult list() {
        return BasicResult.success(shopSettingService.getSettings(loginMerchant().getShopId()));
    }

    /** 批量保存设置（upsert）；只写传入的键，未传的键保持不变 */
    @ApiOperation(value = "保存门店营业设置")
    //A-08 操作日志：门店设置项变更，快照记保存后的全量键值
    @Log(module = Log.MODULE_SHOP, moduleName = "门店设置", action = "修改门店设置", businessType = Log.TYPE_UPDATE)
    @PostMapping(value = "/save")
    public BasicResult save(@RequestBody Map<String, String> settings) {
        shopSettingService.saveSettings(loginMerchant().getShopId(), settings);
        Map<String, String> afterSettings = shopSettingService.getSettings(loginMerchant().getShopId());
        LogContext.setAfter(afterSettings);
        return BasicResult.success(afterSettings);
    }

    /** 该模块支持的全部设置键（供前端渲染与校验，避免前后端键名漂移） */
    @ApiOperation(value = "门店营业设置的键清单")
    @PostMapping(value = "/keys")
    public BasicResult keys() {
        return BasicResult.success(shopSettingService.knownKeys());
    }
}
