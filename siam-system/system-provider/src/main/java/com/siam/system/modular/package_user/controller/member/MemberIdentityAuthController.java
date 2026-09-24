package com.siam.system.modular.package_user.controller.member;

import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_user.model.param.MemberIdentityAuthParam;
import com.siam.system.modular.package_user.service.MemberIdentityAuthService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员身份认证模块相关接口（顾客端 C-17 / C-17a / C-17b）
 *
 * @author 暹罗
 */
@Slf4j
@RestController
@RequestMapping(value = "/rest/member/identityAuth")
@Transactional(rollbackFor = Exception.class)
@Api(tags = "会员身份认证模块相关接口", description = "MemberIdentityAuthController")
public class MemberIdentityAuthController {

    @Autowired
    private MemberIdentityAuthService memberIdentityAuthService;

    /**
     * 认证中心：查询当前登录会员两阶段认证状态
     */
    @ApiOperation(value = "认证中心-查询当前会员两阶段认证状态")
    @PostMapping(value = "/detail")
    public BasicResult detail(@RequestBody @Validated(value = {}) MemberIdentityAuthParam param) {
        return BasicResult.success(memberIdentityAuthService.getDetailByLoginMember());
    }

    /**
     * 阶段一：提交实名认证（姓名 + 身份证人像面/国徽面）
     */
    @ApiOperation(value = "阶段一-提交实名认证")
    @PostMapping(value = "/submitRealName")
    public BasicResult submitRealName(@RequestBody @Validated(value = {}) MemberIdentityAuthParam param) {
        memberIdentityAuthService.submitRealName(param);
        return BasicResult.success();
    }

    /**
     * 阶段二：提交身份认证（学生证 或 教师资格证）
     */
    @ApiOperation(value = "阶段二-提交身份认证")
    @PostMapping(value = "/submitIdentity")
    public BasicResult submitIdentity(@RequestBody @Validated(value = {}) MemberIdentityAuthParam param) {
        memberIdentityAuthService.submitIdentity(param);
        return BasicResult.success();
    }
}
