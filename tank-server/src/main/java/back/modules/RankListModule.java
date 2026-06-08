package back.modules;

import back.modules.data.Write;
import back.modules.data.gameset.RankListBO;
import common.ServerMsgDef;
import framework.ByteUtils;
import framework.ServerSet;
import framework.back.BacKernel;
import framework.back.IBackModule;
import framework.back.net.IDataCallBack;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RankListModule implements IBackModule {
    @Override
    public boolean onInit(BacKernel kernel) {
        return true;
    }

    public void updateRankListConfig(BacKernel kernel, RankListBO rankListBO, IDataCallBack cb) {
        try {
            kernel.sendServerMsg(ServerSet.SERVER_LOGIC_NAME_GAME, ServerMsgDef.B2G_UPDATE_RANK_LIST_CONFIG.ordinal(), ByteUtils.objectToByte(rankListBO));
        } catch (Exception e) {
            e.printStackTrace();
            cb.push(new Write("更新配置失败"));
        }
        cb.push(new Write());
    }

    public void deleteRankListConfig(BacKernel kernel, RankListBO rankListBO, IDataCallBack cb) {
        try {
            kernel.sendServerMsg(ServerSet.SERVER_LOGIC_NAME_GAME, ServerMsgDef.B2G_UPDATE_RANK_LIST_CONFIG.ordinal(), ByteUtils.objectToByte(rankListBO));
        } catch (Exception e) {
            e.printStackTrace();
            cb.push(new Write("更新配置失败"));
        }
        cb.push(new Write());
    }


    @Override
    public void onDestroy() {

    }
}
