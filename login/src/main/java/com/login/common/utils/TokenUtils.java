package com.login.common.utils;

import com.login.common.redis.shiro.RedisManager;

public class TokenUtils {

    private static final String TOKEN_HEADER = "XHG_USER_LOGIN_TOKEN_";

    public static String  generateToken(int uid,RedisManager redisManager){
        String token = StringUtils.getRandomString(15);
        redisManager.set(TOKEN_HEADER + uid,token);
        return token;
    }

    /**
     * 校验玩家登陆时的token
     * @param uid 玩家uid
     * @param token 登陆token
     * @return 返回校验结果
     */
    public static boolean verifyToken(int uid, String token,RedisManager redisManager){
        String key =  TOKEN_HEADER + uid;
        String preToken = redisManager.get(key);
        redisManager.del(key);
        return token.equals(preToken);
    }
}
