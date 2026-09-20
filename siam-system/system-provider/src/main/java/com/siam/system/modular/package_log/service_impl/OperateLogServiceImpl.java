package com.siam.system.modular.package_log.service_impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.system.modular.package_log.entity.OperateLog;
import com.siam.system.modular.package_log.mapper.OperateLogMapper;
import com.siam.system.modular.package_log.service.OperateLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志Service实现（A-08）
 */
@Service
public class OperateLogServiceImpl implements OperateLogService {

    @Autowired
    private OperateLogMapper operateLogMapper;

    @Override
    public Page<OperateLog> getListByPage(OperateLog param) {
        return operateLogMapper.getListByPage(new Page(param.getPageNo(), param.getPageSize()), param);
    }

    @Override
    public OperateLog selectById(Long id) {
        return operateLogMapper.selectById(id);
    }

    @Override
    public void insert(OperateLog record) {
        operateLogMapper.insert(record);
    }

    @Override
    public List<OperateLog> exportList(OperateLog param) {
        return operateLogMapper.exportList(param);
    }
}