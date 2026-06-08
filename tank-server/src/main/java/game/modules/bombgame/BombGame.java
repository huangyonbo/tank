package game.modules.bombgame;

import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import enums.PlayerPlace;
import framework.game.*;
import game.custommsg.CommandDef;
import game.custommsg.CustomMsg;
import game.custommsg.RequestMsgDef;
import game.custommsg.ServerCodeDef;
import game.modules.RoomModule;
import game.modules.TimerManager;
import game.modules.fishgame.BulletValModule;
import game.modules.fishgame.FishModule;
import game.modules.items.ItemModule;
import game.modules.utils.ItemTipType;
import game.modules.utils.UtilFunc;
import game.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 兑换bombCoin
 */
public class BombGame implements ILogicModule{

	private static Logger logger = LoggerFactory.getLogger(BombGame.class);

	private ItemModule m_itemModule;
	private Map<String, Integer> m_mapItemPrice = new HashMap<>();
	private Map<Integer, BvSwitch> m_mapBvSwitch = new HashMap<>();
	private Map<Integer, String> m_nBulletUpRate = new HashMap<>();
	private Map<String, String[]> m_transformCount = new HashMap<>();
	private Random random = new Random();
	FishModule m_fishModule;
	private TimerManager timerManager;
	BulletValModule m_BulletValModule;
	public static final String ITEM_SKILL_H_BOMB = "item_skill_hbomb";//传说三叉戟
//	private final String ITEM_SKILL_N_BOMB = "item_skill_nbomb";//至尊三叉戟
	private  int TransformVipLimit = 0;

	class BvSwitch{
		private int level;
		private float rate;
		private int diamond;
		private int purple;
		private int yellow;
		private int red;
	}


	@Override
	public boolean onInit(IKernel kernel) {
		// kernel.RegEvet(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
		kernel.regEvent(KernelEvent.KEVENT_ON_LINE, "Player", this, "OnPlayerOnLine");
		kernel.regRequestMessage(RequestMsgDef.REQ_BOMB_TO_AMMO.ordinal(), this, "OnReqBomb2Ammo");//弹头转弹药
		kernel.regRequestMessage(RequestMsgDef.REQ_AMMO_TO_BOMB.ordinal(), this, "OnReqAmmo2Bomb");//弹药转弹头
		kernel.regRequestMessage(RequestMsgDef.REQ_SMITHING.ordinal(), this, "OnReqSmithing");//炮台锻造
		kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");
		kernel.listenPropertyChange(PLAYER_PROPERTY_VIPLEVEL, "Player", this, "OnVipLevelChanged");
		kernel.regCommand(CommandDef.CMD_CLEAR_BOMB_2_BOMBCOIN.ordinal(), "Player", this, "OnRecvClearBomb2Ammo");
		m_itemModule = (ItemModule) kernel.getModule("ItemModule");
		m_fishModule = (FishModule)kernel.getModule("FishModule");
		timerManager = (TimerManager)kernel.getModule("TimerManager");
		m_BulletValModule = (BulletValModule)kernel.getModule("BulletValModule");
		timerManager.addChangeDayCallBack(this, "OnChangeDay");
		if (!LoadStoreStoneConfig(kernel, "res/StoreItems/StoreStone.xml")){
			return false;
		}
		RefreshCfg(kernel, "res/Items/SkillItem.xml");
		return LoadNBulletValueConfig(kernel, "res/NBulletValue/BvUnlock.xml");
	}

	@Override
	public void onDestroy() {
	}

	void OnRecvClearBomb2Ammo(IKernel kernel, IGameObject player, Object... objects){
		player.setProperty("TodayTransform", 0);
	}

	void OnChangeDay(IKernel kernel, int day) {
		kernel.commandAllPlayer(CommandDef.CMD_CLEAR_BOMB_2_BOMBCOIN.ordinal());
	}

	public void OnPlayerClassCreate(IKernel kernel, String script)
	{
		kernel.declareProperty(script, PLAYER_PROPERTY_BOMB_COIN, ValueType.LONG, true, true, true);	// 弹药
		kernel.declareProperty(script, PLAYER_PROPERTY_BOMB_ITEM, ValueType.INT, false, true, true);	// 弹药
		kernel.declareProperty(script, "TodayTransform", ValueType.INT, true, true, true);	// 今日弹头转弹药数量
		kernel.declareProperty(script, PLAYER_PROPERTY_NBULLETLEVEL, ValueType.INT, true, true, true);	// 核弹炮台
		kernel.declareProperty(script, PLAYER_PROPERTY_NBULLETLEVEL_RATE, ValueType.STRING, true, true, false);
		IRecord rec = kernel.declareRecord(script, "TransformRecord", 4, 100, false, true, true);
		rec.setColType(0,ValueType.STRING); //id  道具id
		rec.setColType(1,ValueType.INT); //doneCount 已转换次数
		rec.setColType(2,ValueType.INT); // leftCount 剩余次数
		rec.setColType(3,ValueType.LONG);  // time 转换时间记录
	}

