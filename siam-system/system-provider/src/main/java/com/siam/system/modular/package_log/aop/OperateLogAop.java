package com.siam.system.modular.package_log.aop;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.ValueFilter;
import com.siam.package_common.exception.PermissionException;
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
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 操作日志切面（A-08）
 *
 * 环绕所有标注 @Log 的方法，自动采集操作人/账号类型/操作类型/请求信息/IP/UA/耗时/结果，
 * 以及请求参数与返回结果（JSON 快照，敏感字段脱敏、超长截断），
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
    private static final int MAX_URL_LEN = 255;
    /** 请求参数/返回结果 JSON 快照的最大长度（DB 列是 text，截断是防止大对象把日志表撑爆） */
    private static final int MAX_JSON_LEN = 2000;

    /** 需脱敏的字段名（小写比较），不落明文到日志表 */
    private static final Set<String> SENSITIVE_KEYS = new HashSet<>(Arrays.asList(
            "password", "oldpassword", "newpassword", "paypassword", "confirmpassword",
            "mobilecode", "token", "idcard"));

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
        Object result = null;
        try {
            //业务语义原样透传：正常返回则返回，异常则继续抛出
            result = proceedingJoinPoint.proceed();
            return result;
        } catch (Throwable throwable) {
            error = throwable;
            throw throwable;
        } finally {
            try {
                //权限拦截（AdminPermissionAop/MerchantPermissionAop 的 @Before 抛 PermissionException）不属于业务操作，
                //落库会污染审计视图（同一请求在日志里既「无操作人」又「未进入业务」，无审计价值），故不记录
                if (!(error instanceof PermissionException)) {
                    saveLog(proceedingJoinPoint, error, result, System.currentTimeMillis() - startMs);
                }
            } catch (Exception e) {
                //日志写入失败绝不影响主流程
                log.error("记录操作日志失败", e);
            } finally {
                LogContext.clear();
            }
        }
    }

    private void saveLog(ProceedingJoinPoint proceedingJoinPoint, Throwable error, Object result, long costMs) {
        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        Log anno = signature.getMethod().getAnnotation(Log.class);
        if (anno == null) {
            return;
        }

        OperateLog record = new OperateLog();
        record.setModule(anno.module());
        record.setModuleName(anno.moduleName());
        record.setAction(anno.action());
        record.setBusinessType(anno.businessType());

        //操作人 + 账号类型（取不到会话记 anon，且不抛异常）
        fillOperator(record);

        //请求信息
        HttpServletRequest request = HttpContext.getRequest();
        if (request != null) {
            record.setRequestMethod(request.getMethod());
            record.setRequestUrl(truncate(resolveUri(request), MAX_URL_LEN));
            record.setIp(truncate(LogUtil.getIp(request), MAX_IP_LEN));
            record.setUa(truncate(LogUtil.getUa(request), MAX_UA_LEN));
        }

        //请求参数 / 返回结果（JSON 快照）
        record.setRequestParam(buildRequestParam(proceedingJoinPoint));
        record.setResponseResult(toJson(result));

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

    /**
     * 采集请求参数（按方法形参名组织成 Map 后序列化）。
     * 跳过 Servlet 对象、文件上传对象与参数校验结果：它们无法（也不应）序列化。
     */
    private String buildRequestParam(ProceedingJoinPoint proceedingJoinPoint) {
        Object[] args = proceedingJoinPoint.getArgs();
        if (args == null || args.length == 0) {
            return null;
        }
        String[] paramNames = ((MethodSignature) proceedingJoinPoint.getSignature()).getParameterNames();

        Map<String, Object> params = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == null || isNotSerializable(arg)) {
                continue;
            }
            params.put(paramNames != null && paramNames.length > i ? paramNames[i] : ("arg" + i), arg);
        }
        return params.isEmpty() ? null : toJson(params);
    }

    private boolean isNotSerializable(Object arg) {
        return arg instanceof ServletRequest
                || arg instanceof ServletResponse
                || arg instanceof MultipartFile
                || arg instanceof MultipartFile[]
                || arg instanceof BindingResult;
    }

    /**
     * 取不含上下文前缀的请求路径：本地 /siam-server/rest/... 与云端 /rest/... 统一口径
     */
    private String resolveUri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (uri != null && contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }

    /**
     * 序列化为 JSON：敏感字段脱敏 + 超长截断；序列化失败返回 null（不影响主流程）
     */
    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            String json = JSON.toJSONString(value, (ValueFilter) (object, name, fieldValue) ->
                    (name != null && SENSITIVE_KEYS.contains(name.toLowerCase())) ? "***" : fieldValue);
            return truncate(json, MAX_JSON_LEN);
        } catch (Exception e) {
            log.warn("操作日志JSON序列化失败：{}", e.getMessage());
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