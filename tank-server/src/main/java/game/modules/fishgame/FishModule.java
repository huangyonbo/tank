package game.modules.fishgame;

import framework.game.*;
import game.custommsg.CommandDef;
import game.modules.skills.SkillModule;

import java.util.HashMap;
import java.util.Map;

public class FishModule implements ILogicModule {
    public enum FishType {
        TYPE_UNKNOW,
        TYPE_SMALL,        // 小型鱼
        TYPE_TEAM,        // 小型鱼组
        TYPE_NORMAL,    // 中型鱼
        TYPE_BIG,        // 大型鱼
        TYPE_COLOR,        // 彩金鱼
        TYPE_GROUP,        // 组合鱼
        TYPE_BOMB,        // 同类炸弹鱼
        TYPE_FUNC,        // 功能鱼
        TYPE_BOSS,        // BOSS
        TYPE_FMT,        // 鱼阵
        TYPE_SPEC,        // 特殊玩法鱼
        TYPE_SMALL1,      // 小型鱼包1（与 SmallPkg 独立统计）
        TYPE_SMALL2,      // 小型鱼包2
        TYPE_SMALL3,      // 小型鱼包3
        TYPE_SMALL4,      // 小型鱼包4
        TYPE_END
    }

    public class FishData {
        public String name;//名字
        public String cfgid;
        public String describe; // 描述
        public int type;//类型
        public int enumType;
        public String skill;
        public int[] path;
        public String realid;
        public int fmtindex;
        public float[] rand;
        public float speed;
        public int minBet;
        public int maxBet;
        public boolean notDropGold;
        public boolean marquee; // 跑马灯
        public int repeatTime;//刷新开始后能复活的时间:0 不能复活; >0 x分钟内被击杀就自动刷新一只
        public int survivalTime;//存活时间，单位秒
        public float intervalTime;
        public boolean isFrozen;
        public String[] showAnis;
    }

    Map<String, FishData> m_mapFishes = new HashMap<>();
    Map<Integer, FishData> m_mapFishesByEnum = new HashMap<>();
    SkillModule m_SkillModule = null;

    public FishModule(IKernel kernel) {
    }

    @Override
    public boolean onInit(IKernel kernel) {
        kernel.regCommand(CommandDef.CMD_FISH_DIE.ordinal(), "FishDesk", this, "OnFishDie");

        kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");

        RefreshCfg(kernel, "res/Game/Fish.xml");

        m_SkillModule = (SkillModule) kernel.getModule("SkillModule");
        if (m_SkillModule == null) {
            return false;
        }
        return true;
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub

    }

    void RefreshCfg(IKernel kernel, String path) {
        if (path.equals("res/Game/Fish.xml")) {
            m_mapFishes.clear();
            m_mapFishesByEnum.clear();
            LoadConfig(kernel, path);
        }
    }

    boolean LoadConfig(IKernel kernel, String path) {
        ICfgReader fishConfig = kernel.loadXmlConfig(path);
        if (fishConfig == null) {
            return false;
        }
        int count = fishConfig.getItemCount();
        for (int i = 0; i < count; ++i) {
            FishData data = new FishData();
            data.cfgid = fishConfig.getString(i, "Id");
//            data.name = fishConfig.getString(i, PLAYER_PROPERTY_NAME);
            data.name = fishConfig.getString(i, "NewName");
            data.describe = fishConfig.getString(i, "Describe");
            data.type = fishConfig.getInt(i, "Type");
            data.enumType = fishConfig.getInt(i, "Enum");
            data.skill = fishConfig.getString(i, "Skill");
            data.path = fishConfig.getIntArray(i, "Path", ",");
            data.realid = fishConfig.getString(i, "RealID");
            data.fmtindex = fishConfig.getInt(i, "FmtIndex");
            data.rand = fishConfig.getFloatArray(i, "Rand", ",");
            data.speed = fishConfig.getFloat(i, "Speed");
            if (data.speed <= 0) {
                data.speed = 1.0f; // 若未配置速度，默认为1 防止出现鱼打不死的情况
            }
            data.notDropGold = fishConfig.getBool(i, "NotDropGold");
            data.marquee = fishConfig.getBool(i, "Marquee");
            data.repeatTime = fishConfig.getInt(i,"repeatTime");
            data.survivalTime = fishConfig.getInt(i,"survivalTime");
            data.intervalTime = fishConfig.getFloat(i,"intervalTime");
            data.showAnis = fishConfig.getString(i,"showAnis").split(",");
            data.isFrozen = fishConfig.getInt(i, "isFrozen") == 1;
            int[] bet = fishConfig.getIntArray(i, "Bet", "-");
            if (bet == null) {
                data.minBet = 0;
                data.maxBet = 0;
            } else if (bet.length == 1) {
                data.minBet = bet[0];
                data.maxBet = bet[0];

            } else if (bet.length >= 2) {
                data.minBet = bet[0];
                data.maxBet = bet[1];

            }
            m_mapFishes.put(data.cfgid, data);
            m_mapFishesByEnum.put(data.enumType, data);

        }
        return true;
    }

    public FishData GetFishData(String fishid) {
        return m_mapFishes.get(fishid);
    }

    public FishData GetFishData(int enumType) {
        return m_mapFishesByEnum.get(enumType);
    }

    public int GetFishCount() {
        return m_mapFishes.size();
    }

    public void OnFishDie(IKernel kernel, IGameObject desk, Object... objects) {
        IGameObject player = (IGameObject) objects[0];
        int fishindex = (int) objects[1];
        String fishcfg = (String) objects[2];

        FishData data = GetFishData(fishcfg);
        if (data == null) {
            return;
        }

        if (!data.skill.isEmpty()) {
            m_SkillModule.UseSkill(kernel, player, fishindex, data.skill);
        }
        if (data.cfgid.equals("fish139")) {
            desk.setProperty(DESK_NOT_OUT_FISH_END_TIME, kernel.getServerTime() + 11000);
        }
    }
}
