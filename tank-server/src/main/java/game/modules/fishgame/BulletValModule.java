/**
 *
 * 描述：   炮值管理
 * 文件：BulletValModule.java
 * 创建人：胡中伟
 * 创建时间：2018年4月10日 上午10:37:48
 *
 */
package game.modules.fishgame;

import back.modules.dataenum.RoomType;
import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import common.ServerMsgDef;
import framework.ByteUtils;
import framework.JsonUtil;
import framework.game.*;
import game.custommsg.C2SMsgDef;
import game.custommsg.CustomMsg;
import game.custommsg.RequestMsgDef;
import game.custommsg.ServerCodeDef;
import game.modules.RoomModule;
import game.modules.items.ItemModule;
import game.modules.store.StoreModule;
import game.modules.utils.ItemTipType;
import game.modules.utils.UtilFunc;
import game.util.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;

/**
 *
 * 描述：
 *
 */
public class BulletValModule implements ILogicModule {
	private static final String ITEM_AUTO_FIRE_10_MIN = "item_autofile_10m";
	private static final String PROPERTY_AUTO_FIRE_GIVEN = PLAYER_PROPERTY_GIVENAUTOFIRE10M; // 自动发炮试用是否已给

	static class UnlockData {
		int value;
		float diamond;
		String reward;
		float rate;
		float addRate;
		Map<String, Integer> pros = new HashMap<>();
		Map<String, Integer> items = new HashMap<>();
	}

	static class VipBlUnlockData {
		int vip;
		int bulletLevel;
	}

	private static final String s_needBuy = "item_powerstone"; //强化石


	private int m_nMaxLevel;
	private int m_nNMaxLevel;// 核弹最大等级
	private Map<Integer, UnlockData> m_unlockData = new HashMap<>();
	private Map<Integer, Integer> m_BulletValue = new HashMap<>();
	private Map<Integer, Integer> m_nBulletValue = new HashMap<>();
	private List<VipBlUnlockData> vipBlUnlockDatas = new ArrayList<VipBlUnlockData>();
	private List<Integer> m_index2val = new ArrayList<>();
	private List<Integer> m_nIndex2val = new ArrayList<>();
	private Map<Integer, Integer> m_val2index = new HashMap<>();
	private Map<Integer, Integer> m_nVal2index = new HashMap<>();
	private Random random = new Random();
	private ItemModule m_ItemModule = null;
	//	private WarningModule m_WarningModule = null;
	private StoreModule m_StoreModule = null;
	private BuyFuncItem m_BuyFuncItem = null;
	private XML m_parseXML;
	private int m_limitBulletLevel;
	private static Logger logger = LoggerFactory.getLogger(BulletValModule.class);

	// <value - level>
	private final Map<Integer, Integer> bvSwitchMap = new HashMap<>(64);
	private final Map<Integer, Integer> nBvSwitchMap = new HashMap<>(64);

	/**
	 *
	 */
	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
		kernel.regEvent(KernelEvent.KEVENT_ON_LINE, "Player", this, "OnPlayerLogin");

		kernel.regClientMessage(C2SMsgDef.C2S_ADD_VALUE.ordinal(), this, "OnAddValue");
		kernel.regClientMessage(C2SMsgDef.C2S_SUB_VALUE.ordinal(), this, "OnSubValue");
		kernel.regClientMessage(C2SMsgDef.C2S_CHANGE_BULLETVAL.ordinal(), this, "OnChangeBulletVal");

		kernel.regRequestMessage(RequestMsgDef.REQ_GET_BULLETVAL_LIST.ordinal(), this, "OnGetBulletValList");
		kernel.regRequestMessage(RequestMsgDef.REQ_UNLOCK_BV.ordinal(), this, "OnUnlockBV");
		kernel.regRequestMessage(RequestMsgDef.REQ_UNLOCK_TO.ordinal(), this, "OnUnlockTo");
		//kernel.RegRequestMessage(RequestMsgDef.REQ_SMITHING.ordinal(), this, "OnReqSmithing");
		kernel.listenPropertyChange(PLAYER_PROPERTY_BULLETLEVEL, "Player", this, "OnBulletLevelChanged");

		kernel.regServerRequest(ServerMsgDef.B2G_GET_BULLET_VALUE_PARAM.ordinal(), this, "OnGetBulletLevelValue");
		kernel.regServerRequest(ServerMsgDef.B2G_GET_N_BULLET_VALUE_PARAM.ordinal(), this, "OnGetNBulletLevelValue");
		kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");
		XML nBulletValueSwitch = new XML("res/NBulletValue/BvSwitch.xml", null, kernel,
				this::LoadNSwitchCfg);
		XML nBulletValueUnlock = new XML("res/NBulletValue/BvUnlock.xml", nBulletValueSwitch, kernel,
				(iKernel, cfg) -> LoadNBulletValueCfg(iKernel, cfg));
		XML bulletValueVipUnlock = new XML("res/BulletValue/VipBlUnlock.xml", nBulletValueUnlock, kernel,
				(iKernel, cfg) -> LoadVipBlUnlockCfg(iKernel, cfg));
		XML bulletValueSwitch = new XML("res/BulletValue/BvSwitch.xml", bulletValueVipUnlock, kernel,
				(iKernel, cfg) -> LoadSwitchCfg(iKernel, cfg));
		XML bulletValueUnlock = new XML("res/BulletValue/BvUnlock.xml", bulletValueSwitch, kernel,
				(iKernel, cfg) -> LoadUnlockCfg(iKernel, cfg));
		m_parseXML = new XML("res/Config/DayCard.xml", bulletValueUnlock, kernel,
				(iKernel, cfg) -> LoadDayCardConfig(iKernel, cfg));

