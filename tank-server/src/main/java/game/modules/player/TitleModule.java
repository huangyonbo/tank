package game.modules.player;

import com.google.protobuf.InvalidProtocolBufferException;

import framework.game.*;
import game.custommsg.C2SMsgDef;
import game.custommsg.CustomMsg;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TitleModule implements ILogicModule {
	enum TitleColType {
		COL_TITLE_ID, COL_GET_TIME, // 获取时间
		COL_MAX
	}

	static Logger logger = LoggerFactory.getLogger(TitleModule.class);

	public static String EMPTY_TITLE = "null";

	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
		kernel.regEvent(KernelEvent.KEVENT_ON_LINE, "Player", this, "OnPlayerOnline");

		kernel.regClientMessage(C2SMsgDef.C2S_USE_TITLE.ordinal(), this, "OnUseTitle");

		kernel.declareHeartBeat("HB_CheckTitle", this, "OnCheckTitle");
		return true;
	}

	@Override
	public void onDestroy() {

	}

	public void OnPlayerClassCreate(IKernel kernel, String script) {
		kernel.declareProperty(script, PLAYER_PROPERTY_TITLEID, ValueType.STRING, true, true, true);

		// 获得的称号
		IRecord titleRec = kernel.declareRecord(script, "TitleRec", TitleColType.COL_MAX.ordinal(), 100, false, true,
				true);
		titleRec.setColType(TitleColType.COL_TITLE_ID.ordinal(), ValueType.STRING);
		titleRec.setColType(TitleColType.COL_GET_TIME.ordinal(), ValueType.LONG);
	}

	public void OnPlayerOnline(IKernel kernel, IGameObject player) {
		OnCheckTitle(kernel, player);
		kernel.addHeartBeat("HB_CheckTitle", player, 60000, -1);
	}

	void OnCheckTitle(IKernel kernel, IGameObject player) {
		String currentTitleId = player.getString(PLAYER_PROPERTY_TITLEID);
		IRecord titleRec = player.getRecord("TitleRec");
		long currentTime = kernel.getServerTime();
		for (int i = 0; i < titleRec.getRows(); ++i) {
			String titleId = titleRec.getString(i, TitleColType.COL_TITLE_ID.ordinal());
			boolean isExpire = isExpire(titleId, currentTime, titleRec, i);
			if (isExpire) {
				// 过期了;
				titleRec.removeRow(i);
				i--;
				// 如果当前使用
				if (StringUtils.equals(currentTitleId, titleId)) {
					player.setProperty(PLAYER_PROPERTY_TITLEID, "");// 置为空称号
				}
			}
		}
	}

	public void AddNewTitle(IKernel kernel, IGameObject player, String titleId, int lifeTime) {
		IRecord titleRec = player.getRecord("TitleRec");
		int pos = titleRec.findRow(0, TitleColType.COL_TITLE_ID.ordinal(), titleId);
		long currentTime = kernel.getServerTime();
		if (pos == -1) {
			if (lifeTime == -1) {
				titleRec.addRow(titleId, -1l);
			} else {
				titleRec.addRow(titleId, currentTime + lifeTime);
			}
		} else {
			long endLine = titleRec.getLong(pos, TitleColType.COL_GET_TIME.ordinal());
			if (endLine != -1) {
				if (lifeTime == -1) {
					titleRec.setValue(pos, TitleColType.COL_GET_TIME.ordinal(), -1l);
				} else {
					titleRec.setValue(pos, TitleColType.COL_GET_TIME.ordinal(), endLine + lifeTime);
				}

			}
		}

		// 机器人自动佩戴获取的称号
		if (player.getBool("IsRobot")) {
			player.setProperty(PLAYER_PROPERTY_TITLEID, titleId);
		}
	}

	public void OnUseTitle(IKernel kernel, IGameObject player, int msgid, byte[] msg)
			throws InvalidProtocolBufferException {
		CustomMsg.UseTitle useTitle = CustomMsg.UseTitle.parseFrom(msg);
		String titleId = useTitle.getTitleId();

		// check contain title
		IRecord titleRec = player.getRecord("TitleRec");
		int pos = titleRec.findRow(0, TitleColType.COL_TITLE_ID.ordinal(), titleId);
		if (-1 == pos) {
			return;
		}

		// check expire
		long currentTime = kernel.getServerTime();
		boolean isExpire = isExpire(titleId, currentTime, titleRec, pos);
		if (false == isExpire) {
			player.setProperty(PLAYER_PROPERTY_TITLEID, titleId);
		}
	}

	private boolean isExpire(String titleId, long currentTime, IRecord titleRec, int pos) {
		long endLine = titleRec.getLong(pos, TitleColType.COL_GET_TIME.ordinal());
		if (endLine == -1l) {
			return false;
		}
		return endLine < currentTime;
	}
}
