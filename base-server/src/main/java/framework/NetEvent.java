package framework;

import org.apache.mina.core.session.IoSession;

public class NetEvent extends Message {
	public static final int SESSION_OPENED = 0;
	public static final int SESSION_CREATED = 1;
	public static final int SESSION_CLOSED = 2;
	public static final int SESSION_IDLE = 3;

	public IoSession ioSession;

	public NetEvent(int msgID, IoSession session) {
		super(msgID, NetMessage.SYS_NET_EVENT);

		this.ioSession = session;
	}
}
