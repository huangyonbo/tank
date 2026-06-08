package game.modules.items;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import framework.game.ICfgReader;
import framework.game.IGameObject;
import framework.game.IKernel;
import framework.game.ILogicModule;
import framework.game.KernelEvent;
import framework.game.ValueType;
import game.modules.utils.ItemTipType;
import game.modules.utils.UtilFunc;

public class ExchangeModule implements ILogicModule {
	class ExchangeData {
		Map<String, Integer> items = new HashMap<>();
		Map<String, Integer> props = new HashMap<>();
		Map<String, Integer> target = new HashMap<>();
	}

	private static Logger logger = LoggerFactory.getLogger(ExchangeModule.class);
	private Map<Integer, ExchangeData> m_mapDatas = new HashMap<>();
	private ItemModule m_ItemModule = null;

	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");
		RefreshCfg(kernel, "res/Combine/Exchange.xml");

		m_ItemModule = (ItemModule) kernel.getModule("ItemModule");
		if (m_ItemModule == null) {
			return false;
		}
		return true;
	}

	@Override
	public void onDestroy() {

	}

	void RefreshCfg(IKernel kernel, String path) {
		if (path.equals("res/Combine/Exchange.xml")) {
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
			int id = cfg.getInt(i, "Id");
			if (m_mapDatas.containsKey(id)) {
				continue;
			}

			String item = cfg.getString(i, "Item");
			String prop = cfg.getString(i, "Property");
			String target = cfg.getString(i, "Target");

			ExchangeData data = new ExchangeData();
			UtilFunc.parseMapStr(item, data.items, ";", "\\*");
			UtilFunc.parseMapStr(prop, data.props, ";", "\\*");
			UtilFunc.parseMapStr(target, data.target, ";", "\\*");

			m_mapDatas.put(id, data);

		}
		return true;
	}

	public boolean Exchange(IKernel kernel, IGameObject player, int id, int count, int system) {
		logger.info("Exchange {} {} {}", player.getInt(PLAYER_PROPERTY_UID), id, count);
		if (!m_mapDatas.containsKey(id)) {
			logger.info("!m_mapDatas.containsKey(id) {}", id);
			return false;
		}

		boolean checked = true;
		for (Entry<String, Integer> entry : m_mapDatas.get(id).items.entrySet()) {
			if (m_ItemModule.GetItemCount(kernel, player, entry.getKey()) < entry.getValue() * count) {
				logger.info("items({}) not enough({}<{})", entry.getKey(),
						m_ItemModule.GetItemCount(kernel, player, entry.getKey()), entry.getValue() * count);
				checked = false;
				break;
			}
		}

		for (Entry<String, Integer> entry : m_mapDatas.get(id).props.entrySet()) {
			long have = player.getProType(entry.getKey()) == ValueType.INT ? player.getInt(entry.getKey())
					: player.getLong(entry.getKey());
			if (have < entry.getValue() * count) {
				logger.info("props({}) not enough({}<{})", entry.getKey(), have, entry.getValue() * count);
				checked = false;
				break;
			}
		}

		if (!checked) {
			return false;
		}

		for (Entry<String, Integer> entry : m_mapDatas.get(id).items.entrySet()) {
			m_ItemModule.SubItem(kernel, player, entry.getKey(), entry.getValue() * count, system, "Exchange " + id);
		}

		for (Entry<String, Integer> entry : m_mapDatas.get(id).props.entrySet()) {
			if (player.getProType(entry.getKey()) == ValueType.INT) {
				player.setProperty(entry.getKey(), player.getInt(entry.getKey()) - entry.getValue() * count, system,
						"Exchange " + id);
			} else {
				player.setProperty(entry.getKey(), player.getLong(entry.getKey()) - entry.getValue() * count, system,
						"Exchange " + id);
			}
		}

		logger.info("Exchange success");
		Object[] tips = new Object[m_mapDatas.get(id).target.size() * 2];
		int index = 0;
		for (Entry<String, Integer> entry : m_mapDatas.get(id).target.entrySet()) {
			m_ItemModule.AddItem(kernel, player, entry.getKey(), entry.getValue() * count, system, "Exchange " + id);
			tips[index * 2] = entry.getKey();
			tips[index * 2 + 1] = entry.getValue() * count;
			++index;
		}
		UtilFunc.sendItemTips(kernel, player, ItemTipType.TIP_GET_SUCCESS, tips);
		return true;
	}

	public Map<String, Integer> ExchangeResult(IKernel kernel, IGameObject player, int id, int count, int system) {
		if (!m_mapDatas.containsKey(id)) {
			return null;
		}

		boolean checked = true;
		for (Entry<String, Integer> entry : m_mapDatas.get(id).items.entrySet()) {
			if (m_ItemModule.GetItemCount(kernel, player, entry.getKey()) < entry.getValue() * count) {
				checked = false;
				break;
			}
		}

		for (Entry<String, Integer> entry : m_mapDatas.get(id).props.entrySet()) {
			long have = player.getProType(entry.getKey()) == ValueType.INT ? player.getInt(entry.getKey())
					: player.getLong(entry.getKey());
			if (have < entry.getValue() * count) {
				checked = false;
				break;
			}
		}

		if (!checked) {
			return null;
		}

		for (Entry<String, Integer> entry : m_mapDatas.get(id).items.entrySet()) {
			m_ItemModule.SubItem(kernel, player, entry.getKey(), entry.getValue() * count, system,
					"ExchangeResult " + id);
		}

		for (Entry<String, Integer> entry : m_mapDatas.get(id).props.entrySet()) {
			if (player.getProType(entry.getKey()) == ValueType.INT) {
				player.setProperty(entry.getKey(), player.getInt(entry.getKey()) - entry.getValue() * count, system,
						"ExchangeResult " + id);
			} else {
				player.setProperty(entry.getKey(), player.getLong(entry.getKey()) - entry.getValue() * count, system,
						"ExchangeResult " + id);
			}
		}

		return m_mapDatas.get(id).target;
	}
}
