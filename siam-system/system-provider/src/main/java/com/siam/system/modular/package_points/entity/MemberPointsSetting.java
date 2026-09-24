package com.siam.system.modular.package_points.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 积分获取规则配置 · 「积分管理」后台界面自定义（用户拍板：不硬编码）。
 * 业务侧只读 {@code getInt(key, defVal)}；比例类(×金额)与固定分值类同表存储。
 */
@Data
@TableName("tb_member_points_setting")
public class MemberPointsSetting {

    /** 消费积分比例：每实付1元得X分 */
    public static final String KEY_CONSUME_RATE = "consume_rate";
    /** 充值积分比例：每充值1元得X分 */
    public static final String KEY_RECHARGE_RATE = "recharge_rate";
    /** 新用户注册赠送积分 */
    public static final String KEY_REGISTER_POINTS = "register_points";
    /** 完成首单赠送积分 */
    public static final String KEY_FIRST_ORDER_POINTS = "first_order_points";
    /** 评价订单赠送积分 */
    public static final String KEY_APPRAISE_POINTS = "appraise_points";
    /** 邀请好友注册并下单赠送积分 */
    public static final String KEY_INVITE_POINTS = "invite_points";

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String itemKey;

    private Integer itemValue;

    private String itemDesc;

    private Date updateTime;
}
