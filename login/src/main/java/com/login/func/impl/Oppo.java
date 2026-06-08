package com.login.func.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.login.app.domain.ChannelLoginInfoDTO;
import com.login.app.domain.LoginInfoDO;
import com.login.app.domain.OppoCheckInfo;
import com.login.app.domain.OppoLoginParamsDTO;
import com.login.common.utils.*;
import com.login.enums.LoginResultEnum;
import com.login.enums.RegisterTypeEnum;
import com.login.func.AbstractChannel;
import com.login.prop.domain.OppoProperties;

import java.util.SortedMap;
import java.util.TreeMap;

public class Oppo extends AbstractChannel {

    private static String loginUrl = "http://i.open.game.oppomobile.com/gameopen/user/fileIdInfo";

    @Override
    public R login(String reqParams) {
        try {
            OppoLoginParamsDTO data = JSONUtils.jsonToBean(reqParams, OppoLoginParamsDTO.class);
            OppoProperties oppoProperties  = getProp();
            OppoCheckInfo checkInfo = data.getCheckInfo();
            ChannelLoginInfoDTO loginInfo = data.getLoginInfo();
            String ssoid  = checkInfo.getSsoid();
            String token  = checkInfo.getToken();
            SortedMap<Object, Object> map = new TreeMap<>();
            map.put("oauthConsumerKey",oppoProperties.getAppKey());
            map.put("oauthToken", token);
            map.put("oauthSignatureMethod", "HMAC-SHA1");
            map.put("oauthTimestamp", System.currentTimeMillis() / 1000);
            map.put("oauthNonce", StringUtils.getRandomString(32));
            map.put("oauthVersion", "1.0");
            String content = StringUtils.getSignContent(map) + "&";
            //content = content.replace("/\\+/g", "%2B");
            String oauthSignatureKey = oppoProperties.getAppSecret() + "&";
            String sign = CryptoUtils.HmacSHA1Encrypt(content, oauthSignatureKey,"base64");
            String domain = loginUrl + "?fileId=" + ssoid + "&token=" + token;
            String result = httpClientApi.oppoLogin(domain,content,sign);
            logger.info("oppo登录: {}",result);
            JSONObject json = JSON.parseObject(result,JSONObject.class);
            int code = json.getObject("resultCode",int.class);
            if (code != 200){
                return R.result(-1).put("msg",json.get("resultMsg"));
            }
            Object _ssoid = json.get("ssoid");
            if (!ssoid.equals(_ssoid)){
                logger.info("oppo登录: 账号不匹配{} , {}",ssoid,_ssoid);
                return R.result(-1).put("msg","账号不匹配");
            }
            String tempUsername = oppoProperties.getChannelFlag() + ":" + ssoid;
            LoginInfoDO loginInfoDO = loginInfoService.search(tempUsername);
            if (loginInfoDO != null) {
                if (loginInfoDO.getStatus() != 0){
                    return R.result(LoginResultEnum.SYS_FROZEN.ordinal());
                }
                int uid = loginInfoDO.getId();
                String _token = TokenUtils.generateToken(uid,redisManager);
                return R.result(0).put("uid", uid).put("token",_token).put("userId",ssoid);
            }
            String deviceID = data.getDeviceID();
            int channelId = loginInfo.getChannelId();
            return autoRegister("",tempUsername,channelId,deviceID, RegisterTypeEnum.OAUTH.ordinal()).put("userId",ssoid);
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
        }
        return R.result(-1);
    }
}
