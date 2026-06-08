package framework.net.http;

import com.dtflys.forest.annotation.Body;
import com.dtflys.forest.annotation.DataVariable;
import com.dtflys.forest.annotation.Query;
import com.dtflys.forest.annotation.Request;
import com.dtflys.forest.callback.OnError;
import com.dtflys.forest.callback.OnSuccess;

import java.util.Map;

public interface HttpClientApi {

    @Request(url = "${domain}", type = "post",async = true, dataType ="application/json; charset=utf-8",contentType ="application/json; charset=utf-8")
    void doPost(@DataVariable("domain")String domain, @Body String body, OnSuccess<String> onSuccess, OnError onError);

    @Request(url = "${domain}", type = "get", dataType ="application/json; charset=utf-8",contentType ="application/json; charset=utf-8")
    void doGet(@DataVariable("domain")String domain, @Query Map<String,Object> params, OnSuccess<String> onSuccess, OnError onError);
}
