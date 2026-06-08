package com.login.func.impl;

import com.login.app.domain.ChannelLoginInfoDTO;
import com.login.app.domain.LoginInfoDO;
import com.login.app.domain.TencentCheckInfo;
import com.login.app.domain.TencentLoginParamsDTO;
import com.login.common.utils.*;
import com.login.enums.LoginResultEnum;
import com.login.enums.RegisterTypeEnum;
import com.login.func.AbstractChannel;
import com.login.prop.domain.TencentProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class Tencent extends AbstractChannel {

    @Override
    public R login(String reqParams) {
        try {
            TencentLoginParamsDTO data = JSONUtils.jsonToBean(reqParams, TencentLoginParamsDTO.class);
            TencentProperties tencentProperties  = getProp();
            ChannelLoginInfoDTO loginInfo = data.getLoginInfo();
            TencentCheckInfo checkInfo    = data.getCheckInfo();
            int channelId = loginInfo.getChannelId();
            if (StringUtils.isEmpty(loginInfo.getUserId())) {
                loginInfo.setUserId(checkInfo.getOpenid());
            }
            String host = "true".equals(tencentProperties.getTest()) ? "http://ysdktest.qq.com" : "http://ysdk.qq.com";
            String path, appId, signContent;
            String userType = checkInfo.getUserType();
            long time = System.currentTimeMillis() / 1000;
            String channelFlag = tencentProperties.getChannelFlag();
            if ("QQ".equals(userType)){
                path = "/auth/qq_check_token";
                appId = tencentProperties.getAppIdQq();
                signContent = tencentProperties.getAppKeyQq() + time;
                channelFlag += "_qq_";
            } else if ("WX".equals(userType)) {
                path = "/auth/wx_check_token";
                appId = tencentProperties.getAppIdWx();
                signContent = tencentProperties.getAppKeyWx() + time;
                channelFlag += "_wx_";
            } else {
                return R.result(-1);
            }
            String sign = CryptoUtils.md5(signContent);
            SortedMap<Object, Object> queryInfo = new TreeMap<>();
            queryInfo.put("timestamp",time);
            queryInfo.put("appid", appId);
            queryInfo.put("sig", sign);
            queryInfo.put("openid", checkInfo.getOpenid());
            queryInfo.put("openkey", checkInfo.getAccessToken());
            queryInfo.put("userip", data.getClientIP());
            String url = host + path + "?" + StringUtils.getSignContent(queryInfo);
            Map<String,Object> result = httpClientApi.tencentLogin(url);
            logger.info("tencent登录: {}",result);
            Integer ret = (Integer) result.get("ret");
            if (ret != 0) {
                return R.result(-1);
            }
            //设置payInfo
            Map<String, String> payInfo = new HashMap<>();
            payInfo.put("openid", checkInfo.getOpenid());
            payInfo.put("payToken", checkInfo.getPayToken());
            payInfo.put("pf", checkInfo.getPf());
            payInfo.put("pfkey", checkInfo.getPfkey());
            payInfo.put("userType", checkInfo.getUserType());
            String tempUsername = channelFlag+ ":" + loginInfo.getUserId();
            LoginInfoDO loginInfoDO = loginInfoService.search(tempUsername);
            if (loginInfoDO != null) {
                if (loginInfoDO.getStatus() != 0){
                    return R.result(LoginResultEnum.SYS_FROZEN.ordinal());
                }
                int uid = loginInfoDO.getId();
                String token = TokenUtils.generateToken(uid,redisManager);
                redisManager.set("payInfo" + uid, JSONUtils.beanToJson(payInfo));
                return R.result(0).put("uid", uid).put("token",token);
            }
            String deviceID = data.getDeviceID();
            return autoRegister("",tempUsername,channelId,deviceID, RegisterTypeEnum.OAUTH.ordinal());
        }catch (Exception e){
            logger.error(e.getMessage(),e);
        }
        return R.result(-1);
    }
}
