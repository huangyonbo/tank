package game.modules.items;

import back.modules.dataenum.RoomType;
import framework.game.*;
import game.modules.utils.UtilFunc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ItemLogModule implements ILogicModule {

    private static final Logger logger = LoggerFactory.getLogger(ItemLogModule.class);

    enum WayType{
        USE,
        GET
    }

    private static final Map<String, Boolean> configMap = new HashMap<>();

    @Override
    public boolean onInit(IKernel kernel) {
        kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG,"World",this,"RefreshCfg");
        RefreshCfg(kernel,"res/Items/ItemLog.xml");
        return true;
    }

    void RefreshCfg(IKernel kernel, String path) {
        if (!"res/Items/ItemLog.xml".equals(path)) {
            return;
        }
        ICfgReader cfgReader = kernel.loadXmlConfig(path);
        int count = cfgReader.getItemCount();
        for (int i = 0; i < count; i++) {
            String id = cfgReader.getString(i, "Id");
            Boolean flag = cfgReader.getBool(i,"Flag");
            configMap.put(id, flag);
        }
    }

//    public static void AddItemLog(IKernel kernel, IGameObject player, String itemId, int count, int ordinal) {
//        configMap.forEach((k,v)->{
//            if (itemId.equals(k) && v) {
//                ItemLogEnum value = ItemLogEnum.getValue(ordinal);
//                if (value == null) {
//                    logger.warn("ItemLogEnum is not define");
//                    return;
//                }
//                int roomType = UtilFunc.getRoomType(kernel, player);
//                String roomName = RoomType.getRoomName(roomType);
//                if (value.getFlag() == WayType.USE.ordinal()) {
//                    kernel.AddItemLog(player, itemId, count, roomName, "-", value.getName());
//                }
//                else if (value.getFlag() == WayType.GET.ordinal()) {
//                    kernel.AddItemLog(player, itemId, count, roomName, value.getName(), "-");
//                }
//            }
//        });
//    }

    public static void AddItemLog(IKernel kernel, IGameObject player, String itemId, int count, String output, String useWay) {
        configMap.forEach((k,v)->{
            if (itemId.equals(k) && v) {
                int roomType = UtilFunc.getRoomType(kernel, player);
                kernel.addItemLog(player, itemId, count, RoomType.getRoomName(roomType), output, useWay);
            }
        });
    }


    @Override
    public void onDestroy() {

    }
}
