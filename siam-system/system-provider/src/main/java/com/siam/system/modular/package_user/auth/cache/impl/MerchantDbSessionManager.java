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
import org.springframework.dao.CannotAcquireLockException;
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
        doCreateSessionWithRetry(token, merchant);
    }

    private void doCreateSessionWithRetry(String token, Merchant merchant) {
        //残余死锁兜底：跨账号偶发死锁时 InnoDB 回滚牺牲者（MySQL 建议 restart transaction）→ 有界重试
        int maxAttempts = 4;
        for (int attempt = 1; ; attempt++) {
            try {
                doCreateSession(token, merchant);
                return;
            } catch (CannotAcquireLockException e) {
                if (attempt >= maxAttempts) {
                    throw e;
                }
                try {
                    Thread.sleep(20L * attempt + (long) (Math.random() * 30));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    private void doCreateSession(String token, Merchant merchant) {
        //单语句 upsert：依赖 merchant_id 唯一索引，落库即「踢旧会话」，与 Redis 版单 key 语义等价；
        //无 select+delete+insert 多语句交叉 → 从根上消除并发同账号登录的 InnoDB 死锁环（L4 压测实证）
        MerchantToken record = new MerchantToken();
        record.setMerchantId(merchant.getId());
        record.setUsername(merchant.getUsername());
        record.setToken(token);
        record.setType("mvp");
        record.setCreateTime(new Date());
        merchantTokenService.upsertSession(record);
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
