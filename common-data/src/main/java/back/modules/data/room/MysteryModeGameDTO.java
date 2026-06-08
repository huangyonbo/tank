package back.modules.data.room;

import lombok.Data;

import java.io.Serializable;

@Data
public class MysteryModeGameDTO implements Serializable {

    private static final long serialVersionUID = -3763385429092560181L;
    /**
     * ID
     */
    private Integer id;

    /**
     * 房间类型
     */
    private Integer type;

    /**
     * 难度等级
     */
    private Integer level;

    /**
     * 最大炮值
     */
    private Integer maxBulletValue;

    /**
     * 最小炮值
     */
    private Integer minBulletValue;

    /**
     * 自动踢出
     */
    private Integer timeLimit;
}
