package framework.Store;

import framework.JsonUtil;
import framework.mybatis.DataManager;
import framework.mybatis.service.AbstractService;
import framework.net.message.InnerMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class StoreData {
	private static Map<String,Method> methodCache = new HashMap<>();
	static Logger logger = LoggerFactory.getLogger(StoreData.class);

	public static Object execute(DataManager dataManager,byte[] datas){
		String serviceName = null;
		String method = null;
		try {
			InnerMsg.LoadDataFromDb msg = InnerMsg.LoadDataFromDb.parseFrom(datas);
			serviceName   = msg.getDao();
			method = msg.getMethod();
			AbstractService<?> service = dataManager.getService(serviceName);
			if (service != null){
				Class<?>[] types = null;
				Object[] values = null;
				int size = msg.getTypesList().size();
				if (size > 0){
					types = new Class<?>[size];
					values = new Object[size];
					for (int i = 0 ; i < size ; i++){
						types[i]  = Class.forName(msg.getTypes(i));
						if (types[i] == String.class){
							values[i] = msg.getValues(i);
						}else{
							ParamData paramData = JsonUtil.decodeToObj(msg.getValues(i),ParamData.class);
							values[i] = paramData.decodeToObject(types[i]);
						}
					}
				}
				String key = serviceName + "." + method + "_" + size;
				Method _method = methodCache.get(key);
				if (_method == null){
					_method = service.getClass().getMethod(method,types);
					methodCache.put(key,_method);
				}
				return _method.invoke(service,values);
			}
		} catch (Exception e) {
			logger.error(serviceName + "." + method,e);
		}
		return null;
	}
}
