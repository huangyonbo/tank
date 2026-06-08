package framework.logic;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import framework.*;
import framework.Store.CacheObject;
import framework.Store.StoreData;
import framework.game.*;
import framework.mybatis.DataManager;
import framework.mybatis.domain.*;
import framework.mybatis.service.impl.*;
import framework.net.InnerMsgDef;
import framework.net.SendMessage;
import framework.net.message.InnerMsg;
import io.netty.util.internal.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;

@Component
public class StoreLogic implements ILogic {

    public enum LoadRoleCode {
        SUCCESS, // 成功
        NOROLE, // 没有角色
        FROZEN, // 被冻结
        EXCEPTION, // 状态异常
        END
    }

    public enum RoleState {
        NORMAL, // 正常
        FROZEN, // 冻结
        BACKOPT, // 后台操作
        WARNING, // 预警冻结
        END
    }

    enum LogType {
        itemlog, proplog, maillog, playlog, playerLog, activityLog, gmLog, areaRoom, payError,kills,killsMj,
        count
    }

    enum MailState {
        STATE_UNREAD, STATE_READ, STATE_DEL,
    }

    private Logger logger = LoggerFactory.getLogger(GateLogic.class);
    private BaseServer baseServer;

    private StringBuilder[] strLogs = new StringBuilder[LogType.count.ordinal()];

    private LinkedList<OfflineData> offlineData = new LinkedList<>();
    private List<String> usedName = new ArrayList<>();
    //	private List<Integer> m_listNeedCheck = new ArrayList<>();
    //玩家数据缓存
    private List<CacheObject> rolesCache = new ArrayList<>();
    @Autowired
    private DataManager dataManager;
    @Autowired
    private RolesService rolesService;
    //@Autowired
    //private GameLogService gameLogService;
    @Autowired
    private OfflineDataService offlineDataService;
    //@Autowired
    //private BlackListService blackListService;
    @Autowired
    private CheatService cheatService;

