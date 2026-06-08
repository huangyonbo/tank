package back.modules.dataenum;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 厅类型
 * Created by Administrator on 2018/4/12.
 */
@Getter
public enum RoomTypeEnum {
    NOVICE(0, "神秘遗迹"),
    PRIMARY(1, "蒙水海湾"),
    MIDDLE(2, "深海归墟"),
    SENIOR(3, "熔岩洞窟"),
    SUPER(4, "至尊选座"),
    CHI_YAN(5, "赤炎火龙"),
    KUANG_BAO(6, "狂暴火龙"),
    ARENA(7, "秘境试炼"),
    ROOM_BOSS(8, "鱼王争霸"),
    ROOM_SUPREME_1(9, "珊瑚海峡-英雄"),
    ROOM_SUPREME_2(10, "地心熔岩-英雄"),
    ROOM_SUPREME_3(11, "神秘洞窟-英雄"),
    ROOM_N_BOMB(12, "蓬莱仙境"),
    ROOM_N_BOMB_PRIME(13, "狱法山脉"),
    ROOM_N_BOMB_MID(14, "雷泽秘境"),
    ROOM_N_BOMB_SENIOR(15, "昆仑瑶台"),
    ROOM_NEW_DOWNLOAD(16, "新手渔村"),
    ROOM_PERSONAL(17, "上古战场"),
    ROOM_MYSTERY_LEGEND(18, "休闲场深海秘境"),//未开启
    ROOM_N_MYSTERY_LEGEND(19, "海神深海秘境"),//未开启
    ROOM_ANCIENT_RELICS(20, "万岳千山"),
    ROOM_XIN_SHOU_YU_CUN(21, "新手渔场"),
    ROOM_BIHAI_FUSANG(22, "碧海扶桑"),
    ROOM_EXPERIENCE_BOMB(23, "蓬莱仙境(体验模式)"),
    ROOM_EXPERIENCE_BOMB_PRIME(24, "狱法山脉(体验模式)"),
    ROOM_EXPERIENCE_BOMB_MID(25, "雷泽秘境(体验模式)"),
    ROOM_EXPERIENCE_BOMB_SENIOR(26, "昆仑瑶台(体验模式)"),
    ROOM_EXPERIENCE_PERSONAL(27, "上古战场(体验模式)"),
    ROOM_EXPERIENCE_BIHAI_FUSANG(28, "碧海扶桑(体验模式)"),
    ROOM_END(23, "结束没有意义");

    private int id;
    private String name;

