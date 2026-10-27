package com.login.common.utils;

import com.login.common.redis.shiro.RedisManager;

public class TokenUtils {
    private static final String TOKEN_HEADER = "XHG_USER_LOGIN_TOKEN_";
    static final int TOKEN_TTL_SECONDS = 1800;

    static String tokenKey(int uid) {
        return TOKEN_HEADER + uid;
    }

    public static String  generateToken(int uid,RedisManager redisManager){
        String token = StringUtils.getRandomString(15);
        redisManager.set(tokenKey(uid), token, TOKEN_TTL_SECONDS);
        return token;
    }

    /**
     * 校验玩家登陆时的token
     * @param uid 玩家uid
     * @param token 登陆token
     * @return 返回校验结果
     */
    public static boolean verifyToken(int uid, String token,RedisManager redisManager){
        if (uid <= 0 || token == null || token.trim().isEmpty()) {
            return false;
        }
        // A login ticket is single-use. Wrong or superseded tickets must not
        // consume the current ticket, and concurrent claims may succeed only once.
        return redisManager.matchesAndDelete(tokenKey(uid), token);
    }
}
