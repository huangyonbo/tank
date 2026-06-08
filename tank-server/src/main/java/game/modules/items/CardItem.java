package game.modules.items;

import com.google.protobuf.InvalidProtocolBufferException;
import framework.game.*;
import game.custommsg.C2SMsgDef;
import game.custommsg.CustomMsg;
import game.modules.player.BagModule;
import game.modules.utils.UtilFunc;

// 兑换卡
public class CardItem implements ILogicModule {
	ItemModule m_ItemModule = null;

	public CardItem(IKernel kernel) {
		kernel.addClass("CardItem", "Item");
	}

	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "CardItem", this, "OnItemClassCreate");
		kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");
		kernel.preLoadConfig("res/Items/CardItem.xml");
		kernel.regClientMessage(C2SMsgDef.C2S_USE_CARD.ordinal(), this, "OnUseCard");
		m_ItemModule = (ItemModule) kernel.getModule("ItemModule");
		return true;
	}

	void RefreshCfg(IKernel kernel, String path) {

	}

	@Override
	public void onDestroy() {

	}

	public void OnItemClassCreate(IKernel kernel, String script) {
		kernel.declareProperty(script, "Material", ValueType.BOOL, false, true, false);

	}

	public void OnUseCard(IKernel kernel, IGameObject player, int msgid, byte[] msg)
			throws InvalidProtocolBufferException {
		CustomMsg.UseCard useCard = CustomMsg.UseCard.parseFrom(msg);
		int pos = useCard.getPos();
		int type = useCard.getType();

		IGameObject itemBag = player.getContainer(BagModule.ITEM_BAG);
		if (itemBag == null) {
			return;
		}

		IGameObject item = itemBag.getChild(pos);
		if (item == null) {
			return;
		}

		if (!item.getScript().equals("CardItem")) {
			return;
		}

		item.addTempData("CardType", ValueType.INT, type);
		m_ItemModule.UseItem(kernel, player, item, 1, UtilFunc.System.BAG.ordinal(), "Client use item");
	}
}
