package com.login.http;

import com.dtflys.forest.annotation.*;
import com.dtflys.forest.callback.OnError;
import com.dtflys.forest.callback.OnSuccess;

import java.util.Map;

public interface HttpClientApi {

    @Request(url = "${domain}", type = "post", dataType ="json" ,contentType = "application/x-www-form-urlencoded;charset=utf-8")
    Map<String,Object> doVerify(
        @DataVariable("domain")String domain,
        @Header("AppKey")String appKey,
        @Header("Nonce")String nonce,
        @Header("CurTime")String curTime,
        @Header("CheckSum")String checkSum,
        @Body("mobile")String mobile,
        @Body("templateid")Integer templateid,
        @Body("authCode")String authCode
    );

    @Request(url = "${domain}", type = "post", dataType ="json" ,contentType = "application/x-www-form-urlencoded;charset=utf-8")
    Map<String,Object> doNotify(
        @DataVariable("domain")String domain,
        @Header("AppKey")String appKey,
        @Header("Nonce")String nonce,
        @Header("CurTime")String curTime,
        @Header("CheckSum")String checkSum,
        @Body("mobile")String mobile,
        @Body("templateid")Integer templateid,
        @Body("params")String params
    );

    @Request(url = "${domain}", type = "post", dataType = "json",contentType="application/json; charset=utf-8")
    Map<String,Object> doRealLogic(
        @DataVariable("domain")String domain,
        @Header("appId")String appId,
        @Header("bizId")String bizId,
        @Header("timestamps")long timestamps,
        @Header("sign")String sign,
        @Body String data
    );

    @Request(url = "${domain}", type = "post", dataType = "json")
    Map<String,Object> huaweiLogin(
        @DataVariable("domain")String domain,
        @Body("method")String method,
        @Body("appId")String appId,
        @Body("cpId")String cpId,
        @Body("ts")String ts,
        @Body("playerId")String playerId,
        @Body("playerLevel")String playerLevel,
        @Body("playerSSign")String playerSSign,
        @Body("cpSign")String cpSign
    );

    @Request(url = "${domain}", type = "get", dataType = "json")
    Map<String,Object> tencentLogin(
        @DataVariable("domain")String domain
    );

    @Request(url = "${domain}", type = "get", dataType = "json")
    String xiaomiLogin(@DataVariable("domain")String domain);

    @Request(url = "${domain}", type = "get", dataType = "json",contentType="application/json; charset=utf-8")
    String oppoLogin(@DataVariable("domain")String domain, @Header("param")String param,@Header("oauthSignature")String oauthSignature);

    @Request(url = "${domain}", type = "post", dataType = "json")
    String vivoLogin(@DataVariable("domain")String domain, @Body("opentoken")String opentoken);

    @Request(url = "${domain}", type = "post", dataType = "json")
    int quickLogin(
        @DataVariable("domain")String domain,
        @Body("uid")String uid,
        @Body("token")String token
    );

    @Request(url = "${domain}", type = "get", dataType = "json")
    Map<String,Object>  wechatLoginToken(
        @DataVariable("domain")String domain,
        @Query("appid")String uid,
        @Query("secret")String secret,
        @Query("code")String code,
        @Query("grant_type")String granType
    );

    @Request(url = "${domain}", type = "get", dataType = "json")
    Map<String,Object> wechatLoginUserInfo(
        @DataVariable("domain")String domain,
        @Query("access_token")String token,
        @Query("openid")String openId
    );

    @Request(url = "${domain}", type = "post", dataType = "json",contentType="application/json; charset=utf-8")
    String postCommon(@DataVariable("domain")String domain, @Body String body);

    @Request(url = "${domain}", type = "get", dataType = "json",contentType="application/json; charset=utf-8")
    String getCommon(@DataVariable("domain")String domain, @Query Map<String,Object> params);

    /**
     * 异步post
     */
    @Request(url = "${domain}", type = "post", dataType = "json",contentType="application/json; charset=utf-8")
    void postCommonAsync(@DataVariable("domain")String domain, @Body String body, OnSuccess<?> onSuccess, OnError onError);
}
