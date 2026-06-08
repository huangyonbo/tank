package framework;

import io.netty.util.Timeout;
import io.netty.util.TimerTask;

import java.util.concurrent.TimeUnit;

public class ActorTimer implements TimerTask {
	protected Actor _actor;

	protected Object _msgInfo;

	protected long _time;
	protected TimerHandler _handler;

	protected int _loopCount;
	protected boolean _stoped = false;

	protected void setTimeout(Timeout value) {
		_timeout = value;
	}

	private Timeout _timeout;

	public ActorTimer(Actor actor, long time, TimerHandler handler, int loopCount, Object msgInfo) {
		_actor = actor;
		_time = time;
		_handler = handler;
		_loopCount = loopCount;
		_msgInfo = msgInfo;
	}

	public Object getMsgInfo() {
		return _msgInfo;
	}

	public int getRemainCount() {
		return _loopCount;
	}

	public void stop() {
		_stoped = true;
		if (_timeout != null) {
			_timeout.cancel();
			_timeout = null;
		}
	}

	@Override
	public void run(Timeout timeout) throws Exception {
		if (_actor.isShutdown()) {
			return;
		}
		if (_loopCount != -1)
			_loopCount--;

		if (!_stoped && (_loopCount > 0 || _loopCount == -1)) {
			_timeout = _actor._timer.newTimeout(this, _time, TimeUnit.MILLISECONDS);
		} else {
			stop();
		}
		_actor.send(new TimerMsg(this,_loopCount));
	}

	public void runCallBack(int count) {
		try {
			_handler.handle(_msgInfo, count);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
