package com.login.common.utils;

import com.login.common.config.ApplicationContextRegister;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Deliberately malformed HTTP UID for the account-save T-10 test. Disabled by default. */
public final class LoginResponseTestHook {
    private static final Logger logger = LoggerFactory.getLogger(LoginResponseTestHook.class);
    private static final Set<String> TEST_PROFILES = new HashSet<>(Arrays.asList("local", "test", "remoteTest", "aliTest"));

    private LoginResponseTestHook() { }

    public static R apply(R response, int realUid) {
        ApplicationContext context = ApplicationContextRegister.getApplicationContext();
        return context == null ? response : apply(response, realUid, context.getEnvironment());
    }

    static R apply(R response, int realUid, Environment environment) {
        if (!Boolean.parseBoolean(environment.getProperty("tank.login-test-hooks.enabled", "false"))) return response;
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0 || !TEST_PROFILES.containsAll(Arrays.asList(profiles))) return response;
        Set<String> whitelist = new HashSet<>(Arrays.asList(environment.getProperty("tank.login-test-hooks.uids", "").split(",")));
        if (realUid <= 0 || !whitelist.contains(Integer.toString(realUid))) return response;
        String mode = environment.getProperty("tank.login-test-hooks.uid-mode", "");
        if ("zero".equals(mode)) response.put("uid", 0);
        else if ("missing".equals(mode)) response.remove("uid");
        else return response;
        logger.warn("login response test hook realUid={} mode={}", realUid, mode);
        return response;
    }
}
