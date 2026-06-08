package game.modules.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import framework.game.ICfgReader;
import framework.game.IGameObject;
import framework.game.IKernel;
import game.modules.statemachine.StateMachine.State;

public class StateOut extends BaseState {

	static Logger logger = LoggerFactory.getLogger(StateOut.class);

	StateOut(StateMachine machine, IKernel kernel, ICfgReader config) {
		super(machine, kernel, State.STATE_OUT.ordinal(), config);
	}

	@Override
	public void OnEnter(IKernel kernel, IGameObject desk) {
		super.OnEnter(kernel, desk);
	}

	@Override
	public void Run(IKernel kernel, IGameObject desk) {
        long serverTime = kernel.getServerTime();
        long endTime = desk.getLong(DESK_NOT_OUT_FISH_END_TIME);
        if (serverTime > endTime) {
            int[] fmts = (int[]) desk.getParent().getProperty("Formation");
            if (fmts.length > 0) {
                // 有鱼阵，检测下一状态
                super.Run(kernel, desk);
            }
        }
	}
}
