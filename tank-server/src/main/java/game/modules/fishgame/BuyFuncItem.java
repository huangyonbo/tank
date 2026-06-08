/**   
*    
* 描述：   购买功能道具并立刻使用
* 文件：BuyFuncItemModule.java
* 创建人：胡中伟
* 创建时间：2018年6月6日 上午10:16:25 
*    
*/
package game.modules.fishgame;

import com.google.protobuf.InvalidProtocolBufferException;
import framework.game.*;
import game.custommsg.CustomMsg;
import game.custommsg.RequestMsgDef;
import game.custommsg.ServerCodeDef;
import game.modules.items.ItemModule;
import game.modules.statemachine.StateMachine.State;
import game.modules.utils.UtilFunc;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * 描述：
 * 
 */
public class BuyFuncItem implements ILogicModule {
	ItemModule m_ItemModule;
//	WarningModule m_WarningModule;
	Map<String, Integer> m_mapDatas = new HashMap<>();

	/**
	 * @param kernel
	 * @return
	 */
	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regRequestMessage(RequestMsgDef.REQ_BUY_FUNCITEM.ordinal(), this, "OnReqBuyItem");
		kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");
		RefreshCfg(kernel, "res/Game/BuyFuncItem.xml");
		m_ItemModule = (ItemModule) kernel.getModule("ItemModule");
		//m_WarningModule = (WarningModule) kernel.GetModule("WarningModule");
		return true;
	}

	/**
	 * 
	 */
	@Override
	public void onDestroy() {
	}

	void RefreshCfg(IKernel kernel, String path) {
		if (path.equals("res/Game/BuyFuncItem.xml")) {
			m_mapDatas.clear();
			LoadConfig(kernel, path);
		}
	}

	boolean LoadConfig(IKernel kernel, String path) {
		ICfgReader cfg = kernel.loadXmlConfig(path);
		if (cfg == null) {
			return false;
		}
		int count = cfg.getItemCount();
		for (int i = 0; i < count; ++i) {
			String itemid = cfg.getString(i, "Item");
			int diamond = cfg.getInt(i, PLAYER_PROPERTY_DIAMOND);
			m_mapDatas.put(itemid, diamond);
		}
		return true;
	}

	public void OnReqBuyItem(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
			throws InvalidProtocolBufferException {
		CustomMsg.CommonVal reward = CustomMsg.CommonVal.parseFrom(msg);
		String itemid = reward.getValString();
		CustomMsg.ServerCode.Builder build = CustomMsg.ServerCode.newBuilder();
		do {
			if (player.getLong(PLAYER_PROPERTY_DESKID) == 0l) {
				build.setCode(ServerCodeDef.CODE_NOT_IN_GAME.ordinal());
				break;
			}
			if (!m_mapDatas.containsKey(itemid)) {
				build.setCode(ServerCodeDef.CODE_PARAM_ERR.ordinal());
				break;
			}
			long diamond = m_mapDatas.get(itemid);
			long havaDiamond = player.getLong(PLAYER_PROPERTY_DIAMOND);
			if (havaDiamond < diamond) {
				build.setCode(ServerCodeDef.CODE_NOT_ENOUGH.ordinal());
				break;
			}
			player.setProperty(PLAYER_PROPERTY_DIAMOND, havaDiamond - diamond, UtilFunc.System.BUY_FUNC_ITEM.ordinal(), "Buy item " + itemid);
			//计算道具积分 add by 胡中伟, 2019年5月10日 下午1:53:41
			String cfg = kernel.getCfgProperty(itemid, PLAYER_PROPERTY_ITEMSCORE);
			int is = 0;
			try {
				is = Integer.parseInt(cfg);
			} catch (Exception e) {
				is = 0;
			}
			if (is > 0) {
				player.setProperty(PLAYER_PROPERTY_ITEMSCORE, player.getInt(PLAYER_PROPERTY_ITEMSCORE) + is);
			}
			IGameObject desk = kernel.getGameObject(player.getLong(PLAYER_PROPERTY_DESKID));
			if (desk == null) {
				build.setCode(ServerCodeDef.CODE_FAILED.ordinal());
				kernel.response(player, reqid, build.build().toByteArray());
				return;
			}
			// 鱼阵时不能使用购买使用至尊和传说三叉戟
			if (desk.getInt("State") == State.STATE_GROUP.ordinal() && (itemid.equals("item_skill_hbomb") || itemid.equals("item_skill_nbomb"))) {
				build.setCode(ServerCodeDef.CODE_FAILED.ordinal());
				kernel.response(player, reqid, build.build().toByteArray());
				return;
			}
			m_ItemModule.AddItem(kernel, player, itemid, 1, UtilFunc.System.BUY_FUNC_ITEM.ordinal(), "Client buy");
			boolean useSuccess = m_ItemModule.UseItem(kernel, player, itemid, 1, UtilFunc.System.BUY_FUNC_ITEM.ordinal(), "Client buy and auto use");
			if (useSuccess){
				build.setCode(ServerCodeDef.CODE_SUCCESS.ordinal());
			} else {
				m_ItemModule.SubItem(kernel, player, itemid, 1, UtilFunc.System.AUTO_USE_FAILED.ordinal(), "item auto use failed");
				player.setProperty(PLAYER_PROPERTY_DIAMOND, player.getLong(PLAYER_PROPERTY_DIAMOND) + diamond);
				int curItemScore = player.getInt(PLAYER_PROPERTY_ITEMSCORE);
				// 使用失败扣除道具积分
				if (is > 0 && curItemScore - is > 0){
					player.setProperty(PLAYER_PROPERTY_ITEMSCORE, curItemScore - is);
				}
				build.setCode(ServerCodeDef.CODE_FAILED.ordinal());
			}
		} while (false);
		kernel.response(player, reqid, build.build().toByteArray());
	}

	// 虚拟购买：只扣钱，不发货，返回是否成功
	public boolean VirtualBuy(IKernel kernel, IGameObject player, String itemid, int count) {
		if (count <= 0) {
			return false;
		}
		if (!m_mapDatas.containsKey(itemid)) {
			return false;
		}
		long diamond = m_mapDatas.get(itemid) * count;
		if (player.getLong(PLAYER_PROPERTY_DIAMOND) < diamond) {
			return false;
		}
		player.setProperty(PLAYER_PROPERTY_DIAMOND, player.getLong(PLAYER_PROPERTY_DIAMOND) - diamond, UtilFunc.System.BUY_FUNC_ITEM.ordinal(), "Buy item " + itemid + "*" + count);
		//m_WarningModule.UseDiamond(player.GetInt(PLAYER_PROPERTY_UID), diamond);
		return true;
	}
}
