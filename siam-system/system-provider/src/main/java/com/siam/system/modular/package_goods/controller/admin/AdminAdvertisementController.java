package com.siam.system.modular.package_goods.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_goods.entity.Advertisement;
import com.siam.system.modular.package_goods.model.param.AdvertisementParam;
import com.siam.system.modular.package_goods.service.AdvertisementService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/rest/admin/advertisement")
@Transactional(rollbackFor = Exception.class)
@Api(tags = "后台广告轮播图模块相关接口", description = "AdminAdvertisementController")
public class AdminAdvertisementController {

    @Autowired
    private AdvertisementService advertisementService;

    @ApiOperation(value = "广告轮播图列表")
    @PostMapping(value = "/list")
    public BasicResult list(@RequestBody @Validated(value = {}) AdvertisementParam param) {
        Page<Advertisement> page = advertisementService.getListByPage(param);
        return BasicResult.success(page);
    }

    @ApiOperation(value = "新增广告轮播图")
    @PostMapping(value = "/insert")
    public BasicResult insert(@RequestBody @Validated(value = {}) AdvertisementParam param) {
        advertisementService.insertSelective(param);
        return BasicResult.success();
    }


    // 商家端「营销中心 → 首页活动弹窗」需要能编辑弹窗内容，
    // 故此处不再要求超管身份（@AdminPermission 只放过 username==admin）。
    // 安全性由 /rest/admin/** 的全局登录拦截保证：未登录仍返回 code 2。
    @ApiOperation(value = "修改广告轮播图")
    @PutMapping(value = "/update")
    public BasicResult update(@RequestBody @Validated(value = {}) AdvertisementParam param) {
        advertisementService.updateByPrimaryKeySelective(param);
        return BasicResult.success();
    }

    @ApiOperation(value = "删除广告轮播图")
    @DeleteMapping(value = "/delete")
    public BasicResult delete(@RequestBody @Validated(value = {}) AdvertisementParam param) {
        //删除广告轮播图
        advertisementService.deleteByPrimaryKey(param);
        return BasicResult.success();
    }

    @ApiOperation(value = "获取单个广告轮播图详情信息")
    @PostMapping(value = "/getById")
    public BasicResult getById(@RequestBody @Validated(value = {}) AdvertisementParam param) {
        Advertisement advertisement = advertisementService.selectByPrimaryKey(param);
        return BasicResult.success(advertisement);
    }
}