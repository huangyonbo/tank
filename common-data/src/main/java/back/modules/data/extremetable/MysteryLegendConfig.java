package back.modules.data.extremetable;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class MysteryLegendConfig implements Serializable {

    private static final long serialVersionUID = 2828644267014376357L;
    /**
     * ID
     */
    private Integer id;
    /**
     * 最小炮值
     */
    private Integer minBulletValue;
    /**
     * 最大炮值
     */
    private Integer maxBulletValue;
    /**
     *难度等级
     */
    private Integer level;
    /**
     * 时限
     */
    private Integer timeLimit;
    /**
     * 总玩
     */
    private Long totalPlay;
    /**
     * 总赢
     */
    private Long totalWin;
    /**
     * 房间类型
     */
    private Integer type;
    /**
     * 在线玩家数量
     */
    private Integer onlinePlayer;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 创建者
     */
    private String createBy;

}
