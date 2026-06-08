package game.modules.statemachine;

import java.util.ArrayList;
import java.util.List;

import framework.game.IRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import framework.game.ICfgReader;
import framework.game.IGameObject;
import framework.game.IKernel;
import game.custommsg.CommandDef;
import game.modules.fishgame.FishModule;
import game.modules.fishgame.FishModule.FishType;
import game.modules.statemachine.StateMachine.State;

public class StateGroup extends BaseState {
	FishModule m_fishModule = null;
	private static Logger logger = LoggerFactory.getLogger(StateGroup.class);

	StateGroup(StateMachine machine, IKernel kernel, ICfgReader config) {
		super(machine, kernel, State.STATE_GROUP.ordinal(), config);
		m_fishModule = (FishModule) kernel.getModule("FishModule");
	}

	@Override
	public void OnEnter(IKernel kernel, IGameObject desk) {
		super.OnEnter(kernel, desk);

		desk.setProperty("NextState", m_next);

		int[] fmts = (int[]) desk.getParent().getProperty("Formation");
		if (fmts.length == 0) {
			// 不出鱼阵
			return;
		}
		int fmt = fmts[0];
		if (fmts.length > 1) {
			int last = desk.getInt("LastFormation");
			List<Integer> temp = new ArrayList<>();
			for (int i = 0; i < fmts.length; i++) {
				int _fmt = fmts[i];
				if (_fmt != last) {
					temp.add(_fmt);
				}
			}
			if (temp.size() == 1) {
				fmt = temp.get(0);
			} else {
				int index = framework.MathUtils.random(temp.size());
				fmt = temp.get(index);
			}
		}
		desk.setProperty("LastFormation", fmt);

	}

	public void OnLeave(IKernel kernel, IGameObject desk) {
		// clear count
		IRecord rec = desk.getRecord(DESK_FISH_COUNT);
		int row = rec.getRows();
		for (int i = 0; i < row; ++i) {
			rec.setValue(i, 1, 0);
		}
		// clear fish list
		rec = desk.getRecord(DESK_FISH_LIST);
		//rec.Clear();
	}
}
