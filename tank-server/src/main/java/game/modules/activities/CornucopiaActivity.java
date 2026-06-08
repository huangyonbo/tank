package game.modules.activities;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.google.gson.JsonObject;
import common.ServerMsgDef;
import framework.PropertyKey;
import framework.SpringContextUtil;
import framework.game.IGameObject;
import framework.game.IKernel;
import framework.game.ValueType;
import framework.mybatis.domain.Config;
import framework.mybatis.domain.Cornucopia;
import framework.mybatis.service.impl.ConfigService;
import framework.mybatis.service.impl.CornucopiaService;
import framework.mybatis.utils.DateUtils;
import game.custommsg.CommandDef;
import game.custommsg.RequestMsgDef;
import game.modules.StatisticsType;
import game.modules.items.ItemModule;
import game.modules.utils.UtilFunc;
import game.util.TimeUtils;
import org.apache.commons.lang.StringUtils;
import redis.clients.jedis.Jedis;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class CornucopiaActivity extends BaseActivity{
    public static String CornucopiaValue = "CornucopiaValue";
    public static String CornucopiaVersion = "CornucopiaVersion";
    public static String CornucopiaGetState = "CornucopiaState";
    public static String CornucopiaModuleHistory = "CornucopiaModule::History::";
    private static Map<String, Double> statisticsRatio = new HashMap<>();//统计比例

    ItemModule itemModule;

    public CornucopiaActivity(int type, ActivityMgr mgr) {
        super(type, mgr);
    }

    @Override
    public boolean OnInit(IKernel kernel) {
        kernel.regServerMsg(ServerMsgDef.B2C_SET_STATISTICS_RATIO_CONFIG_CAISHEN.ordinal(), this, "onUpdateStatisticsConfig");

        itemModule = kernel.getModule(ItemModule.class);
//        kernel.regEvent(KernelEvent.KEVENT_ON_LINE, "Player", this, "OnPlayerOnLine");
//        kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
        kernel.regRequestMessage(RequestMsgDef.REQ_CORNUCOPIA_VALUE.ordinal(), this, "OnCornucopiaValue");
        kernel.regRequestMessage(RequestMsgDef.REQ_CORNUCOPIA_HISTORY.ordinal(), this, "OnCornucopiaHistory");
        kernel.regCommand(CommandDef.CMD_CHANGE_DAY.ordinal(), "Player", this, "OnChangeDay");
        try {
            onUpdateStatisticsConfig(null, 0, 0, null);
        } catch (Exception e) {
        }
        return true;
    }
    void OnChangeDay(IKernel kernel, IGameObject player) {
        long version = player.getLong(CornucopiaVersion);
        long zeroTime = UtilFunc.getZeroTime(kernel.getServerTime());
        if (version != zeroTime) {
            player.setProperty(CornucopiaVersion, zeroTime);
            player.setProperty(CornucopiaGetState, true);
        }
    }
    @Override
    public void OnWorldCreate(IKernel kernel, IGameObject world) {

    }

    @Override
    public void OnPlayerClassCreate(IKernel kernel, String script) {
        kernel.declareProperty(script, CornucopiaValue, ValueType.LONG, false, true, true);
        kernel.declareProperty(script, CornucopiaGetState, ValueType.BOOL, false, true, true);
        kernel.declareProperty(script, CornucopiaVersion, ValueType.LONG, false, false, true);
    }

    @Override
    public void OnPlayerOnLine(IKernel kernel, IGameObject player) {
        long version = player.getLong(CornucopiaVersion);
        long zeroTime = UtilFunc.getZeroTime(kernel.getServerTime());
        if (version != zeroTime) {
            player.setProperty(CornucopiaVersion, zeroTime);
            player.setProperty(CornucopiaGetState, true);
        }
        GetRedisToMysql(kernel,player);
    }

    @Override
    public void OnPlayerOffLine(IKernel kernel, IGameObject player) {

    }

    @Override
    public void RefreshCfg(IKernel kernel, String path) {

    }

    @Override
    protected void OnCheckVersion(IKernel kernel, IGameObject player, ActivityMgr.ActivityData cfg) {

    }

    @Override
    void OnClearData(IKernel kernel, IGameObject player, ActivityMgr.ActivityData cfg) {
        player.setProperty(CornucopiaValue,0L);
    }

    @Override
    void OnInitData(IKernel kernel, IGameObject player, ActivityMgr.ActivityData cfg) {

    }

    @Override
    void OnDailyReset(IKernel kernel, IGameObject player, ActivityMgr.ActivityData cfg) {

    }

    @Override
    void OnCheckReward(IKernel kernel, IGameObject player, ActivityMgr.ActivityData cfg) {

    }

    @Override
    void OnStartShow(IKernel kernel, ActivityMgr.ActivityData cfg) throws Exception {

    }

    @Override
    void OnStopShow(IKernel kernel, ActivityMgr.ActivityData cfg) {

    }

    @Override
    void OnStart(IKernel kernel, ActivityMgr.ActivityData cfg) {

    }

    @Override
    void OnStop(IKernel kernel, ActivityMgr.ActivityData cfg) {

    }

    public void AddCornucopiaValue(IGameObject player, long addValue, StatisticsType type) {
        if (!IsActive(player)){
            return;
        }
        long value = player.getLong(CornucopiaValue);
        player.setProperty(CornucopiaValue, value + (long) (addValue * statisticsRatio.getOrDefault(type.getDesc(), 0d)));
    }
    public void AddCornucopiaValue(IGameObject player, int addValue) {
        long value = player.getLong(CornucopiaValue);
        player.setProperty(CornucopiaValue, value + addValue);
    }

    public void OnCornucopiaHistory(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg){
        JSONObject history = getHistory(kernel, player);

        UtilFunc.respRpcStringToClient(kernel, player, reqid, history.toString());
    }
    void OnCornucopiaValue(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg){
        JsonObject json = new JsonObject();
        do {
            if (!player.getBool(CornucopiaGetState)) {
                json.addProperty("code", 1);
                json.addProperty("msg", "今天领取次数已领取完，请明天再来");
                json.addProperty("value", 0);
                break;
            }
            long BombCoin = player.getLong(CornucopiaValue);
            if (BombCoin <= 0) {
                json.addProperty("code", 1);
                json.addProperty("msg", "当前没有可领取值");
                json.addProperty("value", 0);
                break;
            }
            long have = player.getLong(PropertyKey.PLAYER_PROPERTY_BOMB_COIN);
            player.setProperty(PLAYER_PROPERTY_BOMB_COIN, have + BombCoin);
            player.setProperty(CornucopiaValue,0L);
            player.setProperty(CornucopiaGetState, false);
            itemModule.addItemLog(kernel, player, PropertyKey.PLAYER_PROPERTY_BOMB_COIN, (int) BombCoin, UtilFunc.System.NULL.ordinal(), "聚宝盆领取");
            json.addProperty("code", 0);
            json.addProperty("msg", "sucess");
            json.addProperty("value", BombCoin);
            addHistory(kernel,player,BombCoin);
            break;
        } while (false);
        UtilFunc.respRpcStringToClient(kernel, player, reqid, json.toString());
    }
    void addHistory(IKernel kernel,IGameObject player,long bombValue){
        int uid = player.getInt(PLAYER_PROPERTY_UID);
        String userName = player.getString(PropertyKey.PLAYER_PROPERTY_NAME);
        int channelID = player.getInt(PropertyKey.PLAYER_PROPERTY_CHANNEL);
        ArrayList<Object> objects = new ArrayList<>();
        Cornucopia cornucopia = new Cornucopia();
        cornucopia.setUid(uid);
        cornucopia.setUserName(userName);
        long serverTime = kernel.getServerTime();
        cornucopia.setDate(TimeUtils.GetCurrentDay(kernel, serverTime));
        cornucopia.setCreateTime(DateUtils.timeFormat.format(new Date(serverTime)));
        cornucopia.setValue((int) bombValue);
        cornucopia.setChannelId(channelID);
        objects.add(cornucopia);
        kernel.executeSomeToStore(CornucopiaService.class,"AddOne", objects,null);
    }
    JSONObject getHistory(IKernel kernel,IGameObject player){
//        long nowDay = kernel.getServerTime();
//        long lastTime = nowDay - 15 * 24 * 3600 * 100;
//        JSONObject jsonObject = new JSONObject();
//        String uid = player.getInt(PLAYER_PROPERTY_UID).toString();
//        DateFormat dayFormat = kernel.getServer().getTimeFormat();
//        Jedis jedis = kernel.getJedis();
//        Map<String, String> stringStringMap = jedis.hgetAll(CornucopiaModuleHistory + uid);
//        List<Long> collect = stringStringMap.keySet().stream().map(a -> Long.parseLong(a)).filter(a -> a < lastTime).collect(Collectors.toList());
//        collect.forEach(a->jedis.hdel(CornucopiaModuleHistory+uid,String.valueOf(a)));
//        List<JSONObject> list = jedis.hgetAll(CornucopiaModuleHistory + uid).entrySet().stream().map(entry -> {
//            return new JSONObject().fluentPut("time", Long.parseLong(entry.getKey())).fluentPut("value", Long.parseLong(entry.getValue()));
//        }).collect(Collectors.toList());
        int uid=player.getInt(PLAYER_PROPERTY_UID);
        JSONObject jsonObject = new JSONObject();
        CornucopiaService bean = SpringContextUtil.getBean(CornucopiaService.class);
        List<Cornucopia> cornucopias = bean.GetHistory(uid, 15);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getDefault()); // 指定时区（默认系统时区）
        List<JSONObject> list = cornucopias.stream().map(a -> {
            try {
                return new JSONObject().fluentPut("time", sdf.parse(a.getCreateTime()).getTime()).fluentPut("value", a.getValue());
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
        jsonObject.put("list",list);
        return jsonObject;
    }


    void GetRedisToMysql(IKernel kernel,IGameObject player){
        Jedis jedis = kernel.getJedis();
        int uid = player.getInt(PLAYER_PROPERTY_UID);
        String userName = player.getString(PropertyKey.PLAYER_PROPERTY_NAME);
        String redisKey = CornucopiaModuleHistory + uid;
        if (!jedis.exists(redisKey)){
            return;
        }
        List<Cornucopia> collect = jedis.hgetAll(redisKey).entrySet().stream().map(entry -> {
            Cornucopia cornucopia = new Cornucopia();
            cornucopia.setUid(uid);
            cornucopia.setUserName(userName);
            cornucopia.setDate(TimeUtils.GetCurrentDay(kernel, Long.parseLong(entry.getKey())));
            cornucopia.setCreateTime(DateUtils.timeFormat.format(new Date(Long.parseLong(entry.getKey()))));
            cornucopia.setValue(Integer.parseInt(entry.getValue()));
            cornucopia.setChannelId(164);
            return cornucopia;
        }).collect(Collectors.toList());
        ArrayList<Object> objects = new ArrayList<>();
        objects.add(collect);
        kernel.executeSomeToStore(CornucopiaService.class,"AddAllHistory", objects, null);
        jedis.del(redisKey);
    }

    public void onUpdateStatisticsConfig(IKernel kernel, int serid, int msgid, byte[] msg) throws Exception {
        ConfigService configService = SpringContextUtil.getBean(ConfigService.class);
        Config config = configService.getById("caishen");
        String value = config.getValue();
        try {
            if (StringUtils.isNotEmpty(value)) {
                Map<String, Double> hashMap =
                        JSON.parseObject(value, new TypeReference<Map<String, Object>>() {})
                                .entrySet()
                                .stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        e -> ((Number) e.getValue()).doubleValue()
                                ));
                statisticsRatio.putAll(hashMap);
                logger.info("更新财神送宝值为  {} ", value);
            }
        } catch (Exception e) {
            logger.info("更新财神送宝错误  {} ", value);
        }
    }
}
