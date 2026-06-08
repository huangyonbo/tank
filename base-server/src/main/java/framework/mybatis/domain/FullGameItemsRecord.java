package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FullGameItemsRecord {
    @TableId(type = IdType.AUTO)
    private Integer id;
    /**
     * 用户Uid
     */
    private int uid;
    /**
     * VIP等级
     */
    private Integer vip;
    /**
     * 道具名称
     */
    private String item;
    /**
     * 道具名称
     */
    private String nick_name;
    /**
     * 总量
     */
    private Integer count;
    /**
     * 剩余
     */
    private Integer surplus;
    /**
     * 创建时间
     */
    private String createTime;

    private Integer activityType;

    /**
     * 创建时间
     */
    private String reason;

}
