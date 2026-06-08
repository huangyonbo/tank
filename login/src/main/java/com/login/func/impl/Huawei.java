package com.login.func.impl;

import com.login.app.domain.ChannelLoginInfoDTO;
import com.login.app.domain.HuaweiCheckInfo;
import com.login.app.domain.HuaweiLoginParamsDTO;
import com.login.app.domain.LoginInfoDO;
import com.login.common.utils.*;
import com.login.enums.LoginResultEnum;
import com.login.enums.RegisterTypeEnum;
import com.login.func.AbstractChannel;
import com.login.prop.domain.HuaWeiProperties;

import java.net.URLEncoder;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class Huawei extends AbstractChannel {

    public Huawei(){
        loginUrl = "https://jos-api.cloud.huawei.com/gameservice/api/gbClientApi";
    }

    @Override
    public R login(String reqParams) {
        try {
            HuaweiLoginParamsDTO data = JSONUtils.jsonToBean(reqParams,HuaweiLoginParamsDTO.class);
            HuaWeiProperties huaWeiProperties  = getProp();
            ChannelLoginInfoDTO loginInfo = data.getLoginInfo();
            HuaweiCheckInfo checkInfo = data.getCheckInfo();
            String method = "external.hms.gs.checkPlayerSign";
            String appId  = huaWeiProperties.getAppId();
            String cpId   = huaWeiProperties.getCpId();
            String ts     = checkInfo.getTs();
            String playerId = checkInfo.getPlayerId();
            String playerLevel = checkInfo.getPlayerLevel();
            String playerSSign = URLEncoder.encode(checkInfo.getGameAuthSign(),"UTF-8");
            SortedMap<String, String> queryInfo = new TreeMap<>();
            queryInfo.put("method", "external.hms.gs.checkPlayerSign");
            queryInfo.put("appId",appId);
            queryInfo.put("cpId",cpId);
            queryInfo.put("ts",ts);
            queryInfo.put("playerId",playerId);
            queryInfo.put("playerLevel",playerLevel);
            queryInfo.put("playerSSign",checkInfo.getGameAuthSign());
            String signContent = StringUtils.getHuaweiSignContent(queryInfo);
            String cpSign = CryptoUtils.getSHA256WithRSA(signContent,huaWeiProperties.getGameRsaPrivate(),"UTF-8");
            if (cpSign == null) {
                logger.info("华为登陆：签名失败");
                return R.result(-1);
            }
            cpSign = URLEncoder.encode(cpSign,"UTF-8");
            Map<String,Object> map = httpClientApi.huaweiLogin(loginUrl,method,appId,cpId,ts,playerId,playerLevel,playerSSign,cpSign);
            logger.info("华为登陆:{} ",map);
            Integer code = (Integer)map.get("rtnCode");
            if (code != 0){
                return R.result(-1);
            }
            String userId = checkInfo.getPlayerId();
            String tempUsername = huaWeiProperties.getChannelFlag() + ":" + userId;
            int channelId = loginInfo.getChannelId();
            LoginInfoDO loginInfoDO = loginInfoService.search(tempUsername);
            if (loginInfoDO != null){
                if (loginInfoDO.getStatus() != 0){
                    return R.result(LoginResultEnum.SYS_FROZEN.ordinal());
                }
                //生成token
                int uid = loginInfoDO.getId();
                String token = TokenUtils.generateToken(uid,redisManager);
                return R.result(0).put("uid", uid).put("token", token).put("userId",userId);
            }
            String deviceId = data.getDeviceID();
            return autoRegister("",tempUsername,channelId,deviceId,RegisterTypeEnum.OAUTH.ordinal()).put("userId",userId);
        }catch (Exception e){
            logger.error(e.getMessage(),e);
        }
        return R.result(-1);
    }
}
