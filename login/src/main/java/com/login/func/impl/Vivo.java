package com.login.func.impl;

import com.login.app.domain.ChannelLoginInfoDTO;
import com.login.app.domain.LoginInfoDO;
import com.login.app.domain.VivoCheckInfo;
import com.login.app.domain.VivoLoginParamsDTO;
import com.login.common.utils.JSONUtils;
import com.login.common.utils.R;
import com.login.common.utils.TokenUtils;
import com.login.enums.LoginResultEnum;
import com.login.enums.RegisterTypeEnum;
import com.login.func.AbstractChannel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class Vivo extends AbstractChannel {

    public Vivo(){
        loginUrl = "https://joint-account.vivo.com.cn/cp/user/auth";
    }

    @Override
    public R login(String reqParams) {
        try{
            VivoLoginParamsDTO data = JSONUtils.jsonToBean(reqParams,VivoLoginParamsDTO.class);
            ChannelLoginInfoDTO loginInfo = data.getLoginInfo();
            int channelId = loginInfo.getChannelId();
            VivoCheckInfo checkInfo = data.getCheckInfo();
            String authToken = checkInfo.getAuthToken();
            String deviceID = data.getDeviceID();
            String result = httpClientApi.vivoLogin(loginUrl,authToken);
            logger.info("vivo登录:", result);
            JSONObject json = JSON.parseObject(result);
            int retcode = json.getObject("retcode",int.class);
            if (retcode != 0) {
                return R.result(retcode);
            }
            JSONObject datas = json.getObject("data",JSONObject.class);
            String openId = datas.get("openid").toString();
            String tempUsername = "vivo:" + openId;
            LoginInfoDO loginInfoDO = loginInfoService.search(tempUsername);
            if (loginInfoDO != null) {
                if (loginInfoDO.getStatus() != 0){
                    return R.result(LoginResultEnum.SYS_FROZEN.ordinal());
                }
                int uid = loginInfoDO.getId();
                String _token = TokenUtils.generateToken(uid,redisManager);
                return R.result(0).put("uid", uid).put("token",_token).put("userId",openId);
            }
            return autoRegister("",tempUsername,channelId,deviceID, RegisterTypeEnum.OAUTH.ordinal()).put("userId",openId);
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
        }
        return R.result(-1);
    }
}
