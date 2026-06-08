package com.login.func.impl;

import com.login.app.domain.ChannelLoginInfoDTO;
import com.login.app.domain.LoginInfoDO;
import com.login.app.domain.XiaomiCheckInfo;
import com.login.app.domain.XiaomiLoginParamsDTO;
import com.login.common.utils.*;
import com.login.common.utils.*;
import com.login.enums.LoginResultEnum;
import com.login.enums.RegisterTypeEnum;
import com.login.func.AbstractChannel;
import com.login.prop.domain.XiaoMiProperties;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.SortedMap;
import java.util.TreeMap;

public class Xiaomi extends AbstractChannel {

    public Xiaomi(){
        loginUrl = "http://mis.migc.xiaomi.com/api/biz/service/verifySession.do";
    }

    @Override
    public R login(String reqParams) {
        try {
            XiaomiLoginParamsDTO data = JSONUtils.jsonToBean(reqParams,XiaomiLoginParamsDTO.class);
            XiaoMiProperties xiaomiProperties  = getProp();
            XiaomiCheckInfo xiaomiCheckInfo = data.getCheckInfo();
            ChannelLoginInfoDTO loginInfo = data.getLoginInfo();
            int channelId = loginInfo.getChannelId();
            String userId = xiaomiCheckInfo.getUid();
            SortedMap<Object, Object> map = new TreeMap<>();
            map.put("appId", xiaomiProperties.getAppId());
            map.put("uid", userId);
            map.put("session", xiaomiCheckInfo.getSession());
            String content = StringUtils.getSignContent(map);
            String sign = CryptoUtils.HmacSHA1Encrypt(content, xiaomiProperties.getAppSecret(),"hex");
            String domain = loginUrl + "?" + content + "&signature=" + sign;
            String result = httpClientApi.xiaomiLogin(domain);
            logger.info("小米登录：{}",result);
            JSONObject json = JSON.parseObject(result);
            int code = json.getObject("errcode",int.class);
            if (code != 200){
                return R.result(code);
            }
            String tempUsername = xiaomiProperties.getChannelFlag() + ":" + userId;
            LoginInfoDO loginInfoDO = loginInfoService.search(tempUsername);
            if (loginInfoDO != null) {
                if (loginInfoDO.getStatus() != 0){
                    return R.result(LoginResultEnum.SYS_FROZEN.ordinal());
                }
                int uid = loginInfoDO.getId();
                String token = TokenUtils.generateToken(uid,redisManager);
                return R.result(0).put("uid", uid).put("token",token).put("userId",userId);
            }
            String deviceID = data.getDeviceID();
            return autoRegister("",tempUsername,channelId,deviceID, RegisterTypeEnum.OAUTH.ordinal()).put("userId",userId);
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
        }
        return R.result(-1);
    }
}
