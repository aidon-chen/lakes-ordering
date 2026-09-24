package com.siam.system.modular.package_goods.entity.internal;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 门店营业设置（键值对）· 批次 G · 收银端 W-23
 *
 * <p>为什么复用键值对而不是给 tb_shop 加一堆列：W-23 的 9 项设置彼此独立、且后续还会增，
 * 加列会让 tb_shop 这个高频实体不断变更（本季已因此出过一次线上事故）；
 * 键值对把变更限制在数据层，不动实体结构。
 */
@Data
@TableName("tb_shop_setting")
public class ShopSetting {

    /** 业务键常量（前后端约定，勿改字面量） */
    public static final String KEY_BUSINESS_OPEN = "businessOpen";
    public static final String KEY_ORDER_VOICE_ALERT = "orderVoiceAlert";
    public static final String KEY_VOICE_VOLUME = "voiceVolume";
    public static final String KEY_AUTO_ACCEPT = "autoAccept";
    public static final String KEY_PRODUCTION_TIMEOUT_MIN = "productionTimeoutMin";
    public static final String KEY_CLOUD_PRINT = "cloudPrint";
    public static final String KEY_LOCAL_PRINT = "localPrint";
    public static final String KEY_PRINT_MODE = "printMode";
    public static final String KEY_DEFAULT_PRINTER_ID = "defaultPrinterId";

    /* 打印重试策略（W-25 策略卡 · 4.5 可配置） */
    public static final String KEY_PRINT_RETRY_MAX = "printRetryMax";
    public static final String KEY_PRINT_RETRY_INTERVAL_SEC = "printRetryIntervalSec";
    public static final String KEY_PRINT_FAIL_ALERT = "printFailAlert";
    /** 存酒临期提醒：到期前 N 天提醒（W-19） */
    public static final String KEY_WINE_REMIND_AHEAD_DAYS = "wineRemindAheadDays";

    /* 营业时段（叠加设计 · 白天咖啡/夜间酒水） */
    /** 时段总开关：true=按 salePeriodRules 过滤分类与商品；false/缺省=退回现状（不按时段过滤） */
    public static final String KEY_SALE_PERIOD_ENABLED = "salePeriodEnabled";
    /** 时段窗口：HH:mm-HH:mm=code，多段用【英文逗号】分隔（值内禁用分号，会被脚本/控制台语句切分截断） */
    public static final String KEY_SALE_PERIOD_RULES = "salePeriodRules";

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer shopId;

    /** 设置项键（见上方常量） */
    private String settingKey;

    /** 设置项值：统一存字符串，前端按类型解析（布尔 "true"/"false"、数字、id） */
    private String settingValue;

    private Date createTime;

    private Date updateTime;
}
