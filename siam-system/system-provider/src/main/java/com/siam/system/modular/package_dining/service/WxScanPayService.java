package com.siam.system.modular.package_dining.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 付款码支付（被扫，B 扫 C）—— 收银台用扫码枪/摄像头扫顾客微信付款码
 *
 * <p>走微信 {@code /pay/micropay}（**同步返回**支付结果），USERPAYING 时用
 * {@code /pay/orderquery} 兜底轮询。收银台是网页，调不了小程序专属的
 * {@code wx.requestPayment}，因此不能复用 {@code WxPayController.toPay4Applet}
 * （那是 trade_type=JSAPI 的统一下单），必须新开本通道。
 *
 * <p>⚠️ <b>本通道不传 notify_url</b>：收银台订单的 memberId 被复用为商家自己的 id
 * （见 {@code OrderServiceImpl.insertByMerchant} 的 {@code setMemberId(loginMerchant.getId())}），
 * 一旦进 {@code WxPayController.wxNotify} → {@code orderService.paymentNotify}
 * 就会给商家自己发积分、甚至查不到会员报错。故付款码支付只认「同步返回 + 主动查单」，
 * 支付结果由 {@code DiningTableService.scanPay} 直接落库。
 *
 * <p>⚠️ 前置：{@code wxpay.mchId} / {@code wxpay.mchKey} 必须为**真实**微信支付商户号与
 * APIv2 密钥（且该商户号已开通「付款码支付」产品权限）。未配置时
 * {@link #isConfigured()} 为 false，上层应给出明确业务提示而不是抛系统异常。
 * 仓库内三份配置的现状：application-mvp.yml 取环境变量（默认空）、
 * application-local.yml 是 {@code siam1026@163.com} 邮箱占位（原作者遗留，非真实商户号）、
 * application.yml 无 wxpay 段。
 */
public interface WxScanPayService {

    /**
     * 付款码支付（被扫）
     *
     * @param outTradeNo     商户订单号（复用 tb_order.order_no；微信要求本商户号内唯一）
     * @param amountYuan     金额（元），内部按 ¥1=100 分转成整数分
     * @param body           商品描述（显示在顾客微信账单上）
     * @param authCode       顾客付款码（18 位、以 10~15 开头；格式合法性与有效性由微信校验）
     * @param spbillCreateIp 发起支付的机器 IP
     * @return 微信返回的字段表：{@code return_code} / {@code return_msg} /
     *         {@code result_code} / {@code err_code} / {@code err_code_des} / {@code transaction_id}
     */
    Map<String, String> micropay(String outTradeNo, BigDecimal amountYuan, String body,
                                 String authCode, String spbillCreateIp);

    /**
     * 查单（micropay 返回 USERPAYING 时兜底）
     *
     * @return 微信返回的字段表：{@code trade_state} 取
     *         SUCCESS / REFUND / NOTPAY / CLOSED / REVOKED / USERPAYING / PAYERROR
     */
    Map<String, String> orderQuery(String outTradeNo);

    /** 商户号与密钥是否已配置（真实值，非邮箱占位） */
    boolean isConfigured();
}
