package framework.logic;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import framework.*;
import framework.game.Kernel.KickType;
import framework.json.CheckLoginResult;
import framework.mybatis.domain.Config;
import framework.mybatis.domain.Roles;
import framework.mybatis.service.impl.ConfigService;
import framework.mybatis.service.impl.RolesService;
import framework.net.*;
import framework.net.http.HttpClientApi;
import framework.net.message.ClientMsg;
import framework.net.message.InnerMsg;
import framework.pub.PubUtils;
import framework.ratelimit.RateLimiterManager;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.net.InetSocketAddress;
import java.security.*;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

/**
 *
 * 描述： 网关服逻辑
 *
 */
@Component
@Scope("prototype")
public class GateLogic implements ILogic {
    private Logger logger = LoggerFactory.getLogger(GateLogic.class);
    private BaseServer baseServer;
    private Map<Integer, IoSession> waitLoginSessions = new HashMap<>();
    private Map<Integer, IoSession> loginSessions = new HashMap<>();
    private Map<Integer, DisconnectWaiter> disconnectCache = new HashMap<>();
    static final String CLINET_ATT_KEY = "clientMsgCache";
    private static String s_pub_key = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCJERRwCYSK40dq5AkTNa3+UhD8yJWKp9zTXv95ej+irUmeHQxJQkTcWNuJ0mFSo+CGDz6D5qab3Puli7QxveQBm2qsrn3qJoOSWDUgjOqZrPhX/bzji+4WVrgaaFrsnA6YOZo9okzr7zwkJDm2nfMUZUF5ZDI0KFVdd8FLKEhoRwIDAQAB";
    //	static final String s_pub_key = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC3aBC24nDr2S8+H8lOjUbYQ5f/BXhrhgoSf6mt2oO2Z9qtvM+XDGU6v1dRHZ8qyE3PyDy+QK1DW0xD013lA8HWCaXobPRU3G9MUcwQdQUl/nYZPTftKEGh5rReBFCYgsrp8zw6qXIQOSK3/kYmNqROQksM5rOzpna6lIEt+Nu/vwIDAQAB";
//private static String s_pub_key = "BwIAAACkAABSU0EyAAQAAAEAAQAxsBuZZ4JDIRpvc/1aXWEDPQJKqrLndSy9CZCY1zJDnTMNvNVa/52lH04nSCQhzPGZ2QEVvrJBISiOBrEFV65sZidbgyyMSxhzB+mLCoYTv7PLSwZXyeTQwPp2VfoYrelIt6tWROMvq2VyDaOp5338cAKE+jYt7AI1UNVpKOgBultnro8ncx0DZ/Ix/7riywhD4p7uaUBtzxakr8IRgAvixMLoXQ9cykYbw+7cEygRbljAJytWfO71pJqhjZYHlPNjqGJMeuE3nV9pTn0H0JnjO0gE2JzCS2Irctq2JpQGhozykdCRyK2L1JeBSRw6QWH/xehcqJvC/9xlhXv2Rn7DXyGkckUdLABbrQa+JqmDoMEyX36PPQLlDWAcFN7UDRALbvyPUP5fTvxqDlBTZ+rG+D2vXhgFg9M52LR7LT7ptzMkFbTN/diZxRxdWuvxT4xk2VSYcmHcPjVkNKsZzvqIqENFMwrlbcyAnnmW8AdnLO2fBw+5lnaLBf5VQcmHIYJvwOE0cbvyEqUnv/nwEA7kCB77O0OgMUtD8Z9lE98ctOg+GZlvmxDiVQxS50BdGeMBB2Yg/rBrcuVBp58Z0EeQIXHwRokURGLLtH7NGvKaFi4jWkGvJi9LFkgfTO1WRDTCGtmGp5A3do268JNDjyIYE74Z6dmkRbZbduM892XyyoSbZi1USS0DMGUU42j0yEo6vc5ZqiUK5TEwdYfLwmquapwXCbo4xr4+Bk9P8N8VFNvqycXD9/14QipEEi4OrU0=";
    private RSAPublicKey publicKey = null;
    private byte[] publicKeyData = null;
    private final static String LAST_HEAT_BEAT_TIME_KEY = "lastHeartBeat";
    ActorTimer syncActor;
    @Autowired
    private HttpClientApi httpClientApi;
    @Autowired
    private RateLimiterManager rateLimiterManager;

    class DisconnectWaiter {
        long time = 0;
        String game;
        int uid;

