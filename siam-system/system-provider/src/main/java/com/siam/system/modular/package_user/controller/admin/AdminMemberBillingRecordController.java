package com.siam.system.modular.package_user.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.package_common.annoation.AdminPermission;
import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_user.model.param.MemberBillingRecordParam;
import com.siam.system.modular.package_user.service.MemberBillingRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员资金流水（管理端）
 *
 * <p>用途：管理端「会员详情」的资金流水区块 —— 会员的余额/积分变动明细
 * （充值入账、赠送、下单扣减、退款退回等，见 MemberBillingRecord 的 type/coinType 枚举）。
 *
 * <p>⚠️ 本类 2026-09-24 重写启用。此前的同名文件是**从旧包名
 * （{@code com.siam.package_user.controller.admin}）整类注释遗留下来的**，
 * 不能简单取消注释：① 包名是老路径；② 方法体里引用了未定义的 {@code param}，取消注释会编译失败。
 * 现按 {@code AdminMemberController} 的现行规范重写，并复用现成的
 * {@link MemberBillingRecordService#getListByPage(MemberBillingRecordParam)}（本身支持按 memberId 过滤）。
 *
 * <p>鉴权：/rest/admin/** 由 adminInterceptor 统一保护；本接口沿用同包 {@code @AdminPermission} 标注惯例。
 *
 * @author siam
 */
@RestController
@RequestMapping(value = "/rest/admin/memberBillingRecord")
@Transactional(rollbackFor = Exception.class)
@Api(tags = "后台用户账单记录模块相关接口", description = "AdminMemberBillingRecordController")
public class AdminMemberBillingRecordController {

    @Autowired
    private MemberBillingRecordService memberBillingRecordService;

    /**
     * 会员资金流水列表
     *
     * <p>入参 {@link MemberBillingRecordParam} 继承实体，可直接按
     * {@code memberId} / {@code coinType}（1积分 2余额 …）等字段过滤；分页字段 pageNo/pageSize。
     */
    @ApiOperation(value = "会员资金流水列表")
    @AdminPermission
    @PostMapping(value = "/list")
    public BasicResult list(@RequestBody @Validated(value = {}) MemberBillingRecordParam param) {
        // 必须走 ForAdmin 版本：getListByPage 是会员端专用，内部会用会员会话覆盖 memberId，
        // 管理端没有会员会话 → 直接 NPE（2026-09-24 实测踩到并修正）
        Page page = memberBillingRecordService.getListByPageForAdmin(param);
        return BasicResult.success(page);
    }
}
