package back.modules.data.player;

import lombok.Data;

import java.io.Serializable;

/**
 * 扣除玩家道具
 */
@Data
public class DeductItemGameDTO implements Serializable {

    private static final long serialVersionUID = 6011624956221121060L;

    /**
     * 玩家ID
     */
    private Integer uid;

    /**
     * 道具ID
     */
    private String itemId;

    /**
     * 道具数量
     */
    private Integer count;
}
