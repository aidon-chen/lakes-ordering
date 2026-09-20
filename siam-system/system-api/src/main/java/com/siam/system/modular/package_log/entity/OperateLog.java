package com.siam.system.modular.package_log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 操作日志（A-08）
 *
 * 写入方：package_log/aop/OperateLogAop（@Log 注解环绕切面自动采集）
 * 变更前后值：业务代码通过 package_log/aop/LogContext 显式写入
 */
@Data
@TableName("tb_operate_log")
public class OperateLog {

    //操作结果 1=成功 2=失败
    public static final Integer RESULT_SUCCESS = 1;
    public static final Integer RESULT_FAIL = 2;

    //账号类型 1=管理员 2=商家 3=用户
    public static final Integer ACCOUNT_TYPE_ADMIN = 1;
    public static final Integer ACCOUNT_TYPE_MERCHANT = 2;
    public static final Integer ACCOUNT_TYPE_MEMBER = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人账号 */
    private String operator;

    /** 账号类型 1=管理员 2=商家 3=用户 */
    private Integer accountType;

    /** 模块code */
    private String module;

    /** 模块名称 */
    private String moduleName;

    /** 动作 */
    private String action;

    /** 业务ID */
    private String businessId;

    /** 请求方式 */
    private String requestMethod;

    /** 请求地址 */
    private String requestUrl;

    /** 变更前值(JSON) */
    private String beforeValue;

    /** 变更后值(JSON) */
    private String afterValue;

    /** 操作结果 1=成功 2=失败 */
    private Integer result;

    /** 失败原因 */
    private String errorMsg;

    /** 操作IP */
    private String ip;

    /** User-Agent */
    private String ua;

    /** 耗时(毫秒) */
    private Long costMs;

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