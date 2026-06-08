package back.modules;

import back.modules.data.Write;
import back.modules.data.rewardset.VersionRewardParam;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.back.BacKernel;
import framework.back.IBackModule;
import framework.back.net.IDataCallBack;

public class VersionRewardModule implements IBackModule {

    @Override
    public boolean onInit(BacKernel kernel) {
        return true;
    }

    @Override
    public void onDestroy() {

    }

    public void openVersionReward(BacKernel kernel, VersionRewardParam param, IDataCallBack cb) {
        Integer[] ids = param.getId();

        ServerMsg.IntArray.Builder build = ServerMsg.IntArray.newBuilder();
        for (int id : ids) {
            build.addId(id);
        }

        kernel.broadToServer("game", ServerMsgDef.B2G_REFRESH_VERREWARD.ordinal(), build.build().toByteArray());

        cb.push(new Write());
    }

    public void closeVersionReward(BacKernel kernel, VersionRewardParam param, IDataCallBack cb) {
        Integer[] ids = param.getId();

        ServerMsg.IntArray.Builder build = ServerMsg.IntArray.newBuilder();
        for (int id : ids) {
            build.addId(id);
        }

        kernel.broadToServer("game", ServerMsgDef.B2G_CLOSE_VERREWARD.ordinal(), build.build().toByteArray());

        cb.push(new Write());
    }
}
