/**   
*    
* 描述：   
* 文件：CombineModule.java
* 创建人：胡中伟
* 创建时间：2018年5月23日 下午3:22:18 
*    
*/
package game.modules.items;

import com.google.protobuf.InvalidProtocolBufferException;
import framework.game.*;
import game.custommsg.CustomMsg;
import game.custommsg.RequestMsgDef;
import game.custommsg.ServerCodeDef;
import game.modules.trigger.TriggerModule;
import game.modules.utils.ItemTipType;
import game.modules.utils.UtilFunc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * 描述：
 * 
 */
public class CombineModule implements ILogicModule {
	enum CombineType {
		BAG, // 背包合成
		END
	}

	class BagCombineData {
		String item;
		String target;
		int count;
	}

	private Map<String, BagCombineData> m_mapBagCombine = new HashMap<>();
	private ItemModule m_ItemModule;
	private TriggerModule triggerModule;
	private List<String> m_listLTE1Amount = Arrays.asList("item_debris_GoldenStorm", "item_debris_ChuXi");

	/**
	 * @param kernel
	 * @return
	 */
	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regRequestMessage(RequestMsgDef.REQ_COMBINE.ordinal(), this, "OnReqCombine");
		kernel.regRequestMessage(RequestMsgDef.REQ_COMBINE_TO_GOLD.ordinal(), this, "OnReqCombineToGold");

		kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");
		RefreshCfg(kernel, "res/Combine/BagCombine.xml");

		m_ItemModule = (ItemModule) kernel.getModule("ItemModule");
		triggerModule = (TriggerModule) kernel.getModule("TriggerModule");
		return m_ItemModule != null && triggerModule != null;
	}

	/**
	 * 
	 */
	@Override
	public void onDestroy() {
	}

	void RefreshCfg(IKernel kernel, String path) {
		if (path.equals("res/Combine/BagCombine.xml")) {
			m_mapBagCombine.clear();
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
			String item = cfg.getString(i, "Item");
			String target = cfg.getString(i, "Target");
			int need = cfg.getInt(i, "Count");
			if (!m_mapBagCombine.containsKey(item)) {
				BagCombineData data = new BagCombineData();
				data.item = item;
				data.target = target;
				data.count = need;
				m_mapBagCombine.put(item, data);
			}
		}
		return true;
	}

	void OnReqCombine(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
			throws InvalidProtocolBufferException {
		CustomMsg.Combine msgData = CustomMsg.Combine.parseFrom(msg);
		String item = msgData.getItem();
		long count = msgData.getCount();
		ServerCodeDef code = ServerCodeDef.CODE_SUCCESS;
		do {
			if (count != 1 && m_listLTE1Amount.contains(item)) {
				code = ServerCodeDef.CODE_PARAM_ERR;
				break;
			}
			if (!m_mapBagCombine.containsKey(item)) {
				code = ServerCodeDef.CODE_NOT_EXIST;
				break;
			}
			if (count <= 0){
				code = ServerCodeDef.CODE_MATRI_NOT_ENOUGH;
				break;
			}
			BagCombineData data = m_mapBagCombine.get(item);
			long need = count * data.count ;
			int have  = m_ItemModule.GetItemCount(kernel, player, item);
			if (need < 0 || have < need) {
				code = ServerCodeDef.CODE_MATRI_NOT_ENOUGH;
				break;
			}
			int _need = (int)need;
			m_ItemModule.SubItem(kernel, player, item, _need, UtilFunc.System.BAG_COMBINE.ordinal(), "Combine " + count);
			m_ItemModule.AddItem(kernel, player, data.target, (int) count, UtilFunc.System.BAG_COMBINE.ordinal(), "Combine " + count);
			triggerModule.OnTrigger(kernel, player, TriggerModule.TriggerType.TYPE_COMBINE_ITEM.ordinal(), data.target, (int) count, TriggerModule.ValueType.INC.ordinal());
//			ItemLogModule.AddItemLog(kernel, player, item, _need, ItemLogEnum.BAG_COMBINE_USE.ordinal());
//			ItemLogModule.AddItemLog(kernel, player, data.target, (int) count, ItemLogEnum.BAG_COMBINE_GET.ordinal());
		} while (false);
		UtilFunc.responseSerCode(kernel, player, reqid, code);
	}

	void OnReqCombineToGold(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
			throws InvalidProtocolBufferException {
		CustomMsg.Combine msgData = CustomMsg.Combine.parseFrom(msg);
		String item = msgData.getItem();
		int count = msgData.getCount();
		ServerCodeDef code = ServerCodeDef.CODE_SUCCESS;
		do {
			if (count <= 0) {
				code = ServerCodeDef.CODE_PARAM_ERR;
				break;
			}
			if (!m_mapBagCombine.containsKey(item)) {
				code = ServerCodeDef.CODE_NOT_EXIST;
				break;
			}
			BagCombineData data = m_mapBagCombine.get(item);
			long need = data.count * count;
			int have = m_ItemModule.GetItemCount(kernel, player, item);
			if (have < need) {
				code = ServerCodeDef.CODE_MATRI_NOT_ENOUGH;
				break;
			}
			int _need = (int)need;
			int cost = 5000000 * count;
			m_ItemModule.SubItem(kernel, player, item, _need, UtilFunc.System.BAG.ordinal(), "Combine To Gold " + count);
			m_ItemModule.AddItem(kernel, player, "item_gold_1", cost, UtilFunc.System.BAG.ordinal(), "Combine To Gold " + count);
			UtilFunc.sendItemTips(kernel, player, ItemTipType.TIP_GET_SUCCESS, "item_gold_1", cost);

		} while (false);
		UtilFunc.responseSerCode(kernel, player, reqid, code);
	}
}
