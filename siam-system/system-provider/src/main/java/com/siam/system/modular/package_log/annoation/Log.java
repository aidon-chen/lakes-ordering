package com.siam.system.modular.package_log.annoation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解（A-08）
 *
 * 标注在需要记录操作日志的方法上，由 OperateLogAop 环绕切面自动采集：
 * 操作人/账号类型/请求信息/IP/UA/耗时/结果/异常，并通过 LogContext 读取业务侧写入的变更前后值。
 *
 * 变更前后值由业务方法在方法体内通过 package_log/aop/LogContext.setBefore/setAfter 显式写入。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Log {

    /* ##################################### START 模块code常量 #################################### */

    /** 登录 */
    String MODULE_LOGIN = "LOGIN";
    /** 商品价格 */
    String MODULE_GOODS_PRICE = "GOODS_PRICE";
    /** 订单操作 */
    String MODULE_ORDER = "ORDER";
    /** 库存变更 */
    String MODULE_STOCK = "STOCK";
    /** 开台 */
    String MODULE_TABLE = "TABLE";
    /** 存酒 */
    String MODULE_WINE_STORAGE = "WINE_STORAGE";

    /* ##################################### END 模块code常量 #################################### */

    /** 模块code */
    String module();

    /** 模块名称 */
    String moduleName();

    /** 动作 */
    String action();
}