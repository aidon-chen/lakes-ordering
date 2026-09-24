package com.siam.system.modular.package_user.service;

import com.siam.system.modular.package_user.entity.MerchantToken;
import com.siam.system.modular.package_user.model.example.MerchantTokenExample;

import java.util.List;

public interface MerchantTokenService {
    int countByExample(MerchantTokenExample example);

    void insertSelective(MerchantToken record);

    List<MerchantToken> selectByExample(MerchantTokenExample example);

    MerchantToken selectByPrimaryKey(Integer id);

    void updateByExampleSelective(MerchantToken record, MerchantTokenExample example);

    void updateByPrimaryKeySelective(MerchantToken record);

    void deleteByToken(String token);

    /** 会话单语句 upsert（依赖账号唯一索引），见 MerchantTokenMapper#upsertSession */
    void upsertSession(MerchantToken record);

    /**
     * 获取当前登录商家账号信息
     * @return
     **/
    MerchantToken getLoginMerchantInfo(String token);
}