package com.siam.system.modular.package_goods.controller.member;

import com.siam.package_common.entity.BasicData;
import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_goods.entity.Coupons;
import com.siam.system.modular.package_goods.service.CouponsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/rest/member/coupons")
@Transactional(rollbackFor = Exception.class)
@Api(tags = "会员领券中心接口", description = "MemberCouponsController")
public class MemberCouponsController {

    @Autowired
    private CouponsService couponsService;

    /**
     * 领券中心券池列表：未删除 + 发行方式=自行领取(issueType=2)。
     * 位于 /rest/member/** 拦截器范围内，需会员登录（token 头 Authorization）。
     * 「是否已领取」由前端结合 /rest/member/couponsMemberRelation/list 的 couponsId 集合判断，
     * 本接口保持零状态、不查关系表。
     */
    @ApiOperation(value = "领券中心券池列表")
    @PostMapping(value = "/list")
    public BasicResult list(@RequestBody(required = false) Map<String, Object> param) {
        BasicData basicResult = new BasicData();

        List<Coupons> couponsList = couponsService.listIssuable();

        Map<String, Object> data = new HashMap<>();
        data.put("couponsList", couponsList);
        basicResult.setData(data);
        basicResult.setSuccess(true);
        basicResult.setMessage("获取成功");
        return basicResult;
    }
}
