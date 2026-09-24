package com.siam.system.modular.package_user.model.param;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 收银端「会员现金充值」入参（W-27）
 *
 * <p>场景：顾客到店付现金办卡/储值，店员在收银端录入。
 * 与小程序充值的区别：<b>自由填金额、不走微信支付、无面额档位</b>，
 * 因此 {@code denomination_id} 落库为 null，赠送额由店员手工决定。
 *
 * @author siam
 */
@Data
public class MemberCashRechargeParam {

    /** 会员id（由 /selectByMobile 按手机号查到后带入） */
    private Integer memberId;

    /** 本次实收现金金额（元），须 > 0 */
    private BigDecimal amount;

    /** 赠送余额（元）。可为空/0 = 不赠送；由店员勾选后填写 */
    private BigDecimal giveBalance;

    /** 备注（可选，人工留痕用） */
    private String remark;
}
