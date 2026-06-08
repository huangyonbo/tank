package back.modules.dataenum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 游戏类型
 * Created by Administrator on 2018/4/12.
 */
public enum GameType {
    NULL(0, ""),
    FISH(1, "街机牛魔王");

    private final int id;
    private final String name;

    private GameType(final int id, final String name){
        this.id = id;
        this.name = name;
    }

    public int getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public static List<Object> getGameType(){
        List<Object> list = new ArrayList<>();
        for (GameType gameType : values()){
            if (gameType == NULL)
                continue;
            Map<String, Object> map = new HashMap<>();
            map.put("id", gameType.getId());
            map.put("name", gameType.getName());
            list.add(map);
        }
        return list;
    }

    public static GameType getGameType(int id){
        for (GameType gameType : values()){
            if (gameType.getId() == id)
                return gameType;
        }
        return GameType.NULL;
    }
}
