package com.login.func.impl;

import com.login.app.domain.ChannelLoginInfoDTO;
import com.login.app.domain.LoginInfoDO;
import com.login.app.domain.WechatCheckInfo;
import com.login.app.domain.WechatLoginParamsDTO;
import com.login.common.utils.JSONUtils;
import com.login.common.utils.R;
import com.login.common.utils.StringUtils;
import com.login.common.utils.TokenUtils;
import com.login.enums.LoginResultEnum;
import com.login.enums.RegisterTypeEnum;
import com.login.func.AbstractChannel;
import com.login.prop.domain.WechatProperties;

import java.util.Map;

public class Wechat extends AbstractChannel {
    private String wechatTokenUrl = "https://api.weixin.qq.com/sns/oauth2/access_token";
    private String wechatUserInfoUrl = "https://api.weixin.qq.com/sns/userinfo";
    @Override
    public R login(String reqParams) {
        try{
            WechatLoginParamsDTO data = JSONUtils.jsonToBean(reqParams, WechatLoginParamsDTO.class);
            WechatProperties wechatProperties = getProp();
            WechatCheckInfo checkInfo = data.getCheckInfo();
            ChannelLoginInfoDTO loginInfo = data.getLoginInfo();
            int channelId     = loginInfo.getChannelId();
            String code       = checkInfo.getCode();
            String deviceID   = data.getDeviceID();
            Map<String,Object> result = httpClientApi.wechatLoginToken(wechatTokenUrl,wechatProperties.getAppId(),wechatProperties.getAppSecret(),code,"authorization_code");
            if (result.containsKey("errcode")){
                return R.result(result.get("errcode")).put("msg",result.get("errmsg"));
            }
            String token  = result.get("access_token").toString();
            String openid = result.get("openid").toString();
            result = httpClientApi.wechatLoginUserInfo(wechatUserInfoUrl,token,openid);
            if (result.containsKey("errcode")){
                return R.result(result.get("errcode")).put("msg",result.get("errmsg"));
            }
            String unionid = result.get("unionid").toString();
            String tempUsername = "wehcat:" + unionid;
            LoginInfoDO loginInfoDO = loginInfoService.search(tempUsername);
            if (loginInfoDO != null) {
                if (loginInfoDO.getStatus() != 0){
                    return R.result(LoginResultEnum.SYS_FROZEN.ordinal());
                }
                int _uid = loginInfoDO.getId();
                String _token = TokenUtils.generateToken(_uid,redisManager);
                return R.result(0).put("uid",_uid).put("token",_token).put("userId",unionid);
            }
            String nickname = result.get("nickname").toString();
            //去掉表情符号
            nickname = nickname.replaceAll("[^\\u0000-\\uFFFF]", "");
            return autoRegister("",tempUsername,channelId,deviceID,
                    RegisterTypeEnum.WECHAT.ordinal(),
                    StringUtils.getRandomString(10),
                    nickname,result.get("province"),
                    result.get("city"),result.get("country")).put("userId",unionid);
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
        }
        return R.result(-1);
    }
}