    @Override
    public boolean onInit(BaseServer ser) {
        baseServer = ser;
        baseServer.addRequestListener(this, InnerMsgDef.INNER_MSG_REQUEST_ROLE_DATA.ordinal(), "onRecRequestRoleData");
        baseServer.addRequestListener(this, InnerMsgDef.INNER_MSG_REQ_OFFLINE_ROLE.ordinal(), "onReqOffRoleData");
        baseServer.addRequestListener(this, InnerMsgDef.INNER_MSG_REQ_READ_ROLE.ordinal(), "onReqReadRoleData");
        baseServer.addRequestListener(this, InnerMsgDef.INNER_MSG_REQ_ROLE_PARAM.ordinal(), "onReqRoleParam");
        baseServer.addRequestListener(this, InnerMsgDef.INNER_MSG_STORE_ROLE_PARAM.ordinal(), "onStoreRoleParam");
        baseServer.addRequestListener(this, InnerMsgDef.INNER_MSG_CHANGE_NAME.ordinal(), "onReqChangeName");
        baseServer.addRequestListener(this, InnerMsgDef.INNER_MSG_REQ_OFFLINE.ordinal(), "onReqOffline");
        //m_baseServer.AddRequestListener(this, InnerMsgDef.INNER_MSG_ADD_BLACKLIST.ordinal(),"onReqAddBlackList");
        //m_baseServer.AddRequestListener(this, InnerMsgDef.INNER_MSG_DEL_BLACKLIST.ordinal(),"onReqDelBlackList");
        baseServer.addRequestListener(this, InnerMsgDef.INNER_MSG_REG_ROLE_DATA.ordinal(), "onRegRoleData");
        baseServer.addRequestListener(this, InnerMsgDef.INNER_REQ_REDEEM_CODE.ordinal(), "onReqUseRedeemCode");
        baseServer.addRequestListener(this, InnerMsgDef.INNER_REQ_CARD_ITEM.ordinal(), "onReqUseCardItem");
        //m_baseServer.AddRequestListener(this, InnerMsgDef.INNER_REQ_ACTIVITY_DATA.ordinal(), "onReqActivityData");
        //m_baseServer.AddRequestListener(this, InnerMsgDef.INNER_REQ_VERSION_DATA.ordinal(), "onReqVersionData");
        //m_baseServer.AddRequestListener(this, InnerMsgDef.INNER_REQ_DAILY_DATA.ordinal(), "onReqDailyData");
        baseServer.addRequestListener(this, InnerMsgDef.INNER_MSG_REALNAME.ordinal(), "onRecRealName");
        baseServer.addRequestListener(this, InnerMsgDef.INNER_MSG_EXCHANGE_CARD.ordinal(), "onReqExchangeCard");
        baseServer.addRequestListener(this, InnerMsgDef.INNER_MSG_CARD_STATS.ordinal(), "onReqCardStats");
        baseServer.addRequestListener(this, InnerMsgDef.INNER_MSG_BIND_PROXYID.ordinal(), "onRecProxyBind");
        baseServer.addRequestListener(this, InnerMsgDef.INNER_MSG_EXECUTE_SQL_METHOD.ordinal(), "onExecuteSqlMethod");

        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_STORE_ROLE_DATA.ordinal(), "onRecStoreRoleData");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_STORE_OFFROLE.ordinal(), "onStoreOffRoleData");
        //baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_GAME_LOG.ordinal(), "onRecGameLog");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_ITEM_LOG.ordinal(), "onRecItemLog");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_PROP_LOG.ordinal(), "onRecPropLog");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_MAIL_LOG.ordinal(), "onRecMailLog");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_PLAY_LOG.ordinal(), "onRecPlayLog");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_PLAYER_LOG.ordinal(), "onRecPlayerLog");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_AREA_ROOM_LOG.ordinal(), "onRecAreaRoomLog");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_ACTIVITY_LOG.ordinal(), "onRecActivityLog");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_GM_LOG.ordinal(), "onRecGmLog");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_KILL_FISH_LOG.ordinal(), "onKillFishLog");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_KILL_FISH_LOG_MJ.ordinal(), "onKillFishLogMj");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_OFFLINEDATA.ordinal(), "onRecOfflineData");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_DEL_OFFLINE.ordinal(), "onRecDelOffline");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_FROZEN.ordinal(), "onRecFrozen");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_UNFROZEN.ordinal(), "onRecUnFrozen");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_ADVICE.ordinal(), "onRecAdvice");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_WARNING_ITEMSCORE.ordinal(), "onRecWarningItemScore");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_WARNING_MOJIN.ordinal(), "onRecWarningMoJin");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_MOJIN_ROOM_RECORD.ordinal(), "onRecMoJinRoomRecord");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_STORE_MOJIN_DATA.ordinal(), "onRecMoJinRoomData");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_UPDATE_DAU.ordinal(), "onRecUpdateDau");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_UPDATE_ONLINEPEAK.ordinal(), "onRecUpdateOnlinePeak");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_RECRUIT.ordinal(), "onRecRecruit");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_PAY_CALL_BACK_ERROR.ordinal(), "onRecPayErrorLog");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_ACTIVITY_LUCKY_PUZZLE_LOG.ordinal(), "onRecActivityLuckyPuzzleLog");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_FUN_FISH_RECORD.ordinal(), "onRecFunFishRecord");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_ACTIVITY_FISH_POND_RECORD.ordinal(), "onRecActivityFishPondRecord");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_ACTIVITY_SYSTEM_FISH_RECORD.ordinal(), "onRecActivitySystemFishRecord");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_ACTIVITY_FISH_POND_MSG_RECORD.ordinal(), "onRecActivityFishMsgRecord");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_SYNC_MYSTERY_LEGEND_PLAY_AND_WIN.ordinal(), "onSyncMysteryLegendPlayAndWin");

        //新的邮件系统
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_SEND_MAIL.ordinal(), "onRecSendMail");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_SEND_ITEM.ordinal(), "onRecSendItems");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_DEL_MAIL.ordinal(), "onRecDelMail");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_DEL_SEND_ITEM_RECORD.ordinal(), "onRecDelSendItems");
        baseServer.addRequestListener(this, InnerMsgDef.INNER_MSG_QUERY_MAIL.ordinal(), "onRecQueryMail");
        baseServer.addRequestListener(this, InnerMsgDef.INNER_MSG_QUERY_SEND_ITEM_RECORD.ordinal(), "onRecQuerySendItems");
        baseServer.addRequestListener(this, InnerMsgDef.INNER_MSG_READ_MAIL.ordinal(), "onRecReadMail");
        baseServer.addRequestListener(this, InnerMsgDef.INNER_REQ_SEND_MAIL.ordinal(), "onReqSendMail");

        //命令行修改属性
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_CMD_UPDATE_PRO.ordinal(), "onRecUpdatePro");
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_CMD_UPDATE_REC.ordinal(), "onRecUpdateRec");
        baseServer.addRequestListener(this, InnerMsgDef.INNER_MSG_CREATE_GUILD.ordinal(), "onReqCreateGuild");
        baseServer.addRequestListener(this, InnerMsgDef.INNER_MSG_DELETE_GUILD.ordinal(), "onReqDeleteGuild");

        //玩家在线优化
        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_LOGIN_SUCC.ordinal(), "onRecChangeOnline");

        //Vip等级礼包的的顶下
        baseServer.addRequestListener(this, InnerMsgDef.INNER_MSG_STORE_ORDER.ordinal(), "onRecVipGiftOrder");
        baseServer.addRequestListener(this, InnerMsgDef.INNER_MSG_FULL_GAME_ITEMS.ordinal(), "onRecFullGameItems");

        baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_SEND_BULLET_ADD_SPEED_RECORD.ordinal(), "onRecBulletAddSpeed");
        File logDir = new File("/logs");
        if (!logDir.exists()) {
            logDir.mkdir();
        }
        for (int i = 0; i < LogType.count.ordinal(); ++i) {
            strLogs[i] = new StringBuilder("");
        }
        return true;
    }

    @Override
    public void initStop() {
        Map<String, Boolean> map = baseServer.getMustCloseBeforeMe();
        map.clear();
        ServerSet ss = baseServer.getServerSet();
        Object[] servers = ss.getServersByType("game");
        for (int i = 0; i < servers.length; i++) {
            map.put(servers[i].toString(), false);
        }
        servers = ss.getServersByType("public");
        for (int i = 0; i < servers.length; i++) {
            map.put(servers[i].toString(), false);
        }
    }

    @Override
    public void onReady() {
        rolesService.resetOnline();
        List<String> names = rolesService.loadNames();
        if (names != null) {
            usedName.addAll(names);
        }
        baseServer.onLogicReady();
        baseServer.setTimer(this, 60000, -1, "onRecLog", null);
        //m_baseServer.SetTimer(this, 1800000, -1, "CheckWarning", null);
    }


    @Override
    public void execute() {
        long now = System.currentTimeMillis();
        for (int i = 0; i < rolesCache.size(); ) {
            CacheObject tick = rolesCache.get(i);
            if (now >= tick.preUseTime + 300000) {
                tick.role = null;
                //5分钟没有被引用就清除缓存
                rolesCache.remove(i);
            } else {
                i++;
            }
        }
    }

    @Override
    public BaseServer getServer() {
        return baseServer;
    }

    private Roles getRoles(int uid, boolean load) {
        CacheObject target = rolesCache.stream().filter(obj -> obj.role.getId() == uid).findFirst().orElse(null);
        if (target == null && load) {
            Roles role = rolesService.queryById(uid);
            if (role != null) {
                target = new CacheObject();
                target.role = role;
                rolesCache.add(target);
            }
        }
        if (target != null) {
            target.preUseTime = System.currentTimeMillis();
            return target.role;
        }
        return null;
    }

    void onRecChangeOnline(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.RequestRoleData reqRoleData = InnerMsg.RequestRoleData.parseFrom(bytes);
        int uid = reqRoleData.getUid();
        String addr = reqRoleData.getAddr();
        String devId = reqRoleData.getDeviceId();
        String cliName = reqRoleData.getCliname();
        String phoneBrand = reqRoleData.getPhoneBrand();
        String phoneModel = reqRoleData.getPhoneModel();
        String phoneNo = reqRoleData.getPhone();

        String loginTime = baseServer.getTimeFormat().format(baseServer.getServerTime());
        Roles roles = getRoles(uid, false);
        if (roles != null) {
            roles.setLoginTime(loginTime);
            roles.setOnline(1);
            roles.setLoginAddress(addr);
            roles.setLoginDevice(devId);
            roles.setPhoneBrand(phoneBrand);
            roles.setPhoneModel(phoneModel);
            roles.setPhoneNo(phoneNo);
        }
        rolesService.lambdaUpdate().
                set(Roles::getOnline, 1).
                set(Roles::getLoginAddress, addr).
                set(Roles::getLoginDevice, devId).
                set(Roles::getCliName, cliName).
                set(Roles::getLoginTime, loginTime).
                set(Roles::getPhoneBrand, phoneBrand).
                set(Roles::getPhoneModel, phoneModel).
                set(Roles::getPhoneNo, phoneNo).
                eq(Roles::getId, uid).
                update();
    }

    void onRecRequestRoleData(int reqid, byte[] bytes) throws InvalidProtocolBufferException, SQLException {
        //logger.info("onRecRequestRoleData");
        InnerMsg.RequestRoleData reqRoleData = InnerMsg.RequestRoleData.parseFrom(bytes);
        int uid = reqRoleData.getUid();
//        logger.info(" 玩家登录流程--Store逻辑服务处理  {} {}",uid);
        //String addr = reqRoleData.getAddr();
        //String devId = reqRoleData.getDeviceId();
        InnerMsg.LoadRoleData.Builder builder = InnerMsg.LoadRoleData.newBuilder();
        do {
            Roles roles = getRoles(uid, true);
            if (roles == null) {
                builder.setCode(LoadRoleCode.NOROLE.ordinal());
                break;
            }
            builder.setHeadid(roles.getHeadId());
            int status = roles.getStatus();
			/*
			if (roles.getOnline() != 0) {
				builder.setCode(LoadRoleCode.EXCEPTION.ordinal());
				break;
			} else */
            if (status == RoleState.NORMAL.ordinal()) {
                builder.setRegtime(roles.getCreateTime());
                builder.setData(ByteString.copyFrom(roles.getParam()));
                builder.setName(roles.getUserName());
                builder.setSex(roles.getSex());
                boolean realName = !StringUtil.isNullOrEmpty(roles.getRealName());
                builder.setRealname(realName);
                builder.setInviteVip(roles.getVip());
                builder.setCode(LoadRoleCode.SUCCESS.ordinal());
            } else if (status == RoleState.FROZEN.ordinal() || status == RoleState.WARNING.ordinal()) {
                builder.setCode(LoadRoleCode.FROZEN.ordinal());
                break;
            } else {
                builder.setCode(LoadRoleCode.EXCEPTION.ordinal());
                break;
            }
        } while (false);
        byte[] data = builder.build().toByteArray();
        baseServer.response(reqid, data);
    }

    void onReqReadRoleData(int reqid, byte[] bytes) throws InvalidProtocolBufferException, SQLException {
        InnerMsg.RequestRoleData reqRoleData = InnerMsg.RequestRoleData.parseFrom(bytes);
        int uid = reqRoleData.getUid();
        InnerMsg.LoadRoleData.Builder builder = InnerMsg.LoadRoleData.newBuilder();
        Roles role = getRoles(uid, true);
        if (role != null) {
            builder.setCode(LoadRoleCode.SUCCESS.ordinal());
            builder.setData(ByteString.copyFrom(role.getParam()));
        } else {
            builder.setCode(LoadRoleCode.NOROLE.ordinal());
        }
        baseServer.response(reqid, builder.build().toByteArray());
    }

    void onReqRoleParam(int reqid, byte[] bytes) {
        int uid = Integer.parseInt(new String(bytes));
        InnerMsg.RoleParam.Builder res = InnerMsg.RoleParam.newBuilder();
        Roles roles = getRoles(uid, false);
        if (roles != null) {
            res.setParam(ByteString.copyFrom(roles.getParam()));
            res.setNickname(roles.getUserName());
        } else {
            String sql = "select `param`, `username` from roles where id=" + uid;
            RolesService service = dataManager.getService(RolesService.class);
            Roles one = service.lambdaQuery().eq(Roles::getId, uid).one();
            res.setParam(ByteString.copyFrom(one.getParam()));
            res.setNickname(one.getUserName());
        }
        baseServer.response(reqid, res.build().toByteArray());
    }

    void onStoreRoleParam(int reqid, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.RoleParam roleParam = InnerMsg.RoleParam.parseFrom(bytes);
        int uid = roleParam.getUid();
        byte[] param = roleParam.getParam().toByteArray();
        InnerMsg.ComResponse.Builder res = InnerMsg.ComResponse.newBuilder();
        Roles role = getRoles(uid, true);
        if (role == null) {
            logger.error("请求存储玩家属性异常, 玩家[{}]不存在", uid);
            baseServer.response(reqid, "玩家不存在".getBytes(StandardCharsets.UTF_8));
        } else {
            role.setParam(param);
            RolesService service = dataManager.getService(RolesService.class);
            service.updateById(role);
            baseServer.response(reqid, new byte[0]);
        }
    }

    void onReqOffRoleData(int reqid, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.RequestRoleData reqRoleData = InnerMsg.RequestRoleData.parseFrom(bytes);
        int uid = reqRoleData.getUid();
        InnerMsg.LoadRoleData.Builder builder = InnerMsg.LoadRoleData.newBuilder();
        byte[] param = rolesService.lambdaQuery().eq(Roles::getOnline, 0).eq(Roles::getId, uid).one().getParam();
        if (param != null) {
            builder.setCode(LoadRoleCode.SUCCESS.ordinal());
            builder.setData(ByteString.copyFrom(param));
            rolesService.lambdaUpdate().set(Roles::getStatus, RoleState.BACKOPT.ordinal()).eq(Roles::getId, uid).update();
        } else {
            builder.setCode(-1);
        }
        baseServer.response(reqid, builder.build().toByteArray());
    }

    void onStoreOffRoleData(IoSession session, byte[] bytes) throws InvalidProtocolBufferException, SQLException {
        InnerMsg.StoreRoleData storeRoleData = InnerMsg.StoreRoleData.parseFrom(bytes);
        int uid = storeRoleData.getUid();
        String time = baseServer.getTimeFormat().format(baseServer.getServerTime());
        byte[] datas = storeRoleData.getData().toByteArray();
        Roles role = getRoles(uid, false);
        if (role == null) {
            role = rolesService.lambdaQuery().eq(Roles::getStatus, RoleState.BACKOPT.ordinal()).eq(Roles::getId, uid).one();
        }
        if (role != null) {
//            role.setStatus(RoleState.NORMAL.ordinal());
            role.setParam(datas);
            role.setStoreTime(time);
            rolesService.updateById(role);
        }
    }

    void onRecStoreRoleData(IoSession session, byte[] bytes)
            throws InvalidProtocolBufferException, SQLException {
        InnerMsg.StoreRoleData storeRoleData = InnerMsg.StoreRoleData.parseFrom(bytes);
        String name = storeRoleData.hasName() ? storeRoleData.getName() : null;
        storePlayer(storeRoleData.getUid(), name, storeRoleData.getHeadurl(), storeRoleData.getSex(), storeRoleData.getData().toByteArray(), storeRoleData.getOffline(), storeRoleData.getLastOpt(),storeRoleData);
    }

    char[] tail = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

    String deepCheck(String name, int deep) {
        String check = "";
        for (int i = 0; i < tail.length; ++i) {
            check = name + tail[i];
            if (deep > 0) {
                String res = deepCheck(check, deep - 1);
                if (!res.isEmpty()) {
                    return res;
                }
            } else if (!usedName.contains(check)) {
                return check;
            }
        }

        return "";
    }

    String checkName(String name) {
        int deep = 0;
        while (true) {
            String res = deepCheck(name, deep);
            if (!res.isEmpty()) {
                return res;
            }
            ++deep;
            if (deep >= 20) {
                return null;
            }
        }
    }

    void onReqChangeName(int reqid, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.ChangeName change = InnerMsg.ChangeName.parseFrom(bytes);
        int uid = change.getUid();
        String newName = change.getNewName();
        InnerMsg.ComResponse.Builder build = InnerMsg.ComResponse.newBuilder();
        do {
            if (usedName.contains(newName)) {
                build.setCode(1);
                break;
            }
            Roles role = getRoles(uid, true);
            if (role == null) {
                build.setCode(2);
                break;
            }
            StringBuilder sql = new StringBuilder();
            sql.append("update roles set username=? where id=?");
            usedName.add(newName);
            String old = role.getUserName();
            role.setUserName(newName);
            rolesService.updateById(role);
            usedName.remove(old);
            build.setCode(0);
        } while (false);
        baseServer.response(reqid, build.build().toByteArray());
    }

    void onRegRoleData(int reqid, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.StoreRoleData storeRoleData = InnerMsg.StoreRoleData.parseFrom(bytes);
        int id = storeRoleData.getUid();
        String name = storeRoleData.getName();
        String headUrl = storeRoleData.getHeadurl();
        int sex = storeRoleData.getSex();
        byte[] data = storeRoleData.getData().toByteArray();
        int offline = storeRoleData.getOffline();
        String addr = storeRoleData.getAddr();
        String devId = storeRoleData.getDeviceId();
        String devName = storeRoleData.getDeviceName();
        String cliName = storeRoleData.getCliname();
        int channel = storeRoleData.getChannel();
        String phoneBrand = storeRoleData.getPhoneBrand();
        String phoneModel = storeRoleData.getPhoneModel();
        // check head
        int headid = 4;
        if (StringUtils.isNotEmpty(headUrl)) {
            HeadsService headsService = dataManager.getService(HeadsService.class);
            Heads head = headsService.queryIdByUid(id);
            if (head == null) {
                head = new Heads();
                head.setUid(id);
                head.setUrl(headUrl);
                headsService.save(head);
                headid = head.getId();
                logger.info("Add head {} {}", headid, headUrl);
            } else {
                headid = head.getId();
                if (!headUrl.equals(head.getUrl())) {
                    head.setUrl(headUrl);
                    headsService.updateById(head);
                    logger.info("Update head {} {}", headid, headUrl);
                }
            }
        }
        Roles roles = getRoles(id, true);
        if (roles != null) {
            return;
        }
        // check name
        if (usedName.contains(name)) {
            String res = checkName(name);
            if (res == null) {
                return;
            }
            name = res;
        }
        usedName.add(name);
        String nowTime = baseServer.getTimeFormat().format(baseServer.getServerTime());
        roles = new Roles();
        roles.setId(id);
        roles.setHeadId(headid);
        roles.setUserName(name);
        roles.setSex(sex);
        roles.setOnline(1);
        roles.setCreateTime(nowTime);
        roles.setLoginTime(nowTime);
        roles.setStoreTime(nowTime);
        roles.setParam(data);
        roles.setOnline(offline == 1 ? 0 : 1);
        roles.setRegAddress(addr);
        roles.setLoginAddress(addr);
        roles.setRegDevice(devId);
        roles.setLoginDevice(devId);
        roles.setCliName(cliName);
        roles.setDevName(devName);
        roles.setChannel(channel);
        roles.setPhoneBrand(phoneBrand);
        roles.setPhoneModel(phoneModel);
        roles.setType(0);
        roles.setStatus(RoleState.NORMAL.ordinal());
        rolesService.save(roles);
        InnerMsg.LoadRoleData.Builder builder = InnerMsg.LoadRoleData.newBuilder();
        builder.setName(name);
        builder.setRegtime(nowTime);
        builder.setHeadid(headid);
        baseServer.response(reqid, builder.build().toByteArray());
    }

    void storePlayer(int id, String name, String headUrl, int sex, byte[] data, int offline, String lastOpt, InnerMsg.StoreRoleData storeRoleData) {
        int headId = 0;
        if (StringUtils.isNotEmpty(headUrl)) {
            HeadsService headsService = dataManager.getService(HeadsService.class);
            Heads head = headsService.queryIdByUid(id);
            if (head == null) {
                head = new Heads();
                head.setUid(id);
                head.setUrl(headUrl);
                headsService.save(head);
                headId = head.getId();
                logger.info("Add head {} {}", headId, headUrl);
            } else {
                headId = head.getId();
                if (!headUrl.equals(head.getUrl())) {
                    head.setUrl(headUrl);
                    headsService.updateById(head);
                    logger.info("Update head {} {}", headId, headUrl);
                }
            }
        }
        String storeTime = baseServer.getTimeFormat().format(baseServer.getServerTime());
        int online = offline == 1 ? 0 : 1;
        Roles roles = getRoles(id, true);
        if (roles != null) {
            roles.setParam(data);
            roles.setUserName(name);
            roles.setOnline(online);
            roles.setBombcoin(storeRoleData.getBombCoin());
            roles.setBombItem(storeRoleData.getBombItem());
            roles.setStoreTime(storeTime);
            roles.setVip(storeRoleData.getVip());
            if (headId > 0) {
                roles.setHeadId(headId);
            }
            if (!lastOpt.isEmpty()) {
                roles.setLastOpt(lastOpt);
            }
            rolesService.updateById(roles);
        }
    }

    void onReqOffline(int reqid, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.ReqOfflineData req = InnerMsg.ReqOfflineData.parseFrom(bytes);
        int uid = req.getUid();
        OfflineDataService offlineDataService = dataManager.getService(OfflineDataService.class);
        for (OfflineData data : offlineData) {
            if (data.getUid() == uid) {
                offlineDataService.save(data);
                offlineData.remove(data);
            }
        }
        List<OfflineData> res = offlineDataService.queryByUid(uid);
        InnerMsg.ReqOfflineDataRes.Builder reqRes = InnerMsg.ReqOfflineDataRes.newBuilder();
        for (int i = 0; i < res.size(); ++i) {
            OfflineData data = res.get(i);
            reqRes.addId(data.getId());
            reqRes.addType(data.getType());
            reqRes.addContext(data.getContext());
            reqRes.addReason(data.getReason());
        }
        offlineDataService.setState(uid);
        baseServer.response(reqid, reqRes.build().toByteArray());
    }

    void onReqAddBlackList(int reqid, byte[] bytes) throws InvalidProtocolBufferException {
		/*
		InnerMsg.BlackList list = InnerMsg.BlackList.parseFrom(bytes);
		BlackList bl = new BlackList();
		bl.setType(list.getType());
		bl.setContext(list.getContext());
		bl.setCreateTime(m_baseServer.getTimeFormat().format(m_baseServer.GetServerTime()));
		dataManager.getService(BlackListService.class).save(bl);
		m_baseServer.Response(reqid, null);
		 */
    }

    void onReqDelBlackList(int reqid, byte[] bytes) throws InvalidProtocolBufferException {
		/*
		InnerMsg.BlackList list = InnerMsg.BlackList.parseFrom(bytes);
		dataManager.getService(BlackListService.class).del(list.getType(), list.getContext());
		m_baseServer.Response(reqid, null);
		 */
    }

    void onRecDelOffline(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.DelOfflineData del = InnerMsg.DelOfflineData.parseFrom(bytes);
        if (del.getIdList().size() > 0) {
            dataManager.getService(OfflineDataService.class).setState(del.getIdList());
        }
    }

    void onFrozen(int uid, int code) {
        Roles role = getRoles(uid, true);
        if (role != null) {
            role.setStatus(code);
        }
        rolesService.lambdaUpdate().
                set(Roles::getStatus, code).
                eq(Roles::getId, uid).
                eq(Roles::getStatus, RoleState.NORMAL.ordinal()).
                update();
    }

    void onRecFrozen(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.Frozen msg = InnerMsg.Frozen.parseFrom(bytes);
        int uid = msg.getUid();
        onFrozen(uid, RoleState.FROZEN.ordinal());
    }

    void onRecUnFrozen(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.Frozen msg = InnerMsg.Frozen.parseFrom(bytes);
        int uid = msg.getUid();
        rolesService.lambdaUpdate().
                set(Roles::getStatus, RoleState.NORMAL.ordinal()).
                eq(Roles::getId, uid).
                and(wrapper -> wrapper.eq(Roles::getStatus, RoleState.FROZEN.ordinal()).or().eq(Roles::getStatus, RoleState.WARNING.ordinal())).
                update();
        Roles role = getRoles(uid, false);
        if (role != null) {
            role.setStatus(RoleState.NORMAL.ordinal());
        }
    }

    void onRecAdvice(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.Advice msg = InnerMsg.Advice.parseFrom(bytes);
        Advice data = new Advice();
        data.setUid(msg.getUid());
        data.setType(msg.getType());
        data.setChannel(msg.getChannel());
        data.setCliVer(msg.getCliVer());
        data.setContext(msg.getContext());
        data.setReplyState(0);
        data.setCreateTime(new Date());
        dataManager.getService(AdviceService.class).save(data);
    }

	/*
	void onRecGameLog(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {

		InnerMsg.GameLog gameLog = InnerMsg.GameLog.parseFrom(bytes);
		GameLog log = new GameLog();
		log.setUid(gameLog.getUid());
		log.setChannel(gameLog.getChannel());
		log.setType(gameLog.getType());
		log.setTargetUid(gameLog.getTargetuid());
		log.setContext(gameLog.getContext());
		log.setReason(gameLog.getReason());
		log.setKind(gameLog.getKind());
		log.setSystemType(gameLog.getSystem());
		log.setCreateTime(baseServer.getTimeFormat().format(baseServer.getServerTime()));
		gameLogs.addLast(log);

	}*/

    void onRecOfflineData(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.OfflineData data = InnerMsg.OfflineData.parseFrom(bytes);
        OfflineData offline = new OfflineData();
        offline.setUid(data.getUid());
        offline.setType(data.getType());
        offline.setContext(data.getContext());
        offline.setReason(data.getReason());
        offline.setState(0);
        offline.setCreateTime(baseServer.getTimeFormat().format(baseServer.getServerTime()));
        offlineData.addLast(offline);
    }

    void onRecItemLog(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.ItemLog log = InnerMsg.ItemLog.parseFrom(bytes);
        ItemLog itemLog = new ItemLog();
        itemLog.setUid(log.getUid());
        itemLog.setCreateTime(log.getCreateTime());
        itemLog.setNickName(log.getNickName());
        itemLog.setVipLevel(log.getVipLevel());
        itemLog.setItem(log.getItem());
        itemLog.setCount(log.getCount());
        itemLog.setOutput(log.getOutput());
        itemLog.setUseWay(log.getUesWay());
        itemLog.setPlayerHas(log.getPlayerHas());
        itemLog.setRoomName(log.getRoomName());
        dataManager.getService(ItemLogService.class).save(itemLog);
    }

    void onRecActivityLuckyPuzzleLog(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.ActivityLuckyPuzzleLog log = InnerMsg.ActivityLuckyPuzzleLog.parseFrom(bytes);
        ActivityLuckyPuzzleRecord activityLuckyPuzzleRecord = new ActivityLuckyPuzzleRecord();
        activityLuckyPuzzleRecord.setUid(log.getUid());
        activityLuckyPuzzleRecord.setCreateTime(log.getCreateTime());
        activityLuckyPuzzleRecord.setNickName(log.getNickName());
        activityLuckyPuzzleRecord.setOp(log.getOption());
        activityLuckyPuzzleRecord.setItems(log.getItems());
        dataManager.getService(ActivityLuckyPuzzleRecordService.class).save(activityLuckyPuzzleRecord);
    }

    void onRecFunFishRecord(IoSession session, byte[] bytes) {
        FunFishRecord funFishRecord = JsonUtil.decodeToObj(new String(bytes), FunFishRecord.class);
        dataManager.getService(FunFishRecordService.class).save(funFishRecord);
    }

    void onRecActivityFishPondRecord(IoSession session, byte[] bytes) {
        ActivityFishPondRecord activityFishPondRecord = JsonUtil.decodeToObj(new String(bytes), ActivityFishPondRecord.class);
        dataManager.getService(ActivityFishPondRecordService.class).save(activityFishPondRecord);
    }

    void onRecActivitySystemFishRecord(IoSession session, byte[] bytes) {
        onRecActivityFishPondRecord(session, bytes);
    }

    void onRecActivityFishMsgRecord(IoSession session, byte[] bytes) {
        FishPondMsgRecord fishPondMsgRecord = JsonUtil.decodeToObj(new String(bytes), FishPondMsgRecord.class);
        dataManager.getService(FishPondMsgRecordService.class).save(fishPondMsgRecord);
    }

    void onSyncMysteryLegendPlayAndWin(IoSession session, byte[] bytes) {
        List<MysteryLegendRoom> mysteryLegendRooms = JsonUtil.decodeToList(new String(bytes), MysteryLegendRoom.class);
        MysteryLegendRoomService mysteryLegendRoomService = dataManager.getService(MysteryLegendRoomService.class);
        for (MysteryLegendRoom room : mysteryLegendRooms) {
            mysteryLegendRoomService.lambdaUpdate().
                    set(MysteryLegendRoom::getTotalPlay, room.getTotalPlay()).
                    set(MysteryLegendRoom::getTotalWin, room.getTotalWin()).
                    set(MysteryLegendRoom::getOnlinePlayer, room.getOnlinePlayer()).
                    eq(MysteryLegendRoom::getId, room.getId()).
                    update();
        }
    }

    void onRecPropLog(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.ProLog log = InnerMsg.ProLog.parseFrom(bytes);
        int propIndex = LogType.proplog.ordinal();
        strLogs[propIndex].append(baseServer.getTimeFormat().format(baseServer.getServerTime()));
        strLogs[propIndex].append("|");
        strLogs[propIndex].append(log.getUid());
        strLogs[propIndex].append("|");
        strLogs[propIndex].append(log.getName());
        strLogs[propIndex].append("|");
        strLogs[propIndex].append(log.getContext());
        strLogs[propIndex].append("|");
        strLogs[propIndex].append(log.getReason());
        strLogs[propIndex].append("\n");
    }

    void onRecMailLog(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.MailLog log = InnerMsg.MailLog.parseFrom(bytes);
        int logIndex = LogType.maillog.ordinal();
        boolean isArray = log.getMailid().startsWith("[");
        if (log.getType() == MailLogType.APPENDIX.ordinal() && isArray) {
            List<String> ids = JsonUtil.decodeToList(log.getMailid(), String.class);
            List<String> contens = JsonUtil.decodeToList(log.getContext(), String.class);
            String time = baseServer.getTimeFormat().format(baseServer.getServerTime());
            for (int i = 0; i < ids.size(); i++) {
                strLogs[logIndex].append(time);
                strLogs[logIndex].append("|");
                strLogs[logIndex].append(log.getUid());
                strLogs[logIndex].append("|");
                strLogs[logIndex].append(log.getType());
                strLogs[logIndex].append("|");
                strLogs[logIndex].append(ids.get(i));
                strLogs[logIndex].append("|");
                strLogs[logIndex].append(contens.get(i));
                strLogs[logIndex].append("|");
                strLogs[logIndex].append(log.getReason());
                strLogs[logIndex].append("\n");
            }
        } else if (log.getType() == MailLogType.DEL.ordinal() && isArray) {
            List<String> ids = JsonUtil.decodeToList(log.getMailid(), String.class);
            String time = baseServer.getTimeFormat().format(baseServer.getServerTime());
            for (int i = 0; i < ids.size(); i++) {
                strLogs[logIndex].append(time);
                strLogs[logIndex].append("|");
                strLogs[logIndex].append(log.getUid());
                strLogs[logIndex].append("|");
                strLogs[logIndex].append(log.getType());
                strLogs[logIndex].append("|");
                strLogs[logIndex].append(ids.get(i));
                strLogs[logIndex].append("|");
                strLogs[logIndex].append("|");
                strLogs[logIndex].append(log.getReason());
                strLogs[logIndex].append("\n");
            }
        } else {
            strLogs[logIndex].append(baseServer.getTimeFormat().format(baseServer.getServerTime()));
            strLogs[logIndex].append("|");
            strLogs[logIndex].append(log.getUid());
            strLogs[logIndex].append("|");
            strLogs[logIndex].append(log.getType());
            strLogs[logIndex].append("|");
            strLogs[logIndex].append(log.getMailid());
            strLogs[logIndex].append("|");
            if (!"".equals(log.getContext())) {
                strLogs[logIndex].append(log.getContext());
            }
            strLogs[logIndex].append("|");
            strLogs[logIndex].append(log.getReason());
            strLogs[logIndex].append("\n");
        }
    }

    void onRecPlayLog(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.PlayLog log = InnerMsg.PlayLog.parseFrom(bytes);
        int playIndex = LogType.playlog.ordinal();
        strLogs[playIndex].append(baseServer.getTimeFormat().format(baseServer.getServerTime()));
        strLogs[playIndex].append("|");
        strLogs[playIndex].append(log.getUid());
        strLogs[playIndex].append("|");
        strLogs[playIndex].append(log.getType());
        strLogs[playIndex].append("|");
        strLogs[playIndex].append(log.getRoom());
        strLogs[playIndex].append("|");
        strLogs[playIndex].append(log.getDesk());
        strLogs[playIndex].append("|");
        strLogs[playIndex].append(log.getGold());
        strLogs[playIndex].append("|");
        strLogs[playIndex].append(log.getContext());
        strLogs[playIndex].append("|");
        strLogs[playIndex].append(log.getReason());
        strLogs[playIndex].append("\n");
    }

    void onRecPlayerLog(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.BaseValue log = InnerMsg.BaseValue.parseFrom(bytes);
        strLogs[LogType.playerLog.ordinal()].append(log.getStrValue()).append("\n");
    }

    void onRecAreaRoomLog(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.BaseValue log = InnerMsg.BaseValue.parseFrom(bytes);
        strLogs[LogType.areaRoom.ordinal()].append(log.getStrValue()).append("\n");
    }

    void onRecActivityLog(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.BaseValue log = InnerMsg.BaseValue.parseFrom(bytes);
        strLogs[LogType.activityLog.ordinal()].append(log.getStrValue()).append("\n");
    }

    void onRecGmLog(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.BaseValue log = InnerMsg.BaseValue.parseFrom(bytes);
        strLogs[LogType.gmLog.ordinal()].append(log.getStrValue()).append("\n");
    }
    void onKillFishLog(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.BaseValue log = InnerMsg.BaseValue.parseFrom(bytes);
        strLogs[LogType.kills.ordinal()].append(log.getStrValue()).append("\n");
    }
    void onKillFishLogMj(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.BaseValue log = InnerMsg.BaseValue.parseFrom(bytes);
        strLogs[LogType.killsMj.ordinal()].append(log.getStrValue()).append("\n");
    }

    void onRecWarningItemScore(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.WarningItemScoreLog log = InnerMsg.WarningItemScoreLog.parseFrom(bytes);
        int uid = log.getUid();
        boolean isNew = false;
        WarningItemScoreService warningItemScoreService = dataManager.getService(WarningItemScoreService.class);
        WarningItemScore warn = warningItemScoreService.queryByUid(uid);
        if (warn == null) {
            isNew = true;
            warn = new WarningItemScore();
        }
        warn.setUid(uid);
        warn.setVipLevel(log.getVipLevel());
        warn.setNickName(log.getNickName());
        warn.setChargeScore(log.getChargeScore());
        warn.setKillFishItemScore(log.getKillFishItemScore());
        warn.setDrawAwardItemScore(log.getDrawAwardItemScore());
        warn.setCurItemScore(log.getCurItemScore());
        warn.setMaxItemScore(log.getMaxItemScore());
        warn.setBombCoin(log.getBombCoin());
        warn.setGold(log.getGold());
        warn.setUpdateTime(baseServer.getTimeFormat().format(baseServer.getServerTime()));
        if (isNew) {
            warn.setCreateTime(baseServer.getTimeFormat().format(baseServer.getServerTime()));
            warningItemScoreService.save(warn);
        } else {
            warningItemScoreService.updateById(warn);
        }
    }

    void onRecWarningMoJin(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.WarningMoJinLog log = InnerMsg.WarningMoJinLog.parseFrom(bytes);
        int uid = log.getUid();
        boolean isNew = false;
        WarningMojinService warningMojinService = dataManager.getService(WarningMojinService.class);
        WarningMojin warn = warningMojinService.queryByUid(uid);
        if (warn == null) {
            isNew = true;
            warn = new WarningMojin();
        }
        warn.setUid(uid);
        warn.setVipLevel(log.getVipLevel());
        warn.setNickName(log.getNickName());
        warn.setPlay(log.getPlay());
        warn.setWin(log.getWin());
        warn.setCur(log.getCur());
        warn.setHbomb(log.getHbomb());
        warn.setHbombDebris(log.getHbombDebris());
        warn.setNbomb(log.getNbomb());
        warn.setNbombDebris(log.getNbombDebris());
        warn.setOtherDetail(log.getOtherDetail());
        warn.setDmojin(log.getDmojin());
        warn.setUpdateTime(baseServer.getTimeFormat().format(baseServer.getServerTime()));
        if (isNew) {
            warn.setCreateTime(baseServer.getTimeFormat().format(baseServer.getServerTime()));
            warningMojinService.save(warn);
        } else {
            warningMojinService.updateById(warn);
        }
    }

    void onRecMoJinRoomRecord(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.String tmp = InnerMsg.String.parseFrom(bytes);
        String data = tmp.getValue();
        MojinRoomRecord mojinRoomRecord = JsonUtil.decodeToObj(data, MojinRoomRecord.class);
        dataManager.getService(MojinRoomRecordService.class).save(mojinRoomRecord);
    }

    public void onRecMoJinRoomData(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.String tmp = InnerMsg.String.parseFrom(bytes);
        String data = tmp.getValue();
        List<PlayerDailyPlayData> list = JsonUtil.decodeToList(data, PlayerDailyPlayData.class);
        if (list == null) {
            return;
        }
        PlayerDailyPlayDataService playerDailyPlayDataService = dataManager.getService(PlayerDailyPlayDataService.class);
        for (PlayerDailyPlayData p : list) {
            playerDailyPlayDataService.addOrUpdatePlayerDailyData(p);
        }
    }

    void onRecUpdateDau(IoSession session, byte[] bytes) throws InvalidProtocolBufferException, SQLException {
        InnerMsg.Dau msg = InnerMsg.Dau.parseFrom(bytes);
        int uid = msg.getUid();
        int channel = msg.getChannel();
        int time = msg.getTime();
        int count = msg.getCount();
        String date = msg.getDate();
        boolean isNew = msg.getIsnew();
        DauService dauService = dataManager.getService(DauService.class);
        Dau dau = dauService.queryByUidDate(uid, date);
        if (dau == null) {
            dau = new Dau();
            dau.setUid(uid);
            dau.setChannel(channel);
            dau.setDate(date);
            dau.setOnlineTime(time == -1 ? 0 : time);
            dau.setLoginCount(count == -1 ? 0 : count);
            dauService.save(dau);
        } else {
            if (time != -1) {
                dau.setOnlineTime(dau.getOnlineTime() + time);
            }
            if (count != -1) {
                dau.setLoginCount(dau.getLoginCount() + count);
            }
            dauService.updateById(dau);
        }

        if (isNew) {
            NewDauService newDauService = dataManager.getService(NewDauService.class);
            NewDau newdau = newDauService.queryByUidDate(uid, date);
            if (newdau == null) {
                newdau = new NewDau();
                newdau.setUid(uid);
                newdau.setChannel(channel);
                newdau.setDate(date);
                newdau.setOnlineTime(time == -1 ? 0 : time);
                newdau.setLoginCount(count == -1 ? 0 : count);
                newDauService.save(newdau);
            } else {
                if (time != -1) {
                    newdau.setOnlineTime(newdau.getOnlineTime() + time);
                }
                if (count != -1) {
                    newdau.setLoginCount(newdau.getLoginCount() + count);
                }
                newDauService.updateById(newdau);
            }
        }
    }

    void onRecRealName(int reqid, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.RealName msg = InnerMsg.RealName.parseFrom(bytes);
        int uid = msg.getUid();
        String name = msg.getName();
        String idnum = msg.getIdcard();
        byte[] res = new byte[1];
        res[0] = 0;
        Roles role = getRoles(uid, true);
        if (role != null) {
            role.setRealName(name);
            role.setIdCard(idnum);
            rolesService.updateById(role);
            res[0] = 1;
        }
        baseServer.response(reqid, res);
    }

    /**
     * 收到绑定代理商消息
     *
     * @param reqid
     * @param bytes
     * @throws InvalidProtocolBufferException
     */
    void onRecProxyBind(int reqid, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.BindProxy msg = InnerMsg.BindProxy.parseFrom(bytes);
        int uid = msg.getUid();
        byte[] res = new byte[1];
        res[0] = 0;
        int proxyId = msg.getProxyId();
        Roles role = getRoles(uid, true);
        if (role != null) {
            role.setProxyId(proxyId);
            try {
                rolesService.updateById(role);
                res[0] = 1;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        baseServer.response(reqid, res);
    }

    void onRecUpdateOnlinePeak(IoSession session, byte[] bytes) throws InvalidProtocolBufferException, SQLException {
        InnerMsg.OnlinePeak msg = InnerMsg.OnlinePeak.parseFrom(bytes);
        int channel = msg.getChannel();
        int count = msg.getCount();
        String date = msg.getDate();
        OnlinePeakService onlinePeakService = dataManager.getService(OnlinePeakService.class);
        OnlinePeak peak = onlinePeakService.queryByChanelDate(channel, date);
        if (peak == null) {
            peak = new OnlinePeak();
            peak.setChannel(channel);
            peak.setCount(count);
            peak.setDate(date);
            onlinePeakService.save(peak);
        } else {
            peak.setCount(count);
            onlinePeakService.updateById(peak);
        }
    }

    void Frozen(int uid, int code) {
        InnerMsg.KickPlayer.Builder build = InnerMsg.KickPlayer.newBuilder();
        build.setUid(uid);
        build.setCode(1);
        SendMessage msg = new SendMessage(InnerMsgDef.INNER_MSG_KICK_PLAYER.ordinal(), build.build().toByteArray());
        baseServer.broadToServer("gate", msg);
        onFrozen(uid, code);
    }

    //static final long RMB_GOLD_RADIO = 50000;

    void onRecLog(Object obj, int leftCount) {
        //gameLogService.saveBatch(gameLogs);
        //gameLogs.clear();
        offlineDataService.saveBatch(offlineData);
        offlineData.clear();
        LogType[] logTypes = LogType.values();
        for (int i = 0; i < strLogs.length; i++) {
            if (strLogs[i].length() <= 0) {
                continue;
            }
            String dirName = "/logs/" + logTypes[i].toString();
            File logDir = new File(dirName);
            if (!logDir.exists()) {
                logDir.mkdir();
            }
            String res = strLogs[i].toString();
            FileOutputStream out = null;
            try {
                String fileName = new StringBuilder(dirName).append("/")
                        .append(baseServer.getDayFormat().format(baseServer.getServerTime())).append("-").append((int)new Date().getHours()/4).append(".log")
                        .toString();
                out = new FileOutputStream(fileName, true);
                out.write(res.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            strLogs[i] = strLogs[i].delete(0, strLogs[i].length());
        }
    }

    /**
     * 使用兑换码
     * @param reqid
     * @param bytes
     * @throws InvalidProtocolBufferException
     * @throws SQLException
     */
    void onReqUseRedeemCode(int reqid, byte[] bytes) throws InvalidProtocolBufferException, SQLException {
        InnerMsg.ReqRedeemCode msg = InnerMsg.ReqRedeemCode.parseFrom(bytes);
        int uid = msg.getUid();
        int channel = msg.getChannel();
        String msgDevId = msg.getDevid();
        String code = msg.getCode();
        String now = baseServer.getTimeFormat().format(baseServer.getServerTime());
        RedeemCodeService service = dataManager.getService(RedeemCodeService.class);
        RedeemCode rCode = service.lambdaQuery().eq(RedeemCode::getId, code)
                .and(wrapper -> wrapper.eq(RedeemCode::getChannel, channel).or().eq(RedeemCode::getChannel, -1))
                .and(wrapper -> wrapper.eq(RedeemCode::getUid, uid).or().eq(RedeemCode::getUid, -1))
                .and(wrapper -> wrapper.gt(RedeemCode::getEndTime, now).or().eq(RedeemCode::getLifeTime, -1))
                .one();
        InnerMsg.RedeemCodeRes.Builder build = InnerMsg.RedeemCodeRes.newBuilder();
        build.setContext("");
        if (rCode.getUsed() >= rCode.getLimitCount() && rCode.getLimitCount() != -1) {
            rCode = null;
        }
        if (rCode == null) {
            baseServer.response(reqid, build.build().toByteArray());
            return;
        }
        String context = rCode.getContext();
        int bind = rCode.getBindDevice();
        int used = rCode.getUsed();
        RedeemCodeRecordService recordService = dataManager.getService(RedeemCodeRecordService.class);
        if (bind == 1) {
            Integer count = recordService.lambdaQuery().eq(RedeemCodeRecord::getId, code).eq(RedeemCodeRecord::getDeviceId, msgDevId).count();
            if (count > 0) {
                baseServer.response(reqid, build.build().toByteArray());
                return;
            }
            RedeemCodeRecord record = new RedeemCodeRecord();
            record.setId(code);
            record.setDeviceId(msgDevId);
            record.setCreateTime(now);
            recordService.save(record);
        }
        service.lambdaUpdate().set(RedeemCode::getUsed, used + 1).eq(RedeemCode::getId, code).update();
        build.setContext(context);
        baseServer.response(reqid, build.build().toByteArray());
    }

    void onReqActivityData(int reqid, byte[] bytes) throws InvalidProtocolBufferException, SQLException, UnsupportedEncodingException {

    }

    void onReqVersionData(int reqid, byte[] bytes) throws InvalidProtocolBufferException, SQLException {

    }

    void onReqDailyData(int reqid, byte[] bytes) throws InvalidProtocolBufferException, SQLException {

    }

    void onReqUseCardItem(int reqid, byte[] bytes) throws InvalidProtocolBufferException, SQLException {
        InnerMsg.UseCardItem msg = InnerMsg.UseCardItem.parseFrom(bytes);
        int uid = msg.getUid();
        String itemid = msg.getItemid();
        int type = msg.getType();
        int count = 1;
        String now = baseServer.getTimeFormat().format(baseServer.getServerTime());
        InnerMsg.UseCardItemRes.Builder build = InnerMsg.UseCardItemRes.newBuilder();
        List<Card> cards = CheckCard(itemid, count, type);
        while (cards == null) {
            if (itemid.equals("item_card_recharge_100")) {
                itemid = "item_card_recharge_50";
                count *= 2;
            } else if (itemid.equals("item_card_recharge_50")) {
                itemid = "item_card_recharge_10";
                count *= 5;
            } else {
                break;
            }
            cards = CheckCard(itemid, count, type);
        }
        if (cards != null) {
            CardService cardService = dataManager.getService(CardService.class);
            for (int i = 0; i < cards.size(); i++) {
                Card car = cards.get(i);
                int id = car.getId();
                build.addId(car.getKey());
                build.addPasswd(car.getPwd());
                build.addEnd(car.getExpiry());
                build.addItemid(itemid);
                cardService.lambdaUpdate().
                        set(Card::getStatus, 1).
                        set(Card::getUid, uid).
                        set(Card::getExchangeTime, now).
                        eq(Card::getId, id).
                        update();
            }
        }
        baseServer.response(reqid, build.build().toByteArray());
    }

    List<Card> CheckCard(String itemid, int count, int type) throws SQLException {
        List<Card> cards = dataManager.getService(CardService.class).lambdaQuery().
                eq(Card::getItemId, itemid).
                eq(Card::getType, type).
                eq(Card::getStatus, 0).
                last("limit 0," + count).
                list();
        if (cards.size() == count) {
            return cards;
        }
        return null;
    }

    void onReqExchangeCard(int reqid, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.ExchangeCard card = InnerMsg.ExchangeCard.parseFrom(bytes);
        String items = card.getItems();
        String goods = card.getGoods();
        String now = baseServer.getTimeFormat().format(baseServer.getServerTime());
        InnerMsg.ComResponse.Builder build = InnerMsg.ComResponse.newBuilder();
        ExchangeCard exchangeCard = new ExchangeCard();
        exchangeCard.setCreateTime(now);
        exchangeCard.setItems(items);
        exchangeCard.setGoods(goods);
        exchangeCard.setUid(card.getUid());
        dataManager.getService(ExchangeCardService.class).save(exchangeCard);
        build.setCode(0);
        baseServer.response(reqid, build.build().toByteArray());
    }

    void onRecPayErrorLog(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.String errorInfo = InnerMsg.String.parseFrom(bytes);
        JsonObject json = JsonUtil.decodeToObj(errorInfo.getValue(), JsonObject.class);
        int uid = json.get("uid").getAsInt();
        String goodsId = json.get("goodsId").getAsString();
        int orderId = json.get("orderId").getAsInt();
        byte code = json.get("code").getAsByte();
        int logIndex = LogType.payError.ordinal();
        strLogs[logIndex].append(baseServer.getTimeFormat().format(baseServer.getServerTime()));
        strLogs[logIndex].append("|");
        strLogs[logIndex].append(uid);
        strLogs[logIndex].append("|");
        strLogs[logIndex].append(goodsId);
        strLogs[logIndex].append("|");
        strLogs[logIndex].append(orderId);
        strLogs[logIndex].append("|");
        strLogs[logIndex].append(code);
        strLogs[logIndex].append("\n");
    }

    void onRecRecruit(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.Recruit recruit = InnerMsg.Recruit.parseFrom(bytes);
        Recruit r = new Recruit();
        r.setId(recruit.getUid());
        r.setOpenDate(recruit.getOpenDate());
        dataManager.getService(RecruitService.class).save(r);
    }

    void onReqCardStats(int reqid, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.CardStats card = InnerMsg.CardStats.parseFrom(bytes);
        InnerMsg.ComResponse.Builder build = InnerMsg.ComResponse.newBuilder();
        CardStats cardStats = new CardStats();
        cardStats.setStatsTime(card.getStatsTime());
        cardStats.setUid(card.getUid());
        cardStats.setFriendAmount(card.getFriendAmount());
        cardStats.setFriendRecharge(card.getFriendRecharge());
        cardStats.setFansRecharge(card.getFansRecharge());
        cardStats.setCardAmount(card.getCardAmount());
        dataManager.getService(CardStatsService.class).save(cardStats);
        build.setCode(0);
        baseServer.response(reqid, build.build().toByteArray());
    }

    void onExecuteSqlMethod(int reqId, byte[] bytes) {
        InnerMsg.ComeFromDbData.Builder builder = InnerMsg.ComeFromDbData.newBuilder();
        Object result = StoreData.execute(dataManager, bytes);
        if (result != null) {
            builder.setCode(0);
            builder.setDatas(JsonUtil.encodeToStr(result));
        } else {
            builder.setCode(1);
            builder.setDatas("");
        }
        baseServer.response(reqId, builder.build().toByteArray());
    }

    @Override
    public void onStop() {
        baseServer.broadToAllServer(null, InnerMsgDef.INNER_MSG_NOTIFY_SHUTDOWN.ordinal(), null);
    }

    //邮件系统开始
    private String m_lastMailTime = "";
    private int m_mailIndex = 0;

    String allocMailID() {
        String time = baseServer.getTimeFormat2().format(baseServer.getServerTime());
        if (m_lastMailTime.equals(time)) {
            ++m_mailIndex;
        } else {
            m_lastMailTime = time;
            m_mailIndex = 0;
        }
        return time + String.format("%09d", m_mailIndex);
    }

    void onReqSendMail(int reqid, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.SendMail mailMsg = InnerMsg.SendMail.parseFrom(bytes);
        long now = baseServer.getServerTime();
        int recvUid = mailMsg.getRecvuid();
        int senderUid = mailMsg.getSenderuid();
        String appendix = mailMsg.getAppendix();
        Mails mail = new Mails();
        mail.setId(allocMailID());
        mail.setType(mailMsg.getType());
        mail.setChannel(mailMsg.getChannel());
        mail.setTitle(mailMsg.getTitle());
        mail.setContext(mailMsg.getContext());
        mail.setSenderUid(senderUid);
        mail.setSenderName(mailMsg.getSendername());
        mail.setRecUid(recvUid);
        mail.setRecName(mailMsg.getRecvname());
        mail.setLifeTime(mailMsg.getLifetime());
        mail.setState(MailState.STATE_UNREAD.ordinal());
        mail.setAppendix(appendix);
        mail.setCreateTime(baseServer.getTimeFormat().format(now));
        long time = mailMsg.getLifetime() == -1 ? now + 15 * 24 * 3600000 : now + mailMsg.getLifetime();
        mail.setEndTime(baseServer.getTimeFormat().format(time));
        dataManager.getService(MailsService.class).save(mail);
        sendUpdateMailMsg(mail.getId(), recvUid, senderUid, appendix);
    }

    void onRecSendMail(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.SendMail mailMsg = InnerMsg.SendMail.parseFrom(bytes);
        long now = baseServer.getServerTime();
        int recvUid = mailMsg.getRecvuid();
        int senderUid = mailMsg.getSenderuid();
        String appendix = mailMsg.getAppendix();
        Mails mail = new Mails();
        mail.setId(allocMailID());
        mail.setType(mailMsg.getType());
        mail.setChannel(mailMsg.getChannel());
        mail.setTitle(mailMsg.getTitle());
        mail.setContext(mailMsg.getContext());
        mail.setSenderUid(senderUid);
        mail.setSenderName(mailMsg.getSendername());
        mail.setRecUid(recvUid);
        mail.setRecName(mailMsg.getRecvname());
        mail.setLifeTime(mailMsg.getLifetime());
        mail.setState(MailState.STATE_UNREAD.ordinal());
        mail.setAppendix(appendix);
        mail.setCreateTime(baseServer.getTimeFormat().format(now));
        if (mailMsg.hasSystem()) {
            mail.setSystemType(mailMsg.getSystem());
        } else {
            mail.setSystemType(MailSystemDef.MAIL_NORMAL.ordinal());
        }
        long time = mailMsg.getLifetime() == -1 ? now + 15 * 24 * 3600000 : now + mailMsg.getLifetime();
        mail.setEndTime(baseServer.getTimeFormat().format(time));
        dataManager.getService(MailsService.class).save(mail);
        sendUpdateMailMsg(mail.getId(), recvUid, senderUid, appendix);
    }

    void sendUpdateMailMsg(String mailid, int recvUid, int senderUid, String appendix) {
        InnerMsg.String.Builder builder = InnerMsg.String.newBuilder();
        JsonObject json = new JsonObject();
        json.addProperty("RecvUid", recvUid);
        json.addProperty("SenderUid", senderUid);
        json.addProperty("Appendix", appendix);
        json.addProperty("MailId", mailid);
        builder.setValue(json.toString());
        baseServer.sendMsgToServer(framework.ServerSet.SERVER_LOGIC_NAME_GAME, InnerMsgDef.INNER_MSG_UPDATE_MAIL_PROP.ordinal(), builder.build().toByteArray());
    }

    void onRecDelMail(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.DelMail delMail = InnerMsg.DelMail.parseFrom(bytes);
        dataManager.getService(MailsService.class).updateState(delMail.getMailid(), MailState.STATE_DEL.ordinal());
    }

    void onRecQueryMail(int reqId, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.QueryMail queryMail = InnerMsg.QueryMail.parseFrom(bytes);
        String mailId = queryMail.getMailid();
        List<Mails> mails = dataManager.getService(MailsService.class).queryByConds(baseServer.getDayFormat(), mailId, queryMail.getUid(), queryMail.getChannel());
        InnerMsg.QueryMailRes.Builder builder = InnerMsg.QueryMailRes.newBuilder();
        if (mails != null) {
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < mails.size(); i++) {
                Mails mail = mails.get(i);
                InnerMsg.ReadMailRes.Builder build = InnerMsg.ReadMailRes.newBuilder();
                build.setId(mail.getId());
                build.setType(mail.getType());
                build.setTitle(mail.getTitle());
                build.setContext(mail.getContext());
                build.setSenderuid(mail.getSenderUid());
                build.setSendername(mail.getSenderName());
                build.setRecvuid(mail.getRecUid());
                build.setRecvname(mail.getRecName());
                build.setSendtime(mail.getCreateTime());
                build.setEndtime(mail.getEndTime());
                build.setAppendix(mail.getAppendix());
                build.setSystem(mail.getSystemType());
                builder.addMails(build.build());
                if (mail.getRecUid() != -1) {
                    ids.add(mail.getId());
                }
            }
            if (ids.size() > 0) {
                dataManager.getService(MailsService.class).updateState(ids, MailState.STATE_READ.ordinal());
            }
        }
        baseServer.response(reqId, builder.build().toByteArray());
    }

    void onRecReadMail(int reqid, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.ReadMail readMail = InnerMsg.ReadMail.parseFrom(bytes);
        String mailId = readMail.getMailid();
        MailsService mailsService = dataManager.getService(MailsService.class);
        Mails mail = mailsService.getById(mailId);
        if (mail == null) {
            baseServer.response(reqid, null);
        } else {
            InnerMsg.ReadMailRes.Builder build = InnerMsg.ReadMailRes.newBuilder();
            build.setId(mailId);
            build.setType(mail.getType());
            build.setTitle(mail.getTitle());
            build.setContext(mail.getContext());
            build.setSenderuid(mail.getSenderUid());
            build.setSendername(mail.getSenderName());
            build.setRecvuid(mail.getRecUid());
            build.setRecvname(mail.getRecName());
            build.setSendtime(mail.getCreateTime());
            build.setEndtime(mail.getEndTime());
            build.setAppendix(mail.getAppendix());
            build.setSystem(mail.getSystemType());
            if (mail.getRecUid() != -1) {
                mailsService.updateState(mailId, MailState.STATE_READ.ordinal());
            }
            baseServer.response(reqid, build.build().toByteArray());
        }
    }

    void onRecUpdatePro(IoSession session, byte[] data) throws InvalidProtocolBufferException {
        InnerMsg.CmdUpdateProp request = InnerMsg.CmdUpdateProp.parseFrom(data);
        int size = request.getUidsCount();
        Map<Integer, Object> roles = new HashMap<>();
        List<Integer> uids = new ArrayList<>();
        ValueType[] valueTypes = ValueType.values();
        for (int i = 0; i < size; i++) {
            int uid = request.getUids(i);
            Roles role = getRoles(uid, false);
            if (role != null) {
                roles.put(uid, role);
            } else {
                uids.add(uid);
            }
        }
        if (uids.size() > 0) {
            List<Roles> datas = rolesService.lambdaQuery().in(Roles::getId, uids).list();
            for (int i = 0; i < datas.size(); i++) {
                Roles role = datas.get(i);
                roles.put(role.getId(), role.getParam());
            }
        }
        for (Map.Entry<Integer, Object> entry : roles.entrySet()) {
            Object _obj = entry.getValue();
            byte[] srcs = null;
            Roles target = null;
            if (_obj instanceof Roles) {
                target = (Roles) _obj;
                srcs = target.getParam();
            } else {
                srcs = (byte[]) _obj;
            }
            IoBuffer buffer = IoBuffer.wrap(srcs);
            short proCount = buffer.getShort();
            List<Property> tempProps = new ArrayList<>();
            for (int j = 0; j < proCount; j++) {
                String name = UtilFunc.getStringFromIoBuffer(buffer);
                ValueType type = valueTypes[buffer.getShort()];
                Property pro = new Property(name, type, false, false, true);
                pro.loadFromBuff(buffer);
                tempProps.add(pro);
            }
            for (int i = 0; i < size; i++) {
                int uid = request.getUids(i);
                String key = request.getKeys(i);
                String vaule = request.getValues(i);
                if (uid == entry.getKey()) {
                    for (int j = 0; j < tempProps.size(); j++) {
                        Property pro = tempProps.get(j);
                        if (pro.getName().equals(key)) {
                            pro.setCmdValue(vaule);
                            break;
                        }
                    }
                }
            }
            int len = srcs.length - buffer.position();
            byte[] tails = new byte[len];
            buffer.get(tails);
            //重新存回数据库
            IoBuffer save = IoBuffer.allocate(10);
            save.setAutoExpand(true);
            save.putShort((short) tempProps.size());
            for (int j = 0; j < tempProps.size(); j++) {
                Property pro = tempProps.get(j);
                pro.storeToBuff(save);
            }
            save.put(tails);
            int bufferSize = save.position();
            save.flip();
            byte[] store = Arrays.copyOfRange(save.array(), 0, bufferSize);
            if (target == null) {
                target = rolesService.getById(entry.getKey());
            }
            target.setParam(store);
            rolesService.updateById(target);
        }
    }

    void onRecUpdateRec(IoSession session, byte[] data) throws InvalidProtocolBufferException {
        InnerMsg.CmdUpdateRecord request = InnerMsg.CmdUpdateRecord.parseFrom(data);
        int size = request.getUidsCount();
        Map<Integer, Object> roles = new HashMap<>();
        List<Integer> uids = new ArrayList<>();
        ValueType[] valueTypes = ValueType.values();
        for (int i = 0; i < size; i++) {
            int uid = request.getUids(i);
            Roles role = getRoles(uid, false);
            if (role != null) {
                roles.put(uid, role);
            } else {
                uids.add(uid);
            }
        }
        if (uids.size() > 0) {
            List<Roles> datas = rolesService.lambdaQuery().in(Roles::getId, uids).list();
            for (int i = 0; i < datas.size(); i++) {
                Roles role = datas.get(i);
                roles.put(role.getId(), role.getParam());
            }
        }
        for (Map.Entry<Integer, Object> entry : roles.entrySet()) {
            Object _obj = entry.getValue();
            byte[] srcs = null;
            Roles target = null;
            if (_obj instanceof Roles) {
                target = (Roles) _obj;
                srcs = target.getParam();
            } else {
                srcs = (byte[]) _obj;
            }
            IoBuffer buffer = IoBuffer.wrap(srcs);
            short proCount = buffer.getShort();
            List<Property> tempProps = new ArrayList<>();
            for (int i = 0; i < proCount; i++) {
                String name = UtilFunc.getStringFromIoBuffer(buffer);
                ValueType type = valueTypes[buffer.getShort()];
                Property pro = new Property(name, type, false, false, true);
                pro.loadFromBuff(buffer);
                tempProps.add(pro);
            }
            int recCount = buffer.getShort();
            List<Record> records = new ArrayList<>();
            for (int i = 0; i < recCount; i++) {
                String name = UtilFunc.getStringFromIoBuffer(buffer);
                int cols = buffer.getShort();
                Record rec = new Record(name, cols, 10000, false, false, false);
                rec.loadFromBuff(buffer);
                records.add(rec);
            }
            for (int i = 0; i < size; i++) {
                int uid = request.getUids(i);
                if (uid == entry.getKey()) {
                    String key = request.getKeys(i);
                    String value = request.getValues(i);
                    int op = request.getOps(i);
                    int row = request.getRows(i);
                    int col = request.getCols(i);
                    for (int j = 0; j < records.size(); j++) {
                        Record rec = records.get(j);
                        if (key.equals(rec.getName())) {
                            if (op == 0) {//增加
                                rec.addCmdRow(value);
                            } else if (op == 1) {//修改
                                rec.setCmdValue(row, col, value);
                            } else if (op == 2) {//删除
                                rec.removeRow(row);
                            } else if (op == -1) {//清理
                                rec.clear();
                            }
                            break;
                        }
                    }
                }
            }
            int len = srcs.length - buffer.position();
            byte[] tails = new byte[len];
            buffer.get(tails);
            //重新存回数据库
            IoBuffer save = IoBuffer.allocate(10);
            save.setAutoExpand(true);
            save.putShort((short) tempProps.size());
            for (int i = 0; i < tempProps.size(); i++) {
                Property pro = tempProps.get(i);
                pro.storeToBuff(save);
            }
            save.putShort((short) records.size());
            for (int i = 0; i < records.size(); i++) {
                Record rec = records.get(i);
                rec.storeToBuff(save);
            }
            save.put(tails);
            int bufferSize = save.position();
            save.flip();
            byte[] store = Arrays.copyOfRange(save.array(), 0, bufferSize);
            if (target == null) {
                target = rolesService.getById(entry.getKey());
            }
            target.setParam(store);
            rolesService.updateById(target);
        }
    }

    void onReqCreateGuild(int reqid, byte[] bytes) {
        try {
            InnerMsg.String msg = InnerMsg.String.parseFrom(bytes);
            JsonObject json = JsonUtil.decodeToObj(msg.getValue(), JsonObject.class);
            Guild guild = JsonUtil.decodeToObj(json.get("g"), Guild.class);
            GuildMember guildMember = JsonUtil.decodeToObj(json.get("m"), GuildMember.class);
            GuildService guildService = dataManager.getService(GuildService.class);
            GuildMemberService memberService = dataManager.getService(GuildMemberService.class);
            boolean code = guildService.save(guild);
            if (code) {
                code = memberService.addMember(guildMember);
                if (code) {
                    baseServer.response(reqid, new byte[]{0});
                    return;
                } else {
                    guildService.delete(guild.getId());//回滚
                }
            }
        } catch (Exception e) {
            logger.error("onReqCreateGuild", e);
        }
        baseServer.response(reqid, new byte[]{1});
    }

    void onReqDeleteGuild(int reqid, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.String msg = InnerMsg.String.parseFrom(bytes);
        int guildId = Integer.parseInt(msg.getValue());
        GuildService guildService = dataManager.getService(GuildService.class);
        Guild guild = guildService.queryById(guildId);
        if (guild != null) {
            dataManager.getService(GuildMemberService.class).deleteAllMember(guildId);
            dataManager.getService(GuildRepositoryService.class).removeAll(guildId);
            dataManager.getService(ReqMemberService.class).deleteGuildAllReqMember(guildId);
            guild.setGuildStatus(2);
            guildService.updateById(guild);
            baseServer.response(reqid, new byte[]{1});
        }
        baseServer.response(reqid, new byte[]{0});
    }

    void onRecVipGiftOrder(int reqid, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.String data = InnerMsg.String.parseFrom(bytes);
        logger.info("{} ", data.getValue());
        baseServer.response(reqid, new byte[]{0});
    }

    void onRecFullGameItems(int reqid, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.String data = InnerMsg.String.parseFrom(bytes);
        JSONObject json = JSONObject.parseObject(data.getValue());
        List<FullGameItems> fullGameItems = JsonUtil.decodeToList(json.getString("fullGameItems"), FullGameItems.class);
        List<FullGameItemsRecord> records = JsonUtil.decodeToList(json.getString("fullGameItemsRecord"), FullGameItemsRecord.class);
        if (fullGameItems!=null&&fullGameItems.size()>0){
            FullGameItemsService service = dataManager.getService(FullGameItemsService.class);
            FullGameItemsRecordService recordService = dataManager.getService(FullGameItemsRecordService.class);
            service.updateBatchById(fullGameItems);
            recordService.saveBatch(records);
            baseServer.response(reqid, new byte[]{1});
        }
        baseServer.response(reqid, new byte[]{0});
    }

    void onRecBulletAddSpeed(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.String data = InnerMsg.String.parseFrom(bytes);
        Cheat cheat = JsonUtil.decodeToObj(data.getValue(), Cheat.class);
        cheatService.save(cheat);
    }

    @Override
    public void finalClose() {
        onRecLog(null, 0);
    }
    void onRecSendItems(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.SendMail mailMsg = InnerMsg.SendMail.parseFrom(bytes);
        long now = baseServer.getServerTime();
        int recvUid = mailMsg.getRecvuid();
        int senderUid = mailMsg.getSenderuid();
        String appendix = mailMsg.getAppendix();
        SendItems items = new SendItems();
        items.setId(allocMailID());
        items.setType(mailMsg.getType());
        items.setChannel(mailMsg.getChannel());
        items.setSenderUid(senderUid);
        items.setSenderName(mailMsg.getSendername());
        items.setRecUid(recvUid);
        items.setRecName(mailMsg.getRecvname());
        items.setState(MailState.STATE_UNREAD.ordinal());
        items.setAppendix(appendix);
        items.setCreateTime(baseServer.getTimeFormat().format(now));
        if (mailMsg.hasSystem()) {
            items.setSystemType(mailMsg.getSystem());
        } else {
            items.setSystemType(MailSystemDef.MAIL_NORMAL.ordinal());
        }
        dataManager.getService(SendItemsService.class).save(items);
        sendUpdateMailMsg(items.getId(), recvUid, senderUid, appendix);
    }
    void onRecDelSendItems(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.String delMail = InnerMsg.String.parseFrom(bytes);
        if (delMail.hasValue()){
            List<String> ids = JsonUtil.decodeToList(delMail.getValue(), String.class);
            String nowTime = getServer().getTimeFormat().format(System.currentTimeMillis());
            dataManager.getService(SendItemsService.class).updateState(ids, MailState.STATE_DEL.ordinal(),nowTime);
        }

    }
    void onRecQuerySendItems(int reqId, byte[] bytes) throws InvalidProtocolBufferException {
        InnerMsg.QueryMail queryMail = InnerMsg.QueryMail.parseFrom(bytes);
        String mailId = queryMail.getMailid();
        List<SendItems> items = dataManager.getService(SendItemsService.class).queryByConds(queryMail.getUid());
        InnerMsg.QueryMailRes.Builder builder = InnerMsg.QueryMailRes.newBuilder();
        if (items != null) {
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                SendItems sendItems = items.get(i);
                InnerMsg.ReadMailRes.Builder build = InnerMsg.ReadMailRes.newBuilder();
                build.setId(sendItems.getId());
                build.setType(sendItems.getType());
                build.setSenderuid(sendItems.getSenderUid());
                build.setSendername(sendItems.getSenderName());
                build.setRecvuid(sendItems.getRecUid());
                build.setRecvname(sendItems.getRecName());
                build.setSendtime(sendItems.getCreateTime());
                build.setAppendix(sendItems.getAppendix());
                build.setSystem(sendItems.getSystemType());
                builder.addMails(build.build());
                if (sendItems.getRecUid() != -1) {
                    ids.add(sendItems.getId());
                }
            }
            if (ids.size() > 0) {
                dataManager.getService(SendItemsService.class).updateState(ids, MailState.STATE_READ.ordinal(),baseServer.getTimeFormat().format(baseServer.getServerTime()));
            }
        }
        baseServer.response(reqId, builder.build().toByteArray());
    }
}
