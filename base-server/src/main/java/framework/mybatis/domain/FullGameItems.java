package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class FullGameItems {
    @TableId(type = IdType.AUTO)
    private Integer id;
    /**
     * 道具名称
     */
    private String item;
    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 总量
     */
    private Integer total;
    private Boolean isOnly;
    /**
     * 剩余
     */
    private Integer surplus;
    /**
     * 更新时间
     */
    private String updateTime;
}
