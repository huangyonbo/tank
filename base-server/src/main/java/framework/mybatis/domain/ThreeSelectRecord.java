package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class ThreeSelectRecord {
    @TableId
    private Long id;
    private Integer uid;
    private String createTime;
    private Integer gameId;
    private Integer winIndex;
    private Long one;
    private Long two;
    private Long three;
    private Long total;
    private Long win;
}