		m_ItemModule = (ItemModule) kernel.getModule("ItemModule");
//		m_WarningModule = (WarningModule) kernel.GetModule("WarningModule");
		m_StoreModule = (StoreModule) kernel.getModule("StoreModule");
		m_BuyFuncItem = (BuyFuncItem) kernel.getModule("BuyFuncItem");
		if (m_StoreModule == null || m_ItemModule == null /*|| m_WarningModule == null*/ || m_BuyFuncItem == null) {
			return false;
		}
		return true;
	}

	/**
	 *
	 */
	@Override
	public void onDestroy() {
	}

	void RefreshCfg(IKernel kernel, String path) {
		if (path == null) {
			return;
		}
		m_parseXML.parse(kernel, path);
	}

	// 炮值达到11级，赠送自动发炮 add by 赵俊@2019/9/18 14:44
	public void OnBulletLevelChanged(IKernel kernel, IGameObject player, String name, Object oldParam) {
		/*
		if (player.getInt(name) >= m_limitBulletLevel && !player.getBool(PROPERTY_AUTO_FIRE_GIVEN)) {
			player.setProperty(PROPERTY_AUTO_FIRE_GIVEN, true);
			m_ItemModule.AddItem(kernel, player, ITEM_AUTO_FIRE_10_MIN, 1, UtilFunc.System.BULLET_VALUE.ordinal(),
					"Bullet Level Up");
		}
		 */
	}

	public void OnPlayerLogin(IKernel kernel, IGameObject player) {
		/*
		int bulletLevel = player.getInt(PLAYER_PROPERTY_BULLETLEVEL);
		if (bulletLevel >= m_limitBulletLevel && !player.getBool(PROPERTY_AUTO_FIRE_GIVEN)) {
			player.setProperty(PROPERTY_AUTO_FIRE_GIVEN, true);
			m_ItemModule.AddItem(kernel, player, ITEM_AUTO_FIRE_10_MIN, 1, UtilFunc.System.BULLET_VALUE.ordinal(),
					"Bullet Level Can Give");
		}
		 */
	}

	boolean LoadNBulletValueCfg(IKernel kernel, ICfgReader cfg) {
		m_nBulletValue.clear();
		int count = cfg.getItemCount();
		for (int i = 0; i < count; ++i) {
			int id = cfg.getInt(i, "Id");
			int value = cfg.getInt(i, "Value");
			m_nBulletValue.put(id, value);
			m_nNMaxLevel = (m_nNMaxLevel < id ? id : m_nNMaxLevel);
		}

		return true;
	}

	private boolean LoadUnlockCfg(IKernel kernel, ICfgReader cfg) {
		m_unlockData.clear();
		m_BulletValue.clear();
		int count = cfg.getItemCount();
		for (int i = 0; i < count; ++i) {
			UnlockData data = new UnlockData();

			int id = cfg.getInt(i, "Id");
			data.value = cfg.getInt(i, "Value");
			data.rate = cfg.getFloat(i, "rate");
			data.addRate = cfg.getFloat(i, "addRate");
			data.reward = cfg.getString(i, "Reward");
			data.diamond = cfg.getFloat(i, PLAYER_PROPERTY_DIAMOND);
			m_BulletValue.put(id, data.value);

			String[] pros = cfg.getStringArray(i, "Properties", ";");
			if (pros != null) {
				for (int j = 0; j < pros.length; ++j) {
					String[] pro = pros[j].split("\\*");
					if (pro.length != 2) {
						continue;
					}
					String name = pro[0];
					int value = Integer.parseInt(pro[1]);
					data.pros.put(name, value);
				}
			}
			String[] items = cfg.getStringArray(i, "Items", ";");
			if (items != null){
				for (String s : items) {
					String[] item = s.split("\\*");
					if (item.length != 2) {
						continue;
					}
					String name = item[0];
					int value = Integer.parseInt(item[1]);
					data.items.put(name, value);
				}
			}
			m_unlockData.put(id, data);
			m_nMaxLevel = id;
		}
		return true;
	}

	private boolean LoadSwitchCfg(IKernel kernel, ICfgReader cfg) {
		m_index2val.clear();
		m_val2index.clear();
		int count = cfg.getItemCount();
		for (int i = 0; i < count; ++i) {
			int val = cfg.getInt(i, "Value");
			m_index2val.add(val);
			m_val2index.put(val, i);
			bvSwitchMap.put(val, cfg.getInt(i, "Id"));
		}

		return true;
	}

	private boolean LoadNSwitchCfg(IKernel kernel, ICfgReader cfg) {
		m_nIndex2val.clear();
		m_nVal2index.clear();
		int count = cfg.getItemCount();
		for (int i = 0; i < count; ++i) {
			int val = cfg.getInt(i, "Value");
			m_nIndex2val.add(val);
			m_nVal2index.put(val, i);
			nBvSwitchMap.put(val, cfg.getInt(i, "Id"));
		}

		return true;
	}

	private boolean LoadVipBlUnlockCfg(IKernel kernel, ICfgReader cfg) {
		vipBlUnlockDatas.clear();
		int count = cfg.getItemCount();
		for (int i = 0; i < count; ++i) {
			VipBlUnlockData vipBlUnlockData = new VipBlUnlockData();
			vipBlUnlockData.vip = cfg.getInt(i, "Id");
			vipBlUnlockData.bulletLevel = cfg.getInt(i, PLAYER_PROPERTY_BULLETLEVEL);
			vipBlUnlockDatas.add(vipBlUnlockData);
		}
		return true;
	}

	public void OnPlayerClassCreate(IKernel kernel, String script) {
		kernel.declareProperty(script, PLAYER_PROPERTY_BULLETLEVEL, ValueType.INT, false, true, true);
		kernel.declareProperty(script, PLAYER_PROPERTY_BULLETVALUE, ValueType.INT, true, true, false);
		kernel.declareProperty(script, PROPERTY_AUTO_FIRE_GIVEN, ValueType.BOOL, true, false, true);
	}

	public void OnAddValue(IKernel kernel, IGameObject player, int msgid, byte[] msg) {
		//logger.info("OnAddValue");
		IGameObject desk = kernel.getGameObject(player.getLong(PLAYER_PROPERTY_DESKID));
		if (desk == null) {
			return;
		}
		int mutl = player.getInt(PLAYER_PROPERTY_SKILLMULTIPLE);
		if (mutl <= 0) {
			mutl = 1;
		}

		if (desk.getScript().equals("ArenaDesk")) {
			// 竞技场
			int min = desk.getInt("MinBV");
			int max = desk.getInt("MaxBV");
			int off = desk.getInt("OffBV");
			int curBv = player.getInt(PLAYER_PROPERTY_BULLETVALUE);
			curBv = curBv / mutl;
			int val = curBv + off;
			if (val > max) {
				val = min;
			}
			player.setProperty(PLAYER_PROPERTY_BULLETVALUE, val * mutl);
		} else {
			int selfMax = GetMaxBv(player, desk.getInt(DESK_TYPE_KEY));

			IGameObject room = desk.getParent();
			int min = room.getInt("MinBV");
			int max = room.getInt("MaxBV");
			if (desk.getScript().equals("BossDesk") || RoomModule.isSupreme(desk.getInt(DESK_TYPE_KEY))
					|| RoomType.KUANG_BAO.ordinal() == desk.getInt(DESK_TYPE_KEY)
					|| RoomModule.isNuclear(desk.getInt(DESK_TYPE_KEY))) {
				min = desk.getInt("MinBV");
				max = desk.getInt("MaxBV");
			}

			if (max > selfMax) {
				max = selfMax;
			}
			int val = player.getInt(PLAYER_PROPERTY_BULLETVALUE);
			val = val / mutl;
			if (val >= max) {
				val = min;
			} else {
				if (RoomModule.isNuclear(desk.getInt(DESK_TYPE_KEY))) {
					if (!m_nVal2index.containsKey(val)) {
						return;
					}
					//logger.info("OnAddValue val {} {} {} {}", min, max, selfMax, val);
					int index = m_nVal2index.get(val) + 1;
					val = m_nIndex2val.get(index);
				} else {
					if (!m_val2index.containsKey(val)) {
						return;
					}
					//logger.info("OnAddValue val {} {} {} {}", min, max, selfMax, val);
					int index = m_val2index.get(val) + 1;
					val = m_index2val.get(index);
				}
			}
			logger.info("OnAddValue {} {} {} {}", min, max, selfMax, val);
			player.setProperty(PLAYER_PROPERTY_BULLETVALUE, val * mutl);
		}
	}

	public void OnSubValue(IKernel kernel, IGameObject player, int msgid, byte[] msg) {
		IGameObject desk = kernel.getGameObject(player.getLong(PLAYER_PROPERTY_DESKID));
		if (desk == null) {
			return;
		}
		int mutl = player.getInt(PLAYER_PROPERTY_SKILLMULTIPLE);
		if (mutl <= 0) {
			mutl = 1;
		}

		if (desk.getScript().equals("ArenaDesk")) {
			// 竞技场
			int min = desk.getInt("MinBV");
			int max = desk.getInt("MaxBV");
			int off = desk.getInt("OffBV");
			int curBv = player.getInt(PLAYER_PROPERTY_BULLETVALUE);
			curBv = curBv / mutl;
			int val = curBv - off;
			if (val < min) {
				val = max;
			}
			player.setProperty(PLAYER_PROPERTY_BULLETVALUE, val * mutl);
		} else {
			int selfMax = GetMaxBv(player, desk.getInt(DESK_TYPE_KEY));

			IGameObject room = desk.getParent();
			int min = room.getInt("MinBV");
			int max = room.getInt("MaxBV");
			if (desk.getScript().equals("BossDesk") || RoomModule.isSupreme(desk.getInt(DESK_TYPE_KEY))
					|| RoomType.KUANG_BAO.ordinal() == desk.getInt(DESK_TYPE_KEY)
					|| RoomModule.isNuclear(desk.getInt(DESK_TYPE_KEY))) {
				min = desk.getInt("MinBV");
				max = desk.getInt("MaxBV");
			}

			if (max > selfMax) {
				max = selfMax;
			}
			int val = player.getInt(PLAYER_PROPERTY_BULLETVALUE);
			val = val / mutl;
			if (val <= min) {
				val = max;
			} else {
				if (RoomModule.isNuclear(desk.getInt(DESK_TYPE_KEY))) {
					if (!m_nVal2index.containsKey(val)) {
						return;
					}
					//logger.info("OnSubValue val {} {} {} {}", min, max, selfMax, val);
					int index = m_nVal2index.get(val) - 1;
					val = m_nIndex2val.get(index);
				} else {
					if (!m_val2index.containsKey(val)) {
						return;
					}
					//logger.info("OnSubValue val {} {} {} {}", min, max, selfMax, val);
					int index = m_val2index.get(val) - 1;
					val = m_index2val.get(index);
				}
			}

			logger.info("OnSubValue {} {} {} {}", min, max, selfMax, val);
			player.setProperty(PLAYER_PROPERTY_BULLETVALUE, val * mutl);
		}
	}

	public void AutoSubBV(IKernel kernel, IGameObject player, long coin) {
		IGameObject desk = kernel.getGameObject(player.getLong(PLAYER_PROPERTY_DESKID));

		if (desk == null) {
			return;
		}
		int mutl = player.getInt(PLAYER_PROPERTY_SKILLMULTIPLE);
		if (mutl <= 0) {
			mutl = 1;
		}

		if (desk.getScript().equals("ArenaDesk")) {
			// 竞技场
			int min = desk.getInt("MinBV");
			int off = desk.getInt("OffBV");
			int val = player.getInt(PLAYER_PROPERTY_BULLETVALUE);
			val = val / mutl;
			while (val > min) {
				if (val > coin) {
					val -= off;
				} else {
					break;
				}
			}

			if (val < min || val > coin) {
				val = (int) coin;
			}

			player.setProperty(PLAYER_PROPERTY_BULLETVALUE, val);
		} else {
			int selfMax = GetMaxBv(player, desk.getInt(DESK_TYPE_KEY));

			IGameObject room = desk.getParent();
			int min = room.getInt("MinBV");
			int max = room.getInt("MaxBV");
			if (desk.getScript().equals("BossDesk") || RoomModule.isSupreme(desk.getInt(DESK_TYPE_KEY))
					|| RoomType.KUANG_BAO.ordinal() == desk.getInt(DESK_TYPE_KEY)
					|| RoomModule.isNuclear(desk.getInt(DESK_TYPE_KEY))) {
				min = desk.getInt("MinBV");
				max = desk.getInt("MaxBV");
			}

			if (max > selfMax) {
				max = selfMax;
			}
			int val = (player.getInt(PLAYER_PROPERTY_BULLETVALUE));
			val = val / mutl;
			if (RoomModule.isNuclear(desk.getInt(DESK_TYPE_KEY))) {
				if (!m_nVal2index.containsKey(val)) {
					return;
				}
				int curIndex = m_nVal2index.get(val);
				int minIndex = m_nVal2index.get(min);
				for (int i = curIndex - 1; i >= minIndex; --i) {
					val = m_nIndex2val.get(i);
					if (coin >= val * mutl) {
						player.setProperty(PLAYER_PROPERTY_BULLETVALUE, val * mutl);
						return;
					}
				}
			} else {
				if (!m_val2index.containsKey(val)) {
					return;
				}
				int curIndex = m_val2index.get(val);
				int minIndex = m_val2index.get(min);
				for (int i = curIndex - 1; i >= minIndex; --i) {
					val = m_index2val.get(i);
					if (coin >= val * mutl) {
						player.setProperty(PLAYER_PROPERTY_BULLETVALUE, val * mutl);
						return;
					}
				}
			}
		}
	}

	public int GetMaxBv(IGameObject player) {
		return Integer.MAX_VALUE;
//		int level = player.getInt(PLAYER_PROPERTY_BULLETLEVEL);
//		if (m_unlockData.containsKey(level)) {
//			return m_unlockData.get(level).value;
//		}
//
//		CheckLevel(null, player);
//		level = player.getInt(PLAYER_PROPERTY_BULLETLEVEL);
//		if (m_unlockData.containsKey(level)) {
//			return m_unlockData.get(level).value;
//		}
//
//		return 0;
	}

	/**
	 * 核弹炮值等级
	 *
	 * @param player
	 * @return
	 */
	public int GetNMaxBv(IGameObject player) {
		return Integer.MAX_VALUE;
//		int level = player.getInt(PLAYER_PROPERTY_NBULLETLEVEL);
//		if (m_nBulletValue.containsKey(level)) {
//			return m_nBulletValue.get(level);
//		}
//		CheckNLevel(player);
//		level = player.getInt(PLAYER_PROPERTY_NBULLETLEVEL);
//		if (m_nBulletValue.containsKey(level)) {
//			return m_nBulletValue.get(level);
//		}
//		return 0;
	}

	public int GetMaxBv(IGameObject player, int roomType) {
		return Integer.MAX_VALUE;
//		if (RoomModule.isNuclear(roomType)) {
//			return GetNMaxBv(player);
//		}
//		return GetMaxBv(player);
	}

	public boolean vipUnLockTo(IKernel kernel, IGameObject player) {

		int level = player.getInt(PLAYER_PROPERTY_BULLETLEVEL);
		int vip = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
		if (vip < 1 || vip > vipBlUnlockDatas.size()) {
			return false;
		}
		int target = vipBlUnlockDatas.get(vip - 1).bulletLevel;
		if (target <= level || target > m_nMaxLevel) {
			return false;
		}
		if (!m_unlockData.containsKey(target)) {
			return false;
		}
		player.setProperty(PLAYER_PROPERTY_BULLETLEVEL, target);
		IGameObject desk = kernel.getGameObject(player.getLong(PLAYER_PROPERTY_DESKID));
		if (desk != null) {
			// 解锁后自动提升炮值
			IGameObject room = desk.getParent();
			int roomMax = room.getInt("MaxBV");
			int max = m_unlockData.get(target).value;
			if (max > roomMax) {
				max = roomMax;
			}
			int mutl = player.getInt(PLAYER_PROPERTY_SKILLMULTIPLE);
			if (mutl <= 0){
				mutl = 1;
			}
			player.setProperty(PLAYER_PROPERTY_BULLETVALUE, max * mutl);
		}
		return true;
	}

	public String UnlockTo(IKernel kernel, IGameObject player, int target) {
		logger.info("UnlockTo {}", target);

		int level = player.getInt(PLAYER_PROPERTY_BULLETLEVEL);
		if (target <= level || target > m_nMaxLevel) {
			return "-1";
		}

		if (!m_unlockData.containsKey(target)) {
			return "-1";
		}

		int leftCount = m_ItemModule.GetItemCount(kernel, player, s_needBuy);
		int needDiamond = 0;
		// int needBuyCount = 0;
		// int haveCount = 0;
		//
		Map<String, Integer> mapPros = new HashMap<>();
		Map<String, Integer> mapItems = new HashMap<>();
		Map<String, Integer> mapRewards = new HashMap<>();
		for (int i = level + 1; i <= target; ++i) {
			if (!m_unlockData.containsKey(i)) {
				continue;
			}

			UnlockData data = m_unlockData.get(i);

			// check count
			for (Entry<String, Integer> entry : data.pros.entrySet()) {
				int old = 0;
				if (mapPros.containsKey(entry.getKey())) {
					old = mapPros.get(entry.getKey());
				}
				mapPros.put(entry.getKey(), old + entry.getValue());
			}
			for (Entry<String, Integer> entry : data.items.entrySet()) {
				int old = 0;
				if (mapItems.containsKey(entry.getKey())) {
					old = mapItems.get(entry.getKey());
				}
				if (entry.getKey().equals(s_needBuy)) {
					if (leftCount >= entry.getValue()) {
						leftCount -= entry.getValue();
						mapItems.put(entry.getKey(), old + entry.getValue());
					} else {
						needDiamond += (entry.getValue() - leftCount) * data.diamond;
						if (leftCount > 0) {
							mapItems.put(entry.getKey(), old + leftCount);
							leftCount = 0;
						}
					}
				} else {
					mapItems.put(entry.getKey(), old + entry.getValue());
				}
			}
			if (!data.reward.isEmpty()) {
				int old = 0;
				if (mapRewards.containsKey(data.reward)) {
					old = mapRewards.get(data.reward);
				}
				mapRewards.put(data.reward, old + 1);
			}
		}

		// check count
		for (Entry<String, Integer> entry : mapPros.entrySet()) {
			long val = (long) player.getProperty(entry.getKey());
			if (val < entry.getValue()) {
				return entry.getKey();
			}
		}
		for (Entry<String, Integer> entry : mapItems.entrySet()) {
			int val = m_ItemModule.GetItemCount(kernel, player, entry.getKey());
			if (val < entry.getValue()) {
				if (entry.getKey().equals(s_needBuy)) {
					// needBuy
					// needBuyCount = entry.getValue() - val;
					// haveCount = val;
				} else {
					return entry.getKey();
				}
			}
		}

		if (needDiamond > 0) {
			if (player.getLong(PLAYER_PROPERTY_DIAMOND) < needDiamond) {
				return s_needBuy;
			}
			player.setProperty(PLAYER_PROPERTY_DIAMOND, player.getLong(PLAYER_PROPERTY_DIAMOND) - needDiamond);
		}

		//
		for (Entry<String, Integer> entry : mapPros.entrySet()) {
			long val = (long) player.getProperty(entry.getKey());
			player.setProperty(entry.getKey(), val - entry.getValue());
//			if (entry.getKey().equals(PLAYER_PROPERTY_GOLD)) {
//				m_WarningModule.UseGold(player.GetInt(PLAYER_PROPERTY_UID), entry.getValue());
//			} else if (entry.getKey().equals(PLAYER_PROPERTY_DIAMOND)) {
//				m_WarningModule.UseDiamond(player.GetInt(PLAYER_PROPERTY_UID), entry.getValue());
//			}
		}
		for (Entry<String, Integer> entry : mapItems.entrySet()) {
			int count = entry.getValue();
			m_ItemModule.SubItem(kernel, player, entry.getKey(), count, UtilFunc.System.BULLET_VALUE.ordinal(),
					"Unlock to " + target);
		}

		player.setProperty(PLAYER_PROPERTY_BULLETLEVEL, target);
		Object[] objs = new Object[mapRewards.size() * 2];
		int i = 0;
		for (Entry<String, Integer> entry : mapRewards.entrySet()) {
			m_ItemModule.AddItem(kernel, player, entry.getKey(), entry.getValue(),
					UtilFunc.System.BULLET_VALUE.ordinal(), "unlock to " + target);
			objs[i * 2] = entry.getKey();
			objs[i * 2 + 1] = entry.getValue();
			++i;
		}
		UtilFunc.sendItemTips(kernel, player, ItemTipType.TIP_UNLOCK_BV, objs);

		IGameObject desk = kernel.getGameObject(player.getLong(PLAYER_PROPERTY_DESKID));
		if (desk != null) {
			// 解锁后自动提升炮值
			IGameObject room = desk.getParent();
			int roomMax = room.getInt("MaxBV");
			int max = m_unlockData.get(target).value;
			if (max > roomMax) {
				max = roomMax;
			}
			int mutl = player.getInt(PLAYER_PROPERTY_SKILLMULTIPLE);
			if (mutl <= 0){
				mutl = 1;
			}
			player.setProperty(PLAYER_PROPERTY_BULLETVALUE, max * mutl);
		}

		return "";
	}

	void OnUnlockTo(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
			throws InvalidProtocolBufferException {
		CustomMsg.ServerCode.Builder code = CustomMsg.ServerCode.newBuilder();
		CustomMsg.Int32 val = CustomMsg.Int32.parseFrom(msg);

		int target = -1;
		for (Entry<Integer, UnlockData> entry : m_unlockData.entrySet()) {
			if (entry.getValue().value == val.getValue()) {
				target = entry.getKey();
				break;
			}
		}

		if (target == -1) {
			code.setCode(ServerCodeDef.CODE_PARAM_ERR.ordinal());
		} else {
			logger.info("OnUnlockTo {}", val.getValue());

			String res = UnlockTo(kernel, player, target);

			if (res.isEmpty()) {
				code.setCode(ServerCodeDef.CODE_SUCCESS.ordinal());
			} else if (res.equals(s_needBuy)) {
				code.setCode(ServerCodeDef.CODE_NEED_PRO.ordinal());
			} else if (res.equals("-1")) {
				code.setCode(ServerCodeDef.CODE_LEVEL_MAX.ordinal());
			} else {
				code.setCode(ServerCodeDef.CODE_NEED_ITEM.ordinal());
			}
		}
		kernel.response(player, reqid, code.build().toByteArray());
	}

	void OnUnlockBV(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg) throws InvalidProtocolBufferException {
		JsonObject json = new JsonObject();
		json.addProperty("code", ServerCodeDef.CODE_SUCCESS.ordinal());
		CustomMsg.Int32 valint = CustomMsg.Int32.parseFrom(msg);
		if (valint.getValue() < 0 || valint.getValue() > 2){
			json.addProperty("code", ServerCodeDef.CODE_PARAM_ERR.ordinal());
			UtilFunc.respRpcStringToClient(kernel,player, reqid, json.toString());
			logger.info("OnUnlockBV params error {} ", valint.getValue());
			return;
		}
		int level = player.getInt(PLAYER_PROPERTY_BULLETLEVEL) + 1;
		while (!m_unlockData.containsKey(level) && level <= m_nMaxLevel) {
			++level;
		}
		if (level < 0 || level > m_nMaxLevel) {
			json.addProperty("code", ServerCodeDef.CODE_LEVEL_MAX.ordinal());
			UtilFunc.respRpcStringToClient(kernel,player, reqid, json.toString());
			logger.info("OnUnlockBV 1 {} {}", msgid, reqid);
			return;
		}
		if(!m_unlockData.containsKey(level)){
			logger.info("{}",level);
			return;
		}
		UnlockData data = m_unlockData.get(level);
		int value = data.value;
		if (valint.getValue() == 2) {
			float baseRate = (float)(Math.round(data.rate * 100)) / 100;
			float addRate = (float)(Math.round(data.addRate * 100)) / 100;
			json.addProperty("baseRate",baseRate);
			json.addProperty("addRate",addRate);
			UtilFunc.respRpcStringToClient(kernel,player,reqid,json.toString());
			return;
		}
		if (value > 10000){
			float baseRate = (float)(Math.round(data.rate * 100)) / 100;
			float addRate = (float)(Math.round(data.addRate * 100)) / 100;
			json.addProperty("baseRate",baseRate);
			json.addProperty("addRate",addRate);
		}
		int leftCount = m_ItemModule.GetItemCount(kernel, player, s_needBuy);
		int needDiamond = 0;
		for (Entry<String, Integer> entry : data.pros.entrySet()) {
			String propkey = entry.getKey();
			ValueType _type = player.getProType(propkey);
			long val = 0;
			if (_type == ValueType.LONG){
				val = player.getLong(propkey);
			}else if (_type == ValueType.INT) {
				val = player.getInt(propkey);
			}else if (_type == ValueType.SHORT) {
				val = player.getShort(propkey);
			}
			if (val < entry.getValue()) {
				json.addProperty("code", ServerCodeDef.CODE_NEED_PRO.ordinal());
				UtilFunc.respRpcStringToClient(kernel,player, reqid, json.toString());
				return;
			}
		}
		for (Entry<String, Integer > entry : data.items.entrySet()) {
			if (entry.getKey().equals(s_needBuy)) {
				if (leftCount >= entry.getValue()) {   //leftcount ：道具背包里的强化石数量
					leftCount -= entry.getValue();
				} else {
					needDiamond += (entry.getValue() - leftCount) * data.diamond;
					leftCount = 0;
				}
			} else if (entry.getKey().equals(("item_diamond_1"))){
				if (player.getLong(PLAYER_PROPERTY_DIAMOND) < entry.getValue()){
					json.addProperty("code", ServerCodeDef.CODE_NEED_PRO.ordinal());
					UtilFunc.respRpcStringToClient(kernel,player, reqid, json.toString());
					return;
				}
			}else if(entry.getKey().equals("item_forgerock")) {
				if (valint.getValue() > 0){
					int val = m_ItemModule.GetItemCount(kernel, player, entry.getKey());
					if (val < entry.getValue()) {
						json.addProperty("code", ServerCodeDef.CODE_NEED_ITEM.ordinal());
						UtilFunc.respRpcStringToClient(kernel,player, reqid, json.toString());
						return;
					}
				}
			} else {
				int val = m_ItemModule.GetItemCount(kernel, player, entry.getKey());
				if (val < entry.getValue()) {
					json.addProperty("code", ServerCodeDef.CODE_NEED_ITEM.ordinal());
					UtilFunc.respRpcStringToClient(kernel,player, reqid, json.toString());
					return;
				}
			}
		}
		if (needDiamond > 0) {     //购买强化石所需的钻石
			if (player.getLong(PLAYER_PROPERTY_DIAMOND) < needDiamond) {
				json.addProperty("code", ServerCodeDef.CODE_NEED_PRO.ordinal());
				UtilFunc.respRpcStringToClient(kernel,player, reqid, json.toString());
				return;
			}
			player.setProperty(PLAYER_PROPERTY_DIAMOND, player.getLong(PLAYER_PROPERTY_DIAMOND) - needDiamond); //扣除
		}
		for (Entry<String, Integer> entry : data.pros.entrySet()) {
			String propkey = entry.getKey();
			ValueType _type = player.getProType(propkey);
			if (_type == ValueType.LONG){
				long val = player.getLong(propkey);
				player.setProperty(propkey,val - entry.getValue());
			}else if (_type == ValueType.INT) {
				int val = player.getInt(propkey);
				player.setProperty(propkey,val - entry.getValue());
			}else if (_type == ValueType.SHORT) {
				short val = player.getShort(propkey);
				player.setProperty(propkey,val - entry.getValue());
			}
//			if (entry.getKey().equals(PLAYER_PROPERTY_GOLD)) {
//				m_WarningModule.UseGold(player.GetInt(PLAYER_PROPERTY_UID), entry.getValue());
//			} else if (entry.getKey().equals(PLAYER_PROPERTY_DIAMOND)) {
//				m_WarningModule.UseDiamond(player.GetInt(PLAYER_PROPERTY_UID), entry.getValue());
//			}
		}
		for (Entry<String, Integer> entry : data.items.entrySet()) {
			long costDiamod = player.getLong(PLAYER_PROPERTY_DIAMOND) - entry.getValue();
			if (entry.getKey().equals("item_diamond_1")){
				if (costDiamod < 0){
					// 返还之前扣除的钻石
					if (needDiamond > 0) {
						player.setProperty(PLAYER_PROPERTY_DIAMOND, player.getLong(PLAYER_PROPERTY_DIAMOND) + needDiamond);
					}
					json.addProperty("code", ServerCodeDef.CODE_NEED_PRO.ordinal());
					UtilFunc.respRpcStringToClient(kernel, player, reqid, json.toString());
					return;
				}
				player.setProperty(PLAYER_PROPERTY_DIAMOND, costDiamod);
			}else if(entry.getKey().equals("item_forgerock")){
				if (valint.getValue() > 0){
					m_ItemModule.SubItem(kernel, player, entry.getKey(), entry.getValue(), UtilFunc.System.BULLET_VALUE.ordinal(), "unlock to " + level);
				}
			} else{
				int count = entry.getValue();
				if (entry.getKey().equals(s_needBuy)) {
					count = m_ItemModule.GetItemCount(kernel, player, s_needBuy) - leftCount;
				}
				m_ItemModule.SubItem(kernel, player, entry.getKey(), count, UtilFunc.System.BULLET_VALUE.ordinal(), "unlock to " + level);
			}
		}
		float itemRate = 0;
		if (valint.getValue() > 0) {
			itemRate = data.addRate;
		}
		//10000以后炮值锻造进行概率判断
		if(value > 10000){
			float ran = random.nextFloat();
			float rate = data.rate + itemRate;
			if (ran > rate){
				json.addProperty("code", 4);
				UtilFunc.respRpcStringToClient(kernel,player, reqid, json.toString());
				return;
			}
		}
		if (!data.reward.isEmpty()) {
			m_ItemModule.AddItem(kernel, player, data.reward, 1, UtilFunc.System.BULLET_VALUE.ordinal(), "unlock to " + level);
			UtilFunc.sendItemTips(kernel, player, ItemTipType.TIP_UNLOCK_BV, data.reward, 1);
		}
		player.setProperty(PLAYER_PROPERTY_BULLETLEVEL, level);
		IGameObject desk = kernel.getGameObject(player.getLong(PLAYER_PROPERTY_DESKID));
		if (desk != null) {
			//解锁后自动提升炮值
			IGameObject room = desk.getParent();
			int roomMax = 0;
			int roomType = room.getInt(DESK_TYPE_KEY);
			if (!RoomModule.isNuclear(roomType)){
				//如果在海神殿中锻造普通炮台不自动切换当前炮值
				if (RoomModule.isSupreme(roomType) || RoomModule.isDragon(roomType) || RoomModule.isMysteryLegend(roomType)) {
					roomMax = desk.getInt("MaxBV");
				} else {
					roomMax = room.getInt("MaxBV");
				}
				//int max = m_unlockData.get(level).value;
				if (value > roomMax) {
					value = roomMax;
				}
				int mutl = player.getInt(PLAYER_PROPERTY_SKILLMULTIPLE);
				if (mutl <= 0){
					mutl = 1;
				}
				player.setProperty(PLAYER_PROPERTY_BULLETVALUE, value * mutl);
			}
		}
		json.addProperty("code", ServerCodeDef.CODE_SUCCESS.ordinal());
		UtilFunc.respRpcStringToClient(kernel,player, reqid, json.toString());
	}

	public void CheckLevel(IKernel kernel, IGameObject player) {
		int level = player.getInt(PLAYER_PROPERTY_BULLETLEVEL);
		while (!m_unlockData.containsKey(level) && level <= m_nMaxLevel) {
			++level;
		}
		if (level < 0 || level > m_nMaxLevel) {
			return;
		}
		player.setProperty(PLAYER_PROPERTY_BULLETLEVEL, level);
	}

	/**
	 * 核弹炮值等级
	 * @param player
	 */
	private void CheckNLevel(IGameObject player) {
		int level = player.getInt(PLAYER_PROPERTY_NBULLETLEVEL);
		while (!m_nBulletValue.containsKey(level) && level <= m_nNMaxLevel) {
			++level;
		}
		if (level < 0 || level > m_nNMaxLevel) {
			return;
		}
		player.setProperty(PLAYER_PROPERTY_NBULLETLEVEL, level);
	}

	private void LoadDayCardConfig(IKernel kernel, ICfgReader cfg) {
		//m_limitBulletLevel = cfg.getInt(ITEM_AUTO_FIRE_10_MIN, PLAYER_PROPERTY_BULLETLEVEL);
	}

	// 获取可切换炮值的列表
	void OnGetBulletValList(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
			throws InvalidProtocolBufferException {
		IGameObject desk = kernel.getGameObject(player.getLong(PLAYER_PROPERTY_DESKID));
		List<Integer> bvList = new ArrayList<>();
		if (desk != null) {
			IGameObject room = desk.getParent();
			int roomMax = 0; // 该房间/桌子的最大炮值
			int roomMin = 0; // 该房间/桌子的最小炮值
			int roomType = room.getInt(DESK_TYPE_KEY);
			if (RoomModule.isSupreme(roomType) || RoomModule.isDragon(roomType) || RoomModule.isNuclear(roomType)
					|| roomType == RoomType.ARENA.ordinal() || roomType == RoomType.ROOM_BOSS.ordinal()
					|| RoomModule.isMysteryLegend(roomType)) {
				roomMax = desk.getInt("MaxBV");
				roomMin = desk.getInt("MinBV");
			} else {

				roomMax = room.getInt("MaxBV");
				roomMin = room.getInt("MinBV");
			}
			int playMaxBv = GetMaxBv(player, roomType);
			List<Integer> index2Bv = null;
			// 海神殿
			if (RoomModule.isNuclear(roomType)) {
				index2Bv = m_nIndex2val;
			} else {
				index2Bv = m_index2val;
			}
			for (int i = 0; i < index2Bv.size(); ++i) {
				int bv = index2Bv.get(i);
				if (bv < roomMin) {
					continue;
				}
				if (bv > playMaxBv) {
					continue;
				}
				if (bv > roomMax) {
					break;
				}
				if (bv > playMaxBv && roomType != RoomType.ARENA.ordinal()) {
					break;
				}

				bvList.add(bv);
			}
		}
		JsonObject json = new JsonObject();
		json.add("bvList", JsonUtil.encodeToElement(bvList));
		UtilFunc.respRpcStringToClient(kernel, player, reqid, json.toString());
	}

	// 设置炮台等级
	void OnChangeBulletVal(IKernel kernel, IGameObject player, int msgid, byte[] msg)
			throws InvalidProtocolBufferException {
		CustomMsg.Int32 data = CustomMsg.Int32.parseFrom(msg);
		int tmpBulletVal = data.getValue();

		IGameObject desk = kernel.getGameObject(player.getLong(PLAYER_PROPERTY_DESKID));
		if (desk == null) {
			return;
		}
		IGameObject room = desk.getParent();
		int roomMax = 0; // 该房间/桌子的最大炮值
		int roomMin = 0; // 该房间/桌子的最小炮值
		int roomType = room.getInt(DESK_TYPE_KEY);
		if (RoomModule.isSupreme(roomType) || RoomModule.isDragon(roomType) || RoomModule.isNuclear(roomType)
				|| roomType == RoomType.ARENA.ordinal() || roomType == RoomType.ROOM_BOSS.ordinal()
				|| RoomModule.isMysteryLegend(roomType)) {
			roomMax = desk.getInt("MaxBV");
			roomMin = desk.getInt("MinBV");
		} else {
			roomMax = room.getInt("MaxBV");
			roomMin = room.getInt("MinBV");
		}
		int playMaxBv = GetMaxBv(player, roomType);

		if (tmpBulletVal < roomMin || tmpBulletVal > roomMax || (tmpBulletVal > playMaxBv && roomType != RoomType.ARENA.ordinal())) {
			return;
		}
		int mutl = player.getInt(PLAYER_PROPERTY_SKILLMULTIPLE);
		if (mutl <= 0) {
			mutl = 1;
		}
		player.setProperty(PLAYER_PROPERTY_BULLETVALUE, tmpBulletVal * mutl);
	}

	void OnGetBulletLevelValue(IKernel kernel, int reqid, byte[] data) throws Exception {
		kernel.responseServer(reqid, ByteUtils.objectToByte(m_BulletValue));
	}

	void OnGetNBulletLevelValue(IKernel kernel, int reqid, byte[] data) throws Exception {
		kernel.responseServer(reqid, ByteUtils.objectToByte(m_nBulletValue));
	}
}
