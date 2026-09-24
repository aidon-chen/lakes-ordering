package com.siam.system.modular.package_points.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 会员积分流水 · 会员积分等级体系（优惠券三类型前置工程 2026-09-23）
 *
 * <p>每笔积分获取/红冲一行，可审计。会员累计积分 = SUM(change_points)，
 * 等级由 {@link MemberLevelSetting} 阈值实时推导，不在 member 主表冗余存储。
 * 不复用 tb_member_billing_record（该表混余额/佣金/待结算语义，number 为金额口径）。
 */
@Data
@TableName("tb_member_points_log")
public class MemberPointsLog {

    /** 业务类型：消费（订单支付成功，按实付金额×consume_rate） */
    public static final int BIZ_TYPE_CONSUME = 1;
    /** 业务类型：活动（注册/首单/评价/邀请，分值走 tb_member_points_setting） */
    public static final int BIZ_TYPE_ACTIVITY = 2;
    /** 业务类型：充值（充值成功，按面额×recharge_rate） */
    public static final int BIZ_TYPE_RECHARGE = 3;
    /** 业务类型：退款红冲（负数，扣回原正向流水） */
    public static final int BIZ_TYPE_REDEEM = 4;
    /** 业务类型：后台调整（积分管理后台手工加/扣） */
    public static final int BIZ_TYPE_ADJUST = 5;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer memberId;

    /** 见 BIZ_TYPE_* */
    private Integer bizType;

    /** 关联业务单号（订单号/充值单号/活动标识），后台调整可空 */
    private String bizId;

    /** 变动积分，正加负减 */
    private Integer changePoints;

    /** 变动后累计积分快照 */
    private Integer balanceAfter;

    private String remark;

    private Date createTime;
}
