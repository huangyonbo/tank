package com.login.func;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import com.login.app.domain.DeviceRegisterCount;
import com.login.app.domain.LoginInfoDO;
import com.login.app.service.LoginInfoService;
import com.login.app.service.RegisterDeviceInfoService;
import com.login.bms.service.RegisterMgrService;
import com.login.common.redis.shiro.RedisManager;
import com.login.common.utils.R;
import com.login.common.utils.TokenUtils;
import com.login.enums.RegisterResultEnum;
import com.login.http.HttpClientApi;
import com.login.prop.domain.AppProperties;
import com.login.prop.service.PropConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractChannel implements IChannel{
    protected String loginUrl;
    protected HttpClientApi httpClientApi;
    protected PropConfigService propConfigService;
    protected LoginInfoService loginInfoService;
    protected RedisManager redisManager;
    protected RegisterMgrService registerMgrService;
    protected RegisterDeviceInfoService registerDeviceInfoService;
    protected String logicName;
    private static Map<String,Logger> loggers = new HashMap<>();
    protected Logger logger;

    @Override
    public String name() {
        if (logicName == null){
            logicName = this.getClass().getSimpleName().toLowerCase();
        }
        return logicName;
    }

    @Override
    public void init(int type,HttpClientApi httpClientApi, PropConfigService propConfigService,
                     LoginInfoService loginInfoService, RedisManager redisManager,
                     RegisterMgrService registerMgrService,
                     RegisterDeviceInfoService registerDeviceInfoService) {
        this.httpClientApi     = httpClientApi;
        this.propConfigService = propConfigService;
        this.loginInfoService  = loginInfoService;
        this.redisManager      = redisManager;
        this.registerMgrService   = registerMgrService;
        this.registerDeviceInfoService   = registerDeviceInfoService;
        createLogger(type);
    }

    private void createLogger(int type){
        String name  = name();
        logger = loggers.get(name);
        if (logger == null){
            String basePath = System.getProperty("user.dir");
            logger = LoggerFactory.getLogger(this.getClass());
            LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
            ch.qos.logback.classic.Logger newLogger = (ch.qos.logback.classic.Logger)logger;
            //Remove all previously added appenders from this logger instance.
            newLogger.detachAndStopAllAppenders();
            //define appender
            RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();

            //policy
            TimeBasedRollingPolicy<ILoggingEvent> policy = new TimeBasedRollingPolicy<>();
            policy.setContext(loggerContext);
            //policy.setMaxHistory(5);
            policy.setFileNamePattern(basePath + "/logs/channel/" + name + "/%d{yyyy-MM-dd}/%d{yyyy-MM-dd}.log");
            policy.setParent(appender);
            policy.start();
            //encoder
            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(loggerContext);
            encoder.setPattern("[%d{yyyy-MM-dd HH:mm:ss.SSS}]-[%-5level]-[%F.%M:%L]-%msg%n");
            encoder.start();
            //start appender
            appender.setRollingPolicy(policy);
            appender.setContext(loggerContext);
            appender.setEncoder(encoder);
            appender.setPrudent(true); //support that multiple JVMs can safely write to the same file.
            appender.start();

            newLogger.addAppender(appender);
            //setup level
            newLogger.setLevel(Level.INFO);
            //remove the appenders that inherited 'ROOT'.
            newLogger.setAdditive(true);
            loggers.put(name,newLogger);
        }
        if (type == 0){
            logger.info("init {}",name);
        }else{
            logger.info("reload {}",name);
        }
    }

    protected <T> T getProp(){
        return (T) propConfigService.getProp(name() + "Prop");
    }

    protected DeviceRegisterCount checkDeviceLimit(AppProperties appProperties,int channelId, String deviceId) {
        return FuncUtils.checkDeviceLimit(appProperties,registerMgrService,registerDeviceInfoService,channelId,deviceId);
    }

    protected int registerService(DeviceRegisterCount registerCount,Object... params) {
        return FuncUtils.registerService(registerCount,loginInfoService,registerDeviceInfoService,params);
    }

    protected  <T> T mapToObj(Map<String, Object> map,Class<T> clazz) {
        try {
            T t = clazz.newInstance();
            for (String key : map.keySet()){
                Field field = null;
                try {
                    field = clazz.getDeclaredField(key);
                } catch (Exception e) {
                    //e.printStackTrace();
                }
                if (field != null){
                    Object value = map.get(key);
                    field.setAccessible(true);
                    field.set(t,value);
                }
            }
            return t;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    protected R autoRegister(Object... params){
        String account = params[1].toString();
        int channelID = (int)params[2];
        String deviceID = params[3].toString();
        AppProperties appProperties = propConfigService.getProp("sysProp");
        DeviceRegisterCount registerCount = null;
        if (appProperties.checkAccountRegister() == 1 && !"xhgRobot".equals(deviceID)){
            registerCount = checkDeviceLimit(appProperties,channelID,deviceID);
            logger.info("检查设备限制：username:{} channelId:{} deviceId:{} can:{} use:{}",account,channelID,deviceID,registerCount.getCanRegisterCount(), registerCount.getAlreadyRegisterCount());
            if (registerCount.getCanRegisterCount() <= 0){
                return R.result(RegisterResultEnum.DEVICE_LIMIT.ordinal());
            }
        }
        int result = registerService(registerCount,params);
        if (result != RegisterResultEnum.SUCCESS.ordinal()) {
            return R.result(result);
        }
        LoginInfoDO loginInfoDO = loginInfoService.search(account);
        int uid = loginInfoDO.getId();
        String token = TokenUtils.generateToken(uid,redisManager);
        return R.result(0).put("uid", uid).put("token",token);
    }
}
