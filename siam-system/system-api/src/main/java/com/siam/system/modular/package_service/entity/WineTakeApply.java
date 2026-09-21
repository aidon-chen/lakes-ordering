package com.siam.system.modular.package_service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 取酒申请表（tb_wine_take_apply）· 方案 v1.1
 *
 * <p>单一模型：申请 → 取酒码 → 核销。商家侧能力＝「代发起取酒」，不做直接扣减；
 * 每一次扣瓶都对应一笔取酒申请单。
 *
 * <p>状态：1 待核销 / 2 已核销 / 3 已取消 / 4 已过期（1 起步，与 tb_wine_storage 同惯例）
 * <p>来源：1 顾客自助 / 2 商家代发起
 */
@Data
@TableName("tb_wine_take_apply")
public class WineTakeApply {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会员id */
    private Integer memberId;

    /** 门店id */
    private Integer shopId;

    /** 存酒记录id（tb_wine_storage.id） */
    private Long storageId;

    /** 申请取酒瓶数 */
    private Integer takeCount;

    /** 一次性取酒码（6 位随机，首位非 0） */
    private String takeCode;

    /** 状态 1待核销 2已核销 3已取消 4已过期 */
    private Integer status;

    /** 来源 1顾客自助 2商家代发起 */
    private Integer applySource;

    /** 代发起人（商家id）；顾客自助为 null */
    private Integer applyBy;

    /** 申请时间 */
    private Date applyTime;

    /** 取酒码有效期截止（apply_time + 24h） */
    private Date expireTime;

    /** 核销时间 */
    private Date confirmTime;

    /** 核销操作人（商家id） */
    private Integer confirmBy;

    /** 取消时间 */
    private Date cancelTime;

    /** 备注（商家代发起可填） */
    private String remark;

    private Date createTime;

    private Date updateTime;
}
