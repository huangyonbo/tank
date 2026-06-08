
package game.modules.items;

import framework.game.IGameObject;
import framework.game.IKernel;
import framework.game.ILogicModule;
import framework.game.KernelEvent;
import framework.game.ValueType;
import game.custommsg.CommandDef;

/**
 * 
 * 描述：
 * 
 */
public class TimeItem implements ILogicModule {

	public TimeItem(IKernel kernel) {
		kernel.addClass("TimeItem", "Item"); // 时效类道具
	}

	/**
	 * @param kernel
	 * @return
	 */
	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "TimeItem", this, "OnItemClassCreate");

		kernel.regCommand(CommandDef.CMD_USE_ITEM.ordinal(), "TimeItem", this, "OnUseItem");

		kernel.preLoadConfig("res/Items/TimeItem.xml");


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
		kernel.declareProperty(script, "Time", ValueType.INT, false, false, false);
	}

	public void OnUseItem(IKernel kernel, IGameObject item, Object... objects) {
		IGameObject player = (IGameObject) objects[0];
		int count = (int) objects[1];

		String name = item.getString("Property");
		long value = item.getInt("Time") * count;

		long lastTime = player.getLong(name);
		long now = kernel.getServerTime();
		if (lastTime < now) {
			player.setProperty(name, now + value * 60000L);
		} else {
			player.setProperty(name, lastTime + value * 60000L);
		}
	}
}
