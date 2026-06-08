package back.modules.data.room;

import lombok.Data;

import java.io.Serializable;

@Data
public class CustomDeskPlayerDTO implements Serializable {
    private static final long serialVersionUID = -3600831131994457741L;

    private Integer deskId;

    private Integer seatId;

    private Integer uid;

    private Long totalPlay;

    private Long totalWin;
    /**
     * 至尊三叉戟
     */
    private Long skillNBomb;
    /**
     * 传说三叉戟
     */
    private Long skillHBomb;
    /**
     * 魔晶
     */
    private Long bombCoin;
}
