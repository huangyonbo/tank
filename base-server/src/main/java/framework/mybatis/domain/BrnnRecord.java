package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BrnnRecord {
    @TableId
    private Long id;
    private Integer uid;
    private Integer gameId;
    private String betParam;
    private String cardParam;
    private Long win;
    private Integer leader;
    private String leaderName;
    private String createTime;

}
