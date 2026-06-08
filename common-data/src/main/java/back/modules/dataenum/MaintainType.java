package back.modules.dataenum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 维护类型
 * Created by ZhaoJun on 2018/4/9.
 */
public enum MaintainType {
    NULL(0, ""),
    URGENT(1, "紧急维护"),
    UPDATE(2, "升级维护"),
    RULE(3, "例行维护"),
    DELAY(4, "延迟维护");

    private final int id;
    private final String name;

    private MaintainType(final int id, final String name){
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static List<Object> getMaintainType(){
        List<Object> list = new ArrayList<>();
        for (MaintainType type : values()){
            if (type == NULL)
                continue;
            Map<String, Object> map = new HashMap<>();
            map.put("id", type.getId());
            map.put("name", type.getName());
            list.add(map);
        }
        return list;
    }

    public static String getMaintainName(int id){
        for (MaintainType type : values()){
            if (type.getId() == id)
                return type.getName();
        }
        return NULL.getName();
    }
}