package game.modules.items.funcitems;

import java.math.BigDecimal;

import framework.game.IGameObject;
import framework.game.IKernel;

/**
 * 获得雷龙积分道具
 */
public class ThunderDragonPointsItem extends BaseFuncItem {
	/**
	 * 雷龙积分
	 */
	private static final String PROPERTY_THUNDER_DRAGON_POINTS = "ThunderDragonPoints";
	/**
	 * 雷龙炮台
	 */
	private int m_thunderDragonBattery = 16;

	public boolean OnInit(IKernel kernel) {
		return true;
	}

	public void OnItemClassCreate(IKernel kernel, String script) {
	}

	public void OnUseItem(IKernel kernel, IGameObject item, Object... objects) {
		IGameObject player = (IGameObject) objects[0];
		long count = (int) objects[1];
		int batteryInUse = player.getInt(PLAYER_PROPERTY_BATTERYINUSE);
		if (batteryInUse == m_thunderDragonBattery) {
			BigDecimal base = new BigDecimal(count);
			BigDecimal mul = new BigDecimal("1.15");
			count = base.multiply(mul).longValue();
		}
		long points = player.getLong(PROPERTY_THUNDER_DRAGON_POINTS);
		if (count > points) {
			player.setProperty(PROPERTY_THUNDER_DRAGON_POINTS, count);
		}
	}
}
