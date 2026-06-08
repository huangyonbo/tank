package com.login.common.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.regex.Pattern;

/**
 * @author bootdo
 */
public class StringUtils extends org.apache.commons.lang3.StringUtils{

    public static String getRandomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        int len = str.length();
        for (int i = 0; i < length; i++) {
            sb.append(str.charAt(random.nextInt(len)));
        }
        return sb.toString();
    }

    public static boolean isInteger(String str) {
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        return pattern.matcher(str).matches();
    }

    public static String getMessageCode() {
        String tmp = Math.random()+"";
        return tmp.substring(tmp.length() - 6);
    }

    public static String getHuaweiSignContent(SortedMap<String, String> sortedMap) {
        StringBuffer base = new StringBuffer();
        //获取计算nsp_key的基础串
        try {
            for (Map.Entry<String, String> entry : sortedMap.entrySet()) {
                String k = entry.getKey();
                String v = entry.getValue();
                base.append(k).append("=").append(URLEncoder.encode(v, "UTF-8")).append("&");
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String body = base.toString().substring(0, base.toString().length() - 1);
        //空格和星号转义
        body = body.replaceAll("\\+", "%20").replaceAll("\\*", "%2A");
        return body;
    }

    public static String getSignContent(SortedMap<Object, Object> sortedMap) {
        if (sortedMap.size() == 0) {
            return "";
        }
        StringBuilder sbkey = new StringBuilder();
        Set es = sortedMap.entrySet();
        for (Object e : es) {
            Map.Entry entry = (Map.Entry) e;
            String k = (String) entry.getKey();
            Object v = entry.getValue();
            //2.空值不传递，不参与签名组串
            if (null != v && !"".equals(v) && !"sign".equals(k) && !"signMethod".equals(k)
                    && !"sign_type".equals(k) && !"signature".equals(k)) {
                sbkey.append(k).append("=").append(v).append("&");
            }
        }
        String signStr = sbkey.toString();
        signStr = signStr.substring(0, signStr.length() - 1); // 去掉最后一个&
        return signStr;
    }
}
