package back.modules.data.extremetable;


import lombok.Data;

import java.io.Serializable;

@Data
public class MysteryLegendPlayer implements Serializable {

    private static final long serialVersionUID = -1959525988301143927L;

    private Integer uid;

    private String nickName;

    private Integer vipLevel;

    /**
     * 单次在线时长
     */
    private Long totalTime;

    /**
     * 开始游玩时间
     */
    private Long startTime;

    /**
     * 单次总玩
     */
    private Long totalPlay;

    /**
     * 单次总赢
     */
    private Long totalWin;

}
