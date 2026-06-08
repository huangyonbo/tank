package framework;

import org.apache.mina.core.session.IoSession;

public class NetMsgHandler extends Handler<NetMessage> {
	public NetMsgHandler(Object this_, String methodName) throws Exception {
		super(this_, methodName);
	}

	@Override
	public int getMethodIndex(String methodName) {
		return access.getIndex(methodName, IoSession.class, byte[].class);
	}

	@Override
	public void handle(NetMessage netMsg) {
		access.invoke(this.obj, this.methodIndex, netMsg.ioSession, netMsg.bytes);
	}
}
