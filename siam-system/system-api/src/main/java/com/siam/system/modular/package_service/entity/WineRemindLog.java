package com.siam.system.modular.package_service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 存酒临期提醒记录 · 批次 F · W-19
 *
 * <p>通知方式按用户拍板 = **小程序端弹窗提示**（不做短信）：
 * 顾客进入「我的存酒」页时，客户端检查临期/过期并弹窗，弹一次记一条（channel=1）。
 * 收银端 W-19 的「通知状态」列由此表驱动（「已弹窗 MM-dd」/「待提示」）。
 */
@Data
@TableName("tb_wine_remind_log")
public class WineRemindLog {

    /** 通知渠道：小程序弹窗 */
    public static final int CHANNEL_MINI_POPUP = 1;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer shopId;

    private Integer memberId;

    /** 存酒记录id（tb_wine_storage.id） */
    private Integer storageId;

    /** 通知渠道，见 CHANNEL_* */
    private Integer channel;

    /** 提醒时的有效期快照（过了 N 天再回看，能知道当时还剩多久） */
    private Date expireDateAtNotify;

    private Date createTime;
}
