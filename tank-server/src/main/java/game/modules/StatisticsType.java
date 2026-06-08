package game.modules;

import lombok.Getter;

@Getter
public enum StatisticsType {
    YUCHANG(1, "yuchang"),
    FQZS(2, "feiqin"),
    FQZS_LEADER(3, "feiqinFuzhi"),
    BCBM(4, "benchi"),
    BCBM_LEADER(5, "benchiFuzhi"),
    SHZ(6, "shuihu"),
    BRNN(7, "bairun"),
    BRNN_LEADER(8, "bairunFuzhi"),
    SGML(9, "shuiguo"),
    ;

    private final int id;
    private final String desc;

    StatisticsType(int id, String desc) {
        this.id = id;
        this.desc = desc;
    }
}
