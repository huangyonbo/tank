package framework;

/**
 * 
 * 描述： 创建人：胡中伟 创建时间：2018年3月13日 下午12:01:48
 * 
 */
public class TimerMsg extends Message {
	public ActorTimer GetActorTimer() {
		return _actorTimer;
	}

	protected ActorTimer _actorTimer;
	public int leftCount;

	public TimerMsg(ActorTimer timerTask, int leftCount) {
		this.sysType = Message.SYS_TIMER_MSG;
		this.leftCount = leftCount;
		_actorTimer = timerTask;
	}
}
