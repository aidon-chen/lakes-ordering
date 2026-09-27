package com.siam.system.modular.package_service.controller.admin;

import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_service.model.vo.WineStorageVo;
import com.siam.system.modular.package_service.service.WineStorageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理端存酒接口（供商家端「用户管理 → 会员详情 → 存酒Tab」使用）
 *
 * <p>刻意不走 /rest/merchant/wineStorage：该前缀无登录校验（铁律），
 * 且其 list 是按登录商家门店（listByShop）强过滤，不满足「按会员查」的诉求。
 * 复用顾客端 C-16 已有的 listByMember（含惰性过期、店名填充、待核销标记）。</p>
 */
@RestController
@RequestMapping(value = "/rest/admin/wineStorage")
@Transactional(rollbackFor = Exception.class)
@Api(tags = "管理端存酒接口", description = "AdminWineStorageController")
public class AdminWineStorageController {

    @Autowired
    private WineStorageService wineStorageService;

    @ApiOperation(value = "按会员id查存酒列表")
    @PostMapping(value = "/listByMember")
    public BasicResult listByMember(@RequestBody Map<String, Object> param) {
        Object raw = param == null ? null : param.get("memberId");
        if (raw == null) {
            return BasicResult.success(new java.util.ArrayList<>());
        }
        Integer memberId = ((Number) raw).intValue();
        List<WineStorageVo> list = wineStorageService.listByMember(memberId);
        return BasicResult.success(list);
    }
}
