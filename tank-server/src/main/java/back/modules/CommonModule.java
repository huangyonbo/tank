package back.modules;

import common.ServerMsgDef;
import framework.JsonUtil;
import framework.ServerSet;
import framework.back.BacKernel;
import framework.back.IBackModule;
import framework.back.net.IDataCallBack;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;

/**
 * 通用查询模块
 */
@Slf4j
public class CommonModule implements IBackModule {

    @Override
    public boolean onInit(BacKernel kernel) {
        return true;
    }

    /**
     * 查询渠道奖券掉落概率
     * @param channels 渠道id
     */
    public void getColorTicketDropRatio(BacKernel kernel, HashSet<Integer> channels, IDataCallBack callBack) {
        kernel.requestServer(ServerSet.SERVER_LOGIC_NAME_GAME, ServerMsgDef.B2G_GET_CHANNEL_COLOR_TICKET_DROP_RATIO.ordinal(),
                JsonUtil.encodeToStr(channels).getBytes(), callBack::push);
    }

    /**
     * 查询所有vip对应的道具积分基础参数
     */
    public void getAllItemScoreMax(BacKernel kernel, IDataCallBack callBack) {

        kernel.requestServer(ServerSet.SERVER_LOGIC_NAME_GAME,
                ServerMsgDef.B2G_GET_ALL_ITEM_SCORE_MAX.ordinal(), null, callBack::push);
    }

    /**
     * 查询金币炮等级对应炮值
     */
    public void getBulletLevelValue(BacKernel kernel, IDataCallBack callBack) {
        kernel.requestServer(ServerSet.SERVER_LOGIC_NAME_GAME, ServerMsgDef.B2G_GET_BULLET_VALUE_PARAM.ordinal(), null, callBack::push);
    }

    /**
     * 查询金币炮等级对应炮值
     */
    public void getNBulletLevelValue(BacKernel kernel, IDataCallBack callBack) {
        kernel.requestServer(ServerSet.SERVER_LOGIC_NAME_GAME, ServerMsgDef.B2G_GET_N_BULLET_VALUE_PARAM.ordinal(), null, callBack::push);
    }

    @Override
    public void onDestroy() {

    }
}
