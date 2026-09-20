package com.siam.system.modular.package_log.aop;

import com.alibaba.fastjson.JSON;
import com.siam.system.modular.package_log.annoation.Log;
import com.siam.system.modular.package_log.entity.OperateLog;
import com.siam.system.modular.package_log.service.OperateLogService;
import com.siam.system.modular.package_user.auth.cache.AdminSessionManager;
import com.siam.system.modular.package_user.auth.cache.MerchantSessionManager;
import com.siam.system.modular.package_user.entity.Admin;
import com.siam.system.modular.package_log.util.LogUtil;
import com.siam.system.modular.package_user.entity.Merchant;
import com.siam.system.util.HttpContext;
import com.siam.system.util.TokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * 操作日志切面（A-08）
 *
 * 环绕所有标注 @Log 的方法，自动采集操作人/账号类型/请求信息/IP/UA/耗时/结果，
 * 并从 LogContext 读取业务侧写入的变更前后值，落库 tb_operate_log。
 *
 * 铁律：日志写入失败绝不影响主流程（内部 try/catch 吞掉并 log.error）；返回值与异常原样透传。
 *
 * 优先级最高（最外层）：挂载点所在 Controller 均带 @Transactional，
 * 若本切面处于事务内，业务异常回滚会连带把「失败日志」一起回滚掉；置于最外层后，
 * 事务先回滚、再由本切面写日志，保证失败记录（result=2）也能落库。
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OperateLogAop {

    private static final String ACCOUNT_ANON = "anon";

    private static final int MAX_ERROR_MSG_LEN = 500;
    private static final int MAX_UA_LEN = 255;
    private static final int MAX_IP_LEN = 50;

    @Autowired
    private OperateLogService operateLogService;

    @Autowired
    private AdminSessionManager adminSessionManager;

    @Autowired
    private MerchantSessionManager merchantSessionManager;

    @Around("@annotation(com.siam.system.modular.package_log.annoation.Log)")
    public Object around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        long startMs = System.currentTimeMillis();
        Throwable error = null;
        try {
            //业务语义原样透传：正常返回则返回，异常则继续抛出
            return proceedingJoinPoint.proceed();
        } catch (Throwable throwable) {
            error = throwable;
            throw throwable;
        } finally {
            try {
                saveLog(proceedingJoinPoint, error, System.currentTimeMillis() - startMs);
            } catch (Exception e) {
                //日志写入失败绝不影响主流程
                log.error("记录操作日志失败", e);
            } finally {
                LogContext.clear();
            }
        }
    }

    private void saveLog(ProceedingJoinPoint proceedingJoinPoint, Throwable error, long costMs) {
        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        Log anno = signature.getMethod().getAnnotation(Log.class);
        if (anno == null) {
            return;
        }

        OperateLog record = new OperateLog();
        record.setModule(anno.module());
        record.setModuleName(anno.moduleName());
        record.setAction(anno.action());

        //操作人 + 账号类型（取不到会话记 anon，且不抛异常）
        fillOperator(record);

        //请求信息
        HttpServletRequest request = HttpContext.getRequest();
        if (request != null) {
            record.setRequestMethod(request.getMethod());
            record.setRequestUrl(truncate(request.getRequestURI(), 255));
            record.setIp(truncate(LogUtil.getIp(request), MAX_IP_LEN));
            record.setUa(truncate(LogUtil.getUa(request), MAX_UA_LEN));
        }

        //变更前后值（业务侧通过 LogContext 显式写入）
        record.setBeforeValue(toJson(LogContext.getBefore()));
        record.setAfterValue(toJson(LogContext.getAfter()));

        //结果
        if (error == null) {
            record.setResult(OperateLog.RESULT_SUCCESS);
        } else {
            record.setResult(OperateLog.RESULT_FAIL);
            record.setErrorMsg(truncate(error.getMessage() != null ? error.getMessage() : error.toString(), MAX_ERROR_MSG_LEN));
        }

        record.setCostMs(costMs);
        record.setCreateTime(new Date());

        operateLogService.insert(record);
    }

    /**
     * 解析操作人与账号类型：优先管理员会话，其次商家会话，均取不到则 anon。
     * 管理端登录会桥接建立商家会话，故必须先判管理员，否则管理员操作会被误标为商家。
     */
    private void fillOperator(OperateLog record) {
        try {
            String token = TokenUtil.getToken();
            if (token != null) {
                Admin admin = adminSessionManager.getSession(token);
                if (admin != null) {
                    record.setOperator(admin.getUsername());
                    record.setAccountType(OperateLog.ACCOUNT_TYPE_ADMIN);
                    return;
                }
                Merchant merchant = merchantSessionManager.getSession(token);
                if (merchant != null) {
                    record.setOperator(merchant.getUsername());
                    record.setAccountType(OperateLog.ACCOUNT_TYPE_MERCHANT);
                    return;
                }
            }
        } catch (Exception e) {
            //无请求上下文 / 会话查询异常均降级为 anon，不影响主流程
            log.warn("获取操作人会话失败，降级为anon：{}", e.getMessage());
        }
        record.setOperator(ACCOUNT_ANON);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return JSON.toJSONString(value);
        } catch (Exception e) {
            log.warn("操作日志变更值序列化失败：{}", e.getMessage());
            return null;
        }
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLen ? value.substring(0, maxLen) : value;
    }
}