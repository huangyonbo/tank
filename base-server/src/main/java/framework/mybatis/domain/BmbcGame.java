package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BmbcGame {
    @TableId
    Integer id;
    Integer winIndex;
    String createTime;
    Integer leader;
    String leaderName;
    Long totalGet;
    Long totalGive;
    Double rate;
    Double realRate;
    Long leaderDiamond;
    Long gameValue;

    Long afterDiamond;
}
