/**   
*    
* 描述：   条件模块
* 文件：ConditionModule.java
* 创建人：胡中伟
* 创建时间：2018年9月19日 下午3:34:18 
*    
*/
package game.modules.trigger;

import framework.MethodAccessCache;
import framework.MethodCallBackData;
import framework.game.*;
import game.modules.trigger.TriggerModule.TriggerType;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ConditionModule implements ILogicModule {

	private ConditionData[] conditionData;
	Map<Integer, List<ConditionData>> m_mapCondByTrigger = new HashMap<>();
	Map<Integer, List<MethodCallBackData>> m_mapCbsById = new HashMap<>();

	TriggerModule m_TriggerModule = null;

	public ConditionData[] getConditionData() {
		return conditionData;
	}

	/**
	 * @param kernel
	 * @return
	 */
	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");

		RefreshCfg(kernel, "res/Task/Condition.xml");

		m_TriggerModule = (TriggerModule) kernel.getModule("TriggerModule");
		for (int i = 0; i < TriggerType.TYPE_END.ordinal(); ++i) {
			m_TriggerModule.RegTrigger(i, this, "OnTrigger");
		}
		return true;
	}

	/**
	 * 
	 */
	@Override
	public void onDestroy() {

	}

	void RefreshCfg(IKernel kernel, String path) {
		if (!path.equals("res/Task/Condition.xml")) {
			return;
		}

		ICfgReader cfg = kernel.loadXmlConfig(path);
		int count = cfg.getItemCount();

		conditionData = new ConditionData[count];
		for (int i = 0; i < count; ++i) {
			conditionData[i] = new ConditionData();
			conditionData[i].setId(i);
			int bvLimit = cfg.getInt(i, "BvLimit");
			conditionData[i].setBvLimit(bvLimit);
			int[] RoomLimit = cfg.getIntArray(i, "RoomLimit", ",");
			conditionData[i].setRoomLimit(RoomLimit);
			int triggerId = cfg.getInt(i, "Trigger");
			conditionData[i].setTriggerId(triggerId);
			String[] strs = cfg.getStringArray(i, "Target", ",");
			conditionData[i].setTargets(new ArrayList<>());
			if (strs != null) {
				for (String str : strs) {
					if (!str.isEmpty()) {
						conditionData[i].getTargets().add(str);
					}
				}

			}
			if (!m_mapCondByTrigger.containsKey(conditionData[i].getTriggerId())) {
				m_mapCondByTrigger.put(conditionData[i].getTriggerId(), new ArrayList<ConditionData>());
			}
			m_mapCondByTrigger.get(conditionData[i].getTriggerId()).add(conditionData[i]);
		}
	}

	public void RegCondition(int id, Object listener, String methodName) {
		MethodCallBackData cbdata = new MethodCallBackData();
		cbdata.listener = listener;
		cbdata.access = MethodAccessCache.tryToGet(listener.getClass());
		cbdata.methodIndex = cbdata.access.getIndex(methodName, IKernel.class, IGameObject.class, int.class, int.class,
				int.class);

		if (!m_mapCbsById.containsKey(id)) {
			m_mapCbsById.put(id, new ArrayList<>());
		}
		m_mapCbsById.get(id).add(cbdata);
	}

	void OnCondition(IKernel kernel, IGameObject player, int conid, int count, int valType) {
		if (!m_mapCbsById.containsKey(conid)) {
			return;
		}
		for (MethodCallBackData cb : m_mapCbsById.get(conid)) {
			cb.access.invoke(cb.listener, cb.methodIndex, kernel, player, conid, count, valType);
		}
	}

	void OnTrigger(IKernel kernel, IGameObject player, int id, String target, int count, int valType) {
		if (!m_mapCondByTrigger.containsKey(id)) {
			return;
		}

		for (ConditionData data : m_mapCondByTrigger.get(id)) {
			if (data.getTargets().isEmpty() || data.getTargets().contains(target)) {
				if (player.getInt(PLAYER_PROPERTY_BULLETVALUE) < data.getBvLimit()) {
					continue;
				}
				if (data.getRoomLimit() != null) {
					IGameObject desk = kernel.getGameObject(player.getLong(PLAYER_PROPERTY_DESKID));
					if (desk == null) {
						continue;
					}
					int type = desk.getInt(DESK_TYPE_KEY);
					boolean found = false;
					for (int roomId : data.getRoomLimit()) {
						if (roomId == type) {
							found = true;
							break;
						}
					}
					if (!found) {
						continue;
					}
				}
				OnCondition(kernel, player, data.getId(), count, valType);
			}
		}
	}

	public class TaskObj {
		public String data;
		public int count;
	}

	// 登录、初始化时调用
	public void GetProgress(IKernel kernel, IGameObject player, int conid, TaskObj task) {
		if (conid < 0 || conid >= conditionData.length) {
			return;
		}

		int count = m_TriggerModule.GetProgress(kernel, player, conditionData[conid].getTriggerId());
		if (count != -1) {
			task.count = count;
		}

		// 检测登录
		if (conditionData[conid].getTriggerId() == TriggerType.TYPE_LOGIN.ordinal()) {
			String now = kernel.getServer().getDayFormat().format(kernel.getServerTime());
			if (!now.equals(task.data)) {
				task.data = now;
				task.count += 1;
			}
		}

		// 检测在线时长
		if (conditionData[conid].getTriggerId() == TriggerType.TYPE_ONLINE_TIME.ordinal()) {
			long now = kernel.getServerTime();
			task.data = Long.toString(now);
		}
	}

	public boolean CheckTrigger(IKernel kernel, IGameObject player, int conid, int count, int valType, TaskObj task) {
		if (conid < 0 || conid >= conditionData.length) {
			return false;
		}

		// 检测登录
		if (conditionData[conid].getTriggerId() == TriggerType.TYPE_LOGIN.ordinal()) {
			String now = kernel.getServer().getDayFormat().format(kernel.getServerTime());
			if (now.equals(task.data)) {
				return false;
			}


			task.data = now;
		}

		// 检测在线时长
		if (conditionData[conid].getTriggerId() == TriggerType.TYPE_ONLINE_TIME.ordinal()) {
			long now = kernel.getServerTime();
			if (task.data.isEmpty()) {
				count = 0;
			} else {
				long loginTime = Long.parseLong(task.data);
				count = (int) (now - loginTime);
			}
			task.data = Long.toString(now);
		}

		if (valType == TriggerModule.ValueType.INC.ordinal()) {
			task.count += count;
		} else if (valType == TriggerModule.ValueType.VAL.ordinal()) {
			task.count = count;
		}

		return true;
	}
	public boolean CheckTriggerNew(IKernel kernel, IGameObject player, int conid, int count, int valType, TaskObj task) {
		// 检测在线时长
		if (conditionData[conid].getTriggerId() == TriggerType.TYPE_ONLINE_TIME.ordinal()) {
			return true;
		}
		return false;
	}
}
