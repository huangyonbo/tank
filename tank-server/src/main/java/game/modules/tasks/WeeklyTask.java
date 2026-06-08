package game.modules.tasks;

import com.google.protobuf.InvalidProtocolBufferException;
import framework.game.*;
import game.custommsg.C2SMsgDef;
import game.custommsg.CommandDef;
import game.custommsg.CustomMsg;
import game.modules.items.ItemModule;
import game.modules.trigger.ConditionModule;
import game.modules.trigger.ConditionModule.TaskObj;
import game.modules.utils.ItemTipType;
import game.modules.utils.UtilFunc;
import game.util.TimeUtils;
import game.util.XML;

import java.util.*;

/**
 * 每周任务 Created by 赵俊 on 2019/7/24 10:33.
 */
public class WeeklyTask implements ILogicModule {
	private static final String RECORD_WEEKLY_TASK = "WeeklyTaskList";// 本周玩家需要完成的任务
	private static final String PROPERTY_ASSIGN_TASK_LAST_TIME = PLAYER_PROPERTY_WEEKLYTASKASSIGNLASTTIME;// 上次任务分配时间
	private static final String PROPERTY_WEEKLY_TASK_POINTS = PLAYER_PROPERTY_WEEKLYTASKPOINTS;// 每周任务积分
	public static final String PROPERTY_HAS_AWARDED_POINTS = PLAYER_PROPERTY_WEEKLYTASKHASAWARDEDPOINTS;// 已经领取额外奖励时的积分
	private static final int MAX_TASK_EVERY_PLAYER = 6;
	private ItemModule m_itemModule;
	private ConditionModule m_condictionModule;// 条件模块
	private HashMap<String, TaskData> m_mapRandomTasks = new HashMap<>();// 记录随机任务
	private HashMap<String, TaskData> m_mapMustTasks = new HashMap<>();// 记录固定任务
	private HashMap<String, TaskData> m_mapAllTasks = new HashMap<>();// 记录所有任务
	private Map<Integer, Award> m_mapTaskExtraAwards = new HashMap<>();// 记录所有额外奖励
	private XML m_parseXML;
	private List<Integer> m_regCond = new ArrayList<>();
	private Random m_random = new Random();

	enum TaskCol {
		COL_ID, // 任务id
		COL_COUNT, // 当前进度
		COL_STATE, // 状态
		COL_DATA, // 任务数据

		COL_END
	}

	enum TaskState// 任务状态
	{
		UNKNOWN, // 0未知
		REWARD, // 1已领奖
		COMP, // 2已完成
		ING, // 3进行中
		UNSTART, // 4未开始
		END
	}

	class TaskData {
		String id;// 任务ID
		Award award;// 奖励
		int condition;// 条件
		int count;// 次数
		String preTask;// 前置任务
		boolean must;// 是否固定任务
	}

	class Award {
		String itemId;
		int count = 1;
	}

	@Override
	public void onDestroy() {

	}

	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
		kernel.regEvent(KernelEvent.KEVENT_ON_LOAD, "Player", this, "OnPlayerLoad");

		// 请求领取奖励
		kernel.regClientMessage(C2SMsgDef.C2S_GET_WEEKLY_TASK_AWARD.ordinal(), this, "OnRequestGetReward");
		kernel.regCommand(CommandDef.CMD_CHANGE_WEEK.ordinal(), "Player", this, "OnChangeWeek");
		kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");
		kernel.listenPropertyChange(PROPERTY_WEEKLY_TASK_POINTS, "Player", this, "OnPropertyChange");

