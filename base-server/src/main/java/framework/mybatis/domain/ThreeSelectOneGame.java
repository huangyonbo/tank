package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class ThreeSelectOneGame {
    @TableId
    Integer id;
    Integer winIndex;
    String createTime;
    Long totalValue;
    Long totalValue2;
}
