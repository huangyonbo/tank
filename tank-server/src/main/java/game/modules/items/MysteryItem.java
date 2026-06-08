package game.modules.items;

import framework.game.*;
import game.custommsg.CommandDef;
import game.modules.utils.ItemTipType;
import game.modules.utils.UtilFunc;

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2018/5/10.
 */
public class MysteryItem implements ILogicModule {
	public MysteryItem(IKernel kernel) {
		kernel.addClass("MysteryItem", "Item"); // 神秘礼包
	}

	class Data {
		String baseRewardPkg;
		String extremeRewardPkg;

	}

	private Map<String, Data> m_configs = new HashMap<>();
	private ItemModule m_itemModule = null;

	/**
	 * @param kernel
	 * @return
	 */
	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "MysteryItem", this, "OnItemClassCreate");

		kernel.regCommand(CommandDef.CMD_USE_ITEM.ordinal(), "MysteryItem", this, "OnUseItem");

		kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");
		RefreshCfg(kernel, "res/Items/MysteryItem.xml");

		kernel.preLoadConfig("res/Items/MysteryItem.xml");

		m_itemModule = (ItemModule) kernel.getModule("ItemModule");
		return true;
	}

	/**
	 *
	 */
	@Override
	public void onDestroy() {
	}

	void RefreshCfg(IKernel kernel, String path) {
		if (path.equals("res/Items/MysteryItem.xml")) {
			m_configs.clear();
			LoadConfig(kernel, path);
		}
	}

	public boolean LoadConfig(IKernel kernel, String path) {
		ICfgReader cfg = kernel.loadXmlConfig(path);
		int count = cfg.getItemCount();
		for (int i = 0; i < count; ++i) {
			String id = cfg.getString(i, "Id");
			String baseRewardPkg = cfg.getString(i, "BaseRewardPkg");
			String extremeRewardPkg = cfg.getString(i, "ExtremeRewardPkg");
			Data data = new Data();
			data.baseRewardPkg = baseRewardPkg;
			data.extremeRewardPkg = extremeRewardPkg;
			m_configs.put(id, data);
		}
		return true;
	}

	public void OnItemClassCreate(IKernel kernel, String script) {
	}

	public void OnUseItem(IKernel kernel, IGameObject item, Object... objects) {
		IGameObject player = (IGameObject) objects[0];
		//int count = (int) objects[1];
		String id = item.getString("Id");
		if (!m_configs.containsKey(id)) {
			return;
		}

		Data data = m_configs.get(id);
		String resBasic = "";
		String resExtreme = "";


		// 通知客户端
		if (StringUtils.isNotBlank(resExtreme)) {
			game.modules.utils.UtilFunc.sendItemTips(kernel, player, ItemTipType.TIP_PAY, resBasic, 1, resExtreme, 1);
		} else {
			game.modules.utils.UtilFunc.sendItemTips(kernel, player, ItemTipType.TIP_PAY, resBasic, 1);
		}
	}
}
