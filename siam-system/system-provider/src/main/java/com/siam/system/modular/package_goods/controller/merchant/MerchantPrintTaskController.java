package com.siam.system.modular.package_goods.controller.merchant;

import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_goods.service.internal.PrintService;
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
 * 打印任务（队列 / 告警 / 补打）· 批次 G · 收银端 W-25 + W-21 的补打记录
 *
 * <p>门店维度一律取登录会话的 shopId。
 *
 * <p>⚠️ 「立即重试」只把任务置回待打印并写 nextRetryTime，
 * **真正的下发**由打印执行器消费 —— 该执行器依赖云打印平台账号（P-01），目前未就绪。
 * 因此重试后任务会停在「待打印」，这是当前预期行为，不是 bug。
 */
@RestController
@RequestMapping(value = "/rest/merchant/printTask")
@Transactional(rollbackFor = Exception.class)
@Api(tags = "打印任务接口", description = "MerchantPrintTaskController")
public class MerchantPrintTaskController {

    @Autowired
    private PrintService printService;

    @Autowired
    private MerchantSessionManager merchantSessionManager;

    private Merchant loginMerchant() {
        return merchantSessionManager.getSession(TokenUtil.getToken());
    }

    /**
     * 打印任务列表。
     * 请求体可选：status、orderNo、onlyPendingOrFail（W-25 待重试队列用）、isReprint（W-21 补打记录用）、pageNo、pageSize
     */
    @ApiOperation(value = "打印任务列表")
    @PostMapping(value = "/list")
    public BasicResult list(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> q = body == null ? new java.util.HashMap<>() : body;
        Integer status = intOrNull(q.get("status"));
        Boolean onlyPendingOrFail = boolOrNull(q.get("onlyPendingOrFail"));
        Boolean isReprint = boolOrNull(q.get("isReprint"));
        String orderNo = q.get("orderNo") == null ? null : String.valueOf(q.get("orderNo"));
        int pageNo = intOrDefault(q.get("pageNo"), 1);
        int pageSize = intOrDefault(q.get("pageSize"), 20);
        return BasicResult.success(printService.listTasks(
                loginMerchant().getShopId(), status, orderNo, onlyPendingOrFail, isReprint, pageNo, pageSize));
    }

    /** 队列指标：待重试 / 今日失败 / 今日成功（W-25 指标行） */
    @ApiOperation(value = "打印队列指标")
    @PostMapping(value = "/summary")
    public BasicResult summary() {
        return BasicResult.success(printService.taskSummary(loginMerchant().getShopId()));
    }

    /** 立即重试 */
    @ApiOperation(value = "立即重试打印任务")
    @PostMapping(value = "/retry")
    public BasicResult retry(@RequestBody Map<String, Object> body) {
        Integer id = intOrNull(body.get("id"));
        if (id == null) {
            return BasicResult.error("任务id不能为空");
        }
        printService.retryTask(loginMerchant().getShopId(), id);
        return BasicResult.success();
    }

    /** 忽略任务（人工放弃） */
    @ApiOperation(value = "忽略打印任务")
    @PostMapping(value = "/ignore")
    public BasicResult ignore(@RequestBody Map<String, Object> body) {
        Integer id = intOrNull(body.get("id"));
        if (id == null) {
            return BasicResult.error("任务id不能为空");
        }
        printService.ignoreTask(loginMerchant().getShopId(), id);
        return BasicResult.success();
    }

    /** 补打：以原任务为模板新建一条 is_reprint=true 的待打印任务 */
    @ApiOperation(value = "补打")
    @PostMapping(value = "/reprint")
    public BasicResult reprint(@RequestBody Map<String, Object> body) {
        Integer id = intOrNull(body.get("id"));
        if (id == null) {
            return BasicResult.error("原任务id不能为空");
        }
        return BasicResult.success(printService.createReprint(
                loginMerchant().getShopId(), loginMerchant().getId(), id));
    }

    private static Integer intOrNull(Object v) {
        if (v == null || String.valueOf(v).isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean boolOrNull(Object v) {
        if (v == null || String.valueOf(v).isEmpty()) {
            return null;
        }
        return Boolean.valueOf(String.valueOf(v));
    }

    private static int intOrDefault(Object v, int dft) {
        Integer i = intOrNull(v);
        return i == null ? dft : i;
    }
}
