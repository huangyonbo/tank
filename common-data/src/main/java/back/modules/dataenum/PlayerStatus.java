package back.modules.dataenum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 玩家管理-玩家账号状态
 * Created by Administrator on 2018/4/12.
 */
public enum PlayerStatus {
    NORMAL(0, "正常"),
    FREEZE(1, "冻结"),    //后台冻结
    UNKNOWN1(2, "正常"),  //后台正在操作状态，操作完自动置0
    UNKNOWN2(3, "冻结");  //预警冻结

    private final int id;
    private final String name;

    private PlayerStatus(final int id, final String name){
        this.id = id;
        this.name = name;
    }

    public int getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public static List<Object> getPlayerStatus(){
        List<Object> list = new ArrayList<>();
        for (PlayerStatus gameType : values()){
            Map<String, Object> map = new HashMap<>();
            map.put("id", gameType.getId());
            map.put("name", gameType.getName());
            list.add(map);
        }
        return list;
    }

    public static String getPlayerStatus(int id){
        for (PlayerStatus status : values()){
            if (status.getId() == id)
                return status.getName();
        }
        return "";
    }

    public static Map<String, Integer> getStatusId(){
        Map<String, Integer> map = new HashMap<>();
        map.put("Normal", NORMAL.getId());
        map.put("Freeze", FREEZE.getId());
        return map;
    }
}
