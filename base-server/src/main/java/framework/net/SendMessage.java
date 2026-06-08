package framework.net;

public class SendMessage {
	public short msgID;
	public byte[] data;
	public boolean close;
	
	public SendMessage(int msgID, byte[] data) {
		this(msgID,data,false);
	}
	
	public SendMessage(int msgID, byte[] data,boolean close) {
		this.msgID = (short) msgID;
		this.data = data;
		this.close = close;
	}
}
