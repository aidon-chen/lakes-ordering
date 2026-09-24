package com.siam.system.modular.package_points.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.package_common.annoation.AdminPermission;
import com.siam.package_common.entity.BasicData;
import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_points.entity.MemberLevelSetting;
import com.siam.system.modular.package_points.entity.MemberPointsLog;
import com.siam.system.modular.package_points.entity.MemberPointsSetting;
import com.siam.system.modular.package_points.service.MemberPointsService;
import com.siam.system.modular.package_points.service.MemberPointsSettingService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 积分管理后台接口 · 「积分管理」界面的数据源。
 * 规则配置（消费/充值比例、活动分值）+ 等级阈值 + 会员积分流水 + 手工调分。
 */
@RestController
@RequestMapping(value = "/rest/admin/memberPoints")
@Transactional(rollbackFor = Exception.class)
@Api(tags = "积分管理后台接口", description = "AdminMemberPointsController")
public class AdminMemberPointsController {

    @Autowired
    private MemberPointsSettingService memberPointsSettingService;

    @Autowired
    private MemberPointsService memberPointsService;

    /* ==================== 积分获取规则配置 ==================== */

    @AdminPermission
    @ApiOperation(value = "积分规则配置列表")
    @PostMapping(value = "/settingList")
    public BasicResult settingList() {
        List<MemberPointsSetting> list = memberPointsSettingService.listAll();
        return BasicResult.success(list);
    }

    @AdminPermission
    @ApiOperation(value = "更新积分规则配置项")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "itemKey", value = "配置键", required = true, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "itemValue", value = "配置值", required = true, paramType = "query", dataType = "int"),
    })
    @PostMapping(value = "/settingUpdate")
    public BasicResult settingUpdate(@RequestParam("itemKey") String itemKey,
                                     @RequestParam("itemValue") Integer itemValue) {
        if (itemValue == null || itemValue < 0) {
            return BasicResult.error("配置值不能为负数");
        }
        memberPointsSettingService.updateItem(itemKey, itemValue);
        return BasicResult.success("更新成功");
    }

    /* ==================== 等级阈值配置 ==================== */

    @AdminPermission
    @ApiOperation(value = "会员等级阈值列表")
    @PostMapping(value = "/levelList")
    public BasicResult levelList() {
        List<MemberLevelSetting> list = memberPointsSettingService.levelList();
        return BasicResult.success(list);
    }

    @AdminPermission
    @ApiOperation(value = "更新会员等级阈值")
    @PostMapping(value = "/levelUpdate")
    public BasicResult levelUpdate(@RequestBody MemberLevelSetting setting) {
        if (setting.getThreshold() == null || setting.getThreshold() < 0) {
            return BasicResult.error("阈值不能为负数");
        }
        memberPointsSettingService.updateLevel(setting);
        return BasicResult.success("更新成功");
    }

    /* ==================== 积分流水与手工调分 ==================== */

    @AdminPermission
    @ApiOperation(value = "会员积分流水分页（memberId 可选）")
    @PostMapping(value = "/logList")
    public BasicResult logList(@RequestParam(value = "memberId", required = false) Integer memberId,
                               @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
                               @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        Page<MemberPointsLog> page = memberPointsService.getLogPage(memberId, pageNo, pageSize);
        return BasicResult.success(page);
    }

    @AdminPermission
    @ApiOperation(value = "会员积分查询（等级信息）")
    @ApiImplicitParam(name = "memberId", value = "会员id", required = true, paramType = "query", dataType = "int")
    @PostMapping(value = "/memberLevelInfo")
    public BasicResult memberLevelInfo(@RequestParam("memberId") Integer memberId) {
        Map<String, Object> levelInfo = memberLevelService.getLevelInfo(memberId);
        return BasicResult.success(levelInfo);
    }

    @AdminPermission
    @ApiOperation(value = "手工调分（正加负减）")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "memberId", value = "会员id", required = true, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "points", value = "调整分值 正加负减", required = true, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "remark", value = "备注", required = false, paramType = "query", dataType = "string"),
    })
    @PostMapping(value = "/adjust")
    public BasicResult adjust(@RequestParam("memberId") Integer memberId,
                              @RequestParam("points") Integer points,
                              @RequestParam(value = "remark", required = false) String remark) {
        memberPointsService.adjust(memberId, points, remark);
        return BasicResult.success("调整成功");
    }

    private Map<String, Object> memberPointsLevelInfo(Integer memberId) {
        return memberLevelInfoDelegate(memberId);
    }

    @Autowired
    private com.siam.system.modular.package_points.service.MemberLevelService memberLevelService;

    private Map<String, Object> memberLevelInfoDelegate(Integer memberId) {
        return memberLevelService.getLevelInfo(memberId);
    }
}
