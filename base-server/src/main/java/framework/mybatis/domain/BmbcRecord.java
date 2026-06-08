package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BmbcRecord {
    @TableId
    private Long id;
    private Integer uid;
    private String createTime;
    private Integer gameId;
    private Integer leader;
    private String leaderName;
    private Integer winIndex;
    private String param;
    private Long total;
    private Long win;
}
