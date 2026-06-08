package framework.mybatis.domain;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@TableName("user_withdraw_record")
@Data
@Accessors(chain = true)
public class UserWithdrawRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer userId;
    private Long baseAmount;
    private Long amount;
    private Long exAmount;
    private Integer extractType;
    private Date time;
}