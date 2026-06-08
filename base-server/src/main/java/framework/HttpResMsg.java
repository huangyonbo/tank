package framework;

/**
 * 
 * 描述： 创建人：Zhenglei 创建时间：2018年5月3日 下午12:02:09
 * 
 */
public class HttpResMsg extends Message {
	public byte[] bytes;
	public Object obj;// 额外的参数

	public HttpResMsg(int msgID, byte[] bytes, Object obj) {
		super(msgID, HttpResMsg.SYS_HTTP_RESPONSE_MSG);
		this.bytes = bytes;
		this.obj = obj;
	}
}
