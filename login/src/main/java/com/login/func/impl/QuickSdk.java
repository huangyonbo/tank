package com.login.func.impl;

import com.login.app.domain.ChannelLoginInfoDTO;
import com.login.app.domain.LoginInfoDO;
import com.login.app.domain.QuickCheckInfo;
import com.login.app.domain.QuickLoginParamsDTO;
import com.login.common.utils.JSONUtils;
import com.login.common.utils.R;
import com.login.common.utils.TokenUtils;
import com.login.enums.LoginResultEnum;
import com.login.enums.RegisterTypeEnum;
import com.login.func.AbstractChannel;

public class QuickSdk extends AbstractChannel {

    public QuickSdk(){
        loginUrl = "https://checkuser.quickapi.net/v2/checkUserInfo";
    }

    @Override
    public R login(String reqParams) {
        try{
            QuickLoginParamsDTO data = JSONUtils.jsonToBean(reqParams, QuickLoginParamsDTO.class);
            QuickCheckInfo checkInfo = data.getCheckInfo();
            int channelId     = checkInfo.getSubChannel();
            if (channelId == 0){
                ChannelLoginInfoDTO loginInfo = data.getLoginInfo();
                channelId = loginInfo.getChannelId();
            }
            String token      = checkInfo.getToken();
            String deviceID   = data.getDeviceID();
            String uid        = checkInfo.getUid();
            String plat       = "1".equals(checkInfo.getIosFlag()) ? "Ios_Quick:" : "Android_Quick:";
            int code = httpClientApi.quickLogin(loginUrl,uid,token);
            if (code != 1){
                return R.result(-1);
            }
            String tempUsername = plat + uid;
            LoginInfoDO loginInfoDO = loginInfoService.search(tempUsername);
            if (loginInfoDO != null) {
                if (loginInfoDO.getStatus() != 0){
                    return R.result(LoginResultEnum.SYS_FROZEN.ordinal());
                }
                int _uid = loginInfoDO.getId();
                String _token = TokenUtils.generateToken(_uid,redisManager);
                return R.result(0).put("uid",_uid).put("token",_token).put("userId",uid);
            }
            return autoRegister("",tempUsername,channelId,deviceID, RegisterTypeEnum.OAUTH.ordinal()).put("userId",uid);
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
        }
        return R.result(-1);
    }
}
