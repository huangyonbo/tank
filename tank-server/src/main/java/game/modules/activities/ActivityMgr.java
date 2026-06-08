package game.modules.activities;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.ByteUtils;
import framework.JsonUtil;
import framework.game.IGameObject;
import framework.game.IKernel;
import framework.game.ILogicModule;
import framework.game.KernelEvent;
import framework.mybatis.domain.Activity;
import framework.mybatis.service.impl.ActivityService;
import game.custommsg.C2SMsgDef;
import game.custommsg.CustomMsg;
import game.custommsg.RequestMsgDef;
import game.custommsg.S2CMsgDef;
import org.apache.commons.lang.StringUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ActivityMgr implements ILogicModule {
    public static class ActivityData {
        public int id;
        public int type;
        public long version;
        public List<Integer> channles;
        public boolean bSwitch;
        public String param;
        public String startTime;
        public String stopTime;
        public String startShow;
        public String stopShow;
        public int weight;

        public boolean bIsShow; // 客户端展示中
        public boolean bInDate;
        public boolean bStoped;
        public long startDate;
        public long stopDate;
        public long startShowDate;
        public long stopShowDate;
        public Object data = null;
        public String description;//描述

    }

    public enum Activities {
        UNKNOW,
        BombCoinDayRankActivity,
        BombCoinWeekRankActivity,
        BombCoinLimitDayRankActivity,
        CornucopiaActivity,
        ThreeSelectOne,
        BMBCActivity,
        FQZSActivity,
        SHZActivity,
        SGMLActivity,
        BRNNActivity,
        END
    }

    private static Logger logger = LoggerFactory.getLogger(ActivityMgr.class);
    // <type, <chanel, cfg>>
    Map<Integer, Map<Integer, Integer>> m_mapCfgs = new HashMap<>();
    Map<Integer, ActivityData> m_mapCfgsByID = new HashMap<>();

    BaseActivity[] m_Activities;

    @Override
    public boolean onInit(IKernel kernel) {
        m_Activities = new BaseActivity[Activities.END.ordinal()];
        //m_Activities[Activities.SGMLActivity.ordinal()] = new SgmlModel(Activities.BombCoinDayRankActivity.ordinal(), this);

        for (int i = 1; i < Activities.END.ordinal(); ++i) {
            if (m_Activities[i] == null) {
                continue;
            }
            if (!m_Activities[i].Init(kernel)) {
                return false;
            }
        }

        kernel.declareHeartBeat("HB_ActivityMgrCheck", this, "OnCheckTimer");
        kernel.regEvent(KernelEvent.KEVENT_ON_CREATE, "World", this, "OnWorldCreate");
        kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
        kernel.regEvent(KernelEvent.KEVENT_ON_LINE, "Player", this, "OnPlayerOnLine");
        kernel.regEvent(KernelEvent.KEVENT_ON_RECONNECT, "Player", this, "OnPlayerOnLine");
        kernel.regEvent(KernelEvent.KEVENT_ON_RECONNECT, "Player", this, "OnPlayerOnLine");
        kernel.regEvent(KernelEvent.KEVENT_OFF_LINE, "Player", this, "OnPlayerOffLine");
        kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");
        kernel.regServerMsg(ServerMsgDef.B2G_REFRESH_ACTIVITY.ordinal(), this, "OnRefreshActivity");
        kernel.regServerMsg(ServerMsgDef.B2G_CLOSE_ACTIVITY.ordinal(), this, "OnCloseActivity");
        kernel.regServerRequest(ServerMsgDef.B2G_ACTIVITY_UPDATE_CONFIG.ordinal(), this, "UpdateActivityConfig");
        kernel.regClientMessage(C2SMsgDef.C2S_GET_ACTIVITY_AWARD.ordinal(), this, "OnGetActivityReward");
        kernel.regRequestMessage(RequestMsgDef.REQ_ACTIVITY_REWARD.ordinal(), this, "OnGetActivityRewardNeedCallBack");
        kernel.regRequestMessage(RequestMsgDef.REQ_ACTIVITY_CONFIG_NEW.ordinal(), this, "OnReqActivityConfig");
        kernel.regRequestMessage(RequestMsgDef.REQ_ACTIVITY_INFO.ordinal(), this, "OnReqActivityInfo");
        return true;
    }

    void RefreshCfg(IKernel kernel, String path) {
        for (int i = 1; i < Activities.END.ordinal(); ++i) {
            if (m_Activities[i] == null) {
                continue;
            }
            m_Activities[i].RefreshCfg(kernel, path);
        }
    }

    void OnGetActivityReward(IKernel kernel, IGameObject player, int msgid, byte[] msg) throws InvalidProtocolBufferException {
        CustomMsg.String data = CustomMsg.String.parseFrom(msg);
        JsonObject json = JsonUtil.decodeToObj(data.getValue(), JsonObject.class);
        if (json == null) {
            return;
        }
        int activityId = Integer.parseInt(json.get("activityId").getAsString());
        for (int i = 1; i < Activities.END.ordinal(); ++i) {
            BaseActivity activity = m_Activities[i];
            if (activity == null) {
                continue;
            }
            if (activity.m_type == activityId) {
                activity.OnGetActivityReward(kernel, player, json, msgid);
                break;
            }
        }
    }

    void OnGetActivityRewardNeedCallBack(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg) {
        CustomMsg.String data = null;
        try {
            data = CustomMsg.String.parseFrom(msg);
            JsonObject json = JsonUtil.decodeToObj(data.getValue(), JsonObject.class);
            logger.info(json.toString());
            if (json == null) {
                return;
            }
            int activityId = Integer.parseInt(json.get("activityId").getAsString());
            for (int i = 1; i < Activities.END.ordinal(); ++i) {
                BaseActivity activity = m_Activities[i];
                if (activity == null) {
                    continue;
                }
                if (activity.m_type == activityId) {
                    activity.OnGetActivityRewardNeedCallBack(kernel, player, json, msgid, reqid);
                    break;
                }
            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    void OnReqActivityConfig(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
            throws InvalidProtocolBufferException {
        CustomMsg.Int32 data = CustomMsg.Int32.parseFrom(msg);
        int activityId = data.getValue();
        for (int i = 1; i < Activities.END.ordinal(); ++i) {
            BaseActivity activity = m_Activities[i];
            if (activity == null) {
                continue;
            }
            if (activity.m_type == activityId) {
                activity.OnReqActivityConfig(kernel, player, msgid, reqid);
                break;
            }
        }
    }

    void OnReqActivityInfo(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
            throws InvalidProtocolBufferException {
        CustomMsg.Int32 data = CustomMsg.Int32.parseFrom(msg);
        int activityId = data.getValue();
        for (int i = 1; i < Activities.END.ordinal(); ++i) {
            BaseActivity activity = m_Activities[i];
            if (activity == null) {
                continue;
            }
            if (activity.m_type == activityId) {
                activity.OnReqActivityInfo(kernel, player, msgid, reqid);
                break;
            }
        }
    }

    @Override
    public void onNetReady(IKernel kernel) {
        kernel.executeSomeToStore(ActivityService.class, "loadActives", null, (str) -> {
            List<Activity> acts = framework.JsonUtil.decodeToList(str, Activity.class);
            for (int i = 0; i < acts.size(); i++) {
                LoadCfg(kernel, acts.get(i));
            }
        });
        for (int i = 1; i < Activities.END.ordinal(); ++i) {
            if (m_Activities[i] == null) {
                continue;
            }
            m_Activities[i].OnParentNetReady(kernel);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T GetActivity(Activities activities) {
        return (T) m_Activities[activities.ordinal()];
    }

    @Override
    public void onDestroy() {
        for (int i = 1; i < Activities.END.ordinal(); ++i) {
            if (m_Activities[i] == null) {
                continue;
            }
            m_Activities[i].OnDestroy();
        }
    }

    public boolean LoadCfg(IKernel kernel, Activity cfg) {
        if (cfg.getStatus() == 0) {
            return false;
        }
        ActivityData data = new ActivityData();
        data.id = cfg.getId();
        data.type = cfg.getType();
        data.channles = framework.MathUtils.SplitByFlag(cfg.getChannel(), ",", Integer.class);
        data.bSwitch = cfg.getStatus() == 1;
        try {
            data.param = new String(cfg.getParam(), "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        data.startTime = cfg.getStartTime();
        data.stopTime = cfg.getEndTime();
        data.startShow = cfg.getShowStartTime();
        data.stopShow = cfg.getShowEndTime();
        data.weight = cfg.getWeight();
        data.description = cfg.getDescription();
        ActivityData old = m_mapCfgsByID.get(data.id);
        if (data.bSwitch && old != null && old.bInDate) {
            //活动正在进行就不修改各种标志
            data.bIsShow = old.bIsShow;
            data.bInDate = old.bInDate;
            data.bStoped = old.bStoped;
        }
        try {
            DateFormat format = kernel.getServer().getTimeFormat();
            data.startDate = format.parse(data.startTime).getTime();
            data.stopDate = format.parse(data.stopTime).getTime();
            data.startShowDate = format.parse(data.startShow).getTime();
            data.stopShowDate = format.parse(data.stopShow).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
        data.version = data.startDate;
        if (data.type > m_Activities.length - 1 || m_Activities[data.type] == null) {
            return false;
        }
        if (!m_Activities[data.type].ParseCfg(kernel, data)) {
            logger.error("ParseCfg failed. {}", data.id);
            return false;
        }
        m_mapCfgsByID.put(data.id, data);
        int type = data.type;
        //活动修改渠道不生效 Alter by 赵俊@20190523
//		Map<Integer, Integer> tMap = m_mapCfgs.get(type);
//		if (tMap != null) {
//			Iterator<Entry<Integer, Integer>> iterator = tMap.entrySet().iterator();
//			while (iterator.hasNext()) {
//				Entry<Integer, Integer> entry = iterator.next();
//				if (entry.getValue() == data.id) {
//					iterator.remove();
//				}
//			}
//		}
        for (int channel : data.channles) {
            if (!m_mapCfgs.containsKey(type)) {
                Map<Integer, Integer> map = new HashMap<>();
                m_mapCfgs.put(type, map);
            }
            m_mapCfgs.get(type).put(channel, data.id);
        }
        return true;
    }

    void OnWorldCreate(IKernel kernel, IGameObject world) {
        kernel.addHeartBeat("HB_ActivityMgrCheck", world, 1000, -1);
        for (int i = 1; i < Activities.END.ordinal(); ++i) {
            if (m_Activities[i] == null) {
                continue;
            }
            m_Activities[i].OnWorldCreate(kernel, world);
        }
    }

    void OnPlayerClassCreate(IKernel kernel, String script) {
        for (int i = 1; i < Activities.END.ordinal(); ++i) {
            if (m_Activities[i] == null) {
                continue;
            }
            m_Activities[i].OnPlayerClassCreate(kernel, script);
        }
    }

    void OnChangeDay(IKernel kernel, IGameObject player) {

    }
    void OnPlayerOnLine(IKernel kernel, IGameObject player) {
        int channel = player.getInt(PLAYER_PROPERTY_CHANNEL);
        //推送活动列表
        CustomMsg.ActivityList.Builder build = CustomMsg.ActivityList.newBuilder();
        for (Entry<Integer, Map<Integer, Integer>> map : m_mapCfgs.entrySet()) {
            int id = -1;
            if (map.getValue().containsKey(-1)) {
                id = map.getValue().get(-1);
            }
            if (map.getValue().containsKey(channel)) {
                id = map.getValue().get(channel);
            }
            if (!m_mapCfgsByID.containsKey(id)) {
                continue;
            }
            ActivityData data = m_mapCfgsByID.get(id);
            if (data == null || !data.bSwitch) {
                continue;
            }

            build.addId(data.id);
            build.addType(data.type);
            build.addStart(data.startDate);
            build.addStop(data.stopDate);
            build.addStartShow(data.startShow);
            build.addStopShow(data.stopShow);
            build.addWeight(data.weight);
            JSONObject param = JSONObject.parseObject(StringUtils.isNotBlank(data.param) ? data.param : "{}");
            param.put("startShow", data.startShowDate);
            param.put("stopShow", data.stopShowDate);
            build.addParam(param.toJSONString());
        }
        logger.info("{} {}", channel, build.getTypeList());
        if (build.getIdCount() > 0) {
            kernel.sendMessage(player, S2CMsgDef.S2C_ACTIVITY_LIST.ordinal(), build.build().toByteArray());
        }
        for (int i = 1; i < Activities.END.ordinal(); i++) {
            if (m_Activities[i] == null) {
                continue;
            }
            m_Activities[i].OnPlayerOnLine(kernel, player);
        }
    }

    void OnPlayerOffLine(IKernel kernel, IGameObject player) {
        for (int i = 1; i < Activities.END.ordinal(); ++i) {
            if (m_Activities[i] == null) {
                continue;
            }
            m_Activities[i].OnPlayerOffLine(kernel, player);
        }
    }

    void OnRefreshActivity(IKernel kernel, int serid, int msgid, byte[] msg) throws InvalidProtocolBufferException {
        ServerMsg.IntArray data = ServerMsg.IntArray.parseFrom(msg);
        int count = data.getIdCount();
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < count; ++i) {
            if (i < count - 1) {
                buffer.append(data.getId(i)).append(",");
            } else {
                buffer.append(data.getId(i));
            }
        }
        List<Object> params = new ArrayList<>();
        params.add(buffer.toString());

        kernel.executeSomeToStore(ActivityService.class, "searchByIds", params, (str) -> {
            List<Activity> activities = framework.JsonUtil.decodeToList(str, Activity.class);
            for (int i = 0; i < activities.size(); i++) {
                Activity act = activities.get(i);
                if (!LoadCfg(kernel, act)) {
                    continue;
                }
                int id = act.getId();
                ActivityData cfgData = m_mapCfgsByID.get(id);
                if (cfgData == null) {
                    continue;
                }
                //广播所有玩家，加载活动
                if (cfgData.bSwitch) {
                    CustomMsg.ActivityList.Builder build = CustomMsg.ActivityList.newBuilder();
                    build.addId(cfgData.id);
                    build.addType(cfgData.type);
                    build.addStart(cfgData.startDate);
                    build.addStop(cfgData.stopDate);
                    build.addWeight(cfgData.weight);
                    build.addStartShow(cfgData.startShow);
                    build.addStopShow(cfgData.stopShow);
                    build.addParam(cfgData.param);
                    BaseActivity activity = m_Activities[cfgData.type];
                    activity.refresh(kernel, cfgData);
                    for (int channel : cfgData.channles) {
                        if (channel == -1) {
                            kernel.broadCastCurServer(S2CMsgDef.S2C_REFRESH_ACTIVITY.ordinal(), build.build().toByteArray());
                        } else {
                            //logger.info("channel:{}", channel);
                            kernel.broadCastByChannel(channel, S2CMsgDef.S2C_REFRESH_ACTIVITY.ordinal(), build.build().toByteArray());
                        }
                    }
                    logger.info("OnRefreshActivity: {}", id);
                }
            }
        });
    }

    void OnCloseActivity(IKernel kernel, int serid, int msgid, byte[] msg) throws InvalidProtocolBufferException {
        ServerMsg.IntArray data = ServerMsg.IntArray.parseFrom(msg);
        int count = data.getIdCount();
        for (int i = 0; i < count; ++i) {
            int id = data.getId(i);
            if (!m_mapCfgsByID.containsKey(id)) {
                continue;
            }

            ActivityData cfg = m_mapCfgsByID.get(id);
            if (cfg.bSwitch) {
                cfg.bSwitch = false;
            }
            // 广播所有玩家，关闭活动
            CustomMsg.Int32.Builder build = CustomMsg.Int32.newBuilder();
            build.setValue(id);

            for (int channel : cfg.channles) {
                if (channel == -1) {
                    kernel.broadCastCurServer(S2CMsgDef.S2C_CLOSE_ACTIVITY.ordinal(), build.build().toByteArray());
                } else {
                    kernel.broadCastByChannel(channel, S2CMsgDef.S2C_CLOSE_ACTIVITY.ordinal(), build.build().toByteArray());
                }
                logger.info("后台活动关闭了:通知客户端OnCloseActivity: {}", build.build().toString());
            }
        }
    }

    public void OnCheckTimer(IKernel kernel, IGameObject world) {
        long now = kernel.getServerTime();
        for (Map<Integer, Integer> map : m_mapCfgs.values()) {
            for (Entry<Integer, Integer> entry : map.entrySet()) {
                int id = entry.getValue();
                ActivityData cfg = m_mapCfgsByID.get(id);
                if (cfg == null) {
                    continue;
                }
                BaseActivity activity = m_Activities[cfg.type];
                if (!cfg.bSwitch) {
                    if (cfg.bInDate) {
                        //后台前置关闭正在运行的活动，需要停止活动
                        activity.Stop(kernel, cfg);
                    }
                    continue;
                }
                if (now < cfg.startShowDate) {//未开始
                    continue;
                }
                if (now > cfg.stopShowDate) {//已经结束
                    if (cfg.bIsShow) {
                        cfg.bIsShow = false;
                        activity.StopShow(kernel, cfg);
                    }
                } else {
                    if (!cfg.bIsShow) {
                        cfg.bIsShow = true;
                        activity.StartShow(kernel, cfg);
                    }
                }
                if (now < cfg.startDate) {//尚未开始
                    continue;
                }
                if (now <= cfg.stopDate) {
                    if (!cfg.bInDate) {
                        cfg.bInDate = true;
                        activity.Start(kernel, cfg);
                    }
                } else {
                    if (cfg.bInDate) {
                        activity.Stop(kernel, cfg);
                    }
                }
            }
        }
    }

    public ActivityData GetCfg(int type, int channel) {
        if (!m_mapCfgs.containsKey(type)) {
            return null;
        }
        Map<Integer, Integer> types = m_mapCfgs.get(type);
        if (types == null) {
            return null;
        }
        int id = -1;
        if (types.containsKey(-1)) {
            id = types.get(-1);
        }
        if (types.containsKey(channel)) {
            id = types.get(channel);
        }
        if (id == -1 || !m_mapCfgsByID.containsKey(id)) {
            return null;
        }
        return m_mapCfgsByID.get(id);
    }

    public void OnDailyCheckTimer(IKernel kernel, String timer) {
        logger.info("OnDailyCheckTimer: {}", timer);
        m_Activities[Activities.BombCoinDayRankActivity.ordinal()].OnDailyCheckTimer(kernel, timer);
        m_Activities[Activities.BombCoinLimitDayRankActivity.ordinal()].OnDailyCheckTimer(kernel, timer);
        m_Activities[Activities.BombCoinWeekRankActivity.ordinal()].OnDailyCheckTimer(kernel, timer);
    }

    public void OnWeekCheckTimer(IKernel kernel, String timer) {
        logger.info("OnWeekCheckTimer: {}", timer);
        m_Activities[Activities.BombCoinDayRankActivity.ordinal()].OnDailyWeekTimer(kernel, timer);
        m_Activities[Activities.BombCoinWeekRankActivity.ordinal()].OnDailyWeekTimer(kernel, timer);
    }


    void UpdateActivityConfig(IKernel kernel, int reqid, byte[] data) throws Exception {
        CustomMsg.Int32 parsed = CustomMsg.Int32.parseFrom(data);
        int value = parsed.getValue();
        List<Object> params = new ArrayList<>();
        params.add(String.valueOf(value));
        kernel.executeSomeToStore(ActivityService.class,"searchByIds",params, (str) -> {
            List<Activity> activities = JsonUtil.decodeToList(str, Activity.class);
            for (int i = 0; i < activities.size(); i++) {
                Activity act = activities.get(i);
                if (!LoadCfg(kernel, act)) {
                    continue;
                }
                int id = act.getId();
                ActivityData cfgData = m_mapCfgsByID.get(id);
                if (cfgData == null) {
                    continue;
                }
                //广播所有玩家，加载活动
                if (cfgData.bSwitch) {
                    CustomMsg.ActivityList.Builder build = CustomMsg.ActivityList.newBuilder();
                    build.addId(cfgData.id);
                    build.addType(cfgData.type);
                    build.addStart(cfgData.startDate);
                    build.addStop(cfgData.stopDate);
                    build.addWeight(cfgData.weight);
                    build.addStartShow(cfgData.startShow);
                    build.addStopShow(cfgData.stopShow);
                    build.addParam(cfgData.param);
                    BaseActivity activity = m_Activities[cfgData.type];
                    activity.refresh(kernel, cfgData);
//                    for (int channel : cfgData.channles) {
//                        if (channel == -1) {
//                            kernel.broadCastCurServer(S2CMsgDef.S2C_ACTIVITY_REFRESH_CONFIG.ordinal(), build.build().toByteArray());
//                        } else {
//                            //logger.info("channel:{}", channel);
//                            kernel.broadCastByChannel(channel, S2CMsgDef.S2C_ACTIVITY_REFRESH_CONFIG.ordinal(), build.build().toByteArray());
//                        }
//                    }
                    logger.info("OnRefreshActivity: {}", id);
                }
            }
        });
        kernel.responseServer(reqid,ByteUtils.objectToByte(value) );
    }
}

