package com.siam.system.modular.package_points.controller.member;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.package_common.entity.BasicData;
import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_points.entity.MemberPointsLog;
import com.siam.system.modular.package_points.service.MemberLevelService;
import com.siam.system.modular.package_points.service.MemberPointsService;
import com.siam.system.modular.package_user.auth.cache.MemberSessionManager;
import com.siam.system.modular.package_user.entity.Member;
import com.siam.system.util.TokenUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 会员端积分接口 · 我的积分/等级 + 流水。
 */
@RestController
@RequestMapping(value = "/rest/member/points")
@Api(tags = "会员积分接口", description = "MemberPointsController")
public class MemberPointsController {

    @Autowired
    private MemberPointsService memberPointsService;

    @Autowired
    private MemberLevelService memberLevelService;

    @Autowired
    private MemberSessionManager memberSessionManager;

    @ApiOperation(value = "我的积分与会员等级（含距下一档差值）")
    @PostMapping(value = "/myPoints")
    public BasicResult myPoints(javax.servlet.http.HttpServletRequest request) {
        Member loginMember = memberSessionManager.getSession(TokenUtil.getToken());
        Map<String, Object> levelInfo = memberLevelService.getLevelInfo(loginMember.getId());
        return BasicResult.success(levelInfo);
    }

    @ApiOperation(value = "我的积分流水分页")
    @PostMapping(value = "/myLog")
    public BasicResult myLog(@RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
                             @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
                             javax.servlet.http.HttpServletRequest request) {
        Member loginMember = memberSessionManager.getSession(TokenUtil.getToken());
        Page<MemberPointsLog> page = memberPointsService.getLogPage(loginMember.getId(), pageNo, pageSize);
        return BasicResult.success(page);
    }
}
