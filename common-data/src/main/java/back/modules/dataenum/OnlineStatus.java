package back.modules.dataenum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 玩家在线状态
 * Created by Administrator on 2018/4/12.
 */
public enum OnlineStatus {
    OFFLINE(-2, "离线"),
    ONLINE(-1, "其他系统"),
    NOVICE(0, RoomType.NOVICE.getName()),
    PRIMARY(1, RoomType.PRIMARY.getName()),
    MIDDLE(2, RoomType.MIDDLE.getName()),
    SENIOR(3, RoomType.SENIOR.getName()),
    SUPER(4, RoomType.SUPER.getName()),
    CHI_YAN(5, RoomType.CHI_YAN.getName()),
    KUANG_BAO(6, RoomType.KUANG_BAO.getName()),
    ARENA(7, RoomType.ARENA.getName()),
    ROOM_BOSS(8, RoomType.ROOM_BOSS.getName()),
    ROOM_SUPREME_1(9, RoomType.ROOM_SUPREME_1.getName()),
    ROOM_SUPREME_2(10, RoomType.ROOM_SUPREME_2.getName()),
    ROOM_SUPREME_3(11, RoomType.ROOM_SUPREME_3.getName()),
    ROOM_N_BOMB(12, RoomType.ROOM_N_BOMB.getName());


    private final int id;
    private final String name;

    private OnlineStatus(final int id, final String name){
        this.id = id;
        this.name = name;
    }

    public int getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public static List<Object> getOnlineStatus(){
        List<Object> list = new ArrayList<>();
        for (OnlineStatus status : values()){
            Map<String, Object> map = new HashMap<>();
            map.put("id", status.getId());
            map.put("name", status.getName());
            list.add(map);
        }
        return list;
    }
}
