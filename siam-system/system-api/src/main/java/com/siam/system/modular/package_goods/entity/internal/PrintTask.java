package com.siam.system.modular.package_goods.entity.internal;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 打印任务（队列 + 补打记录）· 批次 G · 收银端 W-25 / W-21
 *
 * <p>一张表同时承担两件事，避免重复建模：
 * <ul>
 *   <li><b>W-25 队列与告警</b>：按 status 过滤（1待打印/4失败→「待重试」），配 retry_count/next_retry_time 做重试</li>
 *   <li><b>W-21 补打记录</b>：整表即「打印尝试流水」，is_reprint=true 即补打，operator_id 即操作人</li>
 * </ul>
 */
@Data
@TableName("tb_print_task")
public class PrintTask {

    public static final int STATUS_WAIT = 1;      // 待打印
    public static final int STATUS_DOING = 2;     // 打印中
    public static final int STATUS_SUCCESS = 3;   // 成功
    public static final int STATUS_FAIL = 4;      // 失败（可重试）
    public static final int STATUS_IGNORED = 5;   // 已忽略（人工放弃）

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer shopId;

    /** 业务单号（堂食 SO… / 自提 PK…） */
    private String orderNo;

    /** 小票类型，同 PrintTemplate.SCENE_* */
    private Integer ticketType;

    private Integer printerId;

    private Integer templateId;

    private Integer copies;

    /** 打印内容快照（渲染后文本；打印不依赖业务表，防止事后被改） */
    private String content;

    private Integer status;

    /** 已重试次数（画板「2 / 3」） */
    private Integer retryCount;

    /** 最大重试次数（来自门店设置 printRetryMax） */
    private Integer maxRetry;

    /** 失败原因（画板「打印机缺纸」） */
    private String failReason;

    /** 下发时间（画板「下发时间」） */
    private Date dispatchTime;

    /** 下次重试时间（队列重试用） */
    private Date nextRetryTime;

    /** 打印成功时间 */
    private Date printTime;

    /** 操作人（补打时记录是谁补的） */
    private Integer operatorId;

    /** 是否补打（画板「补打 / 重打记录」） */
    private Boolean isReprint;

    private Date createTime;

    private Date updateTime;
}
