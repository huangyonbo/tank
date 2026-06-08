package framework.mybatis.domain;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@TableName("user_withdraw")
@Data
@Accessors(chain = true)
public class UserWithdraw {
    @TableId
    private Integer uid;
    private Long baseAmount = 0L;
    private Long amount = 0L;
    private Long exAmount = 0L;
}