        DisconnectWaiter(String game, int uid) {
            this.game = game;
            this.uid = uid;
        }

        boolean tick(long now) {
            if (time == 0) {
                time = now;
                return false;
            }
            if (now > time + 400000) {
                //通知游戏服务器玩家断开连接
                logger.info("上次断开链接的清除 {}  {}  {} {} 状态 {}", uid, getServer().getSerName(), getServer().getFrontPort(), time, now);
                notifyGamePlayerDisconnect(game, uid);
                return true;
            }
            return false;
        }
    }

    @Override
    public boolean onInit(BaseServer ser) {
        baseServer = ser;
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_SYNC_LOAD.ordinal(), "onRecSyncLoad");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_FORWARD.ordinal(), "onRecForwardMsg");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_BROADCAST.ordinal(), "onRecBroadCast");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_BROADCAST_ALL.ordinal(), "onRecBroadCastAll");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_KICK_PLAYER.ordinal(), "onRecKickPlayer");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_UPDATE_RATE_LIMIT.ordinal(), "onUpdateRateLimit");

        baseServer.addClientMsgListener(this, ClientMsgDef.CLIENT_LOGIN.ordinal(), "onRecRequestLogin");
        baseServer.addClientMsgListener(this, ClientMsgDef.CLIENT_CUSTOM.ordinal(), "onRecCustomMsg");
        baseServer.addClientMsgListener(this, ClientMsgDef.CLIENT_REQUEST.ordinal(), "onRecRequestMsg");
        baseServer.addClientMsgListener(this, ClientMsgDef.CLIENT_HEART_BEAT.ordinal(), "onRecHeartBeat");
        baseServer.addClientMsgListener(this, ClientMsgDef.CLIENT_RESET_UID.ordinal(), "onClientResume");

        baseServer.addHttpResMsgListener(this, HttpResMsgDef.CLIENT_LOGIN_CHECK.ordinal(), "onCheckLogin");

        baseServer.addRequestListener(this, InnerMsgDef.INNER_MSG_CHANGE_BACK.ordinal(), "onChangeBack");
        // connect login server
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setTestOnBorrow(true);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            publicKeyData = decryptBASE64(s_pub_key);
            X509EncodedKeySpec keySpec509 = new X509EncodedKeySpec(publicKeyData);
            publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec509);
            //初始化限流器配置
            onUpdateRateLimit(null, null);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void initStop() {
        ServerSet ss = baseServer.getServerSet();
        Object[] servers = ss.getServersByType("game");
        Map<String, Boolean> map = baseServer.getMustCloseBeforeMe();
        map.clear();
        for (int i = 0; i < servers.length; i++) {
            map.put(servers[i].toString(), false);
        }
    }

    public static void main(String[] args) {
        GateLogic rsa = new GateLogic();
        rsa.allocRsaKey();
    }

    void allocRsaKey() {
        KeyPairGenerator keyPairGen = null;
        try {
            keyPairGen = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // 初始化密钥对生成器，密钥大小为96-1024位
        keyPairGen.initialize(1024, new SecureRandom());
        // 生成一个密钥对，保存在keyPair中
        KeyPair keyPair = keyPairGen.generateKeyPair();
        // 得到私钥
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        // 得到公钥
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        try {
            // 得到公钥字符串
            String publicKeyString = new String(encryptBASE64(publicKey.getEncoded()));
            // 得到私钥.net字符串
            String privateKeyString = getRSAPrivateKeyAsNetFormat(privateKey.getEncoded());
            String priKeyStr = new String(encryptBASE64(privateKey.getEncoded()));
            System.out.println(publicKeyString);
            System.out.println(priKeyStr);
            System.out.println(privateKeyString);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String encryptBASE64(byte[] key) throws Exception {
        return Base64.encodeBase64String(key);
    }

    byte[] decryptBASE64(String key) throws Exception {
        return Base64.decodeBase64(key);
    }

    private static byte[] removeMSZero(byte[] data) {
        byte[] data1;
        int len = data.length;
        if (data[0] == 0) {
            data1 = new byte[data.length - 1];
            System.arraycopy(data, 1, data1, 0, len - 1);
        } else
            data1 = data;
        return data1;
    }

    private static String getRSAPrivateKeyAsNetFormat(byte[] encodedPrivateKey) {
        try {
            StringBuffer buff = new StringBuffer(1024);
            PKCS8EncodedKeySpec pvkKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPrivateCrtKey pvkKey = (RSAPrivateCrtKey) keyFactory.generatePrivate(pvkKeySpec);
            buff.append("<RSAKeyValue>");
            buff.append("<Modulus>" + Base64.encodeBase64String(removeMSZero(pvkKey.getModulus().toByteArray()))
                    + "</Modulus>");
            buff.append("<Exponent>" + Base64.encodeBase64String(removeMSZero(pvkKey.getPublicExponent().toByteArray()))
                    + "</Exponent>");
            buff.append("<P>" + Base64.encodeBase64String(removeMSZero(pvkKey.getPrimeP().toByteArray())) + "</P>");
            buff.append("<Q>" + Base64.encodeBase64String(removeMSZero(pvkKey.getPrimeQ().toByteArray())) + "</Q>");
            buff.append("<DP>" + Base64.encodeBase64String(removeMSZero(pvkKey.getPrimeExponentP().toByteArray()))
                    + "</DP>");
            buff.append("<DQ>" + Base64.encodeBase64String(removeMSZero(pvkKey.getPrimeExponentQ().toByteArray()))
                    + "</DQ>");
            buff.append("<InverseQ>" + Base64.encodeBase64String(removeMSZero(pvkKey.getCrtCoefficient().toByteArray()))
                    + "</InverseQ>");
            buff.append("<D>" + Base64.encodeBase64String(removeMSZero(pvkKey.getPrivateExponent().toByteArray()))
                    + "</D>");
            buff.append("</RSAKeyValue>");
            return buff.toString();
        } catch (Exception e) {
            System.err.println(e);
            return null;
        }
    }

    byte[] encrypt(byte[] plainTextData) {
        if (publicKey == null) {
            return null;
        }
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(plainTextData);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    byte[] decrypt(byte[] cipherData) {
        if (publicKey == null) {
            return null;
        }
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            return cipher.doFinal(cipherData);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onReady() {
        baseServer.onLogicReady();
        syncActor = baseServer.setTimer(this, 10000, -1, "syncLoadToEntry", null);
    }

    void syncLoadToEntry(Object obj, int leftCount) {
        InnerMsg.SyncLoad.Builder builder = InnerMsg.SyncLoad.newBuilder();
        builder.setLoad(loginSessions.size());
        byte[] data = builder.build().toByteArray();
        Object[] entrys = baseServer.getServerSet().getServersByType("entry");
        for (int i = 0; i < entrys.length; ++i) {
            baseServer.sendMsgToServer(entrys[i].toString(), InnerMsgDef.INNER_MSG_SYNC_LOAD.ordinal(), data);
        }
    }

    private void sendMessageToClient(IoSession session, SendMessage message, boolean send) {
        if (send) {//需要立刻发送的消息
            if (message.close) {
                writeMsg(session, 0);
                session.write(message).addListener(new IoFutureListener<IoFuture>() {
                    @Override
                    public void operationComplete(IoFuture ioFuture) {
                        ioFuture.getSession().close(true);
                    }
                });
            } else {
                session.write(message);
            }
        } else {
            List<SendMessage> messages = getMessages(session);
            if (messages == null) {
                messages = new ArrayList<>();
                session.setAttribute(CLINET_ATT_KEY, messages);
            }
            messages.add(message);
        }
    }

    @SuppressWarnings("unchecked")
    private List<SendMessage> getMessages(IoSession session) {
        Object list = session.getAttribute(CLINET_ATT_KEY);
        if (list != null) {
            return (List<SendMessage>) list;
        }
        return null;
    }

    private void createWait(IoSession session, int uid) {
        if (uid == 0) {
            return;
        }
        Object _obj = session.getAttribute("BackSer");
        if (_obj == null) {
            return;
        }
        disconnectCache.put(uid, new DisconnectWaiter(_obj.toString(), uid));
    }

    private void notifyGamePlayerDisconnect(String game, int uid) {
        InnerMsg.CliDisconnect.Builder builder = InnerMsg.CliDisconnect.newBuilder();
        builder.setUid(uid);
        builder.setCode(0);
        byte[] data = builder.build().toByteArray();
        baseServer.sendMsgToServer(game, InnerMsgDef.INNER_MSG_CLIENT_DISCONNECT.ordinal(), data);
    }

    public boolean onSessionClosed(IoSession session) {
        Object obj = session.getAttribute(PLAYER_PROPERTY_ID);
        if (obj != null) {
            int uid = (int) obj;
            IoSession waitSession = waitLoginSessions.remove(uid);
            if (waitSession != null) {
                return true;
            }
            IoSession loginSession = loginSessions.remove(uid);
            if (loginSession == null) {
                return true;
            }
            createWait(loginSession, uid);
            return true;
        }
        return false;
    }

    private String getCurGame() {
        String lastGame = null;
        Object[] games = baseServer.getServerSet().getServersByType("game");
        int minLoad = 999999999;
        int minIndex = -1;
        for (int i = 0; i < games.length; ++i) {
            IoSession ses = baseServer.getServerSet().getServer(games[i].toString());
            int load = (int) ses.getAttribute("Load");
            if (load < minLoad) {
                minIndex = i;
                minLoad = load;
            }
        }
        if (minIndex >= 0) {
            lastGame = games[minIndex].toString();
        }
        return lastGame;
    }

    void onClientResume(IoSession session, byte[] bytes) {
        //客户端重连后恢复下session的相关属性
        if (!"Client".equals(session.getAttribute("Type"))) {
            logger.error("onClientResume session is not client");
            session.close(true);
            return;
        }
        ClientMsg.Login loginInfo = null;
        try {
            loginInfo = ClientMsg.Login.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            return;
        }
        int uid = loginInfo.getId();
        String lastGame = null;
        if (loginSessions.containsKey(uid)) {
            //重连后需要关闭上一次的连接
            IoSession oldSession = loginSessions.remove(uid);
            List<SendMessage> messages = getMessages(oldSession);
            if (messages != null) {
                messages.clear();
            }
            Object obj = oldSession.getAttribute("BackSer");
            if (obj != null) {
                lastGame = obj.toString();
            }
            oldSession.close(false);
        }
        if (lastGame == null) {
            lastGame = getCurGame();
        }
        String playerKey = PubUtils.getKey("player_" + uid);
        Jedis jedis = getJedis();
        try {
            jedis.hset(playerKey, "LastGame", lastGame);
        } catch (Exception e) {
            logger.error("onClientResume error", e);
        }
        session.setAttribute(PLAYER_PROPERTY_ID, uid);
        session.setAttribute(PLAYER_PROPERTY_UID, uid);
        session.setAttribute("Token", loginInfo.getToken());
        session.setAttribute("DeviceId", loginInfo.getDeviceId());
        session.setAttribute("DeviceName", loginInfo.getDeviceName());
        session.setAttribute("CliName", loginInfo.getCliName());
        session.setAttribute("CliVer", loginInfo.getCliVer());
        session.setAttribute(PLAYER_PROPERTY_MACADDR, loginInfo.getMacAddr());
        session.setAttribute("PhoneBrand", loginInfo.getPhoneBrand());
        session.setAttribute("PhoneModel", loginInfo.getPhoneModel());
        session.setAttribute("BackSer", lastGame);
        session.setAttribute(LAST_HEAT_BEAT_TIME_KEY, System.currentTimeMillis());
        loginSessions.put(uid, session);
        disconnectCache.remove(uid);
        String uidStr = uid + "";
        byte[] ids = uidStr.getBytes();
        baseServer.sendMsgToServer(lastGame, InnerMsgDef.INNER_MSG_JUST_SEND_DATA.ordinal(), ids);
        logger.info("{} onClientResume ok", uid, lastGame);
    }

    void onRecRequestLogin(IoSession session, byte[] bytes) {
        if (!"Client".equals(session.getAttribute("Type"))) {
            logger.error("onRecRequestLogin session is not client");
            session.close(true);
            return;
        }
        ClientMsg.Login loginInfo = null;
        try {
            loginInfo = ClientMsg.Login.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            return;
        }
        logger.info("玩家登录流程 请求登陆服器 onRecRequestLogin {}", loginInfo.getId());
        session.setAttribute(PLAYER_PROPERTY_ID, loginInfo.getId());
        session.setAttribute(PLAYER_PROPERTY_UID, loginInfo.getId());
        session.setAttribute("Token", loginInfo.getToken());
        session.setAttribute("DeviceId", loginInfo.getDeviceId());
        session.setAttribute("DeviceName", loginInfo.getDeviceName());
        session.setAttribute("CliName", loginInfo.getCliName());
        session.setAttribute("CliVer", loginInfo.getCliVer());
        session.setAttribute(PLAYER_PROPERTY_MACADDR, loginInfo.getMacAddr());
        session.setAttribute("PhoneBrand", loginInfo.getPhoneBrand());
        session.setAttribute("PhoneModel", loginInfo.getPhoneModel());
        Map<String, Object> params = new HashMap<>();
        params.put("uid", loginInfo.getId());
        params.put("token", loginInfo.getToken());
        String extend = loginInfo.getTestPay() + "-" + loginInfo.getAge();
        params.put("extend", extend);//扩展参数，登录服会原路返回
        waitLoginSessions.put(loginInfo.getId(), session);
        disconnectCache.remove(loginInfo.getId());
        String loginUrl = SystemConfigData.getLoginServerUrl("login_Auth");
        AsyncResult asyncResult = AsyncResult.builder()
                .msgId(HttpResMsgDef.CLIENT_LOGIN_CHECK.ordinal())
                .obj(loginInfo.getId())
                .server(baseServer)
                .build();
        HttpClientUtil.doPost(httpClientApi, loginUrl, params, asyncResult, asyncResult);
//		logger.info(" 玩家登录流程--请求登陆服务  {} {}",loginInfo.getId());
    }

    public void onCheckLogin(byte[] bytes, Object obj) {
        int uid = (Integer) obj;
//		logger.info(" 玩家登录流程--请求gate  {} {}");
        if (bytes == null) {
            logger.info("onCheckLogin fail");
        } else {
            String result = new String(bytes);
            logger.info("{}", result);
            CheckLoginResult checkLoginResult = JsonUtil.decodeToObj(result, CheckLoginResult.class);
            onRecCheckLoginRes(uid, checkLoginResult);
        }
    }

    public void onRecCheckLoginRes(int uid, CheckLoginResult loginRes) {
        IoSession cliSession = waitLoginSessions.remove(uid);
        if (cliSession == null) {
            logger.info("!m_waitLoginSessions.containsKey(id) {}", uid);
            return;
        }
        if (loginRes.getCode() == 0) {
            String name = loginRes.getName();
            String head = loginRes.getHeadUrl();
            int sex = loginRes.getSex();
            int channel = loginRes.getChannelId();
            String payInfo = loginRes.getPayInfo();
            String phone = loginRes.getPhone();
            String recruiter = loginRes.getRecruiter();
            if (payInfo == null) {
                payInfo = "";
            }
            if (phone == null) {
                phone = "";
            }
            String extend = loginRes.getExtend();
            checkLoginSuccess(cliSession, uid, name, head, sex, channel, payInfo, phone, recruiter, extend, loginRes.getIp());
        } else {
            logger.info("checkToken fail {}", loginRes.getMessage());
        }
    }

    void checkLoginSuccess(IoSession cliSession, int id, String name, String head, int sex, int channel, String payinfo,
                           String phone, String recruiter, String extend, String ip) {
        //logger.info("CheckLoginSuccess {} {} {} ",id,name,channel);
        String devId = cliSession.getAttribute("DeviceId").toString();
        String devName = cliSession.getAttribute("DeviceName").toString();
        String cliName = cliSession.getAttribute("CliName").toString();
        String cliVer = cliSession.getAttribute("CliVer").toString();
        String macAddr = cliSession.getAttribute(PLAYER_PROPERTY_MACADDR).toString();
        String phoneBrand = cliSession.getAttribute("PhoneBrand").toString();
        String phoneModel = cliSession.getAttribute("PhoneModel").toString();
        IoSession minSession = null;
        Jedis jedis = getJedis();
        String playerKey = PubUtils.getKey("player_" + id);
        String lastGame = null;
        try {
            lastGame = jedis.hget(playerKey, "LastGame");
        } catch (Exception e) {
            //e.printStackTrace();
        }
        if (lastGame != null && lastGame.length() > 0) {
            minSession = baseServer.getServerSet().getServer(lastGame);
        }
        if (minSession == null) {
            lastGame = getCurGame();
        }
        if (lastGame != null) {
            try {
                jedis.hset(playerKey, "LastGame", lastGame);
            } catch (Exception e) {
                //e.printStackTrace();
            }
            cliSession.setAttribute("BackSer", lastGame);
            String addr = ((InetSocketAddress) cliSession.getRemoteAddress()).getAddress().getHostAddress();
            InnerMsg.LoadPlayer.Builder builder = InnerMsg.LoadPlayer.newBuilder();
            builder.setUid(id);
            builder.setName(name);
            builder.setSex(sex);
            builder.setHeadurl(head);
            builder.setAddr(Strings.isBlank(ip) ? addr : ip);
            builder.setDeviceId(devId);
            builder.setDeviceName(devName);
            builder.setDeviceName(devName);
            builder.setCliname(cliName);
            builder.setCliver(cliVer);
            builder.setMacAddr(macAddr);
            builder.setPhoneBrand(phoneBrand);
            builder.setPhoneModel(phoneModel);
            builder.setFront(baseServer.getName());
            builder.setChannel(channel);
            builder.setPayinfo(payinfo);
            builder.setPhone(phone);
            builder.setRecruiter(recruiter == null ? "" : recruiter);
            RolesService rolesService = SpringContextUtil.getBean(RolesService.class);
            Roles roles = rolesService.queryById(id);
            builder.setInviteVip(roles == null ? 0 : roles.getVip());
            String[] ss = extend.split("-");
            int applyUid = Integer.parseInt(ss[1]);
            logger.info("玩家登录流程 登陆服务器验证通过 {} login success applyUid={}", id, applyUid);
            boolean testPay = "true".equals(ss[0]);
            builder.setTestPay(testPay);
            byte[] data = builder.build().toByteArray();
            if (loginSessions.containsKey(id)) {
                kickPlayer(loginSessions.remove(id), KickType.RELOGIN.ordinal());
            }
            loginSessions.put(id, cliSession);
            logger.info(" 玩家登录流程--请求逻辑服务  {} {}", id, lastGame);
            baseServer.request(lastGame, InnerMsgDef.INNER_MSG_LOAD_PLAYER.ordinal(), data, (resp) -> {
                logger.info(" 玩家登录流程--逻辑服务返回  {} {}", id);
                ClientMsg.LoginRes.Builder builder1 = ClientMsg.LoginRes.newBuilder();
                SendMessage msg = null;
                if (resp != null) {
                    byte code = resp[0];
                    if (code == 0) {//通知客户端登录成功
                        builder1.setCode(0);
                        builder1.setUid(id);
                        builder1.setName(name);
                        builder1.setHead(head);
                        builder1.setSex(sex);
                        msg = new SendMessage(ClientMsgDef.CLIENT_LOGIN_RES.ordinal(), builder1.build().toByteArray());
                    } else {
                        try {
                            jedis.hset(playerKey, "LastGame", "");
                        } catch (Exception e) {
                            logger.error("checkLoginSuccess error1 ", e);
                        }
                        builder1.setCode(code);
                        msg = new SendMessage(ClientMsgDef.CLIENT_LOGIN_RES.ordinal(), builder1.build().toByteArray(), true);
                    }
                } else {
                    builder1.setCode(2);
                    msg = new SendMessage(ClientMsgDef.CLIENT_LOGIN_RES.ordinal(), builder1.build().toByteArray(), true);
                }
                if (msg.close) {
                    loginSessions.remove(id);
                }
                sendMessageToClient(cliSession, msg, false);
            });
        } else {
            try {
                jedis.hset(playerKey, "LastGame", "");
            } catch (Exception e) {
                logger.error("checkLoginSuccess error2 ", e);
            }
            ClientMsg.LoginRes.Builder builder = ClientMsg.LoginRes.newBuilder();
            builder.setCode(3);
            SendMessage msg = new SendMessage(ClientMsgDef.CLIENT_LOGIN_RES.ordinal(), builder.build().toByteArray(), true);
            sendMessageToClient(cliSession, msg, true);
        }
    }

    public void onRecSyncLoad(IoSession session, byte[] bytes) {
        InnerMsg.SyncLoad loadData = null;
        try {
            loadData = InnerMsg.SyncLoad.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        session.setAttribute("Load", loadData.getLoad());
    }

    private int getUidBySession(IoSession session) {
        Object obj = session.getAttribute(PLAYER_PROPERTY_ID);
        if (obj != null) {
            return (int) obj;
        }
        return 0;
    }

    public void onRecCustomMsg(IoSession session, byte[] bytes) {
        int uid = getUidBySession(session);
        if (uid == 0) {
            session.close(true);
            return;
        }
        InnerMsg.ForwardMsg.Builder builder = InnerMsg.ForwardMsg.newBuilder();
        builder.setMsgid(ClientMsgDef.CLIENT_CUSTOM.ordinal());
        builder.setUid(uid);
        builder.setData(ByteString.copyFrom(bytes));
        byte[] data = builder.build().toByteArray();
        baseServer.sendMsgToServer(session.getAttribute("BackSer").toString(), InnerMsgDef.INNER_MSG_FORWARD.ordinal(), data);
    }

    public void onRecRequestMsg(IoSession session, byte[] bytes) {
        int uid = getUidBySession(session);
        if (uid == 0) {
            session.close(true);
            return;
        }
        InnerMsg.ForwardMsg.Builder builder = InnerMsg.ForwardMsg.newBuilder();
        builder.setMsgid(ClientMsgDef.CLIENT_REQUEST.ordinal());
        builder.setUid(uid);
        if (bytes != null) {
            builder.setData(ByteString.copyFrom(bytes));
        }
        byte[] data = builder.build().toByteArray();
        baseServer.sendMsgToServer(session.getAttribute("BackSer").toString(), InnerMsgDef.INNER_MSG_FORWARD.ordinal(), data);
    }

    public void onRecForwardMsg(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.ForwardMsg forwardMsg = InnerMsg.ForwardMsg.parseFrom(bytes);
        int uid = forwardMsg.getUid();
        IoSession cliSession = loginSessions.get(uid);
        if (cliSession == null) {
            return;
        }
        SendMessage msg = new SendMessage(forwardMsg.getMsgid(), forwardMsg.getData().toByteArray());
        sendMessageToClient(cliSession, msg, false);
    }

    public void onRecBroadCast(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.BroadCastToClients borad = InnerMsg.BroadCastToClients.parseFrom(bytes);
        SendMessage msg = new SendMessage(borad.getMsgid(), borad.getData().toByteArray());
        int count = borad.getUidsCount();
        for (int i = 0; i < count; ++i) {
            int uid = borad.getUids(i);
            IoSession cli = loginSessions.get(uid);
            if (cli != null) {
                sendMessageToClient(cli, msg, false);
            }
        }
    }

    public void onRecBroadCastAll(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.BroadCastToAllClients broadMsg = InnerMsg.BroadCastToAllClients.parseFrom(bytes);
        SendMessage msg = new SendMessage(broadMsg.getMsgid(), broadMsg.getData().toByteArray());
        for (IoSession _session : loginSessions.values()) {
            sendMessageToClient(_session, msg, false);
        }
    }

    public void onChangeBack(int reqId, byte[] data) throws InvalidProtocolBufferException {
        InnerMsg.ChangeBack back = InnerMsg.ChangeBack.parseFrom(data);
        int uid = back.getUid();
        int backId = back.getBack();
        IoSession bakSer = baseServer.getServerSet().getServer(backId);
        if (bakSer == null) {
            return;
        }
        if (!loginSessions.containsKey(uid)) {
            return;
        }
        IoSession cli = loginSessions.get(uid);
        cli.setAttribute("BackSer", bakSer.getAttribute(PLAYER_PROPERTY_NAME));
        baseServer.response(reqId, null);
    }

    public void kickPlayer(IoSession session, int code) {
        if (code >= 0) {
            ClientMsg.Kick.Builder build = ClientMsg.Kick.newBuilder();
            build.setCode(code);
            SendMessage msg = new SendMessage(ClientMsgDef.CLIENT_KICK.ordinal(), build.build().toByteArray(), true);
            sendMessageToClient(session, msg, true);
        }
    }

    public void onRecKickPlayer(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.KickPlayer kick = InnerMsg.KickPlayer.parseFrom(bytes);
        int uid = kick.getUid();
        int code = kick.getCode();
        if (loginSessions.containsKey(uid)) {
            IoSession cliSession = loginSessions.remove(uid);
            kickPlayer(cliSession, code);
            if (code == KickType.FROZEN.ordinal()) {
                String backSerName = cliSession.getAttribute("BackSer").toString();
                InnerMsg.CliDisconnect.Builder builder = InnerMsg.CliDisconnect.newBuilder();
                builder.setUid(uid);
                builder.setCode(KickType.FROZEN.ordinal());
                byte[] data = builder.build().toByteArray();
                InnerMsg.Frozen.Builder build1 = InnerMsg.Frozen.newBuilder();
                build1.setUid(uid);
                baseServer.sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_FROZEN.ordinal(), build1.build().toByteArray());
                baseServer.sendMsgToServer(backSerName, InnerMsgDef.INNER_MSG_CLIENT_DISCONNECT.ordinal(), data);
            }
        }
    }

    public void onRecHeartBeat(IoSession session, byte[] bytes) throws Exception {
        long curTime = System.currentTimeMillis();
        SendMessage msg = new SendMessage(ClientMsgDef.CLIENT_HEART_BEAT.ordinal(), (curTime + "").getBytes("UTF-8"));
        sendMessageToClient(session, msg, false);
        Object attribute = session.getAttribute(PLAYER_PROPERTY_UID);
        if (Strings.isNotBlank(attribute.toString())) {
            getJedis().expire(attribute.toString(), 600L);
        }
        session.setAttribute(LAST_HEAT_BEAT_TIME_KEY, curTime);
    }

    public void onUpdateRateLimit(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        ConfigService configService = SpringContextUtil.getBean(ConfigService.class);
        Config rateLimiter = configService.getById("RateLimiter");
        String value = rateLimiter.getValue();
        try {
            if (StringUtils.isNotEmpty(value)) {
                JSONObject jsonObject = JSONObject.parseObject(value);
                int limit = jsonObject.getIntValue("limit");
                int errorLimit = jsonObject.getIntValue("errorLimit");
                rateLimiterManager.setLimit(limit, errorLimit);
                logger.info("更新限流值为  {} ", value);
            }
        } catch (Exception e) {
            logger.info("更新限流错误  {} ", value);
        }
    }

    @Override
    public void onStop() {
        for (IoSession session : waitLoginSessions.values()) {
            session.close(false);
        }
        waitLoginSessions.clear();
        for (IoSession session : loginSessions.values()) {
            //关闭所有前端的连接
            session.close(false);
        }
        loginSessions.clear();
        disconnectCache.clear();
        syncActor.stop();
    }

    @Override
    public BaseServer getServer() {
        return baseServer;
    }

    @Override
    public void onAddClient(IoSession session) {
        Random rand = new Random();
        short code = (short) (rand.nextInt() % 65536);
        session.setAttribute("VerifyCode", code);
        int ver = Verify.getVersion();
        byte[] src = new byte[6];
        src[0] = (byte) (ver >> 0);
        src[1] = (byte) (ver >> 8);
        src[2] = (byte) (ver >> 16);
        src[3] = (byte) (ver >> 24);
        src[4] = (byte) (code >> 0);
        src[5] = (byte) (code >> 8);
        byte[] keyBytes = encrypt(src);
        SendMessage msg = new SendMessage(ClientMsgDef.CLIENT_KEY.ordinal(), keyBytes);
        sendMessageToClient(session, msg, true);
        session.setAttribute(LAST_HEAT_BEAT_TIME_KEY, System.currentTimeMillis());
    }

    private int writeMsg(IoSession session, int code) {
        List<SendMessage> messages = getMessages(session);
        if (messages != null) {
            while (messages.size() > 0) {
                SendMessage message = messages.remove(0);
                if (message.msgID != -1) {
                    session.write(message);
                    if (message.close) {
                        return 1;
                    }
                }
            }
        }
        return code;
    }

    @Override
    public void execute() {
        if (loginSessions.size() > 0) {
            long now = System.currentTimeMillis();
            Iterator<Map.Entry<Integer, IoSession>> iter = loginSessions.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<Integer, IoSession> entry = iter.next();
                IoSession session = entry.getValue();
                int code = 0;
                long lastHeart = (long) session.getAttribute(LAST_HEAT_BEAT_TIME_KEY);
                if (lastHeart > 0 && now > lastHeart + 3600000) {//服务器的最大心跳1小时
                    logger.info("心跳间隔大于十分钟时间 {}   状态 {} {} {} {}", lastHeart, code, entry.getKey(), session);
                    code = 1;
                }
                if (writeMsg(session, code) > 0) {
                    Object _obj = session.getAttribute("BackSer");
                    if (_obj != null) {
                        notifyGamePlayerDisconnect(_obj.toString(), entry.getKey());
                    }
                    try {
                        logger.info("写入消息失败 上次心跳时间 {}   状态 {}  {}  {} ", lastHeart, code, entry.getKey(), session);
                    } catch (Exception e) {
                    }
                    iter.remove();
                    session.close(true);
                }
            }
        }
        if (disconnectCache.size() == 0) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, DisconnectWaiter>> iter = disconnectCache.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Integer, DisconnectWaiter> entry = iter.next();
            if (entry.getValue().tick(now)) {
                iter.remove();
            }
        }
    }

    @Override
    public List<String> heartList() {
        List<String> list = new ArrayList<>();
        ServerSet serverSet = baseServer.getServerSet();
        Object[] sers = serverSet.getServersByType("game");
        for (Object ser : sers) {
            list.add(ser.toString());
        }
        sers = serverSet.getServersByType("back");
        for (Object ser : sers) {
            list.add(ser.toString());
        }
        return list;
    }

    @Override
    public Jedis getJedis() {
        return baseServer.getJedis();
    }
}
