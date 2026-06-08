package game.modules.items;

import com.google.protobuf.InvalidProtocolBufferException;
import framework.game.*;
import game.custommsg.C2SMsgDef;
import game.custommsg.CommandDef;
import game.custommsg.CustomMsg;
import game.modules.utils.UtilFunc;
import game.util.TimeUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 * 玩家头像模块
 */
@Slf4j
public class HeadItem implements ILogicModule {

    enum HeadInfo {
        NONE,
        DEFAULT1,// 认类型1
        DEFAULT2,// 认类型2
        DEFAULT3,// 认类型2
        DEFAULT4,// 认类型2
        DEFAULT5,// 认类型2
        DEFAULT6,// 认类型2
        DEFAULT7,// 认类型2
        DEFAULT8,// 认类型2
        DEFAULT9,// 认类型2
        DEFAULT10,// 认类型2
        DEFAULT11,// 认类型2
        DEFAULT12,// 认类型2
        DEFAULT13,// 认类型2
        DEFAULT14,// 认类型2
        DEFAULT15,// 认类型2
        DEFAULT16,// 认类型2
        DEFAULT17,// 认类型2
        DEFAULT18,// 认类型2
    }

    enum HeadInfoCol {
        ID, END_DATE, COL_MAX
    }

    private static String PLAYER_RECODE_HEAD = "RecHadHead";
    private static String PLAYER_HB_HEAD_CHECK = "HB_CheckHead";

    public HeadItem(IKernel kernel) {
        kernel.addClass("HeadItem", "Item"); //头像
    }

