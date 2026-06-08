package framework;

import com.esotericsoftware.reflectasm.MethodAccess;

public class MethodCallBackData {
	public Object listener;
	public int methodIndex;
	public MethodAccess access;
	
	public String info(){
		StringBuffer buffer = new StringBuffer(listener.getClass().getSimpleName());
		buffer.append(".").append(access.getMethodNames()[methodIndex]);
		return buffer.toString();
	}
}
