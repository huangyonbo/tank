package com.login.func.impl;

import com.login.app.domain.LoginInfoDO;
import com.login.common.utils.JSONUtils;
import com.login.common.utils.R;
import com.login.common.utils.TokenUtils;
import com.login.enums.LoginResultEnum;
import com.login.enums.RegisterTypeEnum;
import com.login.func.AbstractChannel;

import java.util.Map;

public class Robot extends AbstractChannel {

    public Robot(){

    }

    @Override
    public R login(String reqParams) {
        try {
            Map<String,Object> map = JSONUtils.jsonToBean(reqParams,Map.class);
            String account = map.get("account").toString();
            String password = map.get("password").toString();
            logger.info("机器人登陆:{} ",reqParams);
            LoginInfoDO loginInfoDO = loginInfoService.search(account);
            if (loginInfoDO != null){
                if (loginInfoDO.getStatus() != 0){
                    return R.result(LoginResultEnum.SYS_FROZEN.ordinal());
                }
                //生成token
                int uid = loginInfoDO.getId();
                String token = TokenUtils.generateToken(uid,redisManager);
                return R.result(0).put("uid", uid).put("token", token);
            }
            return autoRegister("",account,0,"xhgRobot",RegisterTypeEnum.ROBOT.ordinal());
        }catch (Exception e){
            logger.error(e.getMessage(),e);
        }
        return R.result(-1);
    }
}
