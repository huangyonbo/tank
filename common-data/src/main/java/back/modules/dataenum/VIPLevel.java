package back.modules.dataenum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VIP等级
 * Created by Administrator on 2018/4/12.
 */
public enum VIPLevel {
    ZERO(0, "0级"),
    ONE(1, "1级"),
    TWO(2, "2级"),
    THREE(3, "3级"),
    FOUR(4, "4级"),
    FIVE(5, "5级"),
    SIX(6, "6级"),
    SEVEN(7, "7级"),
    EIGHT(8, "8级"),
    NINE(9, "9级"),
    TEN(10, "10级"),
    ELEVEN(11, "11级"),
    TWELVE(12, "12级");

    private final int id;
    private final String name;

    private VIPLevel(final int id, final String name){
        this.id = id;
        this.name = name;
    }

    public int getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public static List<Object> getVIPLevel(){
        List<Object> list = new ArrayList<>();
        for (VIPLevel gameType : values()){
            Map<String, Object> map = new HashMap<>();
            map.put("id", gameType.getId());
            map.put("name", gameType.getName());
            list.add(map);
        }
        return list;
    }
}
