package game.modules.items;

import com.google.protobuf.InvalidProtocolBufferException;
import framework.game.*;
import game.custommsg.C2SMsgDef;
import game.custommsg.CustomMsg;
import game.modules.player.BagModule;
import game.modules.utils.UtilFunc;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// 实物道具
@Slf4j
public class RealItem implements ILogicModule {
	ItemModule m_ItemModule = null;

	private static Map<String,String> realItemMap=new HashMap<>();

	public RealItem(IKernel kernel) {
		kernel.addClass("RealItem", "Item");
	}

	@Data
	class RealItems{
		String id;
		String name;
		long expire;
		long cost;
		boolean virtual;
	}

	@Override
	public boolean onInit(IKernel kernel) {
		//kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "CardItem", this, "OnItemClassCreate");
		//kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");
		//kernel.preLoadConfig("res/Items/RealItem.xml");
		//kernel.regClientMessage(C2SMsgDef.C2S_USE_CARD.ordinal(), this, "OnUseCard");
		m_ItemModule = (ItemModule) kernel.getModule("ItemModule");
		RefreshCfg(kernel,"res/Items/RealItem.xml");
		return true;
	}

	void RefreshCfg(IKernel kernel, String path) {
		if (path.equals("res/Items/RealItem.xml")) {
			realItemMap.clear();
			LoadConfig(kernel, path);
		}
	}

	public boolean LoadConfig(IKernel kernel, String path) {
		ICfgReader cfg = kernel.loadXmlConfig(path);
		int count = cfg.getItemCount();
		for (int i = 0; i < count; ++i) {
			String id = cfg.getString(i, "Id");
			String name = cfg.getString(i, "Name");
			float cost = cfg.getFloat(i, "Cost");
			boolean virtual = cfg.getBool(i, "Virtual");
			realItemMap.put(id,name);
		}
		return true;
	}

	@Override
	public void onDestroy() {

	}

	public void OnItemClassCreate(IKernel kernel, String script) {
		kernel.declareProperty(script, "Material", ValueType.BOOL, false, true, false);

	}

	public void OnUseCard(IKernel kernel, IGameObject player, int msgid, byte[] msg)
			throws InvalidProtocolBufferException {
	}

	public String getName(String id) {
		String name = realItemMap.get(id);
		return name;
	}
}
