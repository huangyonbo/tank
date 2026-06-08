package back.modules.dataenum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 玩家管理-禁言类型
 * Created by Administrator on 2018/4/12.
 */
public enum ShutUpType {
    NULL(-1L, ""),
    NORMAL(0L, "正常"),
    _30_MINUTES(1800000L, "30分钟"),
    _1_HOUR(3600000L, "1小时"),
    _6_HOURS(21600000L, "6小时"),
    _1_DAY(86400000L, "1天"),
    _3_DAY(259200000L, "3天");

    private final long id;
    private final String name;

    private ShutUpType(final long id, final String name){
        this.id = id;
        this.name = name;
    }

    public long getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public static List<Object> getShutUpType(){
        List<Object> list = new ArrayList<>();
        for (ShutUpType gameType : values()){
            if (gameType == NULL)
                continue;
            Map<String, Object> map = new HashMap<>();
            map.put("id", gameType.getId());
            map.put("name", gameType.getName());
            list.add(map);
        }
        return list;
    }

    public static String getShutUpType(long id){
        if (id == NORMAL.getId())
            return "正常";
        return "禁言";
    }

    public static Map<String, Long> getTypeId(){
        Map<String, Long> map = new HashMap<>();
        map.put("Normal", NORMAL.getId());
        return map;
    }
}
