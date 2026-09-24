package com.siam.system.modular.package_points.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 会员等级阈值配置 · 积分管理后台可改。
 * 初始值对齐前端 utils/member-level.js（0/300/600/1000/2000，青铜~钻石）。
 * 等级判定：level = 满足 threshold <= 累计积分 的最高档。
 */
@Data
@TableName("tb_member_level_setting")
public class MemberLevelSetting {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 等级 1~5 */
    private Integer level;

    /** 等级名称（青铜/白银/黄金/铂金/钻石） */
    private String name;

    /** 累计积分达到该值即为该等级 */
    private Integer threshold;

    /** 等级权益说明 */
    private String benefits;

    private Date updateTime;
}
