package com.siam.system.modular.package_order.controller.member.internal;

import com.siam.package_common.constant.BasicResultCode;
import com.siam.package_common.constant.Quantity;
import com.siam.package_common.entity.BasicData;
import com.siam.package_common.entity.BasicResult;
import com.siam.package_common.exception.StoneCustomerException;
import com.siam.package_common.util.GenerateNo;
import com.siam.system.modular.package_goods.entity.Shop;
import com.siam.system.modular.package_goods.entity.internal.VipRechargeDenomination;
import com.siam.system.modular.package_goods.entity.internal.VipRechargeRecord;
import com.siam.system.modular.package_goods.service.ShopService;
import com.siam.system.modular.package_goods.service.internal.VipRechargeDenominationService;
import com.siam.system.modular.package_goods.service.internal.VipRechargeRecordService;
import com.siam.system.modular.package_order.entity.DeliveryAddress;
import com.siam.system.modular.package_order.entity.Order;
import com.siam.system.modular.package_order.entity.internal.PointsMallOrder;
import com.siam.system.modular.package_order.service.CommonService;
import com.siam.system.modular.package_order.service.DeliveryAddressService;
import com.siam.system.modular.package_order.service.OrderService;
import com.siam.system.modular.package_order.service.internal.PointsMallOrderService;
import com.siam.system.modular.package_user.auth.cache.MemberSessionManager;
import com.siam.system.modular.package_user.entity.Member;
import com.siam.system.modular.package_user.entity.MemberTradeRecord;
import com.siam.system.modular.package_user.service.MemberTradeRecordService;
import com.siam.system.util.TokenUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * 模拟支付（★ 仅 mvp profile 装配，生产/其他 profile 下本接口不存在=404）
 *
 * 与微信支付回调（WxPayController.wxNotify）走【同一套】订单状态流转分支
 * （orderService.paymentNotify），保证「支付成功→订单流转→商家端接单」演示链路
 * 被真实代码路径验证。见 04-deploy-cost/mvp-migration/04
 *
 * 支持 4 类交易（与 WxPayController.toPay4Applet 的 type 对齐）：
 *   1=订单付款           → orderService.paymentNotify
 *   2=会员充值           → vipRechargeRecordService.updateByPayNotify
 *   3=自取订单改为配送    → orderService.paymentNotifyOfChangeToDelivery
 *   4=积分商城订单付款    → pointsMallOrderService.paymentNotify
 * 其中「建档」段（交易记录/充值记录/订单回填）复刻 toPay4Applet，「分发」段复刻 wxNotify，
 * 使模拟支付与真实支付共享同一批业务方法。
 *
 * 安全红线：严禁将 @Profile("mvp") 移除或放宽，否则等于免支付下单漏洞。
 */
