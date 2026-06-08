package game.modules.activities;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import framework.JsonUtil;
import framework.PropertyKey;
import framework.game.IGameObject;
import framework.game.IKernel;
import game.custommsg.CommandDef;
import game.custommsg.CustomMsg;
import game.custommsg.S2CMsgDef;
import game.modules.activities.ActivityMgr.ActivityData;
import game.modules.utils.UtilFunc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * 活动基类
 * 1、开始显示时，清理历数据
 * 2、开始活动是，初始化数据
 * 3、活动结束时，结算数据
 *
 * 玩家登陆时：
 * 版本号不一致：1、清理数据
 * 			   2、已开始：初始数据
 */
public abstract class BaseActivity implements PropertyKey {
    protected int m_type;
    protected String m_verPro = "";
    protected ActivityMgr m_ActivityMgr;
    protected CornucopiaActivity cornucopiaActivity;
    protected static Logger logger = LoggerFactory.getLogger(BaseActivity.class);

    public BaseActivity(int type, ActivityMgr mgr) {
        m_type = type;
        m_ActivityMgr = mgr;
    }

    public boolean Init(IKernel kernel) {
        kernel.regCommand(CommandDef.CMD_ACT_START.ordinal(), "Player", this, "OnCmdStart");
        kernel.regCommand(CommandDef.CMD_ACT_STOP.ordinal(), "Player", this, "OnCmdStop");
        kernel.regCommand(CommandDef.CMD_ACT_START_SHOW.ordinal(), "Player", this, "OnCmdStartShow");
        kernel.regCommand(CommandDef.CMD_ACT_STOP_SHOW.ordinal(), "Player", this, "OnCmdStopShow");
        //kernel.regCommand(CommandDef.CMD_ACT_REFRESH.ordinal(), "Player", this, "OnCmdRefreshShow");
        return OnInit(kernel);
    }

    void OnCmdStart(IKernel kernel, IGameObject player, Object... objects) {
        ActivityData cfg = (ActivityData) objects[0];
        if (cfg.type != m_type) {
            return;
        }
        // 开始，检测版本
        OnCheckVersion(kernel, player, cfg);
        OnAddData(kernel, player, cfg);
    }

    void OnCmdStop(IKernel kernel, IGameObject player, Object... objects) {
        ActivityData cfg = (ActivityData) objects[0];
        if (cfg.type != m_type) {
            return;
        }
        OnCheckReward(kernel, player, cfg);
    }

    void OnCmdStartShow(IKernel kernel, IGameObject player, Object... objects) {
        ActivityData cfg = (ActivityData) objects[0];
        if (cfg.type != m_type) {
            return;
        }
        // 开始展示，检测版本
        OnCheckVersion(kernel, player, cfg);
    }

    void OnCmdStopShow(IKernel kernel, IGameObject player, Object... objects) {
        ActivityData cfg = (ActivityData) objects[0];
        if (cfg.type != m_type) {
            return;
        }
    }

    void OnCmdRefreshShow(IKernel kernel, IGameObject player, Object... objects) {
        ActivityData cfg = (ActivityData) objects[0];
        if (cfg.type != m_type) {
            return;
        }
        OnRefreshData(kernel, player, cfg);
    }

    public void CheckVersion(IKernel kernel, IGameObject player, String verPro) {
        ActivityData data = GetCfg(player);
        if (data == null) {
            return;
        }
        if (!verPro.isEmpty()) {
            if (player.getLong(verPro) != data.version) {
                //版本升级，清理数据
                OnClearData(kernel, player, data);
                if (IsActive(player)) {
                    player.setProperty(verPro, data.version);
                    // 活动已经开始，初始化数据
                    OnInitData(kernel, player, data);
                }
            } else {
                if (!IsActive(player)) {
                    //活动结束，检测未发放的奖励
                    OnCheckReward(kernel, player, data);
                }
            }
        }
    }

    public void CheckVersion(IKernel kernel, IGameObject player, String verPro, String dailyVerPro) {
        ActivityData data = GetCfg(player);
        if (data == null) {
            return;
        }
        if (!verPro.isEmpty()) {
            if (player.getLong(verPro) != data.version) {
                // 版本升级，清理数据
                OnClearData(kernel, player, data);
                if (IsActive(player)) {
                    player.setProperty(verPro, data.version);
                    // 活动已经开始，初始化数据
                    OnInitData(kernel, player, data);
                }
            } else {
                if (!IsActive(player)) {
                    // 活动结束，检测未发放的奖励
                    OnCheckReward(kernel, player, data);
                }
            }
        }
        long dailyDate = UtilFunc.getZeroTime(kernel.getServerTime());
        if (!dailyVerPro.isEmpty() && player.getLong(dailyVerPro) != dailyDate) {
            if (IsActive(player)) {
                player.setProperty(dailyVerPro, dailyDate);
                // 活动已经开始，每日数据重置
                OnDailyReset(kernel, player, data);
            }
        }
    }


    public boolean IsActive(IGameObject player) {
        int channel = player.getInt(PLAYER_PROPERTY_CHANNEL);
        ActivityData cfg = m_ActivityMgr.GetCfg(m_type, channel);
        if (cfg == null) {
            return false;
        }
        return cfg.bSwitch && cfg.bInDate;
    }

    public boolean IsShow(IGameObject player) {
        int channel = player.getInt(PLAYER_PROPERTY_CHANNEL);
        ActivityData cfg = m_ActivityMgr.GetCfg(m_type, channel);
        if (cfg == null) {
            return false;
        }

        return cfg.bSwitch && cfg.bIsShow;
    }

    public ActivityData GetCfg(IGameObject player) {
        int channel = player.getInt(PLAYER_PROPERTY_CHANNEL);
        return m_ActivityMgr.GetCfg(m_type, channel);
    }

