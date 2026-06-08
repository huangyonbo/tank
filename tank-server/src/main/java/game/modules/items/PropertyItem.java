/**   
*    
* 描述：   属性道具
* 文件：PropertyItem.java
* 创建人：胡中伟
* 创建时间：2018年4月9日 下午12:01:08 
*    
*/
package game.modules.items;

import common.ServerMsgDef;
import framework.ByteUtils;
import framework.game.*;
import game.custommsg.CommandDef;
import game.modules.player.VipModule;
import game.modules.utils.UtilFunc;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * 描述：
 * 
 */
@Slf4j
public class PropertyItem implements ILogicModule {
	VipModule m_VipModule;

	public PropertyItem(IKernel kernel) {
		kernel.addClass("PropertyItem", "Item"); // 属性道具
	}

	private static final String PROPERTY_ADDITION_KEEP_TIME = PLAYER_PROPERTY_ADDITIONKEEPTIME;
	private static final String PROPERTY_ADDITION_EXPIRE_TIME = PLAYER_PROPERTY_ADDITIONEXPIRETIME;
	private static final String PROPERTY_DISCOUNTS_KEEP_TIME = PLAYER_PROPERTY_DISCOUNTSKEEPTIME;
	public static final String PROPERTY_DISCOUNTS_EXPIRE_TIME = PLAYER_PROPERTY_DISCOUNTSEXPIRETIME;
	private static final String PROPERTY_TOTAL_COLOR_TICKET_EXCLUDE_AC = "TotalColorTicketExcludeAC";
	private static final String PROPERTY_TOTAL_COLOR_TICKET_FOR_AC = PLAYER_PROPERTY_TOTALCOLORTICKETFORAC;

	private final Map<String, XmlPropertyItem> propertyItemMap = new HashMap<>();

