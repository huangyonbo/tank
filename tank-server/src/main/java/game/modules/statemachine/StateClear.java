package game.modules.statemachine;

import framework.game.ICfgReader;
import framework.game.IGameObject;
import framework.game.IKernel;
import framework.game.IRecord;
import game.custommsg.S2CMsgDef;
import game.modules.fishgame.FishModule;
import game.modules.fishgame.FishModule.FishType;
import game.modules.statemachine.StateMachine.State;

public class StateClear extends BaseState {

	private FishModule m_fishModule;

	StateClear(StateMachine machine, IKernel kernel, ICfgReader config) {
		super(machine, kernel, State.STATE_CLEAR.ordinal(), config);
		m_fishModule = (FishModule) kernel.getModule("FishModule");
	}

	@Override
	public void OnEnter(IKernel kernel, IGameObject desk) {
		super.OnEnter(kernel, desk);

		long now = kernel.getServerTime();
		long frozenEnd = desk.getLong("FrozenEnd");
		// BOSS在场也清场 alter by 赵俊@2019/9/6 16:51
		// if(frozenEnd + 3000 <= now && !HaveBoss(kernel, desk))
		// BOSS在场也清场 alter by 赵俊@2019/9/6 16:51
		// BOSS在场也清场 add by 赵俊@2019/9/6 16:52
		if (frozenEnd + 3000 <= now) {
			// BOSS在场也清场 add by 赵俊@2019/9/6 16:52
			Clear(kernel, desk);
		}
	}

	@Override
	public void OnLeave(IKernel kernel, IGameObject desk) {
		desk.setProperty("Cleared", false);
	}

	@Override
	public void Run(IKernel kernel, IGameObject desk) {
		if (desk.getBool("Cleared")) {
			super.Run(kernel, desk);
		} else {
			long now = kernel.getServerTime();
			long frozenEnd = desk.getLong("FrozenEnd");
			// BOSS在场也清场 alter by 赵俊@2019/9/6 16:52
			// if(frozenEnd + 3000 <= now && !HaveBoss(kernel, desk))
			// BOSS在场也清场 alter by 赵俊@2019/9/6 16:52
			// BOSS在场也清场 add by 赵俊@2019/9/6 16:52
			if (frozenEnd + 3000 <= now) {
				// BOSS在场也清场 add by 赵俊@2019/9/6 16:52
				Clear(kernel, desk);
			}
		}
	}

	boolean HaveBoss(IKernel kernel, IGameObject desk) {
		IRecord fishCountRec = desk.getRecord(DESK_FISH_COUNT);

		int pos = fishCountRec.findRow(0, 0, FishType.TYPE_BOSS.ordinal());
		if (pos != -1) {
			int count = fishCountRec.getInt(pos, 1);
			return count > 0;
		}

		return false;
	}

	void Clear(IKernel kernel, IGameObject desk) {
		// clear fishes
		IRecord rec = desk.getRecord(DESK_FISH_LIST);
		// 鱼王可与鱼潮同时存在 alter by 赵俊@2019/9/6 17:06
		// rec.Clear();
		// 鱼王可与鱼潮同时存在 alter by 赵俊@2019/9/6 17:06
		// 鱼王可与鱼潮同时存在 add by 赵俊@2019/9/6 17:06

		// 鱼王可与鱼潮同时存在 add by 赵俊@2019/9/6 17:06

		rec = desk.getRecord(DESK_FISH_COUNT);
		int row = rec.getRows();
		for (int i = 0; i < row; ++i) {
			rec.setValue(i, 1, 0);
		}

		/*
		for (int j = 0; j < desk.GetSeatCount(); j++) {
			IGameObject player = desk.GetSeatObject(j);
			if (player == null) {
				continue;
			}
			// 退还子弹
			IRecord bulletRec = player.GetRecord("BulletRec");
			int count = bulletRec.GetRows();
			long gold = 0;
			for (int i = 0; i < count; ++i) {
				gold += bulletRec.GetInt(i, 1);
			}
			bulletRec.Clear();
			if (gold > 0) {
				player.SetProperty(PLAYER_PROPERTY_GOLD, player.GetLong(PLAYER_PROPERTY_GOLD) + gold);
			}
		}
		*/

		kernel.broadCastByDesk(desk, S2CMsgDef.S2C_CLEAR_FISH.ordinal(), null);
		desk.setProperty("Cleared", true);
		desk.setProperty("StateStartTime", kernel.getServerTime());
	}
}
