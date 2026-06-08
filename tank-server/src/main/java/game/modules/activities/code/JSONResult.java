package game.modules.activities.code;

import com.alibaba.fastjson.JSONObject;
import game.custommsg.ServerCodeDef;

public class JSONResult extends JSONObject {
    public void setDesc(ResponseCode code){
        this.put("code",code.code());
        this.put("desc",code.desc());
    }

    public void setDesc(ServerCodeDef code){
        this.put("code",code.getId());
        this.put("desc",code.getDesc());
    }
    public void setDesc(String message){
        this.put("desc",message);
    }

    public void setDesc(int code) {
        this.put("code", code);
    }
}
