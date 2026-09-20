package com.siam.system.modular.package_log.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.system.modular.package_log.entity.LoginLog;

/**
 * 登录日志Service（A-08）
 */
public interface LoginLogService {

    Page<LoginLog> getListByPage(LoginLog param);

    void insert(LoginLog record);
}