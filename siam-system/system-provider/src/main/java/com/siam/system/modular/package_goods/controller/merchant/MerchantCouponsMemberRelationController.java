package com.siam.system.modular.package_goods.controller.merchant;

import com.siam.package_common.entity.BasicResult;
import com.siam.package_common.constant.BasicResultCode;
import com.siam.package_common.constant.BusinessType;
import com.siam.package_common.exception.StoneCustomerException;
import com.siam.package_common.service.AliyunSms;
import com.siam.system.modular.package_goods.entity.Coupons;
import com.siam.system.modular.package_goods.service.CouponsService;
import com.siam.system.modular.package_goods.entity.CouponsMemberRelation;
import com.siam.system.modular.package_goods.service.CouponsMemberRelationService;
import com.siam.system.modular.package_user.entity.Member;
import com.siam.system.modular.package_user.service.MemberService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/rest/merchant/couponsMemberRelation")
@Transactional(rollbackFor = Exception.class)
@Api(tags = "商家端优惠卷用关系接口", description = "MerchantCouponsMemberRelationController")
public class MerchantCouponsMemberRelationController {

    @Autowired
    private CouponsMemberRelationService couponsMemberRelationService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private CouponsService couponsService;

    @Autowired
    private AliyunSms aliyunSms;


    @ApiOperation(value = "赠送优惠卷")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "couponsId", value = "优惠卷id", required = false, paramType = "query", dataType = "int")
    })
    @PostMapping(value = "/giveCoupons")
    public BasicResult giveCoupons(@RequestBody @Validated(value = {}) CouponsMemberRelation param) {
        BasicResult basicResult = new BasicResult();

        if (param.getCouponsId().equals(BusinessType.NEW_PEOPLE_COUPONS_ID) || param.getCouponsId().equals(BusinessType.INVITE_NEW_PEOPLE_COUPONS_ID)) {
            throw new StoneCustomerException("新人优惠卷和推荐新人优惠卷为系统默认优惠卷，不允许群发");
        }

        //获取优惠卷
        Coupons coupons=couponsService.selectByPrimaryKey(param.getCouponsId());

        //获取开始和结束时间
        Date startTime = new Date();
        Date endTime = startTime;
        Integer validType=coupons.getValidType();
        if(Coupons.VALID_TYPE_DAYS_NUM.equals(validType)){
            Calendar calendar = Calendar. getInstance();
            calendar.setTime( new Date());
            calendar.set(Calendar. HOUR_OF_DAY, 0);
            calendar.set(Calendar. MINUTE, 0);
            calendar.set(Calendar. SECOND, 0);
            calendar.set(Calendar. MILLISECOND, 0);
            calendar.add(Calendar. DAY_OF_MONTH, coupons.getValidDays());
            endTime = calendar.getTime();
        }else if(Coupons.VALID_TYPE_TIME_QUANTUM.equals(validType)){
            startTime=coupons.getValidStartTime();
            endTime=coupons.getValidEndTime();
        }

        //查询出所有的用户
        List<Member> memberList = memberService.selectAllMemberNoneCoupons(param.getCouponsId());

        //给所有用户派发优惠卷
        for (Member member : memberList) {
            CouponsMemberRelation couponsMemberRelation = new CouponsMemberRelation();
            couponsMemberRelation.setCouponsId(param.getCouponsId());
            couponsMemberRelation.setMemberId(member.getId());
            couponsMemberRelation.setCouponsName(coupons.getName());
            couponsMemberRelation.setIsUsed(false);
            couponsMemberRelation.setIsExpired(false);
            couponsMemberRelation.setIsValid(true);
            couponsMemberRelation.setStartTime(startTime);
            couponsMemberRelation.setEndTime(endTime);
            couponsMemberRelation.setCreateTime(new Date());
            couponsMemberRelationService.insertSelective(couponsMemberRelation);

            //发送短信
            /*aliyunSms.sendCouponsDistributeReminderMessage(member.getMobile(), coupons.getName());*/
        }

        basicResult.setSuccess(true);
        basicResult.setCode(BasicResultCode.SUCCESS);
        basicResult.setMessage("操作成功");
        return basicResult;
    }

    @ApiOperation(value = "赠送优惠卷（单发）")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "couponsId", value = "优惠卷id", required = false, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "memberId", value = "用户id", required = false, paramType = "query", dataType = "int")
    })
    @PostMapping(value = "/giveSingleCoupons")
    public BasicResult giveSingleCoupons(@RequestBody @Validated(value = {}) CouponsMemberRelation param) {
        BasicResult basicResult = new BasicResult();

        if (param.getCouponsId().equals(BusinessType.NEW_PEOPLE_COUPONS_ID) || param.getCouponsId().equals(BusinessType.INVITE_NEW_PEOPLE_COUPONS_ID)) {
            throw new StoneCustomerException("新人优惠卷和推荐新人优惠卷为系统默认优惠卷，不允许群发");
        }
        if (param.getMemberId() == null) {
            throw new StoneCustomerException("请指定发放的会员");
        }

        //获取优惠卷
        Coupons coupons=couponsService.selectByPrimaryKey(param.getCouponsId());

        //获取开始和结束时间
        Date startTime = new Date();
        Date endTime = startTime;
        Integer validType=coupons.getValidType();
        if(Coupons.VALID_TYPE_DAYS_NUM.equals(validType)){
            Calendar calendar = Calendar. getInstance();
            calendar.setTime( new Date());
            calendar.set(Calendar. HOUR_OF_DAY, 0);
            calendar.set(Calendar. MINUTE, 0);
            calendar.set(Calendar. SECOND, 0);
            calendar.set(Calendar. MILLISECOND, 0);
            calendar.add(Calendar. DAY_OF_MONTH, coupons.getValidDays());
            endTime = calendar.getTime();
        }else if(Coupons.VALID_TYPE_TIME_QUANTUM.equals(validType)){
            startTime=coupons.getValidStartTime();
            endTime=coupons.getValidEndTime();
        }

        //判断该会员是否已持有此券（未使用且有效即算持有；已用完/已过期不占名额）
        //2026-09-24 修复：原校验为"名下有任何未使用券即拒"，且插库代码整体注释导致接口空转假成功
        CouponsMemberRelation query = new CouponsMemberRelation();
        query.setPageNo(1);
        query.setPageSize(1);
        query.setCouponsId(param.getCouponsId());
        query.setMemberId(param.getMemberId());
        query.setIsUsed(false);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Map<String, Object>> existed =
                couponsMemberRelationService.getListByPage(query.getPageNo(), query.getPageSize(), query);
        if (existed.getRecords() != null && !existed.getRecords().isEmpty()) {
            basicResult.setSuccess(false);
            basicResult.setCode(BasicResultCode.ERR);
            basicResult.setMessage("操作失败，该会员已持有此券（未使用）");
            return basicResult;
        }

        //构建优惠卷关系并落库（字段口径与群发 giveCoupons 一致）
        CouponsMemberRelation couponsMemberRelation = new CouponsMemberRelation();
        couponsMemberRelation.setCouponsId(param.getCouponsId());
        couponsMemberRelation.setMemberId(param.getMemberId());
        couponsMemberRelation.setCouponsName(coupons.getName());
        couponsMemberRelation.setIsUsed(false);
        couponsMemberRelation.setIsExpired(false);
        couponsMemberRelation.setIsValid(true);
        couponsMemberRelation.setStartTime(startTime);
        couponsMemberRelation.setEndTime(endTime);
        couponsMemberRelation.setCreateTime(new Date());
        couponsMemberRelationService.insertSelective(couponsMemberRelation);

        //发送短信
        /*aliyunSms.sendCouponsDistributeReminderMessage(member.getMobile(), coupons.getName());*/

        basicResult.setSuccess(true);
        basicResult.setCode(BasicResultCode.SUCCESS);
        basicResult.setMessage("操作成功");
        return basicResult;

    }

}
