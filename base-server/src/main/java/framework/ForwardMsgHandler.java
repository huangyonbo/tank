package framework;

/**
 * 
 * 描述： 创建人：胡中伟 创建时间：2018年3月13日 下午12:01:58
 * 
 */
public class ForwardMsgHandler extends Handler<ForwardMsg> {
	public ForwardMsgHandler(Object this_, String methodName) throws Exception {
		super(this_, methodName);
	}

	@Override
	public int getMethodIndex(String methodName) {
		return access.getIndex(methodName, int.class, byte[].class);
	}

	@Override
	public void handle(ForwardMsg msg) {
		access.invoke(this.obj, this.methodIndex, msg.uid, msg.bytes);
	}
}
