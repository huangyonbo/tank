package game.modules.statemachine;

import framework.game.ICfgReader;
import framework.game.IKernel;
import game.modules.statemachine.StateMachine.State;

public class StateBoss extends BaseState {

	StateBoss(StateMachine machine, IKernel kernel, ICfgReader config) {
		super(machine, kernel, State.STATE_BOSS.ordinal(), config);
	}
}