    RoomTypeEnum(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public static RoomTypeEnum GetEnumById(int id) {
        RoomTypeEnum[] values = RoomTypeEnum.values();
        for (RoomTypeEnum roomTypeEnum : values) {
            if (roomTypeEnum.getId() == id)
                return roomTypeEnum;
        }
        return null;
    }

    public static boolean isBHFSRoom(int roomType) {
        return ROOM_BIHAI_FUSANG.id == roomType;
    }

    public static boolean isBHFSExperienceRoom(int roomType) {
        return ROOM_EXPERIENCE_BIHAI_FUSANG.id == roomType;
    }

    public static Map<Integer, String> getAsObj() {
        return Arrays.stream(values()).filter(e -> e != ROOM_END).collect(Collectors.toMap(RoomTypeEnum::getId, RoomTypeEnum::getName));
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static List<Object> getRoomType() {
        List<Object> list = new ArrayList<>();
        for (RoomTypeEnum roomTypeEnum : values()) {
            if (roomTypeEnum == CHI_YAN || roomTypeEnum == KUANG_BAO || roomTypeEnum == ARENA || roomTypeEnum == ROOM_END)
                continue;
            Map<String, Object> map = new HashMap<>();
            map.put("id", roomTypeEnum.getId());
            map.put("name", roomTypeEnum.getName());
            list.add(map);
        }
        return list;
    }


    /**
     * 获取所有灵石体验模式下房间
     *
     * @return
     */
    public static Set<Integer> getAllExperienceRoom() {
        Set<Integer> set = new HashSet<>();
        set.add(ROOM_EXPERIENCE_BIHAI_FUSANG.id);
        set.add(ROOM_EXPERIENCE_PERSONAL.id);
        set.add(ROOM_EXPERIENCE_BOMB_SENIOR.id);
        set.add(ROOM_EXPERIENCE_BOMB_MID.id);
        set.add(ROOM_EXPERIENCE_BOMB_PRIME.id);
        set.add(ROOM_EXPERIENCE_BOMB.id);
        return set;
    }

    /**
     * 奖券鱼可以出现的房间
     *
     * @return
     */
    public static boolean checkLottery(int roomType) {
        return (roomType >= NOVICE.getId() && roomType <= SENIOR.getId()) ||
                (roomType >= ROOM_SUPREME_1.getId() && roomType <= ROOM_SUPREME_3.getId());
    }

    public static String getClassicType() {
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

    public static RoomTypeEnum getRoomType(int id) {//根据房间id获取枚举值
        return Arrays.stream(values()).filter(roomType -> roomType.id == id).findFirst().orElse(null);
    }

    public static String getRoomName(int id) {
        RoomTypeEnum type = getRoomType(id);
        return type == null ? "-" : type.name;
    }

    //获取房间货币类型0金币1灵石2体验灵石
    public static int GetRoomType(int id) {
        RoomTypeEnum typeEnum = getRoomType(id);
        int t = -1;
        switch (typeEnum) {
            case NOVICE:
            case PRIMARY:
            case MIDDLE:
            case SENIOR:
            case SUPER:
            case CHI_YAN:
            case KUANG_BAO:
            case ARENA:
            case ROOM_BOSS:
            case ROOM_SUPREME_1:
            case ROOM_SUPREME_2:
            case ROOM_SUPREME_3:
                t = 0;
                break;
            case ROOM_N_BOMB:
            case ROOM_N_BOMB_PRIME:
            case ROOM_N_BOMB_MID:
            case ROOM_N_BOMB_SENIOR:
            case ROOM_PERSONAL:
            case ROOM_ANCIENT_RELICS:
            case ROOM_BIHAI_FUSANG:
                t = 1;
                break;
            case ROOM_EXPERIENCE_BOMB:
            case ROOM_EXPERIENCE_BOMB_PRIME:
            case ROOM_EXPERIENCE_BOMB_MID:
            case ROOM_EXPERIENCE_BOMB_SENIOR:
            case ROOM_EXPERIENCE_PERSONAL:
            case ROOM_EXPERIENCE_BIHAI_FUSANG:
                t = 2;
                break;
            default:
                t = -1;
        }
        return t;
    }

    public static Map<String, Integer> getTypeId() {
        Map<String, Integer> map = new HashMap<>();
        map.put("Novice", NOVICE.getId());
        map.put("Primary", PRIMARY.getId());
        map.put("Middle", MIDDLE.getId());
        map.put("Senior", SENIOR.getId());
        map.put("Super", SUPER.getId());
        return map;
    }

    //是否金币场
    public static boolean isGoldRoom(int type) {

        if (NOVICE.id == type)
            return true;

        if (PRIMARY.id == type)
            return true;

        if (SENIOR.id == type)
            return true;

        if (MIDDLE.id == type)
            return true;

        if (ROOM_XIN_SHOU_YU_CUN.id == type)
            return true;

        if (ROOM_NEW_DOWNLOAD.id == type)
            return true;

        return false;
    }

    //是否灵石场房间 12 13 14 15 17 20 22
    public static boolean isLSRoom(int type) {
        if (RoomTypeEnum.ROOM_BIHAI_FUSANG.ordinal() == type)
            return true;

        if (RoomTypeEnum.ROOM_N_BOMB.ordinal() == type)
            return true;

        if (RoomTypeEnum.ROOM_N_BOMB_PRIME.ordinal() == type)
            return true;

        if (RoomTypeEnum.ROOM_N_BOMB_MID.ordinal() == type)
            return true;

        if (RoomTypeEnum.ROOM_N_BOMB_SENIOR.ordinal() == type)
            return true;

        if (RoomTypeEnum.ROOM_PERSONAL.ordinal() == type)
            return true;

        if (RoomTypeEnum.ROOM_ANCIENT_RELICS.ordinal() == type)
            return true;

        return false;
    }


    //是否灵石场(体验模式)房间
    public static boolean isExperienceRoom(int type) {
        if (RoomTypeEnum.ROOM_EXPERIENCE_BOMB.ordinal() == type)
            return true;

        if (RoomTypeEnum.ROOM_EXPERIENCE_BOMB_PRIME.ordinal() == type)
            return true;

        if (RoomTypeEnum.ROOM_EXPERIENCE_BOMB_MID.ordinal() == type)
            return true;

        if (RoomTypeEnum.ROOM_EXPERIENCE_BOMB_SENIOR.ordinal() == type)
            return true;

        if (RoomTypeEnum.ROOM_EXPERIENCE_PERSONAL.ordinal() == type)
            return true;

        if (RoomTypeEnum.ROOM_EXPERIENCE_BIHAI_FUSANG.ordinal() == type)
            return true;

        return false;
    }

    public static String GetDropName(int roomType) {
        if (isExperienceRoom(roomType))
            return "灵石";

        if (isLSRoom(roomType))
            return "灵石";

        if (isGoldRoom(roomType))
            return "金币";

        return StringUtils.EMPTY;
    }
}
