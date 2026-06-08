package enums;

import lombok.Getter;

/**
 * @Author hyb
 * @Date 2025/11/12
 * @Time 11:32
 * @Desc
 */
public enum PlayerPlace {
    PLACE_0(0, ""),
    PLACE_1(1, "渔场"),
    PLACE_2(2, "BMBC"),
    PLACE_3(3, "FQZS"),
    PLACE_4(4, "BRNN"),
    ;
    @Getter
    private final int id;
    private final String name;


    PlayerPlace(int id, String name) {
        this.id = id;
        this.name = name;
    }
}
