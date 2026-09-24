package com.siam.package_weixin_pay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(value = "wxpay")
public class WxPayConfig {

    private String appId;

    private String mchId;

    private String mchKey;

    private String notifyUrl;

    private String signType;

    private String tradeType;

    private String payUrl;

    /**
     * 付款码支付（被扫）接口地址
     * 收银台扫顾客微信付款码 → POST /pay/micropay，同步返回支付结果。
     * 与 payUrl(unifiedorder) 的区别：unifiedorder 是「主扫/JSAPI」下单预支付，
     * micropay 是「被扫」——商户侧持扫码枪扫 C 端付款码，一步完成扣款。
     */
    private String micropayUrl;

    /**
     * 查单接口地址
     * micropay 返回 result_code=FAIL & err_code=USERPAYING（顾客还在输密码）时兜底轮询。
     */
    private String orderQueryUrl;

    private String refundUrl;

    private String refundSuccessNotifyUrl;

    private String payToBalanceUrl;
}