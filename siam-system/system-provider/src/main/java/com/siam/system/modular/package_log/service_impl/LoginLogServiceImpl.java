package com.siam.system.modular.package_log.service_impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.system.modular.package_log.entity.LoginLog;
import com.siam.system.modular.package_log.mapper.LoginLogMapper;
import com.siam.system.modular.package_log.service.LoginLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 登录日志Service实现（A-08）
 */
@Service
public class LoginLogServiceImpl implements LoginLogService {

    @Autowired
    private LoginLogMapper loginLogMapper;

    @Override
    public Page<LoginLog> getListByPage(LoginLog param) {
        return loginLogMapper.getListByPage(new Page(param.getPageNo(), param.getPageSize()), param);
    }

    /**
     * 登录日志写入。原先标注 @Transactional(REQUIRES_NEW)（失败日志独立提交），
     * 但调用方 controller 是类级 @Transactional——REQUIRES_NEW 会请求第二个连接：
     * 50 并发登录 × 2 连接/请求 > 连接池上限 → 池内互等（L4 压测实证的「大死」直接推手之一）。
     * 改为默认传播（有外层事务则并入）：代价是登录失败回滚时失败日志一并回滚，可接受。
     */
    @Override
    public void insert(LoginLog record) {
        loginLogMapper.insert(record);
    }

    @Override
    public List<LoginLog> exportList(LoginLog param) {
        return loginLogMapper.exportList(param);
    }
}