package game.modules.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import framework.game.ICfgReader;
import framework.game.IGameObject;
import framework.game.IKernel;

public class StateMachine {
	
	public enum State {
		STATE_UNKNOW,
		STATE_OUT,
		STATE_GROUP, 
		STATE_BOSS,
		STATE_CLEAR,//清除
		STATE_SECRET,//隐藏
		STATE_COUNT,
	}

	static Logger logger = LoggerFactory.getLogger(StateMachine.class);
	BaseState[] m_states = new BaseState[State.STATE_COUNT.ordinal()];

	public StateMachine(IKernel kernel) {
		ICfgReader config = kernel.loadXmlConfig("res/Game/GameState.xml");
		m_states[State.STATE_OUT.ordinal()] = new StateOut(this, kernel, config);
		m_states[State.STATE_CLEAR.ordinal()] = new StateClear(this, kernel, config);
		m_states[State.STATE_GROUP.ordinal()] = new StateGroup(this, kernel, config);
		m_states[State.STATE_BOSS.ordinal()] = new StateBoss(this, kernel, config);
		m_states[State.STATE_SECRET.ordinal()] = new StateSecret(this, kernel, config);
	}

	public void ChangeTempState(IKernel kernel, IGameObject desk, int state, int next) {
		desk.setProperty("NextState", next);
		ChangeState(kernel, desk, state);
	}

	public void ChangeState(IKernel kernel, IGameObject desk, int state) {
		int oldState = desk.getInt("State");
		if (oldState > State.STATE_UNKNOW.ordinal() && oldState < State.STATE_COUNT.ordinal()) {
			m_states[oldState].OnLeave(kernel, desk);
		}
		if (state > State.STATE_UNKNOW.ordinal() && state < State.STATE_COUNT.ordinal()) {
			m_states[state].OnEnter(kernel, desk);
		}
		desk.setProperty("State", state);
	}

	public void StateRun(IKernel kernel, IGameObject desk) {
		int state = desk.getInt("State");
		if (state > State.STATE_UNKNOW.ordinal() && state < State.STATE_COUNT.ordinal()) {
			m_states[state].Run(kernel, desk);
		}
	}
}
