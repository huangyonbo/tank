/**   
*    
* 描述：   背包模块
* 文件：BagModule.java
* 创建人：胡中伟
* 创建时间：2018年4月8日 下午2:55:36 
*    
*/
package game.modules.player;

import framework.game.IGameObject;
import framework.game.IKernel;
import framework.game.IKernel.PlayerLogType;
import framework.game.ILogicModule;
import framework.game.KernelEvent;
import game.custommsg.CommandDef;
import game.modules.items.ItemModule;
import game.modules.utils.UtilFunc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * 描述：
 * 
 */
public class BagModule implements ILogicModule {
	private static Logger logger = LoggerFactory.getLogger(BagModule.class);

//	private WarningModule m_WarningModule;
	private ItemModule m_ItemModule;
	public static final String ITEM_BAG = "ItemBag";

	public BagModule(IKernel kernel) {
		kernel.addClass("ItemBag", "Container"); // 道具背包
	}

	/**
	 * @param kernel
	 * @return
	 */
	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_LOAD, "Player", this, "OnPlayerOnLoad");
		kernel.regEvent(KernelEvent.KEVENT_OFF_LINE, "Player", this, "OnPlayerOffLine");
		kernel.regEvent(KernelEvent.KEVENT_ON_CLASS_READY, "ItemBag", this, "OnItemBagClassReady");

		kernel.declareHeartBeat("HB_CheckItemLife", this, "OnCheckItemLife");
//		m_WarningModule = (WarningModule) kernel.GetModule("WarningModule");
		m_ItemModule = (ItemModule) kernel.getModule("ItemModule");

		return true;
	}

	/**
	 * 
	 */
	@Override
	public void onDestroy() {
	}

	public void OnItemBagClassReady(IKernel kernel, String script) {
		kernel.setVisible(script, "Capacity", false, true, true);
	}

	public void OnPlayerOnLoad(IKernel kernel, IGameObject player) {
		boolean isNew = false;
		IGameObject bag = player.getContainer(ITEM_BAG);
		if (bag == null) {
			isNew = true;
			bag = kernel.createContainer(ITEM_BAG, "ItemBag", 100, player);
			kernel.command(player, CommandDef.CMD_REG_SUCCESS.ordinal());
		}
		if (bag == null) {
			return;
		}
		int cap = bag.getCapacity();
		for (int i = 0; i < cap; ++i) {
			IGameObject item = bag.getChild(i);
			if (item == null) {
				continue;
			}

			// check autouse
			if (item.getBool("AutoUse")) {
				int count = item.getInt("Count");
				kernel.command(item, CommandDef.CMD_USE_ITEM.ordinal(), player, count);

				StringBuilder sb = new StringBuilder();
				sb.append(count).append(",").append(-count).append(",").append(0);

				kernel.addPlayerLog(player, item, PlayerLogType.DEL_ITEM.ordinal(), UtilFunc.System.BAG.ordinal(),
						sb.toString(), "use AutoUse item when load");

//				m_WarningModule.UseItem(player.GetInt(PLAYER_PROPERTY_UID), item.GetFloat("Cost") * count);
				kernel.destroyGameObject(item);
			} else {
				OnCheckItemLife(kernel, item);
			}
		}

		if (isNew) {
			//新手礼包
			m_ItemModule.AddItem(kernel, player, "item_pkg_new_user", 1, UtilFunc.System.BAG.ordinal(), "New user");
			player.setProperty(PLAYER_PROPERTY_FIRSTINROOM, 1);
		}
		player.setProperty(PLAYER_PROPERTY_ISNEW, isNew);
		//int channel = player.GetInt(PLAYER_PROPERTY_CHANNEL);
		boolean isIos = false;// channel == 151;
		player.setProperty(PLAYER_PROPERTY_ISIOS, isIos);

		player.addViewport(ViewPortDef.VP_ITEM_BAG.ordinal(), ITEM_BAG);
	}

	public void OnPlayerOffLine(IKernel kernel, IGameObject player) {
		logger.debug("OnPlayerOffLine {}", player.getString(PLAYER_PROPERTY_NAME));
	}

	public void OnCheckItemLife(IKernel kernel, IGameObject item) {
		int leftTime = item.getLeftTime();
		logger.debug("OnCheckItemLife {} {}", item.getString(PLAYER_PROPERTY_NAME), leftTime);

		if (leftTime == 0) {
			StringBuilder sb = new StringBuilder();
			sb.append(item.getInt("Count")).append(",").append(-item.getInt("Count")).append(",").append(0);

			kernel.addPlayerLog(item.getParent().getParent(), item, PlayerLogType.DEL_ITEM.ordinal(),
					UtilFunc.System.BAG.ordinal(), sb.toString(), "OnLifeEnd");

			kernel.destroyGameObject(item);
		} else if (leftTime != -1) {
			kernel.addHeartBeat("HB_CheckItemLife", item, leftTime, 1);
		}
	}
}
