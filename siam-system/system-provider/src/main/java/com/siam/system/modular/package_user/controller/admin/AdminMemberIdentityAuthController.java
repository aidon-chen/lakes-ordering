package com.siam.system.modular.package_user.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.package_common.entity.BasicResult;
import com.siam.package_common.model.valid_group.ValidGroupOfAudit;
import com.siam.package_common.model.valid_group.ValidGroupOfId;
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

import java.util.Map;

/**
 * 后台会员身份认证模块相关接口（管理端 M- 审核）
 *
 * @author 暹罗
 */
@Slf4j
@RestController
@RequestMapping(value = "/rest/admin/memberIdentityAuth")
@Transactional(rollbackFor = Exception.class)
@Api(tags = "后台会员身份认证模块相关接口", description = "AdminMemberIdentityAuthController")
public class AdminMemberIdentityAuthController {

    @Autowired
    private MemberIdentityAuthService memberIdentityAuthService;

    /**
     * 会员身份认证列表（联查会员手机号；auditStatus 不传 = 全部，传 -1 = 全部）
     */
    @ApiOperation(value = "会员身份认证列表")
    @PostMapping(value = "/list")
    public BasicResult list(@RequestBody @Validated(value = {}) MemberIdentityAuthParam param) {
        Page<Map<String, Object>> page = memberIdentityAuthService.getListByPageJoinMember(param);
        return BasicResult.success(page);
    }

    /**
     * 审核弹窗：按会员id取该会员两阶段全部记录（身份证与身份材料双图同屏）
     */
    @ApiOperation(value = "审核弹窗-按会员id取两阶段记录")
    @PostMapping(value = "/detail")
    public BasicResult detail(@RequestBody @Validated(value = {}) MemberIdentityAuthParam param) {
        return BasicResult.success(memberIdentityAuthService.getDetailByMemberId(param));
    }

    /**
     * 审核：入参 status 1=通过 / 2=不通过（不通过时 opinion 必填）
     */
    @ApiOperation(value = "会员身份认证审核")
    @PostMapping(value = "/audit")
    public BasicResult audit(@RequestBody @Validated(value = {ValidGroupOfId.class, ValidGroupOfAudit.class}) MemberIdentityAuthParam param) {
        memberIdentityAuthService.audit(param);
        return BasicResult.success();
    }
}
