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
        //同一用户的历史会话全部作废（与 Redis 版单 key 语义等价）
        MemberTokenExample deleteExample = new MemberTokenExample();
        deleteExample.createCriteria().andMemberIdEqualTo(member.getId());
        List<MemberToken> oldList = memberTokenService.selectByExample(deleteExample);
        if (oldList != null) {
            for (MemberToken old : oldList) {
                memberTokenService.deleteByToken(old.getToken());
            }
        }

        MemberToken record = new MemberToken();
        record.setMemberId(member.getId());
        record.setUsername(member.getUsername());
        record.setToken(token);
        record.setType("mvp");
        record.setCreateTime(new Date());
        memberTokenService.insertSelective(record);
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
