package com.login.common.utils;

import javax.servlet.http.HttpServletRequest;

public class HttpServletUtils {

    public static boolean jsAjax(HttpServletRequest req){
        //判断是否为ajax请求，默认不是
        boolean isAjaxRequest = false;
        if(!StringUtils.isBlank(req.getHeader("x-requested-with")) && req.getHeader("x-requested-with").equals("XMLHttpRequest")){
            isAjaxRequest = true;
        }
        return isAjaxRequest;
    }

    public static String getContentHead(HttpServletRequest request){
        String fullPath = request.getRequestURL().toString();
        int tail    = request.getRequestURI().length();
        String head = fullPath.substring(0,fullPath.length() - tail);
        return head;
    }

    public static String getRealIp(HttpServletRequest request){
        String ip = request.getHeader("X-Real-IP");
        if (!org.apache.commons.lang.StringUtils.isBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        ip = request.getHeader("X-Forwarded-For");
        if (!org.apache.commons.lang.StringUtils.isBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            // 多次反向代理后会有多个IP值，第一个为真实IP。
            int index = ip.indexOf(',');
            if (index != -1) {
                ip = ip.substring(0, index);
            }
            return ip;
        }
        ip = request.getHeader("Proxy-Client-IP");
        if (!org.apache.commons.lang.StringUtils.isBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        ip = request.getHeader("WL-Proxy-Client-IP");
        if (!org.apache.commons.lang.StringUtils.isBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        ip = request.getHeader("HTTP_CLIENT_IP");
        if (!org.apache.commons.lang.StringUtils.isBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (!org.apache.commons.lang.StringUtils.isBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
