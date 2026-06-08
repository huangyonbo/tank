package back.modules.data.player;

import lombok.Data;

import java.io.Serializable;

@Data
public class PlayerPlayWinGameDTO implements Serializable {

    private static final long serialVersionUID = -2238660903764071918L;
    /**
     * 房间id
     */
    private Integer roomId;
    /**
     * 总玩
     */
    private Long play;
    /**
     * 总得
     */
    private Long win;
}
