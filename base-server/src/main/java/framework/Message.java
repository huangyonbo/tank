package framework;

public class Message {
	public static final int ACTOR_INITED = 0;

	/// Indicate it is a normal actor message
	public static final int SYS_NORMAL_MSG = 0;

	/// Received this message only when network is closed by low level
	/// exception. Manually call the Close() will not trigger this notify.
	public static final int SYS_NET_CLOSED = 2;
	/// Network message is composed and post to actor.
	public static final int SYS_NET_MESSAGE = 4;
	public static final int SYS_NET_EVENT = 5;
	public static final int SYS_FORWARD_MSG = 6;
	public static final int SYS_CLIENT_MSG = 7;
	public static final int SYS_HTTP_RESPONSE_MSG = 8; // 作为httpClient发送的返回消息
	public static final int SYS_HTTP_SERVER_MSG = 9; // 作为httpServer接受

	public static final int SYS_TIMER_MSG = 10;

	public static final int SYS_RPC_CALL = 21;
	public static final int SYS_RPC_RET = 22;

	/// ---
	public int sysType;

	public int msgID;

	public Message(int msgID) {
		this.msgID = msgID;
		this.sysType = Message.SYS_NORMAL_MSG;
	}

	public Message(int msgID, int sysType) {
		this.msgID = msgID;
		this.sysType = sysType;
	}

	public Message() {
		this.sysType = Message.SYS_NORMAL_MSG;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Message> T ToConcreteMsg(Message message) {
		assert (Message.class.isInstance(message));
		return (T) message;
	}
}
