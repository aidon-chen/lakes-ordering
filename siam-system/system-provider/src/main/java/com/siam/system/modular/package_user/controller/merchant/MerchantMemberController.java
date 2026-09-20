package com.siam.system.modular.package_user.controller.merchant;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.siam.package_common.entity.BasicResult;
import com.siam.package_common.exception.StoneCustomerException;
import com.siam.system.modular.package_user.entity.Member;
import com.siam.system.modular.package_user.mapper.MemberMapper;
import com.siam.system.modular.package_user.model.param.MemberParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 会员查询（商家侧）· 批次 F
 *
 * <p>用途：收银端 **W-17 存酒登记** 的「手机号 → 查询」按钮 —— 先确认该手机号对应会员存在、
 * 并把姓名与**脱敏**手机号带出到表单与凭证预览，避免填完整张表才在提交时报「未找到该会员」。
 *
 * <p>鉴权：/rest/merchant/** 由 merchantInterceptor 统一保护（本类不再重复校验）。
 * 会员是全局对象（不按门店隔离），故不引入 shopId 过滤。
 */
@RestController
@RequestMapping(value = "/rest/merchant/member")
@Transactional(rollbackFor = Exception.class)
public class MerchantMemberController {

    @Autowired
    private MemberMapper memberMapper;

    /**
     * 按手机号查会员（精确）
     *
     * @return { id, nickname, name, mobile(已脱敏) }
     */
    @PostMapping(value = "/selectByMobile")
    public BasicResult selectByMobile(@RequestBody MemberParam param) {
        String mobile = param.getMobile() == null ? null : param.getMobile().trim();
        if (mobile == null || mobile.isEmpty()) {
            throw new StoneCustomerException("手机号不能为空");
        }
        Member m = memberMapper.selectOne(
                new QueryWrapper<Member>().eq("mobile", mobile).last("LIMIT 1"));
        if (m == null) {
            throw new StoneCustomerException("未找到该手机号对应的会员");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", m.getId());
        data.put("nickname", m.getNickname());
        data.put("name", m.getRealName());
        // 只回脱敏号：收银端用于当面核对，不需要完整号码
        data.put("mobile", maskMobile(mobile));
        return BasicResult.success(data);
    }

    /** 手机号脱敏：138****8601；非 11 位原样返回 */
    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 11) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(7);
    }
}
