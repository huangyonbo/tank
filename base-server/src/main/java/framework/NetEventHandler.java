package framework;

import org.apache.mina.core.session.IoSession;

public class NetEventHandler extends Handler<NetEvent> {
	public NetEventHandler(Object this_, String methodName) throws Exception {
		super(this_, methodName);
	}

	@Override
	public int getMethodIndex(String methodName) {
		return access.getIndex(methodName, IoSession.class);
	}

	@Override
	public void handle(NetEvent netMsg) {
		access.invoke(this.obj, this.methodIndex, netMsg.ioSession);
	}
}
