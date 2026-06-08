package framework;

import com.esotericsoftware.reflectasm.MethodAccess;

/**
 * 
 * 描述： 创建人：Zhenglei * 创建时间：2018年5月3日 下午12:02:09
 * 
 */
public class HttpServerMsgHandler {
	protected int methodIndex = -1;
	protected MethodAccess access;
	protected Object obj;

	public HttpServerMsgHandler(Object this_, String methodName) throws Exception {
		obj = this_;
		// access = MethodAccess.get(obj.getClass());
		access = MethodAccessCache.tryToGet(obj.getClass());
		methodIndex = getMethodIndex(methodName);
		if (this.methodIndex == -1)
			throw new Exception("Method " + methodName + " not found!");
	}

	public int getMethodIndex(String methodName) {
		return access.getIndex(methodName, String.class, String.class);
	}

	public String handle(HttpServerMsg httpServerMsg) {
		return (String) access.invoke(this.obj, this.methodIndex, httpServerMsg.url, httpServerMsg.json);
	}
}
