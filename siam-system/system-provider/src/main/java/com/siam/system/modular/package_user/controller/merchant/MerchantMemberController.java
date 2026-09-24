package com.siam.system.modular.package_user.controller.merchant;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.siam.package_common.constant.Quantity;
import com.siam.package_common.entity.BasicResult;
import com.siam.package_common.exception.StoneCustomerException;
import com.siam.system.modular.package_goods.entity.internal.VipRechargeRecord;
import com.siam.system.modular.package_goods.service.internal.VipRechargeRecordService;
import com.siam.system.modular.package_user.auth.cache.MerchantSessionManager;
import com.siam.system.modular.package_user.entity.Member;
import com.siam.system.modular.package_user.entity.MemberBillingRecord;
import com.siam.system.modular.package_user.entity.MemberTradeRecord;
import com.siam.system.modular.package_user.entity.Merchant;
import com.siam.system.modular.package_user.mapper.MemberMapper;
import com.siam.system.modular.package_user.model.param.MemberCashRechargeParam;
import com.siam.system.modular.package_user.model.param.MemberParam;
import com.siam.system.modular.package_user.service.MemberBillingRecordService;
import com.siam.system.modular.package_user.service.MemberService;
import com.siam.system.modular.package_user.service.MemberTradeRecordService;
import com.siam.system.util.TokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 会员查询与现金充值（商家侧）
 *
 * <p>用途一：收银端 **W-17 存酒登记** 的「手机号 → 查询」按钮 —— 先确认该手机号对应会员存在、
 * 并把姓名与**脱敏**手机号带出到表单与凭证预览，避免填完整张表才在提交时报「未找到该会员」。
 *
 * <p>用途二：收银端 **W-27 会员充值** —— 顾客到店付现金办卡/储值，店员当场录入。
 * 与小程序充值的区别：自由填金额、不走微信支付、无面额档位，因此
 * {@code denomination_id} 留空，赠送额由店员手工决定并快照进充值单。
 *
 * <p>鉴权：/rest/merchant/** 由 merchantInterceptor 统一保护（本类不再重复校验）。
 * 会员是全局对象（不按门店隔离），故不引入 shopId 过滤。
 *
 * @author siam
 */
@Slf4j
@RestController
@RequestMapping(value = "/rest/merchant/member")
@Transactional(rollbackFor = Exception.class)
public class MerchantMemberController {

    /**
     * 单笔现金充值上限（元）
     *
     * <p>依据《单用途商业预付卡管理办法（试行）》对记名卡面值不超过 5000 元的限额口径，
     * 先在此做单笔硬拦截；后续若产品定档更严，改这一个常量即可。
     */
    private static final BigDecimal MAX_SINGLE_AMOUNT = new BigDecimal("5000");

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberTradeRecordService memberTradeRecordService;

    @Autowired
    private MemberBillingRecordService memberBillingRecordService;

    @Autowired
    private VipRechargeRecordService vipRechargeRecordService;

    @Autowired
    private MerchantSessionManager merchantSessionManager;

    /**
     * 按手机号查会员（精确）
     *
     * @return { id, nickname, name, mobile(已脱敏), balance, growth, type, vipNo }
     */
    @PostMapping(value = "/selectByMobile")
    public BasicResult selectByMobile(@RequestBody MemberParam param) {
        String mobile = param.getMobile() == null ? null : param.getMobile().trim();
        if (mobile == null || mobile.isEmpty()) {
            throw new StoneCustomerException("手机号不能为空");
        }
        Member m = memberMapper.selectOne(
                new QueryWrapper<Member>().eq("mobile", mobile).last("LIMIT 1"));
        if (m == null) {
            throw new StoneCustomerException("未找到该手机号对应的会员");
        }
        Map<String, Object> data = new HashMap<>(8);
        data.put("id", m.getId());
        data.put("nickname", m.getNickname());
        data.put("name", m.getRealName());
        // 只回脱敏号：收银端用于当面核对，不需要完整号码
        data.put("mobile", maskMobile(mobile));
        // 充值场景店员需要知道「当前余额多少」，否则无法向顾客交代充值前后的变化
        data.put("balance", nvl(m.getBalance()));
        data.put("growth", m.getGrowth() == null ? 0 : m.getGrowth());
        data.put("type", m.getType());
        data.put("vipNo", m.getVipNo());
        return BasicResult.success(data);
    }

