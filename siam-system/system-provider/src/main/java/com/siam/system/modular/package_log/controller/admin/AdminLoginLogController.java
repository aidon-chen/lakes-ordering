package com.siam.system.modular.package_log.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_log.entity.LoginLog;
import com.siam.system.modular.package_log.service.LoginLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录日志（A-08 管理端）
 * 只读查询，不设删除/编辑入口，故不加 @AdminPermission
 */
@RestController
@RequestMapping(value = "/rest/admin/loginLog")
@Api(tags = "后台登录日志接口", description = "AdminLoginLogController")
public class AdminLoginLogController {

    @Autowired
    private LoginLogService loginLogService;

    @ApiOperation(value = "登录日志列表")
    @PostMapping(value = "/list")
    public BasicResult list(@RequestBody LoginLog param) {
        Page<LoginLog> page = loginLogService.getListByPage(param);
        return BasicResult.success(page);
    }
}