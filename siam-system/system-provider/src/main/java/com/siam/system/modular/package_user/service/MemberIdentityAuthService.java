package com.siam.system.modular.package_user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.system.modular.package_user.model.param.MemberIdentityAuthParam;

import java.util.List;
import java.util.Map;

/**
 * 会员身份认证申请记录表
 *
 * @author 暹罗
 */
public interface MemberIdentityAuthService {

    /**
     * 顾客端-认证中心：查询当前登录会员两阶段认证状态
     */
    Map<String, Object> getDetailByLoginMember();

    /**
     * 顾客端-阶段一：提交实名认证（姓名 + 身份证人像面/国徽面）
     */
    void submitRealName(MemberIdentityAuthParam param);

    /**
     * 顾客端-阶段二：提交身份认证（学生证 或 教师资格证）
     * 服务端强校验：阶段一必须已审核通过
     */
    void submitIdentity(MemberIdentityAuthParam param);

    /**
     * 管理端-分页列表（联查会员手机号）
     */
    Page<Map<String, Object>> getListByPageJoinMember(MemberIdentityAuthParam param);

    /**
     * 管理端-审核弹窗：按会员id取该会员两阶段全部记录（双图同屏用）
     */
    List<Map<String, Object>> getDetailByMemberId(MemberIdentityAuthParam param);

    /**
     * 管理端-审核
     * 入参 status：1=通过 → 落库 audit_status=2；2=不通过 → 落库 audit_status=3（opinion 必填）
     */
    void audit(MemberIdentityAuthParam param);

    /**
     * 「审核即弃」清理：删除审核结论作出后已满 N 天（siam.identityAuth.keepDays）的证件图。
     * <p>
     * 为什么不是定时任务：mvp profile 下 {@code @Scheduled} 全域不启动
     * （SchedulingConfig 标了 {@code @Profile("!mvp")}），云托管最小副本 0 也不适合常驻调度。
     * 故改为**在有流量时顺带清理**：审核出结论时 + 打开审核列表时各触发一次（带节流）。
     * <p>
     * 只删图片、清空图片路径列，**保留审核痕迹**（状态/理由/审核人/时间/脱敏姓名）。
     *
     * @return 本次实际清理的记录条数
     */
    int cleanExpiredImages();

    /**
     * 带节流的清理（内部用，供列表查询等高频入口调用）
     */
    void cleanExpiredImagesThrottled();
}
