package back.modules.data.guild;

import lombok.Data;

import java.io.Serializable;


/**
 * 公会配置
 *
 * @author gzc
 */
@Data
public class GuildConfigBO implements Serializable {

    private static final long serialVersionUID = 1339419014745511698L;
    /**
     * 公会总开关
     */
    private Integer guildSwitch;

    /**
     * 公会仓库容量
     */
    private Integer repoCapacity;

    /**
     * 公会访问等级限制
     */
    private Integer visitLvLimit;

    /**
     * 公会访问VIP限制
     */
    private Integer visitVipLimit;

    /**
     * 公会创建消耗(钻石)
     */
    private Integer createCost;

    /**
     * 公会创建VIP限制
     */
    private Integer createVipLimit;

    /**
     * 公会成员上限
     */
    private Integer memberMax;

    /**
     * 仓库访问VIP限制
     */
    private Integer guildRepoVip;

    /**
     * 换会时间限制
     */
    private Integer timeLimit;

    /**
     * 改名消耗
     */
    private Integer modifyNameCost;

}
