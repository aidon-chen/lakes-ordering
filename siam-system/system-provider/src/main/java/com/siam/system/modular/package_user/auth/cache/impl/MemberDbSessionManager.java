package com.siam.system.modular.package_user.auth.cache.impl;

import com.siam.system.modular.package_user.auth.cache.MemberSessionManager;
import com.siam.system.modular.package_user.entity.Member;
import com.siam.system.modular.package_user.entity.MemberToken;
import com.siam.system.modular.package_user.model.example.MemberTokenExample;
import com.siam.system.modular.package_user.service.MemberService;
import com.siam.system.modular.package_user.service.MemberTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 会话管理-DB实现：会话落 tb_member_token（云托管无 Redis 环境，见 04-deploy-cost/mvp-migration/02）
 * 语义与 Redis 版对齐：有效期固定 7*24 小时，自创建时刻起算
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "siam.session.store", havingValue = "db", matchIfMissing = true)
public class MemberDbSessionManager implements MemberSessionManager {

    private static final long EXPIRE_MILLIS = 7 * 24 * 60 * 60 * 1000L;


    @Autowired
    private MemberTokenService memberTokenService;

    @Autowired
    private MemberService memberService;

    @Override
    public void createSession(String token, Member member) {
        doCreateSessionWithRetry(token, member);
    }

    private void doCreateSessionWithRetry(String token, Member member) {
        //残余死锁兜底：跨账号偶发死锁时 InnoDB 回滚牺牲者（MySQL 建议 restart transaction）→ 有界重试
        int maxAttempts = 4;
        for (int attempt = 1; ; attempt++) {
            try {
                doCreateSession(token, member);
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

    private void doCreateSession(String token, Member member) {
        //单语句 upsert：依赖 member_id 唯一索引，落库即「踢旧会话」，与 Redis 版单 key 语义等价；
        //无 select+delete+insert 多语句交叉 → 从根上消除并发同账号登录的 InnoDB 死锁环（L4 压测实证）
        MemberToken record = new MemberToken();
        record.setMemberId(member.getId());
        record.setUsername(member.getUsername());
        record.setToken(token);
        record.setType("mvp");
        record.setCreateTime(new Date());
        memberTokenService.upsertSession(record);
    }

    @Override
    public Member getSession(String token) {
        if (token == null) {
            return null;
        }
        MemberTokenExample example = new MemberTokenExample();
        example.createCriteria().andTokenEqualTo(token);
        List<MemberToken> list = memberTokenService.selectByExample(example);
        if (list == null || list.isEmpty()) {
            return null;
        }
        MemberToken record = list.get(0);
        if (record.getCreateTime() == null
                || System.currentTimeMillis() - record.getCreateTime().getTime() > EXPIRE_MILLIS) {
            //过期即清理（惰性删除，与 Redis 版固定 TTL 语义一致）
            memberTokenService.deleteByToken(token);
            return null;
        }
        return memberService.selectByPrimaryKey(record.getMemberId());
    }

    @Override
    public void removeSession(String token) {
        memberTokenService.deleteByToken(token);
    }

    @Override
    public boolean haveSession(String token) {
        return getSession(token) != null;
    }
}
