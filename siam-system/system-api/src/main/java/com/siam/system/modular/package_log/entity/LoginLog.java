package com.siam.system.modular.package_log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 登录日志（A-08）
 *
 * 写入方：AdminServiceImpl/MerchantServiceImpl 登录方法（成功与失败都写入）
 */
@Data
@TableName("tb_login_log")
public class LoginLog {

    //登录结果 1=成功 2=失败
    public static final Integer RESULT_SUCCESS = 1;
    public static final Integer RESULT_FAIL = 2;

    //账号类型 1=管理员 2=商家 3=用户
    public static final Integer ACCOUNT_TYPE_ADMIN = 1;
    public static final Integer ACCOUNT_TYPE_MERCHANT = 2;
    public static final Integer ACCOUNT_TYPE_MEMBER = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号 */
    private String account;

    /** 账号类型 1=管理员 2=商家 3=用户 */
    private Integer accountType;

    /** 登录IP */
    private String ip;

    /** 登录地点(预留，暂不解析) */
    private String region;

    /** 浏览器 */
    private String browser;

    /** 操作系统 */
    private String os;

    /** 登录结果 1=成功 2=失败 */
    private Integer result;

    /** 提示信息(失败原因) */
    private String message;

    /** 创建时间 */
    private Date createTime;

    /* ##################################### START 扩展查询字段 #################################### */

    //页码
    @TableField(exist = false)
    private Integer pageNo = 1;

    //页面大小
    @TableField(exist = false)
    private Integer pageSize = 20;

    //开始日期
    @TableField(exist = false)
    private Date startCreateTime;

    //结束日期
    @TableField(exist = false)
    private Date endCreateTime;

    /* ##################################### END 扩展查询字段 #################################### */
}