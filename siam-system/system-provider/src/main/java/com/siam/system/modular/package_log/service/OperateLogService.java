package com.siam.system.modular.package_log.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.system.modular.package_log.entity.OperateLog;

import java.util.List;

/**
 * 操作日志Service（A-08）
 */
public interface OperateLogService {

    Page<OperateLog> getListByPage(OperateLog param);

    OperateLog selectById(Long id);

    void insert(OperateLog record);

    /**
     * 全量查询（不分页），用于导出
     */
    List<OperateLog> exportList(OperateLog param);
}