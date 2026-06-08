package back.modules;

import back.modules.data.Write;
import common.ServerMsgDef;
import framework.back.BacKernel;
import framework.back.IBackModule;
import framework.back.net.IDataCallBack;

public class PopupManageModule implements IBackModule {


    @Override
    public boolean onInit(BacKernel kernel) {
        return true;
    }

    @Override
    public void onDestroy() {

    }

    public void notifyUpdateConfig(BacKernel kernel, IDataCallBack cb) {
        kernel.broadToServer("game", ServerMsgDef.B2G_NOTIFY_UPDATE_POPUP_CONFIG.ordinal(), new byte[0]);
        cb.push(new Write());
    }

}
