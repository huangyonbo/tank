package back.modules.dataenum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 封禁类型
 * Created by ZhaoJun on 2018/5/16.
 */
public enum FreezeType {
    NULL(0, ""),
    IP(1, "IP"),
    DEVICE(2, "设备号"),
    UID(3, "玩家ID");

    private final int id;
    private final String name;

    private FreezeType(final int id, final String name){
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static List<Object> getFreezeType(){
        List<Object> list = new ArrayList<>();
        for (FreezeType type : values()){
            if (type == NULL)
                continue;
            Map<String, Object> map = new HashMap<>();
            map.put("id", type.getId());
            map.put("name", type.getName());
            list.add(map);
        }
        return list;
    }

    public static Map<String, Integer> getTypeId(){
        Map<String, Integer> map = new HashMap<>();
        map.put("Ip", IP.getId());
        map.put("Device", DEVICE.getId());
        map.put("Uid", UID.getId());
        return map;
    }

    public static FreezeType getFreezeType(int id){
        for (FreezeType type : values()){
            if (id == type.getId())
                return type;
        }
        return NULL;
    }
}