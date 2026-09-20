package com.siam.system.modular.package_log.aop;

/**
 * 操作日志上下文（A-08）
 *
 * 业务代码在标注了 @Log 的方法体内显式调用，记录变更前后值；
 * 由 OperateLogAop 在方法执行结束后读取并序列化写入 tb_operate_log，最后 finally 清理。
 *
 * 用法示例：
 *   LogContext.setBefore(oldGoods);
 *   ...业务修改...
 *   LogContext.setAfter(newGoods);
 */
public class LogContext {

    private static final ThreadLocal<Object> BEFORE = new ThreadLocal<>();
    private static final ThreadLocal<Object> AFTER = new ThreadLocal<>();

    /**
     * 记录变更前值（可用旧值时就查库取旧值）
     */
    public static void setBefore(Object before) {
        BEFORE.set(before);
    }

    /**
     * 记录变更后值
     */
    public static void setAfter(Object after) {
        AFTER.set(after);
    }

    public static Object getBefore() {
        return BEFORE.get();
    }

    public static Object getAfter() {
        return AFTER.get();
    }

    /**
     * 清理，避免线程复用（Tomcat 线程池）导致的脏数据
     */
    public static void clear() {
        BEFORE.remove();
        AFTER.remove();
    }
}