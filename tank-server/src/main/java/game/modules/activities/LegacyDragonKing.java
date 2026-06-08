package game.modules.activities;

import back.modules.MailModule;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import framework.JsonUtil;
import framework.MathUtils;
import framework.PropertyKey;
import framework.RateData;
import framework.game.IGameObject;
import framework.game.IKernel;
import framework.game.MailSystemDef;
import framework.game.ValueType;
import game.custommsg.C2SMsgDef;
import game.custommsg.CustomMsg;
import game.custommsg.RequestMsgDef;
import game.modules.items.ItemModule;
import game.modules.utils.UtilFunc;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 活动：龟相来和
 */

public class LegacyDragonKing extends BaseActivity implements PropertyKey{

    //private static final Logger logger = LoggerFactory.getLogger(TortoisePhaseReward.class);

    public LegacyDragonKing(int type, ActivityMgr mgr) {
        super(type, mgr);
    }

    private MailModule mailModule = null;
    private ItemModule itemModule = null;

    private int costNum;//道具消耗
    private String costItemId = "item_longlin";

    static class RewardData {
        int id;
        String itemId;
        int count;
        int weight;
    }


    public boolean OnInit(IKernel kernel) {
        kernel.regRequestMessage(RequestMsgDef.EQ_LDK_DO_PLAY.ordinal(), this, "PlayLogic");
        kernel.regClientMessage(C2SMsgDef.C2S_GET_LDK_REWARD.ordinal(), this, "OnRecClientDone");
        kernel.declareHeartBeat("HB_CheckLdkRewardStatus", this, "OnCheckGetRewardStatus");
        mailModule = (MailModule) kernel.getModule("MailModule");
        itemModule = (ItemModule) kernel.getModule("ItemModule");
        return true;
    }



    public void OnPlayerClassCreate(IKernel kernel, String script) {
        kernel.declareProperty(script, PLAYER_PROPERTY_LDK_REWARD_IDS, ValueType.STRING, false, false,false);
    }


    public void OnPlayerOnLine(IKernel kernel, IGameObject player) {

    }

    @Override
    public void OnWorldCreate(IKernel kernel, IGameObject world) {

    }


    // 玩家下线时如果没有领取奖励 则发邮件给玩家
    public void OnPlayerOffLine(IKernel kernel, IGameObject player) {
        if (!player.haveHeartBeat("HB_CheckLdkRewardStatus")){
            return;
        }
        SentMail(kernel, player);
    }

    @Override
    public void RefreshCfg(IKernel kernel, String path) {

    }

    // 客户端动画播放完 通知服务器发放奖励
    void OnRecClientDone(IKernel kernel, IGameObject player, int msgid, byte[] data) {
        if (!kernel.haveHeartBeat(player,"HB_CheckLdkRewardStatus")){
            return;
        }
        kernel.removeHeartBeat(player, "HB_CheckLdkRewardStatus");
        String itemStr = player.getString(PLAYER_PROPERTY_LDK_REWARD_IDS);
        if (itemStr == null || itemStr.length() == 0){
            return;
        }
        List<String> items = JsonUtil.decodeToList(itemStr,String.class);
        for (String item : items){
            String[] ss = item.split("\\*");
            String itemId = ss[0];
            int count = Integer.parseInt(ss[1]);
            if (count > 0){
                itemModule.AddItem(kernel, player, itemId, count, UtilFunc.System.TREASURE_IN_SEA.ordinal(), "ldk award");
            }
        }
    }

    //超时服务器发放奖励邮件
    void OnCheckGetRewardStatus(IKernel kernel, IGameObject player) {
        if (!kernel.haveHeartBeat(player, "HB_CheckLdkRewardStatus")) {
            return;
        }
        SentMail(kernel,player);
    }

    void SentMail(IKernel kernel, IGameObject player) {
        kernel.removeHeartBeat(player, "HB_CheckLdkRewardStatus");
        String itemStr = player.getString(PLAYER_PROPERTY_LDK_REWARD_IDS);
        if (itemStr == null || itemStr.length() == 0){
            return;
        }
        List<String> items = JsonUtil.decodeToList(itemStr,String.class);
        StringJoiner joiner = new StringJoiner(";");
        for (String item : items){
            joiner.add(item);
        }
        //恭喜你在{0:gameName}游戏中获得金币{1:backGold}，这是你的奖励，请查收!
        List<String> title = new ArrayList<>();
        title.add("TXT_LEGACY_DRAGON_KING_TITLE");
        List<String> context = new ArrayList<>();
        context.add("TXT_LEGACY_DRAGON_KING_CONTEXT");
        context.add("龟相来贺");
    }

