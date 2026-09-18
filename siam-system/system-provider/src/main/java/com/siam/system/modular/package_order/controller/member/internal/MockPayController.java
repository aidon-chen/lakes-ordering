package com.siam.system.modular.package_order.controller.member.internal;

import com.siam.package_common.constant.Quantity;
import com.siam.package_common.entity.BasicData;
import com.siam.package_common.entity.BasicResult;
import com.siam.package_common.exception.StoneCustomerException;
import com.siam.system.modular.package_order.entity.Order;
import com.siam.system.modular.package_order.service.OrderService;
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

import java.util.Date;
import java.util.Map;

/**
 * 模拟支付（★ 仅 mvp profile 装配，生产/其他 profile 下本接口不存在=404）
 *
 * 与微信支付回调（WxPayController.wxNotify）走【同一套】订单状态流转分支
 * （orderService.paymentNotify），保证「支付成功→订单流转→商家端接单」演示链路
 * 被真实代码路径验证。见 04-deploy-cost/mvp-migration/04
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

    /**
     * 模拟支付成功
     *
     * @param params outTradeNo=商户单号（即订单号，与微信支付约定一致）
     */
    @ApiOperation(value = "模拟支付成功（仅MVP演示环境）")
    @PostMapping(value = "/mockPay")
    public BasicResult mockPay(@RequestBody Map<String, Object> params) throws Exception {
        BasicData basicResult = new BasicData();
        Member loginMember = memberSessionManager.getSession(TokenUtil.getToken());
        if (loginMember == null) {
            throw new StoneCustomerException("请先登录");
        }

        String orderNo = params.get("outTradeNo") == null ? null : params.get("outTradeNo").toString();
        if (orderNo == null || orderNo.trim().length() == 0) {
            throw new StoneCustomerException("必须填写商户单号");
        }

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
            BasicResult repeat = new BasicResult();
            repeat.setSuccess(true);
            repeat.setMessage("该订单已支付，请勿重复操作");
            return repeat;
        }

        //复刻回调逻辑：交易记录幂等防护（status!=1 视为已处理，与 wxNotify 一致）
        MemberTradeRecord dbMemberTradeRecord = memberTradeRecordService.selectByOutTradeNo(orderNo);
        if (dbMemberTradeRecord != null && dbMemberTradeRecord.getStatus() != Quantity.INT_1) {
            BasicResult repeat = new BasicResult();
            repeat.setSuccess(true);
            repeat.setMessage("该订单已支付，请勿重复操作");
            return repeat;
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

        //★ 核心复用点：与微信支付回调完全相同的订单状态流转（置已支付/积分/销量/商家冻结资金/通知）
        orderService.paymentNotify(orderNo);

        log.info("\n[MockPay] 订单模拟支付成功，orderNo={}，memberId={}", orderNo, loginMember.getId());
        basicResult.setMessage("模拟支付成功");
        return basicResult;
    }
}