@Slf4j
@RestController
@RequestMapping(value = "/rest/member/pay")
@Profile("mvp")
@Transactional(rollbackFor = Exception.class)
@Api(tags = "模拟支付模块相关接口（仅MVP）", description = "MockPayController")
public class MockPayController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private MemberTradeRecordService memberTradeRecordService;

    @Autowired
    private MemberSessionManager memberSessionManager;

    /** 会员充值（type=2）所需 */
    @Autowired
    private VipRechargeDenominationService vipRechargeDenominationService;

    @Autowired
    private VipRechargeRecordService vipRechargeRecordService;

    /** 自取订单改为配送（type=3）所需 */
    @Autowired
    private DeliveryAddressService deliveryAddressService;

    @Autowired
    private CommonService commonService;

    @Autowired
    private ShopService shopService;

    /** 积分商城订单付款（type=4）所需 */
    @Resource(name = "pointsMallOrderServiceImpl")
    private PointsMallOrderService pointsMallOrderService;

    /**
     * 模拟支付成功
     *
     * @param params type=交易类型(默认1)｜outTradeNo=商户单号(type=1/3/4)
     *               ｜vipRechargeDenominationId=充值面额id(type=2)｜deliveryAddressId=收货地址id(type=3)
     */
    @ApiOperation(value = "模拟支付成功（仅MVP演示环境）")
    @PostMapping(value = "/mockPay")
    public BasicResult mockPay(@RequestBody Map<String, Object> params) throws Exception {
        Member loginMember = memberSessionManager.getSession(TokenUtil.getToken());
        if (loginMember == null) {
            throw new StoneCustomerException("请先登录");
        }

        //交易类型：与 WxPayDto.type 对齐，缺省按「订单付款」处理（兼容旧调用方）
        int type = Quantity.INT_1;
        if (params.get("type") != null) {
            type = Integer.parseInt(params.get("type").toString());
        }

        switch (type) {
            case Quantity.INT_1:
                return mockOrderPay(loginMember, params);
            case Quantity.INT_2:
                return mockVipRecharge(loginMember, params);
            case Quantity.INT_3:
                return mockChangeToDelivery(loginMember, params);
            case Quantity.INT_4:
                return mockPointsMallPay(loginMember, params);
            default:
                throw new StoneCustomerException("交易类型错误");
        }
    }

    /**
     * type=1 订单付款：复刻 wxNotify 中「type=1 → orderService.paymentNotify」
     * 未调起真实支付流程（无交易记录）时补建一条已支付交易记录；已有待支付记录则翻转状态
     */
    private BasicResult mockOrderPay(Member loginMember, Map<String, Object> params) throws Exception {
        String orderNo = getRequiredString(params, "outTradeNo", "必须填写商户单号");

        Order dbOrder = orderService.selectByOrderNo(orderNo);
        if (dbOrder == null) {
            throw new StoneCustomerException("该商户单号不存在");
        }
        if (!loginMember.getId().equals(dbOrder.getMemberId())) {
            throw new StoneCustomerException("无权操作该订单");
        }
        //仅待支付订单可模拟支付（防重复流转：paymentNotify 内部无幂等检查，重复调用会重复记积分/冻结资金）
        if (dbOrder.getStatus() == Quantity.INT_10) {
            throw new StoneCustomerException("该笔订单超时未支付，已自动取消，请重新下单");
        }
        if (dbOrder.getStatus() != Quantity.INT_1) {
            return repeatResult();
        }

        MemberTradeRecord dbMemberTradeRecord = memberTradeRecordService.selectByOutTradeNo(orderNo);
        if (dbMemberTradeRecord != null && dbMemberTradeRecord.getStatus() != Quantity.INT_1) {
            return repeatResult();
        }

        String transactionId = "MOCK" + System.currentTimeMillis();
        if (dbMemberTradeRecord == null) {
            //未调起真实支付流程（无交易记录）→ 补建一条已支付交易记录
            MemberTradeRecord insertRecord = new MemberTradeRecord();
            insertRecord.setMemberId(loginMember.getId());
            insertRecord.setOutTradeNo(orderNo);
            insertRecord.setType(Quantity.INT_1);
            insertRecord.setPaymentMode(Quantity.INT_1);
            insertRecord.setAmount(dbOrder.getActualPrice());
            insertRecord.setTradeNo(transactionId);
            insertRecord.setStatus(Quantity.INT_2);
            insertRecord.setCreateTime(new Date());
            insertRecord.setUpdateTime(new Date());
            memberTradeRecordService.insertSelective(insertRecord);
        } else {
            //已有待支付交易记录 → 与回调逻辑一致：补填平台单号并翻转为已支付
            MemberTradeRecord updateRecord = new MemberTradeRecord();
            updateRecord.setId(dbMemberTradeRecord.getId());
            updateRecord.setTradeNo(transactionId);
            updateRecord.setStatus(Quantity.INT_2);
            updateRecord.setUpdateTime(new Date());
            memberTradeRecordService.updateByPrimaryKeySelective(updateRecord);
        }

        //订单回填：补填用户交易id与支付方式（与 toPay4Applet 的建档口径一致；
        //不补则订单 payment_mode 为 NULL → 商家端订单列表/详情支付方式显示「—」，
        //与真实支付成功的订单不一致，模拟就失真了）
        MemberTradeRecord currentTradeRecord = memberTradeRecordService.selectByOutTradeNo(orderNo);
        Order updateOrder = new Order();
        updateOrder.setId(dbOrder.getId());
        updateOrder.setTradeId(currentTradeRecord.getId());
        updateOrder.setPaymentMode(Quantity.INT_1);
        updateOrder.setUpdateTime(new Date());
        orderService.updateById(updateOrder);

        //★ 核心复用点：与微信支付回调完全相同的订单状态流转（置已支付/积分/销量/商家冻结资金/通知）
        orderService.paymentNotify(orderNo);

        log.info("\n[MockPay] 订单模拟支付成功，orderNo={}，memberId={}", orderNo, loginMember.getId());
        return successResult(null);
    }

    /**
     * type=2 会员充值：复刻 toPay4Applet 的「交易记录 + 充值记录」建档，
     * 再调与回调完全相同的 vipRechargeRecordService.updateByPayNotify 完成入账
     * 注：充值单号由后端生成（前端无单号），每次调用即产生一笔新的充值
     */
    private BasicResult mockVipRecharge(Member loginMember, Map<String, Object> params) {
        Object denominationIdObj = params.get("vipRechargeDenominationId");
        if (denominationIdObj == null) {
            throw new StoneCustomerException("必须填写会员充值面额id");
        }
        Integer denominationId = Integer.valueOf(denominationIdObj.toString());

        VipRechargeDenomination dbDenomination = vipRechargeDenominationService.selectByPrimaryKey(denominationId);
        if (dbDenomination == null) {
            throw new StoneCustomerException("该会员充值面额不存在");
        }
        //实付金额口径与 WxPayController 一致：促销中取促销价，否则取原价
        BigDecimal amount = dbDenomination.getIsSale() ? dbDenomination.getSalePrice() : dbDenomination.getPrice();

        String outTradeNo = getOutTradeNo();
        String transactionId = "MOCK" + System.currentTimeMillis();

        //交易记录建档（type=2 会员充值）→ 直接置为已支付
        MemberTradeRecord insertMemberTradeRecord = new MemberTradeRecord();
        insertMemberTradeRecord.setMemberId(loginMember.getId());
        insertMemberTradeRecord.setOutTradeNo(outTradeNo);
        insertMemberTradeRecord.setType(Quantity.INT_2);
        insertMemberTradeRecord.setPaymentMode(Quantity.INT_1);
        insertMemberTradeRecord.setAmount(amount);
        insertMemberTradeRecord.setTradeNo(transactionId);
        insertMemberTradeRecord.setStatus(Quantity.INT_2);
        insertMemberTradeRecord.setCreateTime(new Date());
        insertMemberTradeRecord.setUpdateTime(new Date());
        memberTradeRecordService.insertSelective(insertMemberTradeRecord);

        //会员充值记录建档（status=1 待处理，由 updateByPayNotify 流转）
        VipRechargeRecord insertVipRechargeRecord = new VipRechargeRecord();
        insertVipRechargeRecord.setMemberId(loginMember.getId());
        insertVipRechargeRecord.setOrderNo(outTradeNo);
        insertVipRechargeRecord.setChannel(Quantity.INT_1);
        insertVipRechargeRecord.setDenominationId(denominationId);
        insertVipRechargeRecord.setAmount(amount);
        insertVipRechargeRecord.setTradeId(insertMemberTradeRecord.getId());
        insertVipRechargeRecord.setStatus(Quantity.INT_1);
        insertVipRechargeRecord.setCreateTime(new Date());
        vipRechargeRecordService.insertSelective(insertVipRechargeRecord);

        //★ 核心复用点：与微信支付回调 type=2 完全相同的充值入账流转（改会员类型/加余额/赠券）
        vipRechargeRecordService.updateByPayNotify(outTradeNo);

        log.info("\n[MockPay] 会员充值模拟支付成功，outTradeNo={}，memberId={}", outTradeNo, loginMember.getId());
        return successResult(outTradeNo);
    }

    /**
     * type=3 自取订单改为配送：复刻 toPay4Applet 的配送费核算与订单回填，
     * 再调与回调完全相同的 orderService.paymentNotifyOfChangeToDelivery
     */
    private BasicResult mockChangeToDelivery(Member loginMember, Map<String, Object> params) {
        String orderNo = getRequiredString(params, "outTradeNo", "必须填写商户单号");
        Object addressIdObj = params.get("deliveryAddressId");
        if (addressIdObj == null) {
            throw new StoneCustomerException("必须填写收获地址");
        }
        Integer deliveryAddressId = Integer.valueOf(addressIdObj.toString());

        Order dbOrder = orderService.selectByOrderNo(orderNo);
        if (dbOrder == null) {
            throw new StoneCustomerException("该商户单号不存在");
        }
        if (!loginMember.getId().equals(dbOrder.getMemberId())) {
            throw new StoneCustomerException("无权操作该订单");
        }
        if (dbOrder.getShoppingWay() != Quantity.INT_1) {
            throw new StoneCustomerException("该笔订单不属于自取订单，不允许操作");
        }
        if (dbOrder.getStatus() != Quantity.INT_2 && dbOrder.getStatus() != Quantity.INT_3) {
            throw new StoneCustomerException("该笔订单状态错误，不允许操作");
        }

        Shop dbShop = shopService.getById(dbOrder.getShopId());
        DeliveryAddress dbDeliveryAddress = deliveryAddressService.selectByPrimaryKey(deliveryAddressId);
        if (dbDeliveryAddress == null) {
            throw new StoneCustomerException("收货地址不存在");
        }

        //配送费核算（与 toPay4Applet 一致：先算距离配送费，再扣减商家立减）
        BigDecimal merchantDeliveryFee = BigDecimal.ZERO; //商家承担配送费
        BigDecimal deliveryFee = commonService.selectDeliveryFee(dbDeliveryAddress.getLongitude(), dbDeliveryAddress.getLatitude(), dbOrder.getShopId());
        if (dbShop.getReducedDeliveryPrice().compareTo(BigDecimal.ZERO) > 0) {
            if (dbShop.getReducedDeliveryPrice().compareTo(deliveryFee) >= 0) {
                merchantDeliveryFee = deliveryFee;
                deliveryFee = BigDecimal.ZERO;
            } else {
                merchantDeliveryFee = dbShop.getReducedDeliveryPrice();
                deliveryFee = deliveryFee.subtract(dbShop.getReducedDeliveryPrice());
            }
        }
        log.debug("配送费：" + deliveryFee);

        //幂等：该订单已有「自取改配送」待支付交易记录时，直接复用它（与 toPay4Applet 的复用策略一致）
        String outTradeNo = dbOrder.getChangeToDeliveryOutTradeNo() == null ? getOutTradeNo() : dbOrder.getChangeToDeliveryOutTradeNo();
        MemberTradeRecord dbMemberTradeRecord = memberTradeRecordService.selectByOutTradeNo(outTradeNo);
        if (dbMemberTradeRecord != null && dbMemberTradeRecord.getStatus() != Quantity.INT_1) {
            return repeatResult();
        }

        String transactionId = "MOCK" + System.currentTimeMillis();
        if (dbMemberTradeRecord == null) {
            MemberTradeRecord insertMemberTradeRecord = new MemberTradeRecord();
            insertMemberTradeRecord.setMemberId(loginMember.getId());
            insertMemberTradeRecord.setOutTradeNo(outTradeNo);
            insertMemberTradeRecord.setType(Quantity.INT_3);
            insertMemberTradeRecord.setPaymentMode(Quantity.INT_1);
            insertMemberTradeRecord.setAmount(deliveryFee);
            insertMemberTradeRecord.setTradeNo(transactionId);
            insertMemberTradeRecord.setStatus(Quantity.INT_2);
            insertMemberTradeRecord.setCreateTime(new Date());
            insertMemberTradeRecord.setUpdateTime(new Date());
            memberTradeRecordService.insertSelective(insertMemberTradeRecord);
        } else {
            MemberTradeRecord updateRecord = new MemberTradeRecord();
            updateRecord.setId(dbMemberTradeRecord.getId());
            updateRecord.setTradeNo(transactionId);
            updateRecord.setStatus(Quantity.INT_2);
            updateRecord.setUpdateTime(new Date());
            memberTradeRecordService.updateByPrimaryKeySelective(updateRecord);
        }
        MemberTradeRecord currentTradeRecord = memberTradeRecordService.selectByOutTradeNo(outTradeNo);

        //订单回填：配送费/收货地址/改配送单号（回调时从这些字段取值，与 toPay4Applet 一致）
        Order updateOrder = new Order();
        updateOrder.setId(dbOrder.getId());
        updateOrder.setMerchantDeliveryFee(merchantDeliveryFee);
        updateOrder.setDeliveryAddressId(deliveryAddressId);
        updateOrder.setChangeToDeliveryOutTradeNo(outTradeNo);
        updateOrder.setChangeToDeliveryTradeId(currentTradeRecord.getId());
        updateOrder.setPaymentMode(Quantity.INT_1);
        updateOrder.setUpdateTime(new Date());
        orderService.updateById(updateOrder);

        //★ 核心复用点：与微信支付回调 type=3 完全相同的流转
        orderService.paymentNotifyOfChangeToDelivery(outTradeNo);

        log.info("\n[MockPay] 自取改配送模拟支付成功，orderNo={}，outTradeNo={}", orderNo, outTradeNo);
        return successResult(outTradeNo);
    }

    /**
     * type=4 积分商城订单付款：复刻 toPay4Applet 的建档与订单回填，
     * 再调与回调完全相同的 pointsMallOrderService.paymentNotify
     */
    private BasicResult mockPointsMallPay(Member loginMember, Map<String, Object> params) {
        String orderNo = getRequiredString(params, "outTradeNo", "必须填写商户单号");

        PointsMallOrder dbOrder = pointsMallOrderService.selectByOrderNo(orderNo);
        if (dbOrder == null) {
            throw new StoneCustomerException("该商户单号不存在");
        }
        if (!loginMember.getId().equals(dbOrder.getMemberId())) {
            throw new StoneCustomerException("无权操作该订单");
        }
        if (dbOrder.getStatus() == Quantity.INT_10) {
            throw new StoneCustomerException("该笔订单超时未支付，已自动取消，请重新下单");
        }
        if (dbOrder.getStatus() != Quantity.INT_1) {
            return repeatResult();
        }

        MemberTradeRecord dbMemberTradeRecord = memberTradeRecordService.selectByOutTradeNo(orderNo);
        if (dbMemberTradeRecord != null && dbMemberTradeRecord.getStatus() != Quantity.INT_1) {
            return repeatResult();
        }

        String transactionId = "MOCK" + System.currentTimeMillis();
        if (dbMemberTradeRecord == null) {
            //交易记录建档（type=6 积分商城订单付款，与 toPay4Applet 的取值一致）
            MemberTradeRecord insertMemberTradeRecord = new MemberTradeRecord();
            insertMemberTradeRecord.setMemberId(loginMember.getId());
            insertMemberTradeRecord.setOutTradeNo(orderNo);
            insertMemberTradeRecord.setType(Quantity.INT_6);
            insertMemberTradeRecord.setPaymentMode(Quantity.INT_1);
            insertMemberTradeRecord.setAmount(dbOrder.getActualPrice());
            insertMemberTradeRecord.setTradeNo(transactionId);
            insertMemberTradeRecord.setStatus(Quantity.INT_2);
            insertMemberTradeRecord.setCreateTime(new Date());
            insertMemberTradeRecord.setUpdateTime(new Date());
            memberTradeRecordService.insertSelective(insertMemberTradeRecord);
        } else {
            MemberTradeRecord updateRecord = new MemberTradeRecord();
            updateRecord.setId(dbMemberTradeRecord.getId());
            updateRecord.setTradeNo(transactionId);
            updateRecord.setStatus(Quantity.INT_2);
            updateRecord.setUpdateTime(new Date());
            memberTradeRecordService.updateByPrimaryKeySelective(updateRecord);
        }
        MemberTradeRecord currentTradeRecord = memberTradeRecordService.selectByOutTradeNo(orderNo);

        //订单回填：补填用户交易id
        PointsMallOrder updatePointsMallOrder = new PointsMallOrder();
        updatePointsMallOrder.setId(dbOrder.getId());
        updatePointsMallOrder.setTradeId(currentTradeRecord.getId());
        updatePointsMallOrder.setPaymentMode(Quantity.INT_1);
        updatePointsMallOrder.setUpdateTime(new Date());
        pointsMallOrderService.updateByPrimaryKeySelective(updatePointsMallOrder);

        //★ 核心复用点：与微信支付回调 type=4 完全相同的流转
        pointsMallOrderService.paymentNotify(orderNo);

        log.info("\n[MockPay] 积分商城订单模拟支付成功，orderNo={}，memberId={}", orderNo, loginMember.getId());
        return successResult(null);
    }

    /**
     * 成功返回体
     * ⚠️ 必须显式设置 success/code：前端（utils/pay.js）以 result.success 判断成败，
     * 而 BasicData 的 success/code 默认是 null（falsy）→ 不设置会被前端判为「支付失败」，
     * 尽管后端业务已流转成功。
     */
    private BasicData successResult(Object data) {
        BasicData result = new BasicData();
        result.setSuccess(true);
        result.setCode(BasicResultCode.SUCCESS);
        result.setMessage("模拟支付成功");
        result.setData(data);
        return result;
    }

    /** 取必填字符串参数（避免直接 toString 到 "null"） */
    private String getRequiredString(Map<String, Object> params, String key, String errorMessage) {
        Object value = params.get(key);
        String text = value == null ? null : value.toString();
        if (text == null || text.trim().length() == 0) {
            throw new StoneCustomerException(errorMessage);
        }
        return text;
    }

    /** 幂等统一返回：与 wxNotify 的「重复回调直接返回」语义一致，前端按成功处理 */
    private BasicResult repeatResult() {
        BasicResult repeat = new BasicResult();
        repeat.setSuccess(true);
        repeat.setCode(BasicResultCode.SUCCESS);
        repeat.setMessage("该订单已支付，请勿重复操作");
        return repeat;
    }

    /**
     * 生成商户单号/订单编号
     * [此处要做两个判断，一个是针对于订单编号，一个是针对于自取订单改为配送的商户单号]
     * => 考虑到商户单号不止针对于订单表，还有Vip充值等操作，所以应该在用户交易记录表中判断商户单号是否重复
     */
    public String getOutTradeNo() {
        return GenerateNo.getOrderNo();
    }
}