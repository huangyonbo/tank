package back.modules.dataenum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 道具管理的配置类型
 * Created by Administrator on 2018/5/12.
 */
public enum ConfigType {
    NULL(0, ""),
    PROP(1, "道具配置"),
    GOODS(2, "商品配置");

    private final int id;
    private final String name;

    private ConfigType(final int id, final String name){
        this.id = id;
        this.name = name;
    }

    public int getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public static List<Object> getConfigType(){
        List<Object> list = new ArrayList<>();
        for (ConfigType type : values()){
            if (type == NULL)
                continue;
            Map<String, Object> map = new HashMap<>();
            map.put("id", type.getId());
            map.put("name", type.getName());
            list.add(map);
        }
        return list;
    }

    public static ConfigType getConfigType(int type){
        for (ConfigType configType : values()){
            if (configType.getId() == type)
                return configType;
        }
        return NULL;
    }

    public static Map<String, Integer> getTypeId(){
        Map<String, Integer> map = new HashMap<>();
        map.put("Prop", PROP.getId());
        map.put("Goods", GOODS.getId());
        return map;
    }
}
