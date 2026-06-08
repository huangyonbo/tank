package game.modules.items;

import com.google.protobuf.InvalidProtocolBufferException;

import framework.game.*;
import game.custommsg.C2SMsgDef;
import game.custommsg.CommandDef;
import game.custommsg.CustomMsg;

// 信息背景道具
public class InfoBgItem implements ILogicModule {

	enum InfoBg {
		DEFAULT, // 默认类型
		BG_1, BG_2,
		MAX
	}

	enum InfoBgCol {
		ID, END_DATE, COL_MAX
	}

	private static String PLAYER_RECODE_INFO_BG = "RecInfoBg";

	private static String PLAYER_HB_INFO_BG_CHECK = "HB_CheckInfoBg";

	public InfoBgItem(IKernel kernel) {
		kernel.addClass("InfoBgItem", "Item"); //信息背景
	}

	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_LOAD, "Player", this, "OnPlayerLoad");
		kernel.regEvent(KernelEvent.KEVENT_ON_LOAD, "Robot", this, "OnPlayerLoad");
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Robot", this, "OnPlayerClassCreate");
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "InfoBgItem", this, "OnItemClassCreate");
		// 从背包使用
		kernel.regCommand(CommandDef.CMD_USE_ITEM.ordinal(), "InfoBgItem", this, "OnUseItemInBag");
		// 从个人信息里选择使用
		kernel.regClientMessage(C2SMsgDef.C2S_SELECT_INFO_BG.ordinal(), this, "OnSelectInfoBg");
		kernel.preLoadConfig("res/Items/InfoBgItem.xml");
		kernel.declareHeartBeat(PLAYER_HB_INFO_BG_CHECK, this, "OnCheckInfoBg");
		return true;
	}


	public void OnPlayerLoad(IKernel kernel, IGameObject player) {
		IRecord rec = player.getRecord(PLAYER_RECODE_INFO_BG);
		int row = rec.findRow(0, InfoBg.DEFAULT.ordinal(), 0);
		if (row == -1) {
			rec.addRow(InfoBg.DEFAULT.ordinal(), -1L);
		}
		OnCheckInfoBg(kernel, player);
		kernel.addHeartBeat(PLAYER_HB_INFO_BG_CHECK, player, 60000, -1);
	}

	public void OnPlayerClassCreate(IKernel kernel, String script) {
		kernel.declareProperty(script, PLAYER_PROPERTY_USE_INFO, ValueType.INT, true, true, true);
		IRecord userBatterySkinList = kernel.declareRecord(script, PLAYER_RECODE_INFO_BG, InfoBgCol.COL_MAX.ordinal(), 100, false, true, true);
		userBatterySkinList.setColType(InfoBgCol.ID.ordinal(), ValueType.INT);
		userBatterySkinList.setColType(InfoBgCol.END_DATE.ordinal(), ValueType.LONG);
	}

	public void OnItemClassCreate(IKernel kernel, String script) {
		kernel.declareProperty(script, PLAYER_PROPERTY_TIMELIMIT, ValueType.INT, false, false, false);
		kernel.declareProperty(script, "BgID", ValueType.INT, false, false, false);
	}

	public void OnUseItemInBag(IKernel kernel, IGameObject item, Object... objects) {
		IGameObject player = (IGameObject) objects[0];
		int skinId = item.getInt("BgID");

		IRecord rec = player.getRecord(PLAYER_RECODE_INFO_BG);
		int row = rec.findRow(0, InfoBgCol.ID.ordinal(), skinId);
		if (row == -1) {
			if (item.getInt(PLAYER_PROPERTY_TIMELIMIT) != -1) {
				// 增加一行并设置好失效日期
				rec.addRow(skinId, kernel.getServerTime() + item.getInt(PLAYER_PROPERTY_TIMELIMIT) * 3600 * 1000);
			} else {
				// 增加一行
				rec.addRow(skinId, -1L);
			}
		} else {// 已使用过并在有效期内的
			long timeEnd = rec.getLong(row, InfoBgCol.END_DATE.ordinal());
			if (timeEnd != -1l) {
				if (item.getInt(PLAYER_PROPERTY_TIMELIMIT) != -1) {
					// 有效时间累加
					rec.setValue(row, InfoBgCol.END_DATE.ordinal(), timeEnd + item.getInt(PLAYER_PROPERTY_TIMELIMIT) * 3600 * 1000);
				} else {
					rec.setValue(row, InfoBgCol.END_DATE.ordinal(), -1L);
				}
			}
		}
		// 秘境之王框有效期内不可被替换
		if (player.getInt(PLAYER_PROPERTY_USE_INFO) == 4) {
			return;
		}
		player.setProperty(PLAYER_PROPERTY_USE_INFO, skinId);
	}

	public void OnSelectInfoBg(IKernel kernel, IGameObject player, int msgid, byte[] msg)
			throws InvalidProtocolBufferException {

		CustomMsg.Int32 selectMsg = CustomMsg.Int32.parseFrom(msg);
		int skinId = selectMsg.getValue();

		IRecord rec = player.getRecord(PLAYER_RECODE_INFO_BG);
		int row_to_use = rec.findRow(0, InfoBgCol.ID.ordinal(), skinId);
		if (row_to_use != -1) {
			long endDate = rec.getLong(row_to_use, InfoBgCol.END_DATE.ordinal());
			if (endDate != -1 && endDate < kernel.getServerTime()) {
				return;
			}

			player.setProperty(PLAYER_PROPERTY_USE_INFO, skinId);
		}
	}

	public void OnCheckInfoBg(IKernel kernel, IGameObject player) {
		int skinid = player.getInt(PLAYER_PROPERTY_USE_INFO);
		long serverTime = kernel.getServerTime();
		IRecord rec = player.getRecord(PLAYER_RECODE_INFO_BG);
		int rows = rec.getRows();
		for (int i = 0; i < rows; i++) {
			long timeLimit = rec.getLong(i, InfoBgCol.END_DATE.ordinal());
			if (timeLimit != -1 && serverTime > timeLimit) {
				if (rec.getInt(i, InfoBgCol.ID.ordinal()) == skinid) {
					player.setProperty(PLAYER_PROPERTY_USE_INFO, 0);
				}

				rec.removeRow(i);
				i--;
				rows--;
			}
		}
	}

	public void setBgExpired(IGameObject player, int skinId) {
		IRecord rec = player.getRecord(PLAYER_RECODE_INFO_BG);
		int rows = rec.getRows();
		for (int i = 0; i < rows; i++) {
			if (rec.getInt(i, InfoBgCol.ID.ordinal()) == skinId) {
				rec.removeRow(i);
				player.setProperty(PLAYER_PROPERTY_USE_INFO, 0);
			}
		}
	}

}
