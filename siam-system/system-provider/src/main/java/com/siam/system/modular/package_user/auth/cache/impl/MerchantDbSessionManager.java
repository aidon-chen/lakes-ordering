package com.siam.system.modular.package_user.auth.cache.impl;

import com.siam.system.modular.package_user.auth.cache.MerchantSessionManager;
import com.siam.system.modular.package_user.entity.Merchant;
import com.siam.system.modular.package_user.entity.MerchantToken;
import com.siam.system.modular.package_user.model.example.MerchantTokenExample;
import com.siam.system.modular.package_user.service.MerchantService;
import com.siam.system.modular.package_user.service.MerchantTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 会话管理-DB实现：会话落 tb_merchant_token（云托管无 Redis 环境，见 04-deploy-cost/mvp-migration/02）
 * 语义与 Redis 版对齐：有效期固定 7*24 小时，自创建时刻起算
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "siam.session.store", havingValue = "db", matchIfMissing = true)
public class MerchantDbSessionManager implements MerchantSessionManager {

    private static final long EXPIRE_MILLIS = 7 * 24 * 60 * 60 * 1000L;

    @Autowired
    private MerchantTokenService merchantTokenService;

    @Autowired
    private MerchantService merchantService;

    @Override
    public void createSession(String token, Merchant merchant) {
        //同一商家的历史会话全部作废（与 Redis 版单 key 语义等价）
        MerchantTokenExample deleteExample = new MerchantTokenExample();
        deleteExample.createCriteria().andMerchantIdEqualTo(merchant.getId());
        List<MerchantToken> oldList = merchantTokenService.selectByExample(deleteExample);
        if (oldList != null) {
            for (MerchantToken old : oldList) {
                merchantTokenService.deleteByToken(old.getToken());
            }
        }

        MerchantToken record = new MerchantToken();
        record.setMerchantId(merchant.getId());
        record.setUsername(merchant.getUsername());
        record.setToken(token);
        record.setType("mvp");
        record.setCreateTime(new Date());
        merchantTokenService.insertSelective(record);
    }

    @Override
    public Merchant getSession(String token) {
        if (token == null) {
            return null;
        }
        MerchantTokenExample example = new MerchantTokenExample();
        example.createCriteria().andTokenEqualTo(token);
        List<MerchantToken> list = merchantTokenService.selectByExample(example);
        if (list == null || list.isEmpty()) {
            return null;
        }
        MerchantToken record = list.get(0);
        if (record.getCreateTime() == null
                || System.currentTimeMillis() - record.getCreateTime().getTime() > EXPIRE_MILLIS) {
            //过期即清理（惰性删除，与 Redis 版固定 TTL 语义一致）
            merchantTokenService.deleteByToken(token);
            return null;
        }
        return merchantService.selectByPrimaryKey(record.getMerchantId());
    }

    @Override
    public void removeSession(String token) {
        merchantTokenService.deleteByToken(token);
    }

    @Override
    public boolean haveSession(String token) {
        return getSession(token) != null;
    }
}
