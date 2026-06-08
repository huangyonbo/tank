package framework;

/**
 * 
 * 描述： 创建人：胡中伟 创建时间：2018年3月13日 下午12:02:02
 * 
 */
public class ForwardMsg extends Message {
	public byte[] bytes;
	public int uid;

	public ForwardMsg(short msgID, int uid, byte[] bytes) {
		super(msgID, ForwardMsg.SYS_FORWARD_MSG);
		this.uid = uid;
		this.bytes = bytes;
	}
}
