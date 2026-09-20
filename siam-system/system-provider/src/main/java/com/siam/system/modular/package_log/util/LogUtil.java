package com.siam.system.modular.package_log.util;

import com.siam.system.modular.package_log.entity.LoginLog;
import com.siam.system.util.HttpContext;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * 日志采集工具（A-08）
 *
 * 统一 IP/UA 采集与浏览器、操作系统解析口径（不引入 eu.bitwalker 等新依赖，用字符串包含判断）。
 * 操作日志切面与登录日志写入共用。
 */
public class LogUtil {

    /**
     * 获取客户端IP：X-Forwarded-For 优先，回退 X-Real-IP，最后 getRemoteAddr
     */
    public static String getIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (isValidIp(ip)) {
            //多级代理时取第一个IP
            int index = ip.indexOf(',');
            return index > 0 ? ip.substring(0, index).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (isValidIp(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    public static String getUa(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return request.getHeader("User-Agent");
    }

    /**
     * 从 UA 简单解析浏览器
     */
    public static String parseBrowser(String ua) {
        if (ua == null || ua.length() == 0) {
            return null;
        }
        if (ua.contains("MicroMessenger")) {
            return "微信内置浏览器";
        }
        if (ua.contains("Edg/")) {
            return "Edge";
        }
        if (ua.contains("OPR/") || ua.contains("Opera")) {
            return "Opera";
        }
        if (ua.contains("Chrome/") || ua.contains("CriOS")) {
            return "Chrome";
        }
        if (ua.contains("Firefox/") || ua.contains("FxiOS")) {
            return "Firefox";
        }
        if (ua.contains("Safari/")) {
            return "Safari";
        }
        if (ua.contains("MSIE") || ua.contains("Trident")) {
            return "IE";
        }
        return "其他";
    }

    /**
     * 从 UA 简单解析操作系统
     */
    public static String parseOs(String ua) {
        if (ua == null || ua.length() == 0) {
            return null;
        }
        if (ua.contains("iPhone") || ua.contains("iPad") || ua.contains("iOS")) {
            return "iOS";
        }
        if (ua.contains("Android")) {
            return "Android";
        }
        if (ua.contains("Windows")) {
            return "Windows";
        }
        if (ua.contains("Mac OS X") || ua.contains("Macintosh")) {
            return "macOS";
        }
        if (ua.contains("Linux")) {
            return "Linux";
        }
        return "其他";
    }

    /**
     * 构建登录日志：IP/UA 取不到则留空，浏览器/操作系统从 UA 简单解析
     */
    public static LoginLog buildLoginLog(String account, Integer accountType, Integer result, String message) {
        LoginLog record = new LoginLog();
        record.setAccount(account);
        record.setAccountType(accountType);
        record.setResult(result);
        record.setMessage(truncate(message, 255));
        record.setCreateTime(new Date());
        try {
            HttpServletRequest request = HttpContext.getRequest();
            if (request != null) {
                String ua = getUa(request);
                record.setIp(truncate(getIp(request), 50));
                record.setBrowser(truncate(parseBrowser(ua), 50));
                record.setOs(truncate(parseOs(ua), 50));
            }
        } catch (Exception e) {
            //拿不到请求上下文时仅留空，不影响登录主流程
        }
        return record;
    }

    private static boolean isValidIp(String ip) {
        return ip != null && ip.trim().length() > 0 && !"unknown".equalsIgnoreCase(ip.trim());
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLen ? value.substring(0, maxLen) : value;
    }
}