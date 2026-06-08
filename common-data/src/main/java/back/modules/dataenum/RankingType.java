package back.modules.dataenum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2018/5/4.
 */
public enum RankingType {
    NULL(0, ""),
    TREASURE(1, "每周富豪榜"),
    PAY(2, "每周充值榜");

    private final int id;
    private final String name;

    private RankingType(final int id, final String name){
        this.id = id;
        this.name = name;
    }

    public int getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public static List<Object> getRankingType(){
        List<Object> list = new ArrayList<>();
        for (RankingType type : values()){
            if (type == NULL)
                continue;
            Map<String, Object> map = new HashMap<>();
            map.put("id", type.getId());
            map.put("name", type.getName());
            list.add(map);
        }
        return list;
    }

    public static RankingType getRankingType(int type){
        for (RankingType rankingType : values()){
            if (rankingType.getId() == type)
                return rankingType;
        }
        return NULL;
    }

    public static Map<String, Integer> getTypeId(){
        Map<String, Integer> map = new HashMap<>();
        map.put("Treasure", TREASURE.getId());
        return map;
    }
}
