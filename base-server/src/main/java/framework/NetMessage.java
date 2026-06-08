package framework;

import org.apache.mina.core.session.IoSession;

public class NetMessage extends Message {
	public byte[] bytes;
	public IoSession ioSession;

	public NetMessage(int type, short msgID, IoSession session, byte[] bytes) {
		super(msgID, type);

		this.ioSession = session;
		this.bytes = bytes;
	}

	public NetMessage(short msgID, IoSession session, byte[] bytes) {
		super(msgID, NetMessage.SYS_NET_MESSAGE);

		this.ioSession = session;
		this.bytes = bytes;
	}
}
