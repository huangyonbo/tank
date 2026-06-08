package framework.mybatis.common;


import java.lang.reflect.InvocationTargetException;

public interface SqlAction<T> {
	T doAction(SqlMap data) throws InvocationTargetException, IllegalAccessException;
}
