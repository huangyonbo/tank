package back.modules.data.room;

import lombok.Data;

import java.io.Serializable;

@Data
public class BaseRoomDTO implements Serializable {

    private static final long serialVersionUID = -4550183782882792709L;
    /**
     * 房间ID
     */
    private Integer roomId;
    /**
     * 在线人数
     */
    private Integer onlinePlayer;
    /**
     * 玩家消耗
     */
    private Long totalPlay;
    /**
     * 玩家所得
     */
    private Long totalWin;
    /**
     * 差值
     */
    private Long totalDeviation;
    /**
     * 最小炮值
     */
    private Integer minBulletValue;
    /**
     * 最大炮值
     */
    private Integer maxBulletValue;
    /**
     * vip等级限制
     */
    private Integer vipLevelLimit;
    /**
     * 自动踢出
     */
    private Integer autoKickTime;

}
