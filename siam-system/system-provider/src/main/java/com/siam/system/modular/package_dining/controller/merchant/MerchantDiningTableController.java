package com.siam.system.modular.package_dining.controller.merchant;

import com.siam.package_common.entity.BasicResult;
import com.siam.package_weixin_pay.util.IpUtils;
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

import javax.servlet.http.HttpServletRequest;
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
    @Log(module = Log.MODULE_TABLE, moduleName = "开台", action = "开台", businessType = Log.TYPE_INSERT)
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
    @Log(module = Log.MODULE_TABLE, moduleName = "开台", action = "转台", businessType = Log.TYPE_UPDATE)
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
     * 确认收款（2026-09-24 新增）
     *
     * <p>口径＝收银台「点单即收款」（先付后吃，用户 2026-09-24 拍板）：
     * 点菜与收款一次完成 → 订单置已付 + status 1→2（待处理=下厨）；
     * **台位保持不变**（顾客仍在用餐），离店由 /clear 一步完成「结台 + 放台」。
     *
     * <p>入参：堂食传 {@code id}(台位id)，外带传 {@code orderNo}；{@code payMode}
     * 1=微信收款码、4=到店支付(现金)。
     *
     * <p>⚠️ 本接口只写法定的支付三字段，**不调 orderService.paymentNotify** ——
     * 收银台订单的 memberId 被复用为商家 id（OrderServiceImpl.insertByMerchant），
     * 走支付回调会给商家发积分 / 查不到会员报错。批次 H 真接入微信收款后，
     * 本接口替换为「拉起收款码 → 回调置 is_payment」，届时方可复用 paymentNotify。
     */
    @Log(module = Log.MODULE_TABLE, moduleName = "开台", action = "确认收款", businessType = Log.TYPE_UPDATE)
    @PostMapping(value = "/confirmPayment")
    public BasicResult confirmPayment(@RequestBody DiningTableParam param) {
        Map<String, Object> data = diningTableService.confirmPayment(
                shopId(), param.getId(), param.getOrderNo(), param.getPayMode());
        //A-08 操作日志：变更后值记 台位/订单号/收款方式/收款笔数/收款金额
        Map<String, Object> after = new HashMap<>();
        after.put("tableId", param.getId());
        after.put("orderNo", param.getOrderNo());
        after.put("payMode", param.getPayMode());
        after.put("paidCount", data.get("paidCount"));
        after.put("paidAmount", data.get("paidAmount"));
        LogContext.setAfter(after);
        return BasicResult.success(data);
    }

    /**
     * 扫码收款（付款码支付 · 被扫，2026-09-24 新增）
     *
     * <p>收银台用扫码枪/摄像头扫顾客手机上的微信付款码 → 后端调微信 /pay/micropay
     * **真扣款** → 成功即落库（is_payment=1 + payment_mode=1 + status 1→2）。
     *
     * <p>与 {@link #confirmPayment} 的分工正好对应画板结算栏的两档：
     * 微信收款码 → 本接口（真扣款）；现金 → confirmPayment（人工确认记账）。
     *
     * <p>结果三态：{@code status=SUCCESS} 已扣款；{@code status=USERPAYING} 顾客仍在输密码
     * （未改库，收银员稍后重扫，微信对同单号同参数幂等）；失败则抛业务异常并透传微信文案。
     *
     * <p>⚠️ 前置：需配置**真实**商户号（WXPAY_APPID / WXPAY_MCHID / WXPAY_MCHKEY）且该商户号
     * 已开通「付款码支付」产品权限；未配置时返回明确业务提示而非系统异常。
     */
    @Log(module = Log.MODULE_TABLE, moduleName = "开台", action = "扫码收款", businessType = Log.TYPE_UPDATE)
    @PostMapping(value = "/scanPay")
    public BasicResult scanPay(@RequestBody DiningTableParam param, HttpServletRequest request) {
        // 微信付款码支付必填 spbill_create_ip
        String clientIp = IpUtils.getIpAddr(request);
        Map<String, Object> data = diningTableService.scanPay(
                shopId(), param.getId(), param.getOrderNo(), param.getAuthCode(), clientIp);
        //A-08 操作日志：**付款码不落日志**（脱敏红线），只记台位/订单号/结果/金额/微信流水号
        Map<String, Object> after = new HashMap<>();
        after.put("tableId", param.getId());
        after.put("orderNo", param.getOrderNo());
        after.put("status", data.get("status"));
        after.put("paidAmount", data.get("paidAmount"));
        after.put("transactionId", data.get("transactionId"));
        LogContext.setAfter(after);
        return BasicResult.success(data);
    }

    /**
     * 清台
     * 兼容「待清台」：会话已结台时本次仅释放台位
     */
    @Log(module = Log.MODULE_TABLE, moduleName = "开台", action = "清台", businessType = Log.TYPE_DELETE)
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
