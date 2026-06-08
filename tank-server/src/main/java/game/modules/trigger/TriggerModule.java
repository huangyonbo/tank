/*
*    
* 描述：   触发器
* 文件：TriggerModule.java
* 创建人：胡中伟
* 创建时间：2018年9月19日 下午1:42:33 
*    
*/
package game.modules.trigger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import back.modules.dataenum.RoomType;
import framework.MethodAccessCache;
import framework.MethodCallBackData;
import framework.game.IGameObject;
import framework.game.IKernel;
import framework.game.ILogicModule;
import framework.game.KernelEvent;
import game.custommsg.CommandDef;
import game.modules.fishgame.BulletValModule;
import game.modules.utils.UtilFunc;


@SuppressWarnings("unused")
public class TriggerModule implements ILogicModule {
	public enum TriggerType {
		TYPE_LOGIN, // 登录 0
		TYPE_KILL_FISH, // 击杀鱼 1
		TYPE_UNLOCK_BV, // 解锁金币炮炮值 2
		TYPE_USE_GOLD, // 消耗金币 3
		TYPE_USE_ITEM, // 使用道具 4
		TYPE_CHARGE, // 累计充值 5
		TYPE_ONLINE_TIME, // 累计在线时长 6
		TYPE_GET_ITEM, // 获得道具 7
		TYPE_STONE_EXCHANGE, // 灵石兑换 8
		TYPE_USE_BOMB_COIN, // 消耗魔晶(发炮) 9
		TYPE_UP_LEVEL, // 等级提升 10
		TYPE_UP_VIP_LEVEL, // VIP等级提升 11
		TYPE_ENTER_ROOM, // 进入渔场 12
		TYPE_UNLOCK_BATTERY_SKIN, // 解锁炮台 13
		TYPE_GET_ACHIEVEMENT, // 获得成就点 14
		TYPE_COMBINE_ITEM, // 合成道具 15
		TYPE_TRIGGER_FIRE, // 引爆火种 16
		TYPE_OPEN_MERMAID_TREASURE, // 开启人鱼秘宝 17
		TYPE_FISH_POND_PUT, // 养鱼池投放 18
		TYPE_FISH_POND_CATCH, // 养鱼池捕获 19
		TYPE_JOIN_GUILD, // 加入公会 20
		TYPE_EXCHANGE_DRAGON_BALL, // 兑换龙珠 21
		TYPE_UNLOCK_NBV, // 解锁魔晶炮炮值 22
		TYPE_ON_FIRE,// 开炮  23
		TYPE_ON_GETGOLD,// 获得金币 24
		TYPE_CONTINUATION_LOGIN,//连续累计登录 25
		TYPE_CONTINUATION_LOGIN_NEW_YEAR,//新春七天乐签到 26
		TYPE_END
	}

	public enum ValueType {
		INC, // 增量
		VAL, // 变量

		END
	}

