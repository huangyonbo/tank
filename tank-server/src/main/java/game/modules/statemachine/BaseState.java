package game.modules.statemachine;

import framework.PropertyKey;
import framework.game.ICfgReader;
import framework.game.IGameObject;
import framework.game.IKernel;
import game.modules.statemachine.StateMachine.State;

public class BaseState implements PropertyKey {

	protected StateMachine m_machine = null;
	protected int m_state = State.STATE_UNKNOW.ordinal();
	protected int m_time = 0;
	protected int m_next = -1;

	public BaseState(StateMachine machine, IKernel kernel, int state, ICfgReader config) {
		m_state = state;
		m_machine = machine;
		m_time = config.getInt(Integer.toString(m_state), "Time");
		m_next = config.getInt(Integer.toString(m_state), "Next");
	}

	public void OnEnter(IKernel kernel, IGameObject desk) {
		desk.setProperty("StateStartTime", kernel.getServerTime());
	}

	public void OnLeave(IKernel kernel, IGameObject desk) {
		
	}

	public void Run(IKernel kernel, IGameObject desk) {
		if (kernel.getServerTime() - desk.getLong("StateStartTime") >= m_time) {
			if (CheckNextState(kernel, desk)) {
				return;
			}
			m_machine.ChangeTempState(kernel, desk, State.STATE_CLEAR.ordinal(), m_next);
			return;
		}
	}

	protected boolean CheckNextState(IKernel kernel, IGameObject desk) {
		int next = desk.getInt("NextState");
		if (next != 0) {
			desk.setProperty("NextState", 0);
			m_machine.ChangeState(kernel, desk, next);
			return true;
		}
		return false;
	}
}
