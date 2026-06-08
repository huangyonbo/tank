package com.login.common.utils;

import cn.hutool.crypto.SecureUtil;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * @author Lambda
 * @date 2021-04-08
 * @description 加密签名类
 */
public class CryptoUtils {
    private static final String MAC_NAME = "HmacSHA1";
    private static final String ENCODING = "UTF-8";
    private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    /**
     * 获取SHA256加密字符串
     *
     * @param str 加密源串
     * @param coding 加密结果的编码格式
     * @return 加密结果
     */
    public static String getSHA256(String str, String coding){
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(str.getBytes(StandardCharsets.UTF_8));
            if ("Base64".equals(coding)){
                return Base64.getEncoder().encodeToString(messageDigest.digest());
            }
            return byte2Hex(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 将byte数组转换成16进制的字符串
     *
     * @param bytes byte数组
     * @return 16进制字符串
     */
    private static String byte2Hex(byte[] bytes){
        StringBuilder stringBuilder = new StringBuilder();
        for (byte aByte : bytes){
            String temp = Integer.toHexString(aByte & 0xFF);
            if(temp.length() == 1){
                stringBuilder.append("0");
            }
            stringBuilder.append(temp);
        }
        return stringBuilder.toString();
    }


    public static String md5(String str) {
        return SecureUtil.md5(str);
    }

    /**
     * 获取SHA256WithRSA加密字符串
     *
     * @param content 加密源串
     * @param privateKey 加密私钥
     * @param charset 加密结果的编码格式
     * @return 加密结果
     */
    public static String getSHA256WithRSA(String content, String privateKey, String charset){
        try {
            PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec(org.apache.commons.codec.binary.Base64.decodeBase64(privateKey));
            PrivateKey priKey = KeyFactory.getInstance("RSA").generatePrivate(priPKCS8);

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(priKey);
            signature.update(content.getBytes(charset));
            return org.apache.commons.codec.binary.Base64.encodeBase64String(signature.sign());
        } catch (Exception e) {
            //签名失败
            return null;
        }
    }

    /**
     * 使用 HMAC-SHA1 签名方法对对encryptText进行签名
     * @param encryptText 被签名的字符串
     * @param encryptKey 密钥
     * @return 返回被加密后的字符串
     * @throws Exception
     */
    public static String HmacSHA1Encrypt( String encryptText, String encryptKey,String type) throws Exception{
        byte[] data = encryptKey.getBytes( ENCODING );
        // 根据给定的字节数组构造一个密钥,第二参数指定一个密钥算法的名称
        SecretKey secretKey = new SecretKeySpec( data, MAC_NAME );
        // 生成一个指定 Mac 算法 的 Mac 对象
        Mac mac = Mac.getInstance( MAC_NAME );
        // 用给定密钥初始化 Mac 对象
        mac.init( secretKey );
        byte[] text = encryptText.getBytes( ENCODING );
        // 完成 Mac 操作
        byte[] digest = mac.doFinal(text);
        if ("hex".equals(type)){
            return bytesToHexString(digest);
        }else if ("base64".equals(type)){
            return Base64.getEncoder().encodeToString(digest);
        }
        return new String(digest);
    }

    /**
     * 转换成Hex
     *
     * @param bytesArray
     */
    public static String bytesToHexString( byte[] bytesArray ){
        if ( bytesArray == null ){
            return null;
        }
        StringBuilder sBuilder = new StringBuilder();
        for ( byte b : bytesArray ){
            String hv = String.format("%02x", b);
            sBuilder.append(hv);
        }
        return sBuilder.toString();
    }

    public static String getCheckSum(String appSecret, String nonce, String curTime) {
        return encode("sha1", appSecret + nonce + curTime);
    }

    private static String encode(String algorithm, String value) {
        if (value == null) {
            return null;
        }
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
            messageDigest.update(value.getBytes());
            return getFormattedText(messageDigest.digest()).toLowerCase();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getFormattedText(byte[] bytes) {
        int len = bytes.length;
        StringBuilder buf = new StringBuilder(len * 2);
        for (int j = 0; j < len; j++) {
            buf.append(HEX_DIGITS[(bytes[j] >> 4) & 0x0f]);
            buf.append(HEX_DIGITS[bytes[j] & 0x0f]);
        }
        return buf.toString();
    }
}
