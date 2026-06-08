package framework.mybatis.domain;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@TableName("Invite")
@Data
@Accessors(chain = true)
public class Invite {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer inviterId;
    private Integer receiverId;
    private Date time;
}