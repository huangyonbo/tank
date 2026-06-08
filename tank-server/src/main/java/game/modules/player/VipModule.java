package game.modules.player;

import back.modules.MailModule;
import framework.game.*;
import game.custommsg.CustomMsg;
import game.custommsg.S2CMsgDef;
import game.modules.fishgame.BulletValModule;
import game.modules.items.ItemModule;
import game.modules.utils.ItemTipType;
import game.modules.utils.UtilFunc;
import game.modules.utils.UtilFunc.BroadCastType;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class VipModule implements ILogicModule {
	enum VipAwardColType {
		COL_IS_GET, COL_MAX
	}

	static Logger logger = LoggerFactory.getLogger(VipModule.class);
	ICfgReader m_vipConfig;
	private ItemModule m_itemModule = null;
	private MailModule m_MailModule = null;
	BulletValModule bulletValModule = null;
	private Map<Integer, int[]> m_mapVipStoneCount = new HashMap<>();

	public static String PROPERTY_TOTAL_RECHARGE_AMOUNT = PLAYER_PROPERTY_TOTALRECHARGEAMOUNT;
	public static String VIP_LEVEL = PLAYER_PROPERTY_VIPLEVEL;


	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
		kernel.regEvent(KernelEvent.KEVENT_ON_SITDOWN, "Player", this, "OnPlayerSitDown");

		//ty 删除充值返利
		//kernel.regCommand(CommandDef.CMD_PAY_BACK.ordinal(), "Player", this, "OnRechargeSuccess");

		kernel.listenPropertyChange(PROPERTY_TOTAL_RECHARGE_AMOUNT, "Player", this, "OnTotalRechargeChange");
		kernel.listenPropertyChange("VipLevel", "Player", this, "OnVipLevelChange");

		// 不再支持手动领取礼包 add by huzw 2019-03-15
		// kernel.RegClientMessage(C2SMsgDef.C2S_GET_AWARD_VIP_PACK.ordinal(),this,"OnGetAwardVipPack");

		kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");
		RefreshCfg(kernel, "res/Config/Vip.xml");
		RefreshCfg(kernel, "res/Config/StoneExchangeCount.xml");

		m_itemModule = (ItemModule) kernel.getModule("ItemModule");
		m_MailModule = (MailModule) kernel.getModule("MailModule");
		bulletValModule = (BulletValModule) kernel.getModule("BulletValModule");
		return true;
	}

	@Override
	public void onDestroy() {

	}

	void RefreshCfg(IKernel kernel, String path) {
		if (path.equals("res/Config/Vip.xml")) {
			m_vipConfig = kernel.loadXmlConfig(path);
			if (m_vipConfig == null) {
				return;
			}
		} else if (path.equals("res/Config/StoneExchangeCount.xml")) {
			ICfgReader iCfgReader = kernel.loadXmlConfig(path);
			if (iCfgReader == null) {
				return;
			}

			int count = iCfgReader.getItemCount();
			for (int i = 0; i < count; ++i) {
				int vipLevel = iCfgReader.getInt(i, "Id");
				String[] countStr = iCfgReader.getString(i, "StoneCount").split(",");
				int[] countInt = new int[countStr.length];
				for (int j = 0; j < countStr.length; j++) {
					countInt[j] = Integer.parseInt(countStr[j]);
				}
				m_mapVipStoneCount.put(vipLevel, countInt);
			}
		}
	}

	public void OnPlayerClassCreate(IKernel kernel, String script) {
		kernel.declareProperty(script, VIP_LEVEL, ValueType.INT, true, true, true);

		// vip 礼包领取
		IRecord vipAwardPackRec = kernel.declareRecord(script, "GetVipAwardPackRec", VipAwardColType.COL_MAX.ordinal(), m_vipConfig.getItemCount(), false, true, true);
		vipAwardPackRec.setColType(VipAwardColType.COL_IS_GET.ordinal(), ValueType.BOOL);
	}

	public int GetVipExp(int level) {
		return m_vipConfig.getInt(level, PLAYER_PROPERTY_TOTALRECHARGEAMOUNT);
	}

	public int GetStoneExchangeCount(int level) {
		// if (!m_mapVipStoneCount.containsKey(level))
		// return null;
		// int[] counts = m_mapVipStoneCount.get(level);
		// return counts;
		return m_vipConfig.getInt(level, "StoneCount");
	}

	public int GetStone2BombLimit(int level) {
		return m_vipConfig.getInt(level - 1, "StoneTransformBombLimit");
	}
	
	// 获取是否可以换桌的权限
	public int GetChangeDeskNeedMinVip() {
		int i = 12;
		while (i > 0) {
			if (!m_vipConfig.getBool(i - 1, "ChangeDesk")){
				return i+1;
			}
			i --;
		}
		return 12;
	}
	
	// 获取vip邮寄道具数量限制
	public int GetMailLimit(IGameObject player) {
		int level = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
		if (level == 0) {
			return 0;
		}
		return m_vipConfig.getInt(level - 1, "MailLimit");
	}
	
	// 获取可存入仓库的限制
	public int GetGuildRepoStoreLimit(int level) {
		if (level == 0) {
			return 0;
		}
		return m_vipConfig.getInt(level - 1, "GuildRepoStoreLimit");
	}

	public int GetCreatePwCount(IGameObject player) {
		int level = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
		if (level == 0) {
			return 0;
		}
		return m_vipConfig.getInt(level - 1, "CreatePwCount");
	}

	public float GetStoneScoreAddition(IGameObject player) {
		int level = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
		if (level == 0) {
			return 0f;
		}
		return m_vipConfig.getFloat(level - 1, "StoneScoreAddition");
	}

	public float GetArenaScoreAddition(IGameObject player) {
		int level = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
		if (level == 0) {
			return 0f;
		}
		return m_vipConfig.getFloat(level - 1, "ArenaScoreAddition");
	}

	public float GetBackGoldAddition(IGameObject player) {
		int level = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
		if (level == 0) {
			return 0f;
		}
		return m_vipConfig.getFloat(level - 1, "BackGold");
	}

	/**
	 * 增加VIP变化的监听，人工修改VIP限制测试时的上限的问题
	 * @param kernel
	 * @param player
	 * @param name
	 * @param oldParam
	 */
	public void OnVipLevelChange(IKernel kernel, IGameObject player, String name, Object oldParam) {
		int vipLevel = player.getInt(PLAYER_PROPERTY_VIPLEVEL);

	}
	public void OnTotalRechargeChange(IKernel kernel, IGameObject player, String name, Object oldParam) {
		//停止VIP的判断
		if (true){
			return;
		}
		int totalRechargeAmount = player.getInt(PROPERTY_TOTAL_RECHARGE_AMOUNT);
		int vipLevel = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
		int newLevel = -1;
		int maxVipLevel = m_vipConfig.getItemCount();
		// not reach max vipLevel
		while (vipLevel < maxVipLevel) {
			// decide vip level up
			int rechargeAmountLevelUp = m_vipConfig.getInt(vipLevel, PLAYER_PROPERTY_TOTALRECHARGEAMOUNT);
			if (totalRechargeAmount >= rechargeAmountLevelUp) {
				vipLevel += 1;
				//Vip 升级
				player.setProperty(PLAYER_PROPERTY_VIPLEVEL, vipLevel);
				newLevel = vipLevel;
				//背景框检测发放
				CheckVipBg(kernel, player, vipLevel);
				// 更新当天赠送次数
				UpdateMailLimit(kernel, player);
				//增加活动的 存储上限
			} else {
				break;
			}
		}
		if (newLevel != -1) {
			CustomMsg.VipLevelUp.Builder vipLevelUp = CustomMsg.VipLevelUp.newBuilder();
			vipLevelUp.setLevel(newLevel);
			kernel.sendMessage(player, S2CMsgDef.S2C_VIP_LEVEL_UP.ordinal(), vipLevelUp.build().toByteArray());
		}
	}
	
	public void UpdateMailLimit(IKernel kernel, IGameObject player){
		int max = GetMailLimit(player);
		if (max == -1){
			player.setProperty(PLAYER_PROPERTY_MAIL_CAN_SEND_NUM, -1);
			return;
		}
		int mUid = player.getInt(PLAYER_PROPERTY_UID);
		IRecord rec = player.getRecord("SendRecord");
		int row = rec.findRow(0, 0, mUid);
		int usedNum = 0;
		if (row != -1) {
			if (UtilFunc.isSameDay(kernel.getServerTime(), rec.getLong(row, 2))) {
				usedNum = rec.getInt(row, 1);
			} else {
				rec.setValue(row, 1, 0);
				rec.setValue(row, 2, kernel.getServerTime());
			}
		}
		player.setProperty(PLAYER_PROPERTY_MAIL_CAN_SEND_NUM, max - usedNum);
	}


	public void CheckVipBg(IKernel kernel, IGameObject player, int level) {
		String strItemPkg = m_vipConfig.getString(level - 1, "BgInfo");
		if (!StringUtil.isNullOrEmpty(strItemPkg)) {
			m_itemModule.AddItem(kernel, player, strItemPkg, 1, UtilFunc.System.VIP.ordinal(), "get vip bg " + level);
		}
	}

	void AutoGetVipGift(IKernel kernel, IGameObject player, int vipLevel) {
		IRecord vipAwardPackRec = player.getRecord("GetVipAwardPackRec");
		if (vipLevel > vipAwardPackRec.getRows()) {
			return;
		}

		boolean isGet = vipAwardPackRec.getBool(vipLevel - 1, VipAwardColType.COL_IS_GET.ordinal());
		if (isGet) {
			return;
		}

		vipAwardPackRec.setValue(vipLevel - 1, VipAwardColType.COL_IS_GET.ordinal(), true);

		String strItemPkg = m_vipConfig.getString(vipLevel - 1, "ItemPkg");
		m_itemModule.AddItem(kernel, player, strItemPkg, 1, UtilFunc.System.VIP.ordinal(), "get reward " + vipLevel);

		UtilFunc.sendItemTips(kernel, player, ItemTipType.TIP_GET_SUCCESS, strItemPkg, 1);
	}

	// 检测老玩家未领取的VIP奖励
	public void CheckUngetGift(IKernel kernel, IGameObject player) {
		IRecord vipAwardPackRec = player.getRecord("GetVipAwardPackRec");
		for (int i = 0; i < vipAwardPackRec.getRows(); ++i) {
			boolean isGet = vipAwardPackRec.getBool(i, VipAwardColType.COL_IS_GET.ordinal());
			if (isGet) {
				continue;
			}
			vipAwardPackRec.setValue(i, VipAwardColType.COL_IS_GET.ordinal(), true);
			String strItemPkg = m_vipConfig.getString(i, "ItemPkg");
		}
	}

	// 不再支持手动领取礼包 add by huzw 2019-03-15
	// public void OnGetAwardVipPack(IKernel kernel, IGameObject player, int
	// msgid, byte[] msg) throws InvalidProtocolBufferException
	// {
	// //check need buy
	// CustomMsg.GetVipPack getVipPack = CustomMsg.GetVipPack.parseFrom(msg);
	// int vipLevel = getVipPack.getVipLevel();
	//
	// IRecord vipAwardPackRec = player.GetRecord("GetVipAwardPackRec");
	// if(vipLevel>vipAwardPackRec.GetRows())
	// {
	// logger.error("over vip level.MaxRow:{}",vipAwardPackRec.GetRows());
	// return;
	// }
	//
	// boolean isGet =
	// vipAwardPackRec.GetBool(vipLevel-1,VipAwardColType.COL_IS_GET.ordinal());
	// if(isGet)
	// {
	// logger.error("already get vipPack.level:{}",vipLevel);
	// return;
	// }
	//
	// //save package
	// String strItemPkg = m_vipConfig.GetString(vipLevel-1,"ItemPkg");
	// m_itemModule.AddItem(kernel,player,strItemPkg,1,
	// UtilFunc.System.VIP.ordinal(), "get reward " + vipLevel);
	//
	// //set status
	// vipAwardPackRec.SetValue(vipLevel-1,VipAwardColType.COL_IS_GET.ordinal(),true);
	//
	// //通知客户端
	// UtilFunc.SendItemTips(kernel, player, ItemTipType.TIP_GET_SUCCESS,
	// strItemPkg, 1);
	// }

	public void OnPlayerSitDown(IKernel kernel, IGameObject player, IGameObject desk) {
		int vipLevel = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
		String name = player.getString(PLAYER_PROPERTY_NAME);
		int broadcast = m_vipConfig.getInt(vipLevel - 1, "Broadcast");
		if (broadcast == 1) {
			UtilFunc.broadCastScrollMsgAllServer(kernel, m_vipConfig.getString(vipLevel - 1, "TipText"), 1,
					BroadCastType.SYSTEM.ordinal()+"", name, String.valueOf(vipLevel));
		}
	}

	public int getMaxLeve() {
		return m_vipConfig.getItemCount();
	}

	public String getBenefitValue(int vipLevel) {
		return m_vipConfig.getString(vipLevel, "BenefitValue");
	}

	public boolean CanSend(IGameObject player) {
		int vipLevel = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
		int broadcast = m_vipConfig.getInt(vipLevel - 1, "SendItem");
		return broadcast == 1;
	}

	public void OnRechargeSuccess(IKernel kernel, IGameObject player, Object... objects) {

		String itemid = (String) objects[0];
		int payMoney = (int) objects[1];
		float addi = GetBackGoldAddition(player);
		if (addi == 0) {
			return;
		}

		String itemName = kernel.getCfgProperty(itemid, PLAYER_PROPERTY_NAME);
		int backGold = (int) (addi * payMoney * 50000);

		// 贵族充值返利
		// 尊敬的贵族玩家，您本次通{0:itemName}消费{1:payMoney}元，您将获得返利{2:backGold}金币，请及时查收附件，祝您游玩愉快！
		List<String> title = new ArrayList<>();
		title.add("TXT_VIP_BACK_GOLD_T");

		List<String> context = new ArrayList<>();
		context.add("TXT_VIP_BACK_GOLD_C");
		context.add(itemName);
		context.add(Integer.toString(payMoney));
		context.add(Integer.toString(backGold));
	}
}
