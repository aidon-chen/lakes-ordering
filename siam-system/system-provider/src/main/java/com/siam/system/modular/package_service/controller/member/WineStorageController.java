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
 * 存酒接口（会员侧）· C-16 / C-01 存酒区
 *
 * 鉴权：/rest/member/** 由 memberInterceptor 统一保护，需登录。
 * 会员只能「查」，取酒扣瓶是商家动作（见 MerchantWineStorageController）。
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
     * 服务端已做惰性过期 + 四态计算 + 排序（存续中→将过期→已过期→已取完）
     */
    @PostMapping(value = "/list")
    public BasicResult list(@RequestBody(required = false) WineStorageParam param) {
        return BasicResult.success(wineStorageService.listByMember(loginMember().getId()));
    }

    /**
     * 存酒详情（校验归属；本批前端不消费，保留）
     */
    @PostMapping(value = "/selectById")
    public BasicResult selectById(@RequestBody WineStorageParam param) {
        return BasicResult.success(wineStorageService.detailByMember(loginMember().getId(), param.getId()));
    }
}