	Map<Integer, List<MethodCallBackData>> m_mapTriggers = new HashMap<>();
	private static final Logger logger = LoggerFactory.getLogger(TriggerModule.class);
	BulletValModule m_BulletValModule = null;
	List<String> m_listBomb = Arrays.asList("item_skill_hbomb", "item_skill_nbomb", "item_skill_missile",
			"item_skill_normal_bomb", "item_skill_bind_bomb");

	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_LINE, "Player", this, "OnPlayerOnLine");
		kernel.regEvent(KernelEvent.KEVENT_ON_LOAD, "Player", this, "OnPlayerOnLoad");
		kernel.regEvent(KernelEvent.KEVENT_OFF_LINE, "Player", this, "OnPlayerOffLine");
		kernel.regEvent(KernelEvent.KEVENT_ON_SITDOWN, "Player", this, "OnPlayerSitDown");

		kernel.listenPropertyChange(PLAYER_PROPERTY_BULLETLEVEL, "Player", this, "OnBvUnlock");
		kernel.listenPropertyChange(PLAYER_PROPERTY_NBULLETLEVEL, "Player", this, "OnNBvUnlock");
		kernel.listenPropertyChange(PLAYER_PROPERTY_TOTALPLAY, "Player", this, "OnTotalPlayChange");
		kernel.listenPropertyChange(PLAYER_PROPERTY_BOMBTOTALPLAY, "Player", this, "OnBombTotalPlayChange");
		kernel.listenPropertyChange(PLAYER_PROPERTY_TOTALRECHARGEAMOUNT, "Player", this, "OnTotalRechargeAmountChange");
		kernel.listenPropertyChange(PLAYER_PROPERTY_LEVEL, "Player", this, "OnPlayerLevelChange");
		kernel.listenPropertyChange(PLAYER_PROPERTY_VIPLEVEL, "Player", this, "OnPlayerVipLevelChange");
		kernel.listenPropertyChange(PLAYER_PROPERTY_ACHIEVEMENTPOINT, "Player", this, "OnPlayerGetAchievement");

		kernel.regCommand(CommandDef.CMD_FISH_DIE.ordinal(), "FishDesk", this, "OnFishDie");
		kernel.regCommand(CommandDef.CMD_USE_ITEM.ordinal(), "Item", this, "OnUseItem");
		kernel.regCommand(CommandDef.CMD_GET_ITEM.ordinal(), "Item", this, "OnGetItem");
		kernel.regCommand(CommandDef.CMD_STONE_EXCHANGE.ordinal(), "Player", this, "OnStoneExchange");
		kernel.regCommand(CommandDef.CMD_USE_ITEM.ordinal(), "BatterySkinItem", this, "OnGetBatterySkinItem");
		kernel.regCommand(CommandDef.CMD_USE_SKILL.ordinal(), "Player", this, "OnUseSkill");

		kernel.declareHeartBeat("TriggerModule::CheckOnlineTime", this, "HB_CheckOnlineTime");

		m_BulletValModule = (BulletValModule) kernel.getModule("BulletValModule");

		return true;
	}

	/**
	 * 
	 */
	@Override
	public void onDestroy() {
	}

	public void RegTrigger(int id, Object listener, String methodName) {
		MethodCallBackData cbdata = new MethodCallBackData();
		cbdata.listener = listener;
		cbdata.access = MethodAccessCache.tryToGet(listener.getClass());
		cbdata.methodIndex = cbdata.access.getIndex(methodName, IKernel.class, IGameObject.class, int.class,
				String.class, int.class, int.class);

		if (!m_mapTriggers.containsKey(id)) {
			m_mapTriggers.put(id, new ArrayList<>());
		}
		m_mapTriggers.get(id).add(cbdata);
	}

	public void OnTrigger(IKernel kernel, IGameObject player, int id, String target, int count, int valType) {
		if (player.getBool("IsRobot")) {
			return;
		}

		if (!m_mapTriggers.containsKey(id)) {
			logger.info("!m_mapTriggers.containsKey(id) {}", id);
			return;
		}

		for (MethodCallBackData cb : m_mapTriggers.get(id)) {
			cb.access.invoke(cb.listener, cb.methodIndex, kernel, player, id, target, count, valType);
		}
	}

	void OnPlayerOnLine(IKernel kernel, IGameObject player) {
		OnTrigger(kernel, player, TriggerType.TYPE_LOGIN.ordinal(), "", 1, ValueType.INC.ordinal());
	}

	void OnPlayerOnLoad(IKernel kernel, IGameObject player) {
		kernel.addHeartBeat("TriggerModule::CheckOnlineTime", player, 20000, -1);
	}

	void OnPlayerOffLine(IKernel kernel, IGameObject player) {
		OnTrigger(kernel, player, TriggerType.TYPE_ONLINE_TIME.ordinal(), "", 0, ValueType.INC.ordinal());
	}

	void OnPlayerSitDown(IKernel kernel, IGameObject player, IGameObject desk) {
		int type = desk.getInt(DESK_TYPE_KEY);
		OnTrigger(kernel, player, TriggerType.TYPE_ENTER_ROOM.ordinal(), String.valueOf(type), 1, ValueType.INC.ordinal());
	}

	void HB_CheckOnlineTime(IKernel kernel, IGameObject player) {
		OnTrigger(kernel, player, TriggerType.TYPE_ONLINE_TIME.ordinal(), "", 0, ValueType.INC.ordinal());
	}

	void OnBvUnlock(IKernel kernel, IGameObject player, String name, Object oldBulletValue) {
		OnTrigger(kernel, player, TriggerType.TYPE_UNLOCK_BV.ordinal(), "", m_BulletValModule.GetMaxBv(player),
				ValueType.VAL.ordinal());
	}
	void OnNBvUnlock(IKernel kernel, IGameObject player, String name, Object oldBulletValue) {
		OnTrigger(kernel, player, TriggerType.TYPE_UNLOCK_NBV.ordinal(), "", m_BulletValModule.GetNMaxBv(player),
				ValueType.VAL.ordinal());
	}


	void OnFishDie(IKernel kernel, IGameObject desk, Object... objects) {
		IGameObject player = (IGameObject) objects[0];
		String fishcfg = (String) objects[2];

		if (desk.getInt(DESK_TYPE_KEY) != RoomType.ARENA.ordinal()) {
			OnTrigger(kernel, player, TriggerType.TYPE_KILL_FISH.ordinal(), fishcfg, 1, ValueType.INC.ordinal());
		}
	}

	void OnUseItem(IKernel kernel, IGameObject item, Object... objects) {
		IGameObject player = (IGameObject) objects[0];
		int count = (int) objects[1];
		String itemId = item.getString("Id");
		// 使用弹头是否是有效场景 add by 赵俊@2019/7/23 13:57
		if (!valid(itemId, objects)) {
			return;
		}
		OnTrigger(kernel, player, TriggerType.TYPE_USE_ITEM.ordinal(), itemId, count, ValueType.INC.ordinal());
	}

	void OnGetItem(IKernel kernel, IGameObject item, Object... objects) {
		IGameObject player = (IGameObject) objects[0];
		int count = (int) objects[1];
		String itemId = item.getString("Id");
		OnTrigger(kernel, player, TriggerType.TYPE_GET_ITEM.ordinal(), itemId, count, ValueType.INC.ordinal());
	}

	void OnStoneExchange(IKernel kernel, IGameObject player, Object... objects) {
		OnTrigger(kernel, player, TriggerType.TYPE_STONE_EXCHANGE.ordinal(), "", 1, ValueType.INC.ordinal());
	}

	void OnGetBatterySkinItem(IKernel kernel, IGameObject item, Object... objects) {
		IGameObject player = (IGameObject) objects[0];
		int skinId = item.getInt(PLAYER_PROPERTY_SKINID);
		OnTrigger(kernel, player, TriggerType.TYPE_UNLOCK_BATTERY_SKIN.ordinal(), String.valueOf(skinId), 1, ValueType.VAL.ordinal());
	}

	void OnUseSkill(IKernel kernel, IGameObject player, Object... objects) {
		String skillId = (String) objects[0];
		if (skillId.contains("skill_tinder")) {
			OnTrigger(kernel, player, TriggerType.TYPE_TRIGGER_FIRE.ordinal(), StringUtils.EMPTY, 1, ValueType.INC.ordinal());
		}
	}

	public void OnTotalPlayChange(IKernel kernel, IGameObject player, String name, Object oldTotalPlay) {
		OnTrigger(kernel, player, TriggerType.TYPE_USE_GOLD.ordinal(), "",
				(int) (player.getLong(PLAYER_PROPERTY_TOTALPLAY) - (long) oldTotalPlay), ValueType.INC.ordinal());
	}
	
	public void OnBombTotalPlayChange(IKernel kernel, IGameObject player, String name, Object oldTotalPlay) {
		OnTrigger(kernel, player, TriggerType.TYPE_USE_BOMB_COIN.ordinal(), "",
				(int) (player.getLong(PLAYER_PROPERTY_BOMBTOTALPLAY) - (long) oldTotalPlay), ValueType.INC.ordinal());
	}

	public void OnTotalRechargeAmountChange(IKernel kernel, IGameObject player, String name, Object old) {
		OnTrigger(kernel, player, TriggerType.TYPE_CHARGE.ordinal(), "",
				player.getInt(PLAYER_PROPERTY_TOTALRECHARGEAMOUNT) - (int) old, ValueType.INC.ordinal());
	}

	void OnPlayerLevelChange(IKernel kernel, IGameObject player, String name, Object oldLevel) {
		OnTrigger(kernel, player, TriggerType.TYPE_UP_LEVEL.ordinal(), "", player.getInt(name), ValueType.VAL.ordinal());
	}

	void OnPlayerVipLevelChange(IKernel kernel, IGameObject player, String name, Object oldLevel) {
		OnTrigger(kernel, player, TriggerType.TYPE_UP_VIP_LEVEL.ordinal(), "", player.getInt(name), ValueType.VAL.ordinal());
	}

	void OnPlayerGetAchievement(IKernel kernel, IGameObject player, String name, Object oldValue) {
		OnTrigger(kernel, player, TriggerType.TYPE_GET_ACHIEVEMENT.ordinal(), "", player.getInt(name) - (int)oldValue, ValueType. INC.ordinal());
	}

	public int GetProgress(IKernel kernel, IGameObject player, int tid) {
		if (tid == TriggerType.TYPE_UNLOCK_BV.ordinal()) {
			return m_BulletValModule.GetMaxBv(player);
		}

		return -1;
	}

	private boolean valid(String itemId, Object... objects) {
		if (m_listBomb.contains(itemId)) {
			int system = UtilFunc.System.BAG.ordinal();
			if (objects.length >= 3) {
				system = (int) objects[2];
			}
			return system != UtilFunc.System.SEND_ITEM.ordinal()
					&& system != UtilFunc.System.BOMB_AMMO_TRANSFORM.ordinal();
		}
		return true;
	}
}
