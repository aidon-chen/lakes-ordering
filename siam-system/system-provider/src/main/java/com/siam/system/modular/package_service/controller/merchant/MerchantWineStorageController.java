package com.siam.system.modular.package_service.controller.merchant;

import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_log.annoation.Log;
import com.siam.system.modular.package_log.aop.LogContext;
import com.siam.system.modular.package_service.entity.WineStorage;
import com.siam.system.modular.package_service.model.param.WineStorageParam;
import com.siam.system.modular.package_goods.entity.internal.ShopSetting;
import com.siam.system.modular.package_goods.service.internal.ShopSettingService;
import com.siam.system.modular.package_service.service.WineRemindService;
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
import io.swagger.annotations.ApiOperation;

/**
 * 存酒接口（商家侧）· W-17 登记 / W-18 存酒列表与取酒核销（M-06 存酒管理）
 *
 * <p>取酒单一模型（方案 v1.1）：申请 → 取酒码 → 核销。商家侧能力＝「代发起取酒」，
 * **不做直接扣减**；每一次扣瓶都对应一笔取酒申请单（`apply_source` 区分来源）。
 *
 * <p>鉴权：/rest/merchant/** 由 merchantInterceptor 统一保护。
 */
@RestController
@RequestMapping(value = "/rest/merchant/wineStorage")
@Transactional(rollbackFor = Exception.class)
public class MerchantWineStorageController {

    @Autowired
    private WineStorageService wineStorageService;

    @Autowired
    private WineRemindService wineRemindService;

    @Autowired
    private ShopSettingService shopSettingService;

    @Autowired
    private MerchantSessionManager merchantSessionManager;

    private Merchant loginMerchant() {
        return merchantSessionManager.getSession(TokenUtil.getToken());
    }

    /**
     * 存酒列表（W-18）
     *
     * <p>按登录商家所在门店限定；支持「顾客手机号」精确筛选。
     * 四态口径与顾客侧 C-16 完全一致，并额外填充会员姓名与**脱敏**手机号供「顾客」列展示。
     */
    @PostMapping(value = "/list")
    public BasicResult list(@RequestBody(required = false) WineStorageParam param) {
        return BasicResult.success(wineStorageService.listByShop(loginMerchant().getShopId(), param));
    }

    /**
     * 录入存酒：手机号定位会员 → 有效期默认 90 天（取酒码在「申请/代发起」时生成）
     */
    @Log(module = Log.MODULE_WINE_STORAGE, moduleName = "存酒", action = "录入存酒", businessType = Log.TYPE_INSERT)
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

        // 回给前端创建结果：仅 id 与有效期（不再有存酒码，W-17 凭证预览改为展示有效期）
        Map<String, Object> data = new HashMap<>();
        data.put("id", created.getId());
        data.put("expireDate", created.getExpireDate());
        return BasicResult.success(data);
    }

    /**
     * 待核销取酒申请列表（收银端弹层待办）
     */
    @PostMapping(value = "/apply/list")
    public BasicResult applyList(@RequestBody(required = false) WineStorageParam param) {
        return BasicResult.success(wineStorageService.listPendingApplies(loginMerchant().getShopId()));
    }

    /**
     * 按取酒码预览待核销详情（只读；供「两段式核销」第一步）
     */
    @PostMapping(value = "/apply/preview")
    public BasicResult applyPreview(@RequestBody WineStorageParam param) {
        return BasicResult.success(wineStorageService.previewTake(loginMerchant().getShopId(), param.getTakeCode()));
    }

    /**
     * 取酒核销（原子扣减，防超扣；扣到 0 自动置已取完，并写取酒记录）
     */
    @Log(module = Log.MODULE_WINE_STORAGE, moduleName = "存酒", action = "取酒核销", businessType = Log.TYPE_UPDATE)
    @PostMapping(value = "/apply/confirm")
    public BasicResult applyConfirm(@RequestBody WineStorageParam param) {
        //A-08 操作日志：核销为扣减动作，变更前值记取酒码，变更后值记核销结果
        Map<String, Object> before = new HashMap<>();
        before.put("takeCode", param.getTakeCode());
        LogContext.setBefore(before);

        Map<String, Object> after = new HashMap<>();
        after.put("status", "已核销");
        LogContext.setAfter(after);

        return BasicResult.success(wineStorageService.confirmTake(loginMerchant().getId(), param.getTakeCode()));
    }

    /**
     * 代发起取酒（商家兜底通道：无小程序顾客 / 登记手机号录错 / 现场代发）
     */
    @Log(module = Log.MODULE_WINE_STORAGE, moduleName = "存酒", action = "代发起取酒", businessType = Log.TYPE_UPDATE)
    @PostMapping(value = "/apply/create")
    public BasicResult applyCreate(@RequestBody WineStorageParam param) {
        Map<String, Object> after = new HashMap<>();
        after.put("storageId", param.getStorageId());
        after.put("takeCount", param.getTakeCount());
        after.put("remark", param.getRemark());
        LogContext.setAfter(after);

        return BasicResult.success(wineStorageService.createApplyByShop(
                loginMerchant().getId(), loginMerchant().getShopId(), param));
    }
    /* ==================== 批次 F · W-19 存酒临期提醒 ==================== */

    /** W-19 任务表：存续中+已过期 且 剩余天数≤提前天数的存酒（含顾客脱敏手机号与通知状态） */
    @ApiOperation(value = "存酒临期提醒列表（W-19）")
    @PostMapping(value = "/remind/list")
    public BasicResult remindList() {
        Integer shopId = loginMerchant().getShopId();
        return BasicResult.success(wineRemindService.listForMerchant(shopId, wineRemindService.aheadDaysForShop(shopId)));
    }

    /** W-19 指标行：临期 / 过期 / 已通知 */
    @ApiOperation(value = "存酒临期提醒指标（W-19）")
    @PostMapping(value = "/remind/summary")
    public BasicResult remindSummary() {
        Integer shopId = loginMerchant().getShopId();
        return BasicResult.success(wineRemindService.summary(shopId, wineRemindService.aheadDaysForShop(shopId)));
    }

    /** W-19 提醒规则：读「到期前 N 天」 */
    @ApiOperation(value = "读提醒规则（W-19）")
    @PostMapping(value = "/remind/setting/list")
    public BasicResult remindSetting() {
        Map<String, String> s = shopSettingService.getSettings(loginMerchant().getShopId());
        Map<String, Object> out = new HashMap<>();
        out.put("wineRemindAheadDays", s.get(ShopSetting.KEY_WINE_REMIND_AHEAD_DAYS));
        return BasicResult.success(out);
    }

    /** W-19 提醒规则：保存「到期前 N 天」 */
    @ApiOperation(value = "保存提醒规则（W-19）")
    @PostMapping(value = "/remind/setting/save")
    public BasicResult remindSettingSave(@RequestBody Map<String, String> body) {
        String days = body.get("wineRemindAheadDays");
        int v = 7;
        try {
            v = Math.max(1, Integer.parseInt(String.valueOf(days == null ? "7" : days)));
        } catch (NumberFormatException ignore) {
        }
        shopSettingService.saveSettings(loginMerchant().getShopId(),
                java.util.Collections.singletonMap(ShopSetting.KEY_WINE_REMIND_AHEAD_DAYS, String.valueOf(v)));
        return BasicResult.success(java.util.Collections.singletonMap(ShopSetting.KEY_WINE_REMIND_AHEAD_DAYS, String.valueOf(v)));
    }
}