package back.modules.data.player;

import lombok.Data;

import java.io.Serializable;

@Data
public class OnlinePlayer implements Serializable {

    private static final long serialVersionUID = 5261845121512841809L;
    /**
     * 玩家ID
     */
    private Integer uid;
    /**
     * 玩家昵称
     */
    private String nickname;

    /**
     * 渠道
     */
    private Integer channel;
    /**
     * 等级
     */
    private int level;
    /**
     * VIP等级
     */
    private int vipLevel;
    /**
     * 累计充值
     */
    private int totalRecharge;
    /**
     * 钻石
     */
    private long diamond;
    /**
     * 金币
     */
    private long gold;
    /**
     * 奖券
     */
    private long colorTicket;
    /**
     * 道具积分
     */
    private int itemScore;
    /**
     * 充值积分
     */
    private int rechargeScore;
    /**
     * 核能
     */
    private long bombCoin;
    /**
     * 金币炮等级
     */
    private int bulletLevel;
    /**
     * 核能炮等级
     */
    private int nuclearBulletLevel;
    /**
     * 道具
     */
    private String item;
}
