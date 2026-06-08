package game.modules.rules;

import framework.PropertyKey;
import framework.game.IGameObject;
import framework.game.IKernel;
import framework.game.IRecord;
import game.modules.statemachine.StateMachine;

public class NormalRule extends BaseRule implements PropertyKey {

	StateMachine m_machine = null;

	public NormalRule(IKernel kernel) {
		super(kernel);
		m_machine = new StateMachine(kernel);
	}

	public void Start(IKernel kernel, IGameObject desk) {
		m_machine.ChangeState(kernel, desk, StateMachine.State.STATE_OUT.ordinal());
	}

	public void Stop(IKernel kernel, IGameObject desk) {
		m_machine.ChangeState(kernel, desk, StateMachine.State.STATE_UNKNOW.ordinal());
		// clear fishes
		IRecord rec = desk.getRecord(DESK_FISH_LIST);
		rec.clear();

		rec = desk.getRecord(DESK_FISH_COUNT);
		int row = rec.getRows();
		for (int i = 0; i < row; ++i) {
			rec.setValue(i, 1, 0);
		}
	}

	public void Run(IKernel kernel, IGameObject desk) {
		m_machine.StateRun(kernel, desk);
	}

	public StateMachine GetMachine() {
		return m_machine;
	}
}
