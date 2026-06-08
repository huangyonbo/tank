package framework;

import com.esotericsoftware.reflectasm.MethodAccess;

public abstract class Handler<T extends Message> {

	protected int methodIndex = -1;
	protected MethodAccess access;
	protected Object obj;

	public Handler(Object this_, String methodName) throws Exception {
		obj = this_;
		// access = MethodAccess.get(obj.getClass());
		access = MethodAccessCache.tryToGet(obj.getClass());
		methodIndex = getMethodIndex(methodName);

		if (this.methodIndex == -1)
			throw new Exception("Method " + methodName + " not found!");
	}

	public abstract int getMethodIndex(String methodName);

	public abstract void handle(T msg);
}
