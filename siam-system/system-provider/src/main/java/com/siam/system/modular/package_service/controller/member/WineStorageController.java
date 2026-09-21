package com.siam.system.modular.package_service.controller.member;

import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_service.model.param.WineStorageParam;
import com.siam.system.modular.package_service.service.WineStorageService;
import com.siam.system.modular.package_user.auth.cache.MemberSessionManager;
import com.siam.system.modular.package_user.entity.Member;
import com.siam.system.util.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 存酒接口（会员侧）· C-16 / C-01 存酒区 ＋ 取酒申请（方案 v1.1）
 *
 * 鉴权：/rest/member/** 由 memberInterceptor 统一保护，需登录。
 * 会员可「查」存酒、发起/查看/取消「取酒申请」；取酒码核销是商家动作（见 MerchantWineStorageController）。
 */
@RestController
@RequestMapping(value = "/rest/member/wineStorage")
public class WineStorageController {

    @Autowired
    private WineStorageService wineStorageService;

    @Autowired
    private MemberSessionManager memberSessionManager;

    private Member loginMember() {
        return memberSessionManager.getSession(TokenUtil.getToken());
    }

    /**
     * 我的存酒列表（C-16 全量 / C-01 存酒区取前 3 条）
     */
    @PostMapping(value = "/list")
    public BasicResult list(@RequestBody(required = false) WineStorageParam param) {
        return BasicResult.success(wineStorageService.listByMember(loginMember().getId()));
    }

    /**
     * 存酒详情（校验归属）
     */
    @PostMapping(value = "/selectById")
    public BasicResult selectById(@RequestBody WineStorageParam param) {
        return BasicResult.success(wineStorageService.detailByMember(loginMember().getId(), param.getId()));
    }

    /**
     * 申请取酒：登录态发起 → 生成 6 位取酒码（24h 有效）
     */
    @PostMapping(value = "/applyTake")
    public BasicResult applyTake(@RequestBody WineStorageParam param) {
        return BasicResult.success(wineStorageService.applyTake(loginMember().getId(), param));
    }

    /**
     * 我的取酒申请列表（apply_time 倒序）
     */
    @PostMapping(value = "/applyList")
    public BasicResult applyList(@RequestBody(required = false) WineStorageParam param) {
        return BasicResult.success(wineStorageService.listMyApplies(loginMember().getId()));
    }

    /**
     * 取消我的待核销取酒申请（不限来源）
     */
    @PostMapping(value = "/cancelApply")
    public BasicResult cancelApply(@RequestBody WineStorageParam param) {
        wineStorageService.cancelApply(loginMember().getId(), param.getApplyId());
        return BasicResult.success();
    }
}
