package framework.net;

import com.dtflys.forest.callback.OnError;
import com.dtflys.forest.callback.OnSuccess;
import framework.JsonUtil;
import framework.net.http.HttpClientApi;

import java.util.Map;

public class HttpClientUtil {

	public static void doPost(HttpClientApi api, String domain, Map<String,Object> params, OnSuccess<String> onSuccess, OnError onError){
		String body = "{}";
		if (params  != null){
			body = JsonUtil.encodeToStr(params);
		}
		api.doPost(domain,body,onSuccess,onError);
	}
}