		m_condictionModule = (ConditionModule) kernel.getModule("ConditionModule");
		m_itemModule = (ItemModule) kernel.getModule("ItemModule");
		if (m_condictionModule == null || m_itemModule == null) {
			return false;
		}
		XML weeklyTask = new XML("res/Task/WeeklyTask.xml", null, kernel,
				(iKernel, cfg) -> loadWeeklyTask(iKernel, cfg));
		m_parseXML = new XML("res/Task/WeeklyTaskExtraAwards.xml", weeklyTask, kernel,
				(iKernel, cfg) -> loadWeeklyTaskExtraAwards(iKernel, cfg));
		return true;
	}

	public void OnPlayerClassCreate(IKernel kernel, String script) {
		kernel.declareProperty(script, PROPERTY_ASSIGN_TASK_LAST_TIME, ValueType.LONG, false, false, true);
		kernel.declareProperty(script, PROPERTY_WEEKLY_TASK_POINTS, ValueType.INT, false, true, true);
		kernel.declareProperty(script, PROPERTY_HAS_AWARDED_POINTS, ValueType.INT, false, true, true);
		IRecord rec = kernel.declareRecord(script, RECORD_WEEKLY_TASK, TaskCol.COL_END.ordinal(), m_mapAllTasks.size(),
				false, true, true);
		rec.setColType(TaskCol.COL_ID.ordinal(), ValueType.STRING);
		rec.setColType(TaskCol.COL_COUNT.ordinal(), ValueType.INT);
		rec.setColType(TaskCol.COL_STATE.ordinal(), ValueType.INT);
		rec.setColType(TaskCol.COL_DATA.ordinal(), ValueType.STRING);
	}

	public void OnPropertyChange(IKernel kernel, IGameObject player, String name, Object oldValue) {
		int currentPoints = player.getInt(PROPERTY_WEEKLY_TASK_POINTS);
		if (PROPERTY_WEEKLY_TASK_POINTS.equals(name) && currentPoints - (int) oldValue > 0) {
			for (Integer points : m_mapTaskExtraAwards.keySet()) {
				int hasAwarded = player.getInt(PROPERTY_HAS_AWARDED_POINTS);
				if (currentPoints >= points && points > hasAwarded) {
					Award award = m_mapTaskExtraAwards.get(points);
					if (award != null) {
						player.setProperty(PROPERTY_HAS_AWARDED_POINTS, points);
						m_itemModule.AddItem(kernel, player, award.itemId, award.count,
								UtilFunc.System.WEEKLY_TASK.ordinal(), "weekly task get");
						UtilFunc.sendItemTips(kernel, player, ItemTipType.TIP_GET_SUCCESS, award.itemId, award.count);
					}
				}
			}
			player.setProperty(PROPERTY_HAS_AWARDED_POINTS, currentPoints);
		}
	}

	public void OnPlayerLoad(IKernel kernel, IGameObject player) {
		checkWeeklyTask(kernel, player);
	}

	void OnChangeWeek(IKernel kernel, IGameObject player) {
		checkWeeklyTask(kernel, player);
	}

	public void OnRequestGetReward(IKernel kernel, IGameObject player, int msgid, byte[] msg)
			throws InvalidProtocolBufferException {

		CustomMsg.String getRewardMsg = CustomMsg.String.parseFrom(msg);
		String taskId = getRewardMsg.getValue();

		IRecord rec = player.getRecord(RECORD_WEEKLY_TASK);
		int rows = rec.getRows();
		for (int i = 0; i < rows; ++i) {
			if (rec.getString(i, TaskCol.COL_ID.ordinal()).equals(taskId)
					&& rec.getInt(i, TaskCol.COL_STATE.ordinal()) == TaskState.COMP.ordinal()) {
				TaskData taskData = m_mapAllTasks.get(taskId);
				if (taskData == null || taskData.award == null) {
					break;
				}
				m_itemModule.AddItem(kernel, player, taskData.award.itemId, taskData.award.count,
						UtilFunc.System.WEEKLY_TASK.ordinal(), "weekly task get");
				int finishCnt = rec.getInt(i, TaskCol.COL_COUNT.ordinal());
				rec.removeRow(i);
				rec.addRow(taskId, finishCnt, TaskState.REWARD.ordinal(), "");// 已领取,排到最后
				UtilFunc.sendItemTips(kernel, player, ItemTipType.TIP_GET_SUCCESS, taskData.award.itemId,
						taskData.award.count);

				for (int j = 0; j < rows; ++j) {
					TaskData data = m_mapAllTasks.get(rec.getString(j, TaskCol.COL_ID.ordinal()));
					if (data == null || data.preTask == null || "".equals(data.preTask)) {
						continue;
					}
					String preTask = data.preTask;
					if (preTask.equals(taskId)
							&& rec.getInt(j, TaskCol.COL_STATE.ordinal()) == TaskState.UNSTART.ordinal()) {
						TaskObj obj = m_condictionModule.new TaskObj();
						obj.data = "";
						obj.count = 0;
						m_condictionModule.GetProgress(kernel, player, data.condition, obj);
						rec.setValue(j, TaskCol.COL_COUNT.ordinal(), obj.count);
						rec.setValue(j, TaskCol.COL_DATA.ordinal(), obj.data);
						if (obj.count >= data.count) {
							rec.setValue(j, TaskCol.COL_STATE.ordinal(), TaskState.COMP.ordinal());
						} else {
							rec.setValue(j, TaskCol.COL_STATE.ordinal(), TaskState.ING.ordinal());
						}
					}
				}
				break;
			}
		}
	}

	void RefreshCfg(IKernel kernel, String path) {
		if (path == null) {
			return;
		}
		m_parseXML.parse(kernel, path);
	}

	/**
	 * 触发器触发回调函数，由条件模块保证
	 * 
	 * @param kernel
	 * @param player
	 * @param conid
	 *            条件ID
	 * @param count
	 *            次数
	 * @param valType
	 */
	void OnCondition(IKernel kernel, IGameObject player, int conid, int count, int valType) {
		IRecord rec = player.getRecord(RECORD_WEEKLY_TASK);
		if (rec == null){
			return;
		}
		int rows = rec.getRows();
		for (int i = 0; i < rows; ++i) {
			String id = rec.getString(i, TaskCol.COL_ID.ordinal());
			if (!m_mapAllTasks.containsKey(id)) {
				continue;
			}
			TaskData data = m_mapAllTasks.get(id);
			if (data.condition != conid) {
				continue;
			}
			// 任务不是正在进行，退出
			if (rec.getInt(i, TaskCol.COL_STATE.ordinal()) != TaskState.ING.ordinal()) {
				continue;
			}

			TaskObj obj = m_condictionModule.new TaskObj();
			obj.data = rec.getString(i, TaskCol.COL_DATA.ordinal());
			obj.count = rec.getInt(i, TaskCol.COL_COUNT.ordinal());
			if (!m_condictionModule.CheckTrigger(kernel, player, conid, count, valType, obj)) {
				continue;
			}
			if (obj.count > data.count) {
				obj.count = data.count;
			}
			rec.setValue(i, TaskCol.COL_DATA.ordinal(), obj.data);
			rec.setValue(i, TaskCol.COL_COUNT.ordinal(), obj.count);
			if (obj.count >= data.count) {
				rec.setValue(i, TaskCol.COL_STATE.ordinal(), TaskState.COMP.ordinal());
			}
		}
	}

	private void loadWeeklyTask(IKernel kernel, ICfgReader cfg) {
		m_mapRandomTasks.clear();
		m_mapMustTasks.clear();
		m_mapAllTasks.clear();
		for (int i = 0; i < cfg.getItemCount(); i++) {
			TaskData data = new TaskData();
			data.id = cfg.getString(i, "Id");
			data.condition = cfg.getInt(i, "Condition");
			data.count = cfg.getInt(i, "Count");
			data.must = cfg.getBool(i, "Must");
			String[] rewards = cfg.getString(i, "Reward").split("\\*");
			if (rewards.length < 1) {
				continue;
			}
			Award award = new Award();
			award.itemId = rewards[0];
			if (rewards.length >= 2) {
				award.count = Integer.parseInt(rewards[1]);
			}
			data.award = award;
			data.preTask = cfg.getString(i, "PreTask");

			if (data.must) {
				m_mapMustTasks.put(data.id, data);// 装载固定任务
			} else {
				m_mapRandomTasks.put(data.id, data);// 装载随机任务
			}
			m_mapAllTasks.put(data.id, data);
			if (!m_regCond.contains(data.condition)) {
				/////////////////// 向任务条件模块注册触发条件及对应的回调////////////////////
				m_condictionModule.RegCondition(data.condition, this, "OnCondition");
				m_regCond.add(data.condition);
			}
		}
	}

	private void loadWeeklyTaskExtraAwards(IKernel kernel, ICfgReader cfg) {
		m_mapTaskExtraAwards.clear();
		for (int i = 0; i < cfg.getItemCount(); i++) {
			String[] awards = cfg.getString(i, "Award").split("\\*");
			if (awards.length < 1) {
				continue;
			}
			Award award = new Award();
			award.itemId = awards[0];
			if (awards.length >= 2) {
				award.count = Integer.parseInt(awards[1]);
			}
			m_mapTaskExtraAwards.put(cfg.getInt(i, "Id"), award);
		}
	}

	private void checkWeeklyTask(IKernel kernel, IGameObject player) {
		long current = UtilFunc.getZeroTime(kernel.getServerTime());
		if (!TimeUtils.isSameWeek(player.getLong(PROPERTY_ASSIGN_TASK_LAST_TIME), current)) {
			player.setProperty(PROPERTY_ASSIGN_TASK_LAST_TIME, current);
			player.setProperty(PROPERTY_WEEKLY_TASK_POINTS, 0);
			player.setProperty(PROPERTY_HAS_AWARDED_POINTS, 0);
			IRecord record = player.getRecord(RECORD_WEEKLY_TASK);
			assignTask(kernel, player, record);
		}
	}

	private void assignTask(IKernel kernel, IGameObject player, IRecord record) {
		record.clear();
		List<String> list = new ArrayList<>();
		Iterator<String> mustTaskIterator = m_mapMustTasks.keySet().iterator();
		while (mustTaskIterator.hasNext()) {
			String taskId = mustTaskIterator.next();
			if (list.size() >= MAX_TASK_EVERY_PLAYER) {
				break;
			}
			list.add(taskId);
		}
		int rest = m_mapRandomTasks.size();
		Iterator<String> iterator = m_mapRandomTasks.keySet().iterator();
		while (iterator.hasNext()) {
			String taskId = iterator.next();
			if (list.size() >= MAX_TASK_EVERY_PLAYER) {
				break;
			}
			if (m_random.nextInt(2) == 0 || (MAX_TASK_EVERY_PLAYER - list.size() >= rest)) {
				list.add(taskId);
			}
			rest--;
		}
		for (String taskId : list) {
			TaskData data = m_mapAllTasks.get(taskId);
			if (data.preTask.isEmpty()) {
				TaskObj obj = m_condictionModule.new TaskObj();
				obj.data = "";
				obj.count = 0;
				m_condictionModule.GetProgress(kernel, player, data.condition, obj);
				if (obj.count >= data.count) {
					record.addRow(data.id, obj.count, TaskState.COMP.ordinal(), obj.data);
				} else {
					record.addRow(data.id, obj.count, TaskState.ING.ordinal(), obj.data);
				}
			} else {
				record.addRow(data.id, 0, TaskState.UNSTART.ordinal(), "");
			}
		}
	}
}