    @Override
    public boolean ParseCfg(IKernel kernel,ActivityMgr.ActivityData cfg) {
        JsonObject json = JsonUtil.decodeToObj(cfg.param,JsonObject.class);
        Map<Integer, List<RewardData>> mapPlateInfo = new HashMap<>();
        for (int i = 0; i < 14; i++) {
            JsonArray array = json.get("vip" + (i + 1)).getAsJsonArray();
            List<RewardData> list = new ArrayList<>();
            for (int j = 0; j < array.size(); j++) {
                RewardData data = new RewardData();
                JsonObject item = array.get(j).getAsJsonObject();
                data.id = j + 1;
                data.itemId = item.get("type").getAsString();
                data.count = item.get("count").getAsInt();
                data.weight = item.get("weight").getAsInt();
                list.add(data);
            }
            mapPlateInfo.put(i + 1, list);

        }
        costNum = json.get("costNum").getAsInt();
        cfg.data = mapPlateInfo;
        return true;
    }

    @SuppressWarnings("unchecked")
    void PlayLogic(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg) throws InvalidProtocolBufferException {
        Map<String,Object> result = new HashMap<>();
        if (kernel.haveHeartBeat(player,"HB_CheckLdkRewardStatus")){
            result.put("code",1);
            result.put("msg","奖励未领取");
            UtilFunc.respRpcStringToClient(kernel, player, reqid, JsonUtil.encodeToStr(result));
            return;
        }
        Map<Integer, List<RewardData>> mapPlateInfo = (Map<Integer, List<RewardData>>) GetCfg(player).data;
        int vipLevel = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
        if (vipLevel < 1) {
            result.put("code",2);
            result.put("msg","vip等级过低");
            UtilFunc.respRpcStringToClient(kernel, player, reqid, JsonUtil.encodeToStr(result));
            return;
        }
        List<RewardData> rewardDataList = mapPlateInfo.get(vipLevel);
        if (rewardDataList.size() == 0) {
            result.put("code",3);
            result.put("msg","配置异常");
            UtilFunc.respRpcStringToClient(kernel, player, reqid, JsonUtil.encodeToStr(result));
            return;
        }
        CustomMsg.Int32 int32 = CustomMsg.Int32.parseFrom(msg);
        int count = int32.getValue();
        if (count <= 0){
            result.put("code",4);
            result.put("msg","参数异常");
            UtilFunc.respRpcStringToClient(kernel, player, reqid, JsonUtil.encodeToStr(result));
            return;
        }
        int hadCount = itemModule.GetItemCount(kernel,player,costItemId);
        if (hadCount < count){
            result.put("code",5);
            result.put("msg","龙鳞不足");
            UtilFunc.respRpcStringToClient(kernel, player, reqid, JsonUtil.encodeToStr(result));
            return;
        }
        itemModule.SubItem(kernel,player,costItemId,(int)count,UtilFunc.System.TPR_COST.ordinal(),"tpr cost");
        List<String> items = new ArrayList<>();
        int plateId = 0;
        for (int i = 0 ; i < count ; i++) {
            List<RateData> rateDataList = rewardDataList.stream().map(data -> new RateData(data, data.weight)).collect(Collectors.toList());
            List<RateData> list = MathUtils.randomMore(rateDataList, 1);
            RewardData rewardData = (RewardData) list.get(0).getValue();
            items.add(rewardData.itemId + "*" + rewardData.count);
            if (i == 0){
                plateId = rewardData.id;
            }
        }
        player.setProperty(PLAYER_PROPERTY_LDK_REWARD_IDS,JsonUtil.encodeToStr(items));
        kernel.addHeartBeat("HB_CheckLdkRewardStatus", player, 8000, -1);
        result.put("code",0);
        result.put("items",items);
        result.put("plateId",plateId);
        logger.info(JsonUtil.encodeToStr(result));
        UtilFunc.respRpcStringToClient(kernel, player, reqid,JsonUtil.encodeToStr(result));
    }


    @Override
    protected void OnCheckVersion(IKernel kernel, IGameObject player, ActivityMgr.ActivityData cfg) {

    }

    @Override
    void OnStartShow(IKernel kernel, ActivityMgr.ActivityData cfg) {

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

    @Override
    void OnClearData(IKernel kernel, IGameObject player, ActivityMgr.ActivityData cfg) {

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
}