	void OnVipLevelChanged(IKernel kernel, IGameObject player, String name, Object oldAmount) {
		IRecord rec = player.getRecord("TransformRecord");
		int vipLv = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
		int TotalCount = Integer.parseInt(m_transformCount.get("item_skill_hbomb")[vipLv]);
		int n_row = rec.findRow(0, 0, "item_skill_nbomb");
		int h_row = rec.findRow(0, 0, "item_skill_hbomb");
		if (TotalCount == -1) {
			rec.setValue(n_row,2,-1);
			rec.setValue(h_row,2,-1);
			if (!TimeUtils.isSameDay(rec.getLong(n_row, 3), kernel.getServerTime())) {
				rec.setValue(n_row, 1, 0);
			}
			if (!TimeUtils.isSameDay(rec.getLong(h_row, 3), kernel.getServerTime())) {
				rec.setValue(h_row, 1, 0);
			}
			return;
		}
		int n_doneCount = 0, h_doneCount = 0;
		if (n_row != -1) {
			n_doneCount = rec.getInt(n_row, 1);
		}
		if (h_row != -1) {
			h_doneCount = rec.getInt(h_row, 1);
		}
		int leftCount = TotalCount*2 - h_doneCount*2 - n_doneCount;
		if (leftCount >= 0) {
			rec.setValue(n_row, 2, leftCount);
			rec.setValue(h_row, 2, leftCount/2);
		}

	}

	void OnPlayerOnLine(IKernel kernel, IGameObject player) {
		SetBombItemCoutn(kernel,player);
		int nBulletLevel = player.getInt(PLAYER_PROPERTY_NBULLETLEVEL);
		if (nBulletLevel == 0){
			player.setProperty(PLAYER_PROPERTY_NBULLETLEVEL, 1);
		}
		String rate = m_nBulletUpRate.containsKey(nBulletLevel+1) ? m_nBulletUpRate.get(nBulletLevel+1) : "0%";
		player.setProperty(PLAYER_PROPERTY_NBULLETLEVEL_RATE, rate); // 更新升到炮台的成功率
		long offLineTime = player.getLong(PLAYER_PROPERTY_OFFLINETIME);
		if (offLineTime == 0l){
			offLineTime = kernel.getServerTime();
		}
		if (UtilFunc.getZeroTime(offLineTime) != UtilFunc.getZeroTime(kernel.getServerTime())){
			player.setProperty(" ", 0);
		}
		IRecord rec = player.getRecord("TransformRecord");
		if (rec.findRow(0, 0, "item_skill_nbomb") == -1 && rec.findRow(0, 0, "item_skill_hbomb") == -1) {
			rec.addRow("item_skill_nbomb",0,0,kernel.getServerTime());
			rec.addRow("item_skill_hbomb",0,0,kernel.getServerTime());
		}
		int vipLv = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
		int n_row = rec.findRow(0, 0, "item_skill_nbomb");
		int h_row = rec.findRow(0, 0, "item_skill_hbomb");
		int TotalCount = Integer.parseInt(m_transformCount.get("item_skill_hbomb")[vipLv]);
		if (TotalCount == -1) {
			rec.setValue(n_row,2,-1);
			rec.setValue(h_row,2,-1);
			if (!TimeUtils.isSameDay(rec.getLong(n_row, 3), kernel.getServerTime())) {
				rec.setValue(n_row, 1, 0);
			}
			if (!TimeUtils.isSameDay(rec.getLong(h_row, 3), kernel.getServerTime())) {
				rec.setValue(h_row, 1, 0);
			}
			return;
		}
		int n_doneCount = 0, h_doneCount = 0;
		if (n_row != -1) {
			n_doneCount = rec.getInt(n_row, 1);
		}
		if (h_row != -1) {
			h_doneCount = rec.getInt(h_row, 1);
		}
		int leftCount = TotalCount*2 - n_doneCount - h_doneCount*2;
		if (n_row != -1) {
			if (!TimeUtils.isSameDay(rec.getLong(n_row, 3), kernel.getServerTime())) {
				rec.setValue(n_row, 1, 0);
				rec.setValue(n_row, 2, TotalCount * 2);
			} else {
				rec.setValue(n_row, 2, leftCount);
			}
		}
		if (h_row != -1) {
			if (!TimeUtils.isSameDay(rec.getLong(h_row, 3), kernel.getServerTime())) {
				rec.setValue(h_row,1,0);
				rec.setValue(h_row,2,TotalCount);
			} else {
				rec.setValue(h_row, 2, leftCount/2);
			}
		}
	}
	public void SetBombItemCoutn(IKernel kernel,IGameObject player){
		int itemCount = m_itemModule.GetItemCount(kernel,player, ITEM_SKILL_H_BOMB);
		player.setProperty(PLAYER_PROPERTY_BOMB_ITEM, itemCount);
		logger.info("设置玩家 {} 炸弹数量 {},当前数量 {}",player.getProperty(PLAYER_PROPERTY_UID),itemCount,player.getProperty(PLAYER_PROPERTY_BOMB_ITEM));
		player.setProperty(PLAYER_PROPERTY_BOMB_ITEM, itemCount);
	}

