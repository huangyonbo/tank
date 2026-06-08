package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class Cornucopia {
    @TableId
    private Integer id;
    private Integer uid;
    private String createTime;
    private String userName;
    private Integer value;
    private Integer channelId;;
    private String date;;
}
