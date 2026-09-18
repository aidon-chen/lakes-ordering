package com.siam.system.modular.package_dining.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 开台会话表（tb_dining_session）：一次就餐的开台记录
 */
@Data
@TableName("tb_dining_session")
public class DiningSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer shopId;

    private Long tableId;

    /** 开台会员id（收银开台为空，扫码开台回填） */
    private Integer memberId;

    /** 就餐人数 */
    private Integer memberNum;

    /** 状态 1=进行中 2=已结台 3=已失效 */
    private Integer status;

    private Date openTime;

    private Date closeTime;

    private Date createTime;

    private Date updateTime;
}