    /**
     * 会员现金充值（收银端 W-27）
     *
     * <p>一次调用完成五件事：
     * <ol>
     *   <li>写 {@code tb_member_trade_record}（{@code merchant_id} 记经手人、{@code payment_mode=5 现金}）</li>
     *   <li>写 {@code tb_vip_recharge_record}（{@code channel=2 商家端}、{@code status=2} 直接成功）</li>
     *   <li>会员 {@code balance}/{@code totalBalance} 加钱；普通用户顺带升为 VIP（与小程序充值口径一致）</li>
     *   <li>写余额流水：本金一条；有赠送再加一条</li>
     * </ol>
     *
     * <p>不做幂等保护：现金由人当面收讫、非回调场景；重复提交由前端按钮置灰兜底。
     */
    @PostMapping(value = "/recharge")
    public BasicResult recharge(@RequestBody MemberCashRechargeParam param) {
        Merchant loginMerchant = merchantSessionManager.getSession(TokenUtil.getToken());

        if (param.getMemberId() == null) {
            throw new StoneCustomerException("请先选择会员");
        }
        if (param.getAmount() == null || param.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new StoneCustomerException("充值金额必须大于 0");
        }
        if (param.getAmount().compareTo(MAX_SINGLE_AMOUNT) > 0) {
            throw new StoneCustomerException("单笔充值金额不能超过 " + MAX_SINGLE_AMOUNT.toPlainString() + " 元");
        }
        Member dbMember = memberService.selectByPrimaryKey(param.getMemberId());
        if (dbMember == null) {
            throw new StoneCustomerException("该会员不存在");
        }

        BigDecimal amount = param.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal giveBalance = param.getGiveBalance() == null
                ? BigDecimal.ZERO
                : param.getGiveBalance().setScale(2, BigDecimal.ROUND_HALF_UP);
        if (giveBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new StoneCustomerException("赠送余额不能为负数");
        }
        boolean hasGive = giveBalance.compareTo(BigDecimal.ZERO) > 0;

        String orderNo = genCashOrderNo();
        String operator = loginMerchant == null ? "未知" : String.valueOf(loginMerchant.getId());
        Date now = new Date();

        // 1) 交易单：merchant_id = 经手人，这是现金对账的唯一依据
        MemberTradeRecord trade = new MemberTradeRecord();
        trade.setMemberId(dbMember.getId());
        trade.setMerchantId(loginMerchant == null ? null : loginMerchant.getId());
        trade.setOutTradeNo(orderNo);
        trade.setType(Quantity.INT_2);
        trade.setPaymentMode(MemberTradeRecord.PAYMENT_MODE_OF_CASH);
        trade.setAmount(amount);
        trade.setStatus(Quantity.INT_2);
        trade.setCreateTime(now);
        trade.setUpdateTime(now);
        memberTradeRecordService.insertSelective(trade);

        // 2) 充值单：无面额档位 → denomination_id 留空；赠送额快照进 denomination_* 字段
        VipRechargeRecord record = new VipRechargeRecord();
        record.setMemberId(dbMember.getId());
        record.setOrderNo(orderNo);
        record.setChannel(Quantity.INT_2);
        record.setAmount(amount);
        record.setDenominationName("现金充值");
        record.setDenominationPrice(amount);
        record.setDenominationIsSale(false);
        record.setDenominationSalePrice(BigDecimal.ZERO);
        record.setDenominationIsGiveBalance(hasGive);
        record.setDenominationGiveBalance(giveBalance);
        record.setDenominationIsGiveCoupons(false);
        record.setTradeId(trade.getId());
        record.setStatus(Quantity.INT_2);
        record.setCreateTime(now);
        vipRechargeRecordService.insertSelective(record);

        // 3) 加余额（本金 + 赠送一次写入），并顺带升级为 VIP
        BigDecimal balanceAfter = nvl(dbMember.getBalance()).add(amount).add(giveBalance);
        Member updateMember = new Member();
        updateMember.setId(dbMember.getId());
        updateMember.setBalance(balanceAfter);
        updateMember.setTotalBalance(nvl(dbMember.getTotalBalance()).add(amount).add(giveBalance));
        if (Integer.valueOf(Quantity.INT_1).equals(dbMember.getType())) {
            updateMember.setType(Quantity.INT_2);
        }
        memberService.updateByPrimaryKeySelective(updateMember);

        // 4) 流水：本金
        insertBilling(dbMember.getId(), MemberBillingRecord.TYPE_VIP_RECHARGE_PRICE_SWITCH_TO_BALANCE,
                amount, "现金充值转入余额（经手人：" + operator + "）", now);
        // 5) 流水：赠送（仅勾选时）
        if (hasGive) {
            insertBilling(dbMember.getId(), MemberBillingRecord.TYPE_VIP_RECHARGE_REWARD_BALANCE,
                    giveBalance, "现金充值奖励余额（经手人：" + operator + "）", now);
        }

        log.info("现金充值成功 orderNo={} memberId={} amount={} give={} operator={}",
                orderNo, dbMember.getId(), amount, giveBalance, operator);

        Map<String, Object> data = new HashMap<>(4);
        data.put("orderNo", orderNo);
        data.put("amount", amount);
        data.put("giveBalance", giveBalance);
        data.put("balanceAfter", balanceAfter);
        return BasicResult.success(data);
    }

    /** 写一条余额流水 */
    private void insertBilling(Integer memberId, Integer type, BigDecimal number, String message, Date now) {
        MemberBillingRecord billing = new MemberBillingRecord();
        billing.setMemberId(memberId);
        billing.setType(type);
        billing.setOperateType(MemberBillingRecord.OPERATE_TYPE_ADD);
        billing.setCoinType(MemberBillingRecord.COIN_TYPE_BALANCE);
        billing.setNumber(number);
        billing.setMessage(message);
        billing.setCreateTime(now);
        memberBillingRecordService.insertSelective(billing);
    }

    /**
     * 现金充值单号：XR + yyyyMMddHHmmss + 4 位随机
     * 前缀 XR 便于与小程序单号区分，运营可按前缀筛出现金单。
     */
    private String genCashOrderNo() {
        String ts = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        int rnd = 1000 + new Random().nextInt(9000);
        return "XR" + ts + rnd;
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** 手机号脱敏：138****8601；非 11 位原样返回 */
    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 11) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(7);
    }
}
