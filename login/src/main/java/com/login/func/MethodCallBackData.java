package com.login.func;

import com.esotericsoftware.reflectasm.MethodAccess;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MethodCallBackData {
	private Object listener;
	private int methodIndex;
	private MethodAccess access;

	public Object invoke(Object... params){
		Map<Integer,Object> map = new HashMap<>();
		for (int i = 0; i < params.length ; i++) {
			map.put(i,params[i]);
		}
		return access.invoke(listener,methodIndex,map);
	}
}