	void OnReqBomb2Ammo(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
			throws InvalidProtocolBufferException {
        JsonObject json = new JsonObject();
        int place = player.getInt(PLAYER_CURRENT_PLACE);
        if (place == PlayerPlace.PLACE_2.getId() || place == PlayerPlace.PLACE_3.getId() || place == PlayerPlace.PLACE_4.getId()) {
            json.addProperty("code", ServerCodeDef.CODE_IN_OTHER_GAME.ordinal());
            UtilFunc.respRpcStringToClient(kernel, player, reqid, json.toString());
            return;
        }
		CustomMsg.BombAmmoTransform msgData = CustomMsg.BombAmmoTransform.parseFrom(msg);
		if (!msgData.hasAmount() || !msgData.hasItemId()){
			json.addProperty("code",ServerCodeDef.CODE_NOT_ENOUGH.ordinal());
			UtilFunc.respRpcStringToClient(kernel,player, reqid, json.toString());
			return;
		}
		
		int amount = msgData.getAmount();
		String itemid = msgData.getItemId();
		long serverTime = kernel.getServerTime();
		
		// 不是至尊或者传说 无法转换成魔晶
//		if (!itemid.equals("item_skill_hbomb") && !itemid.equals("item_skill_nbomb")){
//			json.addProperty("code",ServerCodeDef.CODE_NOT_ENOUGH.ordinal());
//			UtilFunc.respRpcStringToClient(kernel,player, reqid, json.toString());
//			return;
//		}
		
		if (amount <= 0) {
			json.addProperty("code",ServerCodeDef.CODE_NOT_ENOUGH.ordinal());
			UtilFunc.respRpcStringToClient(kernel,player, reqid, json.toString());
			return;
		}
		if (amount > m_itemModule.GetItemCount(kernel, player, itemid)){
			json.addProperty("code",ServerCodeDef.CODE_NEED_ITEM.ordinal());
			UtilFunc.respRpcStringToClient(kernel,player, reqid, json.toString());
			return;
		}

		//每天转换的次数限制
		int vipLv = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
		int TotalCount = Integer.parseInt(m_transformCount.get("item_skill_hbomb")[vipLv]);
		if (TotalCount == 0 ) {
			json.addProperty("code",ServerCodeDef.CODE_VIP_LIMIT.ordinal());
			json.addProperty("msg","无法转换魔晶，贵族"+ TransformVipLimit + "解锁海神殿玩法");
			UtilFunc.respRpcStringToClient(kernel,player, reqid, json.toString());
			return;
		}
		IRecord rec = player.getRecord("TransformRecord");
		int row = rec.findRow(0, 0, itemid);
		if (row != -1) {
			if (TotalCount != -1) {
				if (itemid.equals("item_skill_nbomb")) {
					int n_doneCount = rec.getInt(row, 1);
					int row_ = rec.findRow(0, 0, "item_skill_hbomb");
					int h_doneCount = 0;
					if (row_ != -1) {
						h_doneCount = rec.getInt(row_, 1);
					}
					int leftCount = TotalCount*2 - n_doneCount - amount - h_doneCount*2;
					if (leftCount >= 0 && TimeUtils.isSameDay(rec.getLong(row, 3), serverTime)) {
						rec.setValue(row, 1, n_doneCount + amount);
						rec.setValue(row,2, leftCount);
						rec.setValue(row_,2, leftCount/2);
						rec.setValue(row,3, serverTime);

					} else if (leftCount < 0 && TimeUtils.isSameDay(rec.getLong(row, 3), serverTime)) {
						json.addProperty("code", ServerCodeDef.CODE_COUNT_LIMIT.ordinal());
						UtilFunc.respRpcStringToClient(kernel, player, reqid, json.toString());
						return;
					} else {
						rec.setValue(row, 1, 0);
						rec.setValue(row, 3, serverTime);
						int leftCount_ =  TotalCount*2 - amount - h_doneCount*2;
						if (leftCount_ >= 0) {
							rec.setValue(row, 1, amount);
							rec.setValue(row,2, leftCount_);
							rec.setValue(row_,2, leftCount_/2);
						} else {
							json.addProperty("code", ServerCodeDef.CODE_COUNT_LIMIT.ordinal());
							UtilFunc.respRpcStringToClient(kernel, player, reqid, json.toString());
							return;
						}
					}
				} else {
					int h_doneCount = rec.getInt(row, 1);
					int row_ = rec.findRow(0, 0, "item_skill_nbomb");
					int n_doneCount = 0;
					if (row_ != -1) {
						n_doneCount = rec.getInt(row_, 1);
					}
					int leftCount = TotalCount*2 - n_doneCount - (h_doneCount + amount)*2;
					if (leftCount >= 0 && TimeUtils.isSameDay(rec.getLong(row, 3), serverTime)) {
						rec.setValue(row, 1, h_doneCount + amount);
						rec.setValue(row,2, leftCount/2);
						rec.setValue(row_,2, leftCount);
						rec.setValue(row,3, serverTime);
					} else if (leftCount < 0 && TimeUtils.isSameDay(rec.getLong(row, 3), serverTime)) {
						json.addProperty("code", ServerCodeDef.CODE_COUNT_LIMIT.ordinal());
						UtilFunc.respRpcStringToClient(kernel, player, reqid, json.toString());
						return;
					} else {
						rec.setValue(row, 1, 0);
						rec.setValue(row, 3, serverTime);
						int leftCount_ = TotalCount*2 - n_doneCount - amount*2;
						if (leftCount_ >= 0){
							rec.setValue(row, 1, amount);
							rec.setValue(row,2, leftCount_ /2);
							rec.setValue(row_,2, leftCount_);
						} else {
							json.addProperty("code", ServerCodeDef.CODE_COUNT_LIMIT.ordinal());
							UtilFunc.respRpcStringToClient(kernel, player, reqid, json.toString());
							return;
						}
					}
				}
			} else {
				rec.setValue(row,1,amount+rec.getInt(row,1));
				rec.setValue(row,2,-1);
				rec.setValue(row,3,serverTime);
			}
		} else {
			logger.info("Not Found ItemID");
			json.addProperty("code",ServerCodeDef.CODE_NOT_EXIST.ordinal());
			UtilFunc.respRpcStringToClient(kernel,player, reqid, json.toString());
			return;
		}
		m_itemModule.SubItem(kernel, player, itemid, amount, UtilFunc.System.BOMB_AMMO_TRANSFORM.ordinal(), "bomb transform to ammo");
//		ItemLogModule.AddItemLog(kernel,player,itemid,amount, ItemLogEnum.BOMB_TRANSFORM_TO_AMMO.ordinal());
		player.setProperty(PLAYER_PROPERTY_BOMB_COIN,
				player.getLong(PLAYER_PROPERTY_BOMB_COIN) + 10000L * amount * m_mapItemPrice.get(itemid));
		json.addProperty("code",ServerCodeDef.CODE_SUCCESS.ordinal());
		SetBombItemCoutn(kernel,player);
		UtilFunc.respRpcStringToClient(kernel,player, reqid, json.toString());
	}

	void OnReqAmmo2Bomb(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
			throws InvalidProtocolBufferException {
        CustomMsg.BombAmmoTransform msgData = CustomMsg.BombAmmoTransform.parseFrom(msg);
        CustomMsg.ServerCode.Builder build = CustomMsg.ServerCode.newBuilder();
        int place = player.getInt(PLAYER_CURRENT_PLACE);
        if (place == PlayerPlace.PLACE_2.getId() || place == PlayerPlace.PLACE_3.getId() || place == PlayerPlace.PLACE_4.getId()) {
            build.setCode(ServerCodeDef.CODE_IN_OTHER_GAME.ordinal());
            kernel.response(player, reqid, build.build().toByteArray());
            return;
        }
		if (!msgData.hasAmount() || !msgData.hasItemId()){
			build.setCode(ServerCodeDef.CODE_NOT_ENOUGH.ordinal());
			kernel.response(player, reqid, build.build().toByteArray());
			return;
		}
		int count = msgData.getAmount();
		if (count <= 0) {
			build.setCode(ServerCodeDef.CODE_PARAM_ERR.ordinal());
			kernel.response(player, reqid, build.build().toByteArray());
			return;
		}
		long ammo  = player.getLong(PLAYER_PROPERTY_BOMB_COIN);
		long price = m_mapItemPrice.get(msgData.getItemId()) * 10000L;
		long need  = price * count;
		if (ammo < need){
			build.setCode(ServerCodeDef.CODE_NEED_PRO.ordinal());
			kernel.response(player, reqid, build.build().toByteArray());
			return;
		}
		ammo -= need;
		player.setProperty(PLAYER_PROPERTY_BOMB_COIN, ammo);
		List<Object> list = new ArrayList<>();
		if (msgData.getItemId().equals(ITEM_SKILL_H_BOMB)){
			m_itemModule.AddItem(kernel, player, ITEM_SKILL_H_BOMB, count, UtilFunc.System.AMMO_TRANSFORM_TO_BOMB.ordinal(), "ammo transform to hbomb");
//			ItemLogModule.AddItemLog(kernel, player, ITEM_SKILL_H_BOMB, count, ItemLogEnum.AMMO_TRANSFORM_TO_BOMB.ordinal());
			list.add(ITEM_SKILL_H_BOMB);
			list.add(count);
		}
//		if (msgData.getItemId().equals(ITEM_SKILL_N_BOMB)) {
//			m_itemModule.AddItem(kernel, player, ITEM_SKILL_N_BOMB, count, UtilFunc.System.AMMO_TRANSFORM_TO_BOMB.ordinal(), "ammo transform to nbomb");
////			ItemLogModule.AddItemLog(kernel, player, ITEM_SKILL_N_BOMB, count, ItemLogEnum.AMMO_TRANSFORM_TO_BOMB.ordinal());
//			list.add(ITEM_SKILL_N_BOMB);
//			list.add(count);
//		}
		UtilFunc.sendItemTips(kernel, player, ItemTipType.TIP_TRANSFORM_SUCCESS, list.toArray(new Object[list.size()]));
		build.setCode(ServerCodeDef.CODE_SUCCESS.ordinal());
		kernel.response(player, reqid, build.build().toByteArray());
		SetBombItemCoutn(kernel,player);
	}

	void OnReqSmithing(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg) {
		int bulletLevel = player.getInt(PLAYER_PROPERTY_NBULLETLEVEL);
		CustomMsg.ServerCode.Builder build = CustomMsg.ServerCode.newBuilder();
		bulletLevel = bulletLevel+1;
		if (!m_mapBvSwitch.containsKey(bulletLevel)){
			build.setCode(ServerCodeDef.CODE_LEVEL_MAX.ordinal());
			kernel.response(player, reqid, build.build().toByteArray());
			return;
		}
		BvSwitch bvSwitch = m_mapBvSwitch.get(bulletLevel);
		//材料够不够
		if (player.getLong(PLAYER_PROPERTY_DIAMOND) < bvSwitch.diamond
				|| m_itemModule.GetItemCount(kernel, player, "item_PurpleWafer") < bvSwitch.purple
				|| m_itemModule.GetItemCount(kernel, player, "item_YellowWafer") < bvSwitch.yellow
				|| m_itemModule.GetItemCount(kernel, player, "item_RedWafer") < bvSwitch.red){
			build.setCode(ServerCodeDef.CODE_NEED_PRO.ordinal());
			kernel.response(player, reqid, build.build().toByteArray());
			return;
		}

		//扣除相关材料
		player.setProperty(PLAYER_PROPERTY_DIAMOND, player.getLong(PLAYER_PROPERTY_DIAMOND)-bvSwitch.diamond);
		m_itemModule.SubItem(kernel, player, "item_PurpleWafer", bvSwitch.purple, UtilFunc.System.N_BOMB_SMITH_ING.ordinal(), "NBomb Smithing");
		m_itemModule.SubItem(kernel, player, "item_YellowWafer", bvSwitch.yellow, UtilFunc.System.N_BOMB_SMITH_ING.ordinal(), "NBomb Smithing");
		m_itemModule.SubItem(kernel, player, "item_RedWafer", bvSwitch.red, UtilFunc.System.N_BOMB_SMITH_ING.ordinal(), "NBomb Smithing");
		if (random.nextFloat() > bvSwitch.rate){
			build.setCode(ServerCodeDef.CODE_FAILED.ordinal());
			kernel.response(player, reqid, build.build().toByteArray());
			return;
		}
		player.setProperty(PLAYER_PROPERTY_NBULLETLEVEL, bulletLevel);
		IGameObject desk = kernel.getGameObject(player.getLong(PLAYER_PROPERTY_DESKID));
		if (desk != null) {
			// 解锁后自动提升炮值
			IGameObject room = desk.getParent();

			int roomType = room.getInt(DESK_TYPE_KEY);
			if (RoomModule.isNuclear(roomType)){
				//海神殿升级炮台才自动修改炮值
				int roomMax = desk.getInt("MaxBV");
				int value = m_BulletValModule.GetNMaxBv(player);
				int multi = player.getInt(PLAYER_PROPERTY_SKILLMULTIPLE);
				if (multi <= 0) {
					multi = 1;
				}
				if (value > roomMax) {
					value = roomMax;
				}
				player.setProperty(PLAYER_PROPERTY_BULLETVALUE,value * multi);
			}
		}
		String rate = m_nBulletUpRate.getOrDefault(bulletLevel + 1, "0%");
		player.setProperty(PLAYER_PROPERTY_NBULLETLEVEL_RATE, rate); // 更新升到炮台的成功率
		build.setCode(ServerCodeDef.CODE_SUCCESS.ordinal());
		kernel.response(player, reqid, build.build().toByteArray());
	}

	boolean LoadStoreStoneConfig(IKernel kernel, String path) {
		ICfgReader cfg = kernel.loadXmlConfig(path);
		if(cfg == null) {
			return false;
		}

		int count = cfg.getItemCount();
		for(int i = 0; i < count; ++i) {
			String id = cfg.getString(i, "Id");
			int price = cfg.getInt(i, "Price");
			m_mapItemPrice.put(id, price);
		}
		return true;
	}

	void RefreshCfg(IKernel kernel, String path) {
		if (!path.equals("res/Items/SkillItem.xml")){
			return ;
		}
		ICfgReader cfg = kernel.loadXmlConfig(path);
		if (cfg == null) {
			return ;
		}
		int count = cfg.getItemCount();
		for(int i = 0; i < count; ++i) {
 			String id = cfg.getString(i, "Id");
 			String[] transformCounts = cfg.getString(i, "TransformCount").split(",");
   			if (transformCounts[0].equals("") ) continue;
 			for (int j = 0; j < transformCounts.length; j++) {
				if (!id.equals("item_skill_hbomb"))
					break;
				if (!transformCounts[j].equals("0")) {
    						TransformVipLimit = j;
    						break;
				}
			}
			m_transformCount.put(id, transformCounts);
		}

	}


	boolean LoadNBulletValueConfig(IKernel kernel, String path) {
		ICfgReader cfg = kernel.loadXmlConfig(path);
		if(cfg == null) {
			return false;
		}
		int count = cfg.getItemCount();
		for(int i = 0; i < count; ++i) {
			BvSwitch bvSwitch = new BvSwitch();
			bvSwitch.level = cfg.getInt(i, "Id");
			bvSwitch.rate = cfg.getFloat(i, "Rate");
			bvSwitch.diamond = cfg.getInt(i, PLAYER_PROPERTY_DIAMOND);
			String[] materials = cfg.getString(i, "Material").split(",");
			bvSwitch.purple = (materials.length > 0 ? Integer.parseInt(materials[0]) : 0);
			bvSwitch.yellow = (materials.length > 1 ? Integer.parseInt(materials[1]) : 0);
			bvSwitch.red = (materials.length > 2 ? Integer.parseInt(materials[2]) : 0);
			m_mapBvSwitch.put(bvSwitch.level, bvSwitch);

			String _rate = (int)(bvSwitch.rate * 100) + "%";
			m_nBulletUpRate.put(bvSwitch.level, _rate);
		}
		return true;
	}
}
