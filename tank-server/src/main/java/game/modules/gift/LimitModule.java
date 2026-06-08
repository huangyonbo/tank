package game.modules.gift;

import framework.game.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;

import common.ServerMsg;
import common.ServerMsgDef;
import game.custommsg.C2SMsgDef;
import game.custommsg.CommandDef;
import game.custommsg.CustomMsg;
import game.modules.store.StoreModule;

public class LimitModule implements ILogicModule {

	static Logger logger = LoggerFactory.getLogger(LimitModule.class);
	private String m_version;
	private boolean m_isOpen;
	private ICfgReader m_limitConfig;

	private StoreModule m_storeModule = null;

	enum LimitColType {
		COL_LIMIT_ID, COL_BUY_COUNT, // 当前购买的数量
		COL_MAX
	}

	class LimitInfo {
		public String version;
		public String limitId;
	}

	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
		kernel.regEvent(KernelEvent.KEVENT_ON_LINE, "Player", this, "OnPlayerOnline");

		kernel.regCommand(CommandDef.CMD_PAY_BACK.ordinal(), "Player", this, "OnPayBack");
		kernel.regClientMessage(C2SMsgDef.C2S_BUY_LIMIT_GIFT.ordinal(), this, "OnBuyLimitGift");

		kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");
		RefreshCfg(kernel, "res/Gift/LimitGift.xml");
		RefreshCfg(kernel, "res/Gift/GiftVersion.xml");

		m_isOpen = false;

		m_storeModule = (StoreModule) kernel.getModule("StoreModule");

		return true;
	}

	@Override
	public void onDestroy() {

	}

	void RefreshCfg(IKernel kernel, String path) {
		if (path.equals("res/Gift/LimitGift.xml")) {
			m_limitConfig = kernel.loadXmlConfig(path);
		} else if (path.equals("res/Gift/GiftVersion.xml")) {
			ICfgReader m_giftVersion = kernel.loadXmlConfig(path);
			if (m_giftVersion == null) {
				return;
			}
			m_version = m_giftVersion.getString("limitGift", PLAYER_PROPERTY_VERSION);
		}
	}

	public void OnPlayerClassCreate(IKernel kernel, String script) {
		kernel.declareProperty(script, PLAYER_PROPERTY_LIMITVERSION, ValueType.STRING, false, true, true);
		// 限购礼包
		IRecord limitRec = kernel.declareRecord(script, "LimitRec", LimitColType.COL_MAX.ordinal(), 50, false, true,
				true);
		limitRec.setColType(LimitColType.COL_LIMIT_ID.ordinal(), ValueType.STRING);
		limitRec.setColType(LimitColType.COL_BUY_COUNT.ordinal(), ValueType.INT);
	}

	public void OnPlayerOnline(IKernel kernel, IGameObject player) {
		IRecord limitRec = player.getRecord("LimitRec");
		if (limitRec.getRows() < m_limitConfig.getItemCount()) {
			for (int i = limitRec.getRows(); i < m_limitConfig.getItemCount(); i++) {
				String limitId = m_limitConfig.getString(i, "Id");
				limitRec.addRow(limitId, 0);
			}
		}

		CheckVersion(kernel, player);
	}

	// 活动开启
	public void StartGift(IKernel kernel, String strId) {
		m_isOpen = true;
		// 向pub服发送清理数据
		ServerMsg.PubStartLimit.Builder build = ServerMsg.PubStartLimit.newBuilder();
		build.setVersion(this.m_version);
		kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.PUBMSG_START_LIMIT.ordinal(), build.build().toByteArray());
	}

	// 活动结束
	public void EndGift(IKernel kernel, String strId) {
		m_isOpen = false;
	}

	void CheckVersion(IKernel kernel, IGameObject player) {
		// check version
		String strVersion = player.getString(PLAYER_PROPERTY_LIMITVERSION);
		IRecord limitRec = player.getRecord("LimitRec");
		if (!StringUtils.equals(strVersion, m_version)) {
			// version不同 清空Record
			for (int i = 0; i < limitRec.getRows(); i++) {
				limitRec.setValue(i, LimitColType.COL_BUY_COUNT.ordinal(), 0);
			}
			player.setProperty(PLAYER_PROPERTY_LIMITVERSION, m_version);
		}
	}

	public void OnBuyLimitGift(IKernel kernel, IGameObject player, int msgid, byte[] msg)
			throws InvalidProtocolBufferException {
		// check open or not
		if (m_isOpen == false) {
			return;
		}

		CheckVersion(kernel, player);

		IRecord limitRec = player.getRecord("LimitRec");
		// check
		CustomMsg.BuyLimitGift buyLimitGiftMsg = CustomMsg.BuyLimitGift.parseFrom(msg);
		int pos = buyLimitGiftMsg.getPos();
		int buyCount = limitRec.getInt(pos, LimitColType.COL_BUY_COUNT.ordinal());
		if (buyCount >= 1) {
			return;
		}
		String limitId = limitRec.getString(pos, LimitColType.COL_LIMIT_ID.ordinal());
		// check store
		// IRecord record = player.GetRecord("BuyGoodsRec");
		// int row = record.FindRow(0,0,limitId);
		// if(row!=-1)
		// {
		// return;
		// }
		// buy
		CustomMsg.PreOrder.Builder builder = CustomMsg.PreOrder.newBuilder();
		LimitInfo limitInfo = new LimitInfo();
		limitInfo.limitId = limitId;
		limitInfo.version = m_version;

		builder.setGoodsId(limitId);
		builder.setDescription(buyLimitGiftMsg.getDescription());
		builder.setExtra(new Gson().toJson(limitInfo));
		builder.setIndex(buyLimitGiftMsg.getIndex());

		m_storeModule.InnerBuyStorePay(kernel, player, builder.build(), false);
	}

	public void OnPayBack(IKernel kernel, IGameObject player, Object... objects) {
		String goodsId = (String) objects[0];
		if (m_limitConfig.containsKey(goodsId)) {
			String info = (String) objects[2];
			try {
				LimitInfo limitInfo = new Gson().fromJson(info, LimitInfo.class);
				this.buy(kernel, player, limitInfo.version, limitInfo.limitId);
			} catch (Exception exp) {
				return;
			}
		}
	}

	public void buy(IKernel kernel, IGameObject player, String version, String limitId) {
		String strVersion = player.getString(PLAYER_PROPERTY_LIMITVERSION);
		if (StringUtils.equals(strVersion, version)) {
			IRecord limitRec = player.getRecord("LimitRec");
			int row = limitRec.findRow(0, LimitColType.COL_LIMIT_ID.ordinal(), limitId);
			if (row != -1) {
				int buyCount = limitRec.getInt(row, LimitColType.COL_BUY_COUNT.ordinal());
				limitRec.setValue(row, LimitColType.COL_BUY_COUNT.ordinal(), buyCount + 1);

				ServerMsg.PubAddLimit.Builder build = ServerMsg.PubAddLimit.newBuilder();
				build.setId(limitId);
				kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.PUBMSG_ADD_LIMIT.ordinal(), build.build().toByteArray());
			}
		}
	}

}
