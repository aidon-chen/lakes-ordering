package com.siam.system.modular.package_service.controller.member;

import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_service.model.param.WineStorageParam;
import com.siam.system.modular.package_service.service.WineRemindService;
import com.siam.system.modular.package_service.service.WineStorageService;
import com.siam.system.modular.package_user.auth.cache.MemberSessionManager;
import com.siam.system.modular.package_user.entity.Member;
import com.siam.system.util.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.annotations.ApiOperation;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

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
    private WineRemindService wineRemindService;

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
    /* ==================== 批次 F · W-19 存酒临期提醒（小程序弹窗口径） ==================== */

    /**
     * 我的临期/过期存酒（小程序「我的存酒」进页时调用；客户端据此弹窗）
     * 只返回剩余天数 ≤ 提前天数(默认7) 的存续中/已过期记录，按紧急程度升序。
     */
    @ApiOperation(value = "我的临期与过期存酒（W-19 弹窗数据源）")
    @PostMapping(value = "/expiringList")
    public BasicResult expiringList() {
        return BasicResult.success(wineRemindService.listExpiringForMemberAuto(loginMember().getId()));
    }

    /**
     * 弹窗回报（幂等：同一存酒同一天只记一条）—— 收银端 W-19 的「通知状态」列由此驱动
     */
    @ApiOperation(value = "临期提醒弹窗回报")
    @PostMapping(value = "/remindAck")
    public BasicResult remindAck(@RequestBody Map<String, Object> body) {
        Object idsObj = body.get("storageIds");
        List<Integer> ids = new ArrayList<>();
        if (idsObj instanceof List) {
            for (Object o : (List<?>) idsObj) {
                try {
                    ids.add(Integer.valueOf(String.valueOf(o)));
                } catch (NumberFormatException ignore) {
                }
            }
        }
        return BasicResult.success(wineRemindService.ack(loginMember().getId(), ids));
    }
}