package com.siam.system.modular.package_log.service_impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.system.modular.package_log.entity.LoginLog;
import com.siam.system.modular.package_log.mapper.LoginLogMapper;
import com.siam.system.modular.package_log.service.LoginLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
     * 独立事务写入：登录失败时外层事务会因异常回滚，
     * 这里用 REQUIRES_NEW 挂起外层事务单独提交，保证失败记录也能落库
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insert(LoginLog record) {
        loginLogMapper.insert(record);
    }
}