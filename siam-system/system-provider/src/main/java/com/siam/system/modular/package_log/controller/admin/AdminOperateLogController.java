package com.siam.system.modular.package_log.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.package_common.entity.BasicResult;
import com.siam.package_common.exception.StoneCustomerException;
import com.siam.system.modular.package_log.entity.OperateLog;
import com.siam.system.modular.package_log.service.OperateLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志（A-08 管理端）
 * 只读查询，不设删除/编辑入口，故不加 @AdminPermission
 */
@RestController
@RequestMapping(value = "/rest/admin/operateLog")
@Api(tags = "后台操作日志接口", description = "AdminOperateLogController")
public class AdminOperateLogController {

    @Autowired
    private OperateLogService operateLogService;

    @ApiOperation(value = "操作日志列表")
    @PostMapping(value = "/list")
    public BasicResult list(@RequestBody OperateLog param) {
        Page<OperateLog> page = operateLogService.getListByPage(param);
        return BasicResult.success(page);
    }

    @ApiOperation(value = "操作日志详情")
    @PostMapping(value = "/selectById")
    public BasicResult selectById(@RequestBody OperateLog param) {
        OperateLog dbLog = operateLogService.selectById(param.getId());
        if (dbLog == null) {
            throw new StoneCustomerException("该日志不存在");
        }
        return BasicResult.success(dbLog);
    }

    @ApiOperation(value = "操作日志导出（全量，不分页）")
    @PostMapping(value = "/export")
    public BasicResult export(@RequestBody OperateLog param) {
        return BasicResult.success(operateLogService.exportList(param));
    }
}