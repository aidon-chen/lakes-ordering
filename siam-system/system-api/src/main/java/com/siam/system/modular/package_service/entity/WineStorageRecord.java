package com.siam.system.modular.package_service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 取酒记录表（tb_wine_storage_record）· 对应原型 C-16
 *
 * 注意：本表属本批次新增，执行 update.sql §7 前库中不存在。
 * 仅商家端「取酒」动作写入，顾客侧不展示时间线（原型无此板）。
 */
@Data
@TableName("tb_wine_storage_record")
public class WineStorageRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 存酒记录id（tb_wine_storage.id） */
    private Long storageId;

    /** 本次取酒瓶数 */
    private Integer takeCount;

    /** 操作人id（商家端） */
    private Integer operatorId;

    private String remark;

    /** 取酒时间 */
    private Date takeTime;

    private Date createTime;

    private Date updateTime;
}
