package com.siam.system.modular.package_goods.entity.internal;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/** 打印模板 · 批次 G · 收银端 W-21（P-05 双规格 58/80mm） */
@Data
@TableName("tb_print_template")
public class PrintTemplate {

    /** 小票类型（与 tb_print_task.ticket_type 同口径） */
    public static final int SCENE_ACCEPT = 1;     // 接单小票
    public static final int SCENE_PRODUCTION = 2; // 出品小票
    public static final int SCENE_CHECKOUT = 3;   // 结账小票
    public static final int SCENE_PICKUP = 4;     // 自提小票

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer shopId;

    /** 模板名，如「结账小票 58mm」 */
    private String name;

    /** 小票类型，见上方 SCENE_* */
    private Integer scene;

    /** 纸宽：58 / 80（画板「58 / 80mm 双规格」） */
    private Integer paperWidth;

    /** 页脚文字，如「感谢光临，请勿酒后驾车」 */
    private String footerText;

    /** 打印份数 */
    private Integer copies;

    /** 是否打印二维码（画板的「二维码」开关） */
    private Boolean showQr;

    /** 是否该类型的默认模板（同 shop + scene 下唯一由应用层保证） */
    private Boolean isDefault;

    private Date createTime;

    private Date updateTime;
}
