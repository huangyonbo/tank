package game.modules.tasks;

import framework.game.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import game.custommsg.C2SMsgDef;
import game.custommsg.CommandDef;
import game.custommsg.CustomMsg;
import game.modules.items.ItemModule;
import game.modules.utils.ItemTipType;
import game.modules.utils.UtilFunc;

/**
 * 
 * 描述： 随机骰子任务模块
 */
public class DiceTaskModule implements ILogicModule {

	enum TaskRecColEnum {
		COL_TASK_ID, COL_FINISH_COUNT, COL_STATE, COL_MAX
	}

	private static Logger logger = LoggerFactory.getLogger(DiceTaskModule.class);

	private ICfgReader diceTaskConfig;
	private ItemModule itemModule = null;

	private static int[] DICE_BIT = { 0x1, 0x2, 0x4, 0x8, 0x10, 0x20 };
	// private static int DICE2_BIT = 0x2;
	// private static int DICE3_BIT = 0x4;
	// private static int DICE4_BIT = 0x8;
	// private static int DICE5_BIT = 0x10;
	// private static int DICE6_BIT = 0x20;

	public DiceTaskModule(IKernel kernel) {
		// kernel.AddClass("UserTask", "Task");// 走load不需要AddClass
	}

	@Override
	public boolean onInit(IKernel kernel) {
		// 注册事件
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
		kernel.regEvent(KernelEvent.KEVENT_ON_LOAD, "Player", this, "OnPlayerLoad");

		// 注册命令
		// kernel.RegCommand(CommandDef.CMD_FISH_DIE.ordinal(), "FishDesk",
		// this,
		// "OnFishDie");
		kernel.regCommand(CommandDef.CMD_DROP_DICE.ordinal(), "Player", this, "OnDropDice");
		kernel.regCommand(CommandDef.CMD_DROP_DICE_S.ordinal(), "Player", this, "OnDropDice_s");

		// 注册客户端消息
		kernel.regClientMessage(C2SMsgDef.C2S_GET_REWARD.ordinal(), this, "OnRequestGetReward");

		diceTaskConfig = kernel.loadXmlConfig("res/Config/DiceTask.xml");
		itemModule = (ItemModule) kernel.getModule("ItemModule");

		return true;
	}

	@Override
	public void onDestroy() {
	}

	public void OnPlayerClassCreate(IKernel kernel, String script) {
		// 声明表
		IRecord userDiceTaskList = kernel.declareRecord(script, "userDiceTaskList", TaskRecColEnum.COL_MAX.ordinal(),
				20, false, true, true);
		userDiceTaskList.setColType(TaskRecColEnum.COL_TASK_ID.ordinal(), ValueType.STRING);
		userDiceTaskList.setColType(TaskRecColEnum.COL_FINISH_COUNT.ordinal(), ValueType.INT);
		userDiceTaskList.setColType(TaskRecColEnum.COL_STATE.ordinal(), ValueType.INT);
	}

	public void OnPlayerLoad(IKernel kernel, IGameObject player) {
		//logger.info("------------- load dice tasks ...");
		LoadDiceTask(kernel, player);
	}

	public void LoadDiceTask(IKernel kernel, IGameObject player) {
		IRecord userDiceTaskRec = player.getRecord("userDiceTaskList");
		int count = diceTaskConfig.getItemCount();
		if (userDiceTaskRec.getRows() < count) {
			for (int i = userDiceTaskRec.getRows(); i < count; ++i) {
				String id = diceTaskConfig.getString(i, "Id");
				userDiceTaskRec.addRow(id, 0, 3);
			}
		}
	}

	public void OnDropDice(IKernel kernel, IGameObject player, Object... objects) {
		String itemId = (String) objects[0];
		IRecord rec = player.getRecord("userDiceTaskList");
		int finishCnt = rec.getInt(0, TaskRecColEnum.COL_FINISH_COUNT.ordinal());
		String dice_num = itemId.substring(itemId.length() - 1);
		int dice_bit = DICE_BIT[Integer.parseInt(dice_num) - 1];
		//logger.info("骰子点数 = {}, 数字代表 = {}", dice_num, dice_bit);
		if ((finishCnt & dice_bit) == 0) {
			finishCnt += dice_bit;
			//logger.debug("收集到{}点骰子", Integer.parseInt(dice_num));
			rec.setValue(0, TaskRecColEnum.COL_FINISH_COUNT.ordinal(), finishCnt);
		}

		int state = rec.getInt(0, TaskRecColEnum.COL_STATE.ordinal());
		if (finishCnt == 63 && state == 3) {
			//logger.debug("完成骰子收集任务！！");
			rec.setValue(0, TaskRecColEnum.COL_STATE.ordinal(), 2); // 已完成
		}
	}

	public void OnDropDice_s(IKernel kernel, IGameObject player, Object... objects) {
		String itemId = (String) objects[0];
		IRecord rec = player.getRecord("userDiceTaskList");
		int finishCnt = rec.getInt(1, TaskRecColEnum.COL_FINISH_COUNT.ordinal());
		String dice_num = itemId.substring(itemId.length() - 1);
		int dice_bit = DICE_BIT[Integer.parseInt(dice_num) - 1];
		//logger.info("骰子点数 = {}, 数字代表 = {}", dice_num, dice_bit);
		if ((finishCnt & dice_bit) == 0) {
			finishCnt += dice_bit;
			//logger.info("收集到{}点骰子", Integer.parseInt(dice_num));
			rec.setValue(1, TaskRecColEnum.COL_FINISH_COUNT.ordinal(), finishCnt);
		}

		int state = rec.getInt(1, TaskRecColEnum.COL_STATE.ordinal());
		if (finishCnt == 63 && state == 3) {
			//logger.info("完成骰子收集任务！！");
			rec.setValue(1, TaskRecColEnum.COL_STATE.ordinal(), 2); // 已完成
		}
	}

	public void OnRequestGetReward(IKernel kernel, IGameObject player, int msgid, byte[] msg)
			throws InvalidProtocolBufferException {

		CustomMsg.String getRewardMsg = CustomMsg.String.parseFrom(msg);
		String taskId = getRewardMsg.getValue();

		IRecord rec = player.getRecord("userDiceTaskList");
		int rows = rec.getRows();

		for (int i = 0; i < rows; ++i) {
			if (rec.getString(i, TaskRecColEnum.COL_TASK_ID.ordinal()).equals(taskId)
					&& rec.getInt(i, TaskRecColEnum.COL_STATE.ordinal()) == 2) {
				String itemPkg = diceTaskConfig.getString(taskId, "Reward");
				itemModule.AddItem(kernel, player, itemPkg, 1, UtilFunc.System.DICE_TASK.ordinal(), "client get");
				// 重置随机骰子任务
				rec.setValue(i, TaskRecColEnum.COL_STATE.ordinal(), 3);// 未完成
				rec.setValue(i, TaskRecColEnum.COL_FINISH_COUNT.ordinal(), 0);// 完成次数0
				logger.info("pick task:{} reward, send item tips to player:{}", taskId, player.getProperty(PLAYER_PROPERTY_NAME));
				UtilFunc.sendItemTips(kernel, player, ItemTipType.TIP_GET_SUCCESS, itemPkg, 1);
				return;
			}
		}
	}
}
