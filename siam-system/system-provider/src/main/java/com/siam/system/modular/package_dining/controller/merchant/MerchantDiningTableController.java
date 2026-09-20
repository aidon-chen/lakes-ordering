package com.siam.system.modular.package_dining.controller.merchant;

import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_dining.model.param.DiningTableParam;
import com.siam.system.modular.package_dining.service.DiningTableService;
import com.siam.system.modular.package_log.annoation.Log;
import com.siam.system.modular.package_log.aop.LogContext;
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
 * 收银台台位作业接口（开台/结账/清台/转台）
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
    @Log(module = Log.MODULE_TABLE, moduleName = "开台", action = "开台")
    @PostMapping(value = "/open")
    public BasicResult open(@RequestBody DiningTableParam param) {
        Map<String, Object> data = diningTableService.openTable(shopId(), param.getId(), param.getMemberNum());
        //A-08 操作日志：开台无变更前值，变更后值记 台位/人数/会话
        Map<String, Object> after = new HashMap<>();
        after.put("tableId", param.getId());
        after.put("memberNum", param.getMemberNum());
        after.put("sessionId", data.get("sessionId"));
        LogContext.setAfter(after);
        return BasicResult.success(data);
    }

    /**
     * 转台
     */
    @Log(module = Log.MODULE_TABLE, moduleName = "开台", action = "转台")
    @PostMapping(value = "/change")
    public BasicResult change(@RequestBody DiningTableParam param) {
        //A-08 操作日志：变更前值记源台，变更后值记目标台
        Map<String, Object> before = new HashMap<>();
        before.put("tableId", param.getId());
        LogContext.setBefore(before);

        diningTableService.changeTable(shopId(), param.getId(), param.getTargetTableId());

        Map<String, Object> after = new HashMap<>();
        after.put("tableId", param.getId());
        after.put("targetTableId", param.getTargetTableId());
        LogContext.setAfter(after);
        return BasicResult.success();
    }

    /**
     * 结账（批次 C 2026-09-19 新增）
     * 只闭台、不放台 → 台位进入「待清台」，由 /clear 真正释放。
     * 对齐 W-04 画板底注「收款后：订单→已完成 · 台位→待清台」。
     */
    @PostMapping(value = "/settle")
    public BasicResult settle(@RequestBody DiningTableParam param) {
        diningTableService.settleTable(shopId(), param.getId());
        return BasicResult.success();
    }

    /**
     * 清台
     * 兼容「待清台」：会话已结台时本次仅释放台位
     */
    @Log(module = Log.MODULE_TABLE, moduleName = "开台", action = "清台")
    @PostMapping(value = "/clear")
    public BasicResult clear(@RequestBody DiningTableParam param) {
        //A-08 操作日志：变更前值记被释放的台位
        Map<String, Object> before = new HashMap<>();
        before.put("tableId", param.getId());
        LogContext.setBefore(before);

        diningTableService.clearTable(shopId(), param.getId());
        return BasicResult.success();
    }
}
