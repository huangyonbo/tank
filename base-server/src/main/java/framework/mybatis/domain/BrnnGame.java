package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BrnnGame {
    @TableId
    private Integer id;
    private Long totalGet;
    private Long totalGive;
    private Integer leader;
    private String leaderName;
    private Long leaderDiamond;
    private Double realRate;
    private Long leaderWin;
    private String cardParam;
    private String createTime;
}
