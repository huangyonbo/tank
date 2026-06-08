package framework.mybatis.domain;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@TableName("user_consume_daily")
@Data
@Accessors(chain = true)
public class UserConsumeDaily {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer userId;
    private String bizDate;
    private Long todayAmount;
    private Long totalAmount;
}