    @Override
    public boolean onInit(IKernel kernel) {
        kernel.regEvent(KernelEvent.KEVENT_ON_LOAD, "Player", this, "OnPlayerLoad");
        kernel.regEvent(KernelEvent.KEVENT_ON_LOAD, "Robot", this, "OnPlayerLoad");
        kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
        kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Robot", this, "OnPlayerClassCreate");
        kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "HeadItem", this, "OnItemClassCreate");
        //从背包使用
        kernel.regCommand(CommandDef.CMD_USE_ITEM.ordinal(), "HeadItem", this, "OnUseItemInBag");
        //从个人信息里选择使用
        kernel.regClientMessage(C2SMsgDef.C2S_SET_HEAD.ordinal(), this, "OnSelectHead");
        kernel.preLoadConfig("res/Items/HeadItem.xml");
        kernel.declareHeartBeat(PLAYER_HB_HEAD_CHECK, this, "OnCheckHead");
        return true;
    }

    public void OnPlayerLoad(IKernel kernel, IGameObject player) {
        OnCheckHead(kernel, player);
        kernel.addHeartBeat(PLAYER_HB_HEAD_CHECK, player, 60000, -1);
    }

    public void OnPlayerClassCreate(IKernel kernel, String script) {
        IRecord headRec = kernel.declareRecord(script, PLAYER_RECODE_HEAD, HeadInfoCol.COL_MAX.ordinal(), 100, false, true, true);
        headRec.setColType(HeadInfoCol.ID.ordinal(), ValueType.INT);//头像id
        headRec.setColType(HeadInfoCol.END_DATE.ordinal(), ValueType.LONG);//到期时间戳
    }

    public void OnItemClassCreate(IKernel kernel, String script) {
        kernel.declareProperty(script, PLAYER_PROPERTY_TIMELIMIT, ValueType.INT, false, false, false);
        kernel.declareProperty(script, "BgID", ValueType.INT, false, false, false);
    }

    public void OnUseItemInBag(IKernel kernel, IGameObject item, Object... objects) {
        IGameObject player = (IGameObject) objects[0];
        int skinId = item.getInt("BgID");
        IRecord rec = player.getRecord(PLAYER_RECODE_HEAD);
        int row = rec.findRow(0, HeadInfoCol.ID.ordinal(), skinId);
        long limitTime = item.getInt(PLAYER_PROPERTY_TIMELIMIT);//头像框失效时长(小时)
        long nowTime = kernel.getServerTime();
        //判断玩家是否有该头像框
        if (row == -1) {
            //获得新头像
            long expireTime = limitTime == -1L ? -1L : nowTime + limitTime * 1 * 60 * 60 * 1000;
            rec.addRow(skinId, limitTime == -1L ? -1L : expireTime);
            log.info("玩家{}获得头像框{},失效时长{}小时,失效时间{}", player.getInt(PLAYER_PROPERTY_UID), skinId, limitTime, TimeUtils.formatDate(new Date(expireTime),null));
        } else {
            // 正在使用中
            long timeEnd = rec.getLong(row, HeadInfoCol.END_DATE.ordinal());//当前头像失效时间
            if (-1L == timeEnd) {
                log.info("玩家{}使用头像框{},但是该头像已经获得且无失效时间,不再执行获得逻辑...{}", player.getInt(PLAYER_PROPERTY_UID), skinId, limitTime);
                return;
            }
            long expireTime = limitTime == -1L ? -1L : timeEnd + limitTime * 1 * 60 * 60 * 1000;
            rec.setValue(row, HeadInfoCol.END_DATE.ordinal(), expireTime);
            log.info("玩家{}获得头像框{},失效时长{}小时,原失效时间{},获得后失效时间{}", player.getInt(PLAYER_PROPERTY_UID), skinId, limitTime, TimeUtils.formatDate(new Date(timeEnd),null),TimeUtils.formatDate(new Date(expireTime),null));
        }
        player.setProperty(PLAYER_PROPERTY_HEADID, skinId);
        log.info("玩家{}使用头像后表单数据{}",player.getInt(PLAYER_PROPERTY_UID),player.getRecordMeta(rec));
    }

    public void OnSelectHead(IKernel kernel, IGameObject player, int msgId, byte[] msg)
            throws InvalidProtocolBufferException {
        CustomMsg.Int32 selectMsg = CustomMsg.Int32.parseFrom(msg);
        int skinId = selectMsg.getValue();
        if (skinId >= HeadInfo.DEFAULT1.ordinal() && skinId <= HeadInfo.values().length) {
            player.setProperty(PLAYER_PROPERTY_HEADID, skinId);
            UtilFunc.showTip(kernel, player, "TXT_CHANGE_HEAD_OK");
            return;
        }
        IRecord rec = player.getRecord(PLAYER_RECODE_HEAD);
        int row_to_use = rec.findRow(0, HeadInfoCol.ID.ordinal(), skinId);
        if (row_to_use != -1) {
            long endDate = rec.getLong(row_to_use, HeadInfoCol.END_DATE.ordinal());
            if (endDate != -1 && endDate < kernel.getServerTime()) {
                UtilFunc.showTip(kernel, player, "TXT_NOT_HAD_HEAD");
                return;
            }
            player.setProperty(PLAYER_PROPERTY_HEADID, skinId);
            UtilFunc.showTip(kernel, player, "TXT_CHANGE_HEAD_OK");
        } else {
            UtilFunc.showTip(kernel, player, "TXT_NOT_HAD_HEAD");
        }
    }

    public void OnCheckHead(IKernel kernel, IGameObject player) {
        int headId = player.getInt(PLAYER_PROPERTY_HEADID);//头像id
        long serverTime = kernel.getServerTime();//当前时间
        IRecord rec = player.getRecord(PLAYER_RECODE_HEAD);//头像表单
        int rows = rec.getRows();
        for (int i = 0; i < rows; i++) {
            long timeLimit = rec.getLong(i, HeadInfoCol.END_DATE.ordinal());//头像失效时间
            if (timeLimit != -1 && serverTime > timeLimit) {
                if (rec.getInt(i, HeadInfoCol.ID.ordinal()) == headId) {
                    player.setProperty(PLAYER_PROPERTY_HEADID, HeadInfo.DEFAULT1.ordinal());//改为默认头像
                }
                rec.removeRow(i);
                i--;
                rows--;
            }
        }
    }
}
