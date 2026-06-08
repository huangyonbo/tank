package framework;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TimerHandler {
	private Method _handler;
	private Object _this;

	public TimerHandler(Object this_, String methodName) throws Exception {
		_this = this_;
		Method[] methods = this_.getClass().getDeclaredMethods();
		for (int i = 0; i < methods.length; i++) {
			String name = methods[i].getName();
			if (name.equals(methodName)) {
				_handler = methods[i];
				break;
			}
		}
		if (_handler == null){
			throw new Exception("Method " + methodName + " not found!");
		}
		_handler.setAccessible(true);
	}

	public void handle(Object timerInfo, int leftCount) throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		_handler.invoke(_this, timerInfo, leftCount);
	}
}
