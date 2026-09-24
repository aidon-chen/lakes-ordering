package com.siam.system.modular.package_dining.service_impl;

import com.siam.package_common.exception.StoneCustomerException;
import com.siam.package_common.util.StringUtils;
import com.siam.package_weixin_pay.config.WxPayConfig;
import com.siam.package_weixin_pay.util.PayUtil;
import com.siam.system.modular.package_dining.service.WxScanPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 付款码支付（被扫）实现 —— 复用 weixin-pay 的 PayUtil 签名/请求/解析五件套，
 * 不重造签名轮子（{@code paraFilter} → {@code createLinkString} → {@code sign} →
 * {@code httpRequest} → {@code doXMLParse}）。
 *
 * <p>详见接口 {@link WxScanPayService} 的类注释（为何新开通道、为何不传 notify_url）。
 */
@Slf4j
@Service
public class WxScanPayServiceImpl implements WxScanPayService {

    /** 请求字段顺序固定：微信对 XML 报文的字段顺序敏感（历史踩坑见 WxPayController 类注释） */
    private static final String[] MICROPAY_FIELDS = {
            "appid", "mch_id", "nonce_str", "body", "out_trade_no",
            "total_fee", "spbill_create_ip", "auth_code"
    };

    private static final String[] ORDER_QUERY_FIELDS = {
            "appid", "mch_id", "out_trade_no", "nonce_str"
    };

    @Autowired
    private WxPayConfig wxPayConfig;

    @Override
    public boolean isConfigured() {
        return isRealValue(wxPayConfig.getMchId()) && isRealValue(wxPayConfig.getMchKey())
                && isRealValue(wxPayConfig.getAppId());
    }

    /** 真值判定：非空、且不是仓库里遗留的邮箱占位（application-local.yml 的 siam1026@163.com） */
    private boolean isRealValue(String v) {
        return v != null && !v.trim().isEmpty() && !v.contains("@");
    }

    @Override
    public Map<String, String> micropay(String outTradeNo, BigDecimal amountYuan, String body,
                                        String authCode, String spbillCreateIp) {
        assertConfigured();
        // 微信金额单位为「分」，必须整数；元→分四舍五入
        String totalFee = String.valueOf(
                amountYuan.multiply(new BigDecimal(100)).setScale(0, BigDecimal.ROUND_HALF_UP).longValue());

        Map<String, String> params = new LinkedHashMap<>();
        params.put("appid", wxPayConfig.getAppId());
        params.put("mch_id", wxPayConfig.getMchId());
        params.put("nonce_str", StringUtils.getRandomStringByLength(32));
        params.put("body", body);
        params.put("out_trade_no", outTradeNo);
        params.put("total_fee", totalFee);
        params.put("spbill_create_ip", spbillCreateIp);
        params.put("auth_code", authCode);
        // ⚠️ 刻意不传 notify_url —— 见接口类注释（收银台订单 memberId 被复用为商家 id）

        Map<String, String> signed = PayUtil.paraFilter(params);
        String sign = PayUtil.sign(PayUtil.createLinkString(signed), wxPayConfig.getMchKey(), "utf-8").toUpperCase();

        String xml = buildXml(signed, sign, MICROPAY_FIELDS);
        log.info("[付款码支付] 请求 out_trade_no={} total_fee={} auth_code={}...",
                outTradeNo, totalFee, maskAuthCode(authCode));

        Map<String, String> result = post(xml, wxPayConfig.getMicropayUrl(), "付款码支付");
        log.info("[付款码支付] 返回 out_trade_no={} return_code={} result_code={} err_code={} err_code_des={}",
                outTradeNo, result.get("return_code"), result.get("result_code"),
                result.get("err_code"), result.get("err_code_des"));
        return result;
    }

    @Override
    public Map<String, String> orderQuery(String outTradeNo) {
        assertConfigured();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("appid", wxPayConfig.getAppId());
        params.put("mch_id", wxPayConfig.getMchId());
        params.put("out_trade_no", outTradeNo);
        params.put("nonce_str", StringUtils.getRandomStringByLength(32));

        Map<String, String> signed = PayUtil.paraFilter(params);
        String sign = PayUtil.sign(PayUtil.createLinkString(signed), wxPayConfig.getMchKey(), "utf-8").toUpperCase();

        String xml = buildXml(signed, sign, ORDER_QUERY_FIELDS);
        Map<String, String> result = post(xml, wxPayConfig.getOrderQueryUrl(), "查单");
        log.info("[查单] out_trade_no={} return_code={} result_code={} trade_state={}",
                outTradeNo, result.get("return_code"), result.get("result_code"), result.get("trade_state"));
        return result;
    }

    // ==================== 内部工具 ====================

    /**
     * 按固定字段顺序拼 XML（微信对报文顺序敏感；body 需 CDATA 包裹）
     * 只拼入参里真实存在的字段，避免出现空标签。
     */
    private String buildXml(Map<String, String> params, String sign, String[] fieldOrder) {
        StringBuilder sb = new StringBuilder("<xml>");
        for (String field : fieldOrder) {
            String value = params.get(field);
            if (value == null || value.isEmpty()) {
                continue;
            }
            if ("body".equals(field)) {
                sb.append("<body><![CDATA[").append(value).append("]]></body>");
            } else {
                sb.append("<").append(field).append(">").append(value).append("</").append(field).append(">");
            }
        }
        sb.append("<sign>").append(sign).append("</sign>");
        sb.append("</xml>");
        return sb.toString();
    }

    /** 发请求并解析返回 XML；通信层异常统一转业务异常（别让它冒成「系统异常」） */
    @SuppressWarnings("unchecked")
    private Map<String, String> post(String xml, String url, String scene) {
        if (url == null || url.trim().isEmpty()) {
            throw new StoneCustomerException(scene + "接口地址未配置（wxpay.micropayUrl / orderQueryUrl）");
        }
        String raw;
        try {
            raw = PayUtil.httpRequest(url, "POST", xml);
        } catch (Exception e) {
            log.error("[{}] 请求微信失败", scene, e);
            throw new StoneCustomerException(scene + "请求微信失败，请检查网络后重试");
        }
        if (raw == null || raw.trim().isEmpty()) {
            throw new StoneCustomerException(scene + "微信未返回内容，请稍后重试");
        }
        try {
            Map<String, String> map = new HashMap<>();
            Map parsed = PayUtil.doXMLParse(raw);
            for (Object k : parsed.keySet()) {
                Object v = parsed.get(k);
                map.put(String.valueOf(k), v == null ? null : String.valueOf(v));
            }
            return map;
        } catch (Exception e) {
            log.error("[{}] 解析微信返回失败，原文={}", scene, raw, e);
            throw new StoneCustomerException(scene + "返回内容无法解析，请联系管理员");
        }
    }

    private void assertConfigured() {
        if (!isConfigured()) {
            throw new StoneCustomerException(
                    "尚未配置微信支付商户号，无法发起付款码支付（需 WXPAY_APPID / WXPAY_MCHID / WXPAY_MCHKEY）");
        }
    }

    /** 付款码脱敏：只留前 4 后 2，避免完整码进日志 */
    private String maskAuthCode(String authCode) {
        if (authCode == null || authCode.length() <= 6) {
            return "***";
        }
        return authCode.substring(0, 4) + "******" + authCode.substring(authCode.length() - 2);
    }
}
