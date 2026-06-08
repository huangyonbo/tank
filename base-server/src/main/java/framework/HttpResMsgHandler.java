package framework;

public class HttpResMsgHandler extends Handler<HttpResMsg> {
	public HttpResMsgHandler(Object this_, String methodName) throws Exception {
		super(this_, methodName);
	}

	@Override
	public int getMethodIndex(String methodName) {
		return access.getIndex(methodName, byte[].class, Object.class);
	}

	@Override
	public void handle(HttpResMsg httpResMsg) {
		access.invoke(this.obj, this.methodIndex, httpResMsg.bytes, httpResMsg.obj);
	}
}