	public Map<String, XmlPropertyItem> getPropertyItemMap() {
		return propertyItemMap;
	}
	/**
	 * @param kernel
	 * @return
	 */
	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "PropertyItem", this, "OnItemClassCreate");
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
		kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "Player", this, "OnRefreshCfg");
		kernel.regCommand(CommandDef.CMD_USE_ITEM.ordinal(), "PropertyItem", this, "OnUseItem");
		kernel.regCommand(CommandDef.CMD_SUB_PROPERTY_ITEM.ordinal(), "PropertyItem", this, "OnSubPropertyItem");
		kernel.preLoadConfig("res/Items/PropertyItem.xml");
		kernel.listenPropertyChange(PROPERTY_ADDITION_KEEP_TIME, "Player", this, "OnPlayerProChanged");
		kernel.listenPropertyChange(PROPERTY_DISCOUNTS_KEEP_TIME, "Player", this, "OnPlayerProChanged");
		kernel.regServerRequest(ServerMsgDef.B2G_REQ_PROPERTY_ITEM.ordinal(), this, "OnGetPropertyItem");
		OnRefreshCfg(kernel, "res/Items/PropertyItem.xml");

		return true;
	}

	/**
	 * 
	 */
	@Override
	public void onDestroy() {
	}

	public void OnItemClassCreate(IKernel kernel, String script) {
		kernel.declareProperty(script, "Property", ValueType.STRING, false, false, false);
		kernel.declareProperty(script, "Value", ValueType.INT, false, false, false);
	}

	public void OnPlayerClassCreate(IKernel kernel, String script) {
		kernel.declareProperty(script, PROPERTY_ADDITION_KEEP_TIME, ValueType.INT, false, false, false);// 灵石积分加成保持时间增加
		kernel.declareProperty(script, PROPERTY_ADDITION_EXPIRE_TIME, ValueType.LONG, false, true, true);// 灵石积分加成有效期
		kernel.declareProperty(script, PROPERTY_DISCOUNTS_KEEP_TIME, ValueType.INT, false, false, false);// 灵石兑换优惠保持时间增加
		kernel.declareProperty(script, PROPERTY_DISCOUNTS_EXPIRE_TIME, ValueType.LONG, false, true, true);// 灵石兑换优惠有效期
		kernel.declareProperty(script, PROPERTY_TOTAL_COLOR_TICKET_FOR_AC, ValueType.LONG, false, false, true);// 活动获得话费券数量
	}

	void OnRefreshCfg(IKernel kernel, String path) {
		ICfgReader reader = kernel.loadXmlConfig(path);
		if (reader == null) {
			return;
		}
		propertyItemMap.clear();
		int itemCount = reader.getItemCount();
		for (int i = 0; i < itemCount; i++) {
			XmlPropertyItem propertyItem = new XmlPropertyItem();
			propertyItem.setId(reader.getString(i, "Id"));
			propertyItem.setName(reader.getString(i, "Name"));
			propertyItem.setProperty(reader.getString(i, "Property"));
			propertyItemMap.put(propertyItem.getId(), propertyItem);
		}
	}

	public void OnPlayerProChanged(IKernel kernel, IGameObject player, String proName, Object oldVal) {
		if (PROPERTY_ADDITION_KEEP_TIME.equals(proName)) {
			int old = (int) oldVal;
			int cur = player.getInt(PROPERTY_ADDITION_KEEP_TIME);
			if (cur > old) {
				long expireTime = player.getLong(PROPERTY_ADDITION_EXPIRE_TIME);
				long currentTime = kernel.getServerTime();
				long addBase = (currentTime > expireTime ? currentTime : expireTime);
				player.setProperty(PROPERTY_ADDITION_EXPIRE_TIME, addBase + (cur - old) * 3600000L);
			}
		} else if (PROPERTY_DISCOUNTS_KEEP_TIME.equals(proName)) {
			int old = (int) oldVal;
			int cur = player.getInt(PROPERTY_DISCOUNTS_KEEP_TIME);
			if (cur > old) {
				long expireTime = player.getLong(PROPERTY_DISCOUNTS_EXPIRE_TIME);
				long currentTime = kernel.getServerTime();
				long addBase = (currentTime > expireTime ? currentTime : expireTime);
				player.setProperty(PROPERTY_DISCOUNTS_EXPIRE_TIME, addBase + (cur - old) * 3600000L);
			}
		}
	}

	void OnUseItem(IKernel kernel, IGameObject item, Object... objects) {
		IGameObject player = (IGameObject) objects[0];
		int count = (int) objects[1];
		int system = (int) objects[2];
		//属于点券购买商品  金币和钻石

		String name = item.getString("Property");
//		long value  = item.getInt("Value") * count;
		String itemId = item.getString("Id");
		long value  = item.getInt("Value") * count;
//		double effect = 1.0d;
//		if (system==UtilFunc.System.GET_GOODS_COUPONS.ordinal()&&(itemId.contains("gold")|| itemId.contains("diamond"))){
//			JSONObject playerLog = kernel.getPlayerLog(player);
//			if (itemId.contains("gold")){
//				effect=new BigDecimal(player.getDouble("ShangGuBloodGoldEffect")).add(new BigDecimal(effect)).doubleValue();
//			}else if(itemId.contains("diamond")){
//				effect=new BigDecimal(player.getDouble("TaiGuBloodDiamondEffect")).add(new BigDecimal(effect)).doubleValue();
//			}
//			value=new BigDecimal(value).multiply(new BigDecimal(effect+"")).longValue();
//			playerLog.put("itemId",itemId);
//			playerLog.put("effect",effect);
//			playerLog.put("value",value);
//			log.info("商城购买货币{} {}",itemId,playerLog.toString());
//		}
		if (name.equals(PLAYER_PROPERTY_COLORTICKET)) {
			player.setProperty(PLAYER_PROPERTY_TOTALCOLORTICKET, player.getLong(PLAYER_PROPERTY_TOTALCOLORTICKET) + value);
			// 话费券获得记录[指定系统] add by 赵俊@2019/8/26 10:56
			if (system == UtilFunc.System.BULLET_VALUE.ordinal() || system == UtilFunc.System.COLOR_DRAW.ordinal()
					|| system == UtilFunc.System.KILL_FISH.ordinal() || system == UtilFunc.System.SIGN_IN.ordinal()
					|| system == UtilFunc.System.STORE_CONTROL.ordinal()) {
				player.setProperty(PROPERTY_TOTAL_COLOR_TICKET_EXCLUDE_AC, player.getLong(PROPERTY_TOTAL_COLOR_TICKET_EXCLUDE_AC) + value);
			} else if (system == UtilFunc.System.ACTIVITY.ordinal()) {
				player.setProperty(PROPERTY_TOTAL_COLOR_TICKET_FOR_AC, player.getLong(PROPERTY_TOTAL_COLOR_TICKET_FOR_AC) + value);
			}
		} else if (name.equals(PLAYER_PROPERTY_SPIRITSTONESCORE)) {
//			// 火龙助力礼包加成 alter by 赵俊@20190705
//			float fieryDragonPkgAddition = (player.getLong(PROPERTY_ADDITION_EXPIRE_TIME) > kernel.getServerTime() ? 0.2f : 0f);
//			float vipAddition = m_VipModule.GetStoneScoreAddition(player);
//			int base = value;
//			int fieryDragon = (int) (value * fieryDragonPkgAddition);
//			int vip = (int) (value * vipAddition);
//			value = base + fieryDragon + vip;
//			IGameObject desk = kernel.getGameObject(player.getLong(PLAYER_PROPERTY_DESKID));
//			if (desk != null) {
//				CustomMsg.SpiritScore.Builder builder = CustomMsg.SpiritScore.newBuilder();
//				builder.setBase(base);
//				builder.setVip(vip);
//				builder.setFieryDragon(fieryDragon);
//				builder.setSeatId(player.getShort(PLAYER_PROPERTY_SEATID));
//				kernel.broadCastByDesk(desk, S2CMsgDef.S2C_SPIRIT_SCORE.ordinal(), builder.build().toByteArray());
//			}
		}

		if (player.getProType(name) == ValueType.LONG) {
			player.setProperty(name, player.getLong(name) + value);
		} else {
			player.setProperty(name, player.getInt(name) + value);
		}
	}

	void OnSubPropertyItem(IKernel kernel, IGameObject item, Object...objects){
		IGameObject player = (IGameObject) objects[0];
		int count = (int) objects[1];
		String name = item.getString("Property");

		int value = item.getInt("Value") * count;
		if (player.getProType(name) == ValueType.LONG) {
			player.setProperty(name, player.getLong(name) - value);
		} else {
			player.setProperty(name, player.getInt(name) - value);
		}
	}

	void OnGetPropertyItem(IKernel kernel, int reqId, byte[] msg) throws Exception {
		String itemId = new String(msg);
		XmlPropertyItem item = propertyItemMap.get(itemId);
		if (item == null) {
			kernel.responseServer(reqId, new byte[0]);
			return;
		}
		kernel.responseServer(reqId, ByteUtils.objectToByte(item));
	}
}