    public boolean OnInit(IKernel kernel) {
        return true;
    }

    public void OnDestroy() {

    }

    public boolean ParseCfg(IKernel kernel,ActivityData cfg) {
        return true;
    }

    public void StartShow(IKernel kernel, ActivityData cfg) {
        CustomMsg.ActivityList.Builder build = CustomMsg.ActivityList.newBuilder();
        build.addId(cfg.id);
        build.addType(cfg.type);
        build.addStart(cfg.startDate);
        build.addStop(cfg.stopDate);
        build.addWeight(cfg.weight);
        build.addStartShow(cfg.startShow);
        build.addStopShow(cfg.stopShow);
        //build.addParam(cfg.param);
        for (int channel : cfg.channles) {
            if (channel == -1) {
                kernel.broadCastCurServer(S2CMsgDef.S2C_REFRESH_ACTIVITY.ordinal(),build.build().toByteArray());
            } else {
                kernel.broadCastByChannel(channel, S2CMsgDef.S2C_REFRESH_ACTIVITY.ordinal(),build.build().toByteArray());
            }
        }
        //logger.info("OnRefreshActivity: {}", build.build());
        kernel.commandAllPlayer(CommandDef.CMD_ACT_START_SHOW.ordinal(), cfg);
        try {
            OnStartShow(kernel, cfg);
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    public void StopShow(IKernel kernel, ActivityData cfg) {
        kernel.commandAllPlayer(CommandDef.CMD_ACT_STOP_SHOW.ordinal(), cfg);
        OnStopShow(kernel, cfg);
    }

    public void Start(IKernel kernel, ActivityData cfg) {
        kernel.commandAllPlayer(CommandDef.CMD_ACT_START.ordinal(), cfg);
        OnStart(kernel, cfg);
    }

    public void Stop(IKernel kernel, ActivityData cfg) {
        cfg.bInDate = false;
        cfg.bStoped = true;
        kernel.commandAllPlayer(CommandDef.CMD_ACT_STOP.ordinal(), cfg);
        OnStop(kernel,cfg);
    }

    public void refresh(IKernel kernel, ActivityData cfg) {
        kernel.commandAllPlayer(CommandDef.CMD_ACT_REFRESH.ordinal(), cfg);
    }



    //活动增加数据
    public void OnAddData(IKernel kernel, IGameObject player, ActivityData cfg) {

    }

    // 刷新数据
    public void OnRefreshData(IKernel kernel, IGameObject player, ActivityData cfg) {

    }

    //客户端领取活动奖励通用方法
    public void OnGetActivityReward(IKernel kernel, IGameObject player,JsonObject json, int msgid) {

    }
    //客户端领取活动配置通用方法
    public void OnReqActivityConfig(IKernel kernel, IGameObject player, int msgid, int reqid) {
        JSONObject info = new JSONObject();
        ActivityMgr.ActivityData data = GetCfg(player);
        if (data==null){
            logger.error("请求活动信息报错");
            return;
        }
        info.put("params",JsonUtil.encodeToStr(data.param));
        info.put("description",data.description);
        info.put("type",data.type);
        info.put("startTime",data.startTime);
        info.put("stopTime",data.stopTime);
        info.put("id",data.id);
        UtilFunc.respRpcStringToClient(kernel, player, reqid, JsonUtil.encodeToStr(info));
    }
    //客户端领取活动配置通用方法
    public void OnReqActivityInfo(IKernel kernel, IGameObject player, int msgid, int reqid) {
    }

    public void OnGetActivityRewardNeedCallBack(IKernel kernel, IGameObject player, JsonObject json, int msgid, int reqid) {

    }

    public void OnParentNetReady(IKernel kernel){

    }

    protected void updateRealInfo(IGameObject player,String name,String phone,String address){
        player.setProperty(PLAYER_PROPERTY_REAL_INFO_NAME,name);
        player.setProperty(PLAYER_PROPERTY_REAL_INFO_PHONE,phone);
        player.setProperty(PLAYER_PROPERTY_REAL_INFO_ADDRESS,address);
    }

    //世界属性声明
    public abstract void OnWorldCreate(IKernel kernel, IGameObject world);

    //玩家属性申明
    public abstract void OnPlayerClassCreate(IKernel kernel, String script);

    //玩家上线
    public abstract void OnPlayerOnLine(IKernel kernel, IGameObject player);

    //玩家离线
    public abstract void OnPlayerOffLine(IKernel kernel, IGameObject player);

    //热更新配置文件
    public abstract void RefreshCfg(IKernel kernel, String path);

    // 检测版本
    protected abstract void OnCheckVersion(IKernel kernel, IGameObject player, ActivityData cfg);

    // 清理数据
    abstract void OnClearData(IKernel kernel, IGameObject player, ActivityData cfg);

    // 初始化数据
    abstract void OnInitData(IKernel kernel, IGameObject player, ActivityData cfg);

    // 每日数据重置
    abstract void OnDailyReset(IKernel kernel, IGameObject player, ActivityData cfg);

    // 检测活动奖励
    abstract void OnCheckReward(IKernel kernel, IGameObject player, ActivityData cfg);

    // 开始显示
    abstract void OnStartShow(IKernel kernel, ActivityData cfg) throws Exception;

    // 结束显示
    abstract void OnStopShow(IKernel kernel, ActivityData cfg);

    // 开始
    abstract void OnStart(IKernel kernel, ActivityData cfg);

    // 结束
    abstract void OnStop(IKernel kernel, ActivityData cfg);
    public void OnDailyCheckTimer(IKernel kernel, String timer) {
    }
    public void OnDailyWeekTimer(IKernel kernel, String timer) {
    }
}
