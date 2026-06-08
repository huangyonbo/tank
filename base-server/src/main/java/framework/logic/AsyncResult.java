package framework.logic;

import com.dtflys.forest.callback.OnError;
import com.dtflys.forest.callback.OnSuccess;
import com.dtflys.forest.exceptions.ForestRuntimeException;
import com.dtflys.forest.http.ForestRequest;
import com.dtflys.forest.http.ForestResponse;
import framework.BaseServer;
import framework.HttpResMsg;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Builder
@Slf4j
public class AsyncResult implements OnSuccess<String>, OnError {
    private int msgId;
    private Object obj;
    private BaseServer server;

    @Override
    public void onError(ForestRuntimeException ex, ForestRequest request, ForestResponse response) {
        HttpResMsg httpResMsg = new HttpResMsg(msgId, null,obj);
        server.send(httpResMsg);
    }

    @Override
    public void onSuccess(String data, ForestRequest request, ForestResponse response) {
        HttpResMsg httpResMsg;
        if (response.getStatusCode() == 200) {
            httpResMsg = new HttpResMsg(msgId,data.getBytes(StandardCharsets.UTF_8), obj);
        } else {
            httpResMsg = new HttpResMsg(msgId, null, obj);
        }
        server.send(httpResMsg);
    }
}
