package back.modules.dataenum;

import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 厅类型
 * Created by Administrator on 2018/4/12.
 */
@Getter
public enum RoomType {
    NOVICE(0, "翼望山脉"),
    PRIMARY(1, "邽山峡谷"),
    MIDDLE(2, "恒冰石湖"),
    SENIOR(3, "不周残垣"),
    SUPER(4, "至尊选座"),
    CHI_YAN(5, "赤炎火龙"),
    KUANG_BAO(6, "狂暴火龙"),
    ARENA(7, "秘境试炼"),
    ROOM_BOSS(8, "鱼王争霸"),
    ROOM_SUPREME_1(9, "珊瑚海峡-英雄"),
    ROOM_SUPREME_2(10, "地心熔岩-英雄"),
    ROOM_SUPREME_3(11, "神秘洞窟-英雄"),
//    ROOM_N_BOMB(12, "青丘桃林"),
//	ROOM_N_BOMB_PRIME(13, "祥瑞东望"),
//	ROOM_N_BOMB_MID(14, "东海填沟"),
//	ROOM_N_BOMB_SENIOR(15, "天帝花园"),
//    ROOM_NEW_DOWNLOAD(16, "新手渔村"),
//    ROOM_PERSONAL(17, "常羊战场"),
//    ROOM_MYSTERY_LEGEND(18, "休闲场深海秘境"),
//    ROOM_N_MYSTERY_LEGEND(19, "海神深海秘境"),
//    ROOM_ANCIENT_RELICS(20, "万岳千山"),
    ROOM_N_BOMB(12, "魔晶一场"),
    ROOM_N_BOMB_PRIME(13, "魔晶二场"),
    ROOM_N_BOMB_MID(14, "魔晶三场"),
    ROOM_N_BOMB_SENIOR(15, "魔晶四场"),
    ROOM_NEW_DOWNLOAD(16, "新手渔村"),
    ROOM_PERSONAL(17, "魔晶五场"),
    ROOM_MYSTERY_LEGEND(18, "休闲场深海秘境"),
    ROOM_N_MYSTERY_LEGEND(19, "海神深海秘境"),
    ROOM_ANCIENT_RELICS(20, "魔晶六场"),
    ROOM_END(22, "结束没有意义");

    private final int id;
    private final String name;

    RoomType(final int id, final String name){
        this.id = id;
        this.name = name;
    }

    public static Map<Integer, String> getAsObj() {
        return Arrays.stream(values()).filter(e -> e != ROOM_END).collect(Collectors.toMap(RoomType::getId, RoomType::getName));
    }

    public int getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public static List<Object> getRoomType(){
        List<Object> list = new ArrayList<>();
        for (RoomType roomType : values()){
            if (roomType == CHI_YAN || roomType == KUANG_BAO || roomType == ARENA || roomType == ROOM_END)
                continue;
            Map<String, Object> map = new HashMap<>();
            map.put("id", roomType.getId());
            map.put("name", roomType.getName());
            list.add(map);
        }
        return list;
    }

    public static List<Object> getRoomType2(){
        List<Object> list = new ArrayList<>();
        for (RoomType roomType : values()){
            if (checkLottery(roomType.ordinal())){
                Map<String, Object> map = new HashMap<>();
                map.put("id",roomType.getId());
                map.put("name",roomType.getName());
                list.add(map);
            }
        }
        return list;
    }

    /**
     * 奖券鱼可以出现的房间
     * @return
     */
    public static boolean checkLottery(int roomType){
        return (roomType >= NOVICE.ordinal() &&  roomType <= SENIOR.ordinal()) || (roomType >= ROOM_SUPREME_1.ordinal() &&  roomType<= ROOM_SUPREME_3.ordinal());
    }

    public static String getClassicType(){
        StringBuilder stringBuilder = new StringBuilder("(");
        stringBuilder.append(NOVICE.getId());
        stringBuilder.append(",");
        stringBuilder.append(PRIMARY.getId());
        stringBuilder.append(",");
        stringBuilder.append(MIDDLE.getId());
        stringBuilder.append(",");
        stringBuilder.append(SENIOR.getId());
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    public static RoomType getRoomType(int id){
        return Arrays.stream(values()).filter(roomType -> roomType.id == id).findFirst().orElse(null);
    }

    public static String getRoomName(int id){
        RoomType type = getRoomType(id);
        return type == null ? "-" : type.name ;
    }

    public static Map<String, Integer> getTypeId(){
        Map<String, Integer> map = new HashMap<>();
        map.put("Novice", NOVICE.getId());
        map.put("Primary", PRIMARY.getId());
        map.put("Middle", MIDDLE.getId());
        map.put("Senior", SENIOR.getId());
        map.put("Super", SUPER.getId());
        return map;
    }
}
