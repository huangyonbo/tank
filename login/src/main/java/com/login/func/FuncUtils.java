package com.login.func;

import com.login.app.domain.DeviceRegisterCount;
import com.login.app.domain.LoginInfoDO;
import com.login.app.domain.RegisterDeviceInfoDO;
import com.login.app.service.LoginInfoService;
import com.login.app.service.RegisterDeviceInfoService;
import com.login.bms.domain.RegisterMgrDO;
import com.login.bms.service.RegisterMgrService;
import com.login.common.utils.CryptoUtils;
import com.login.common.utils.StringUtils;
import com.login.enums.RegisterResultEnum;
import com.login.prop.domain.AppProperties;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class FuncUtils {

    private static URLClassLoader loader = null;

    public static IFuncLogic create(String fileName, String name){
        try {
            URL url = new URL("file:" + fileName);
            File file = new File(url.getFile());
            Class<?> clazz;
            String className = FuncUtils.class.getPackage().getName() + ".impl." + name;
            if (file.exists()){
                loader = new URLClassLoader(new URL[] {url},Thread.currentThread().getContextClassLoader());
                clazz = loader.loadClass(className);
            }else{
                clazz = Class.forName(className);
            }
            if (IFuncLogic.class.isAssignableFrom(clazz)) {
                return (IFuncLogic)clazz.newInstance();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (loader != null){
                try {
                    loader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static DeviceRegisterCount checkDeviceLimit(AppProperties appProperties, RegisterMgrService registerMgrService, RegisterDeviceInfoService registerDeviceInfoService,int channelId, String deviceId) {
        DeviceRegisterCount registerCount = new DeviceRegisterCount();
        Map<String,Object> query = new HashMap<>();
        query.put("channelId",channelId);
        int registerLimit = Integer.parseInt(appProperties.getDeviceRegisterLimit());
        List<RegisterMgrDO> mgrs = registerMgrService.list(query);
        if (mgrs != null && mgrs.size() > 0){
            registerLimit = mgrs.get(0).getRegisterLimit();
        }
        query.put("channelId",channelId);
        query.put("deviceId",deviceId);
        List<RegisterDeviceInfoDO> deviceDos = registerDeviceInfoService.list(query);
        int alreadyRegisterCount = 0;
        int canRegisterCount     = 0;
        RegisterDeviceInfoDO deviceDo = null;
        if (deviceDos != null && deviceDos.size() > 0){
            deviceDo = deviceDos.get(0);
            alreadyRegisterCount = deviceDos.get(0).getAccountCount();
            canRegisterCount = registerLimit - alreadyRegisterCount;
        }else{
            canRegisterCount = registerLimit;
            deviceDo = new RegisterDeviceInfoDO();
            deviceDo.setDeviceId(deviceId);
            deviceDo.setChannel(channelId);
        }
        registerCount.setCanRegisterCount(canRegisterCount);
        registerCount.setAlreadyRegisterCount(alreadyRegisterCount);
        registerCount.setRegisterDeviceInfoDO(deviceDo);
        return registerCount;
    }

    /**
     * 密码专用校验
     * 必须有小写，有大写，有数字，并且只能由字母数字组成，长度 6~20
     */
    public static boolean isValidPassword(String pwd) {
        if (pwd == null) return false;
        String regex = "^[a-zA-Z0-9]{6,12}$";
        return pwd.matches(regex);
    }

    public static int registerService(DeviceRegisterCount registerCount,LoginInfoService loginInfoService,RegisterDeviceInfoService registerDeviceInfoService,Object... params) {
        String phone     = params[0].toString();
        String username  = params[1].toString();
        int channelID    = (int)params[2];
        String deviceID  = params[3].toString();
        int registerType = (int)params[4];
        int len = params.length;
        String password = len >= 6 ? params[5].toString() : StringUtils.getRandomString(10);
        String nickname = len >= 7 ? params[6].toString() : StringUtils.EMPTY;
        String province = len >= 8 ? params[7].toString() : StringUtils.EMPTY;
        String city     = len >= 9 ? params[8].toString() : StringUtils.EMPTY;
        String country  = len >= 10 ? params[9].toString() : StringUtils.EMPTY;
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password) || StringUtils.isNumeric(username) || !isValidPassword(username) || !isValidPassword(password) || !isValidPassword(phone)) {
            return RegisterResultEnum.DECODE_ERROR.ordinal();
        }
        if (loginInfoService.search(username) != null){
            return RegisterResultEnum.CONFLICT.ordinal();
        }
        String salt = StringUtils.getRandomString(10);
        String hashPassword = CryptoUtils.getSHA256(password + salt, "Base64");
        Date now = new Date();
        LoginInfoDO loginInfoDO = LoginInfoDO.builder()
                .userName(username)
                .password(hashPassword)
                .salt(salt)
                .phone(phone)
                .registerTime(now)
                .loginTime(now)
                .registerType(registerType)
                .channelId(channelID)
                .registerDeviceId(deviceID)
                .recruiter(StringUtils.EMPTY)
                .pi(StringUtils.EMPTY)
                .nickname(nickname)
                .province(province)
                .city(city)
                .country(country)
                .sex(0).status(0)
                .build();
        loginInfoService.save(loginInfoDO);
        if (registerCount != null){
            //更新注册设备信息
            RegisterDeviceInfoDO registerDeviceInfoDO = registerCount.getRegisterDeviceInfoDO();
            if (registerDeviceInfoDO.getId() == 0){
                registerDeviceInfoDO.setAccountCount(1);
                registerDeviceInfoService.save(registerDeviceInfoDO);
            }else{
                registerDeviceInfoDO.setAccountCount(registerCount.getAlreadyRegisterCount() + 1);
                registerDeviceInfoService.update(registerDeviceInfoDO);
            }
        }
        return RegisterResultEnum.SUCCESS.ordinal();
    }

    public static IChannel load(String fileName, String channel){
        try {
            URL url   = new URL("file:" + fileName);
            File file = new File(url.getFile());
            Class<?> clazz;
            String className = FuncUtils.class.getPackage().getName() + ".impl." + channel;
            if (file.exists()){
                loader = new URLClassLoader(new URL[] {url},Thread.currentThread().getContextClassLoader());
                clazz = loader.loadClass(className);
            }else{
                clazz = Class.forName(className);
            }
            if (IChannel.class.isAssignableFrom(clazz)) {
                return (IChannel)clazz.newInstance();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (loader != null){
                try {
                    loader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static void loadChannels(Map<String, IChannel> channels) {
        String basePackage = FuncUtils.class.getPackage().getName() + ".impl";
        String packagePath = basePackage.replace('.', '/');
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Enumeration<URL> resources = cl.getResources(packagePath);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                String protocol = url.getProtocol();
                if ("file".equalsIgnoreCase(protocol)) {
                    File dir = new File(url.toURI());
                    loadChannelsFromDirectory(channels, dir, basePackage, cl);
                } else if ("jar".equalsIgnoreCase(protocol)) {
                    JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
                    loadChannelsFromJar(channels, jar, packagePath, cl);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadChannelsFromDirectory(Map<String, IChannel> channels, File dir, String packageName, ClassLoader cl) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles(pathname -> pathname.isDirectory() || pathname.getName().endsWith(".class"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                loadChannelsFromDirectory(channels, file, packageName + "." + file.getName(), cl);
            } else {
                String simpleName = file.getName().substring(0, file.getName().length() - 6);
                String className = packageName + "." + simpleName;
                try {
                    Class<?> clazz = Class.forName(className, false, cl);
                    if (IChannel.class.isAssignableFrom(clazz)) {
                        IChannel channel = (IChannel) clazz.newInstance();
                        channels.put(channel.name(), channel);
                    }
                } catch (ClassNotFoundException ignored) {
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void loadChannelsFromJar(Map<String, IChannel> channels, JarFile jar, String packagePath, ClassLoader cl) {
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.startsWith("/")) {
                name = name.substring(1);
            }
            if (!name.startsWith(packagePath) || !name.endsWith(".class") || entry.isDirectory()) {
                continue;
            }
            String className = name.replace("/", ".").substring(0, name.length() - 6);
            try {
                Class<?> clazz = Class.forName(className, false, cl);
                if (IChannel.class.isAssignableFrom(clazz)) {
                    IChannel channel = (IChannel) clazz.newInstance();
                    channels.put(channel.name(), channel);
                }
            } catch (ClassNotFoundException ignored) {
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
