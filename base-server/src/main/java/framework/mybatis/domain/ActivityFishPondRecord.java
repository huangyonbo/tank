package framework.mybatis.domain;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class ActivityFishPondRecord {
    @TableId(type = IdType.AUTO)
    private Integer id = 0;
    private String createTime;
    private Integer uid = 0;
    @TableField("nickname")
    private String nickName;
    private Integer vipLevel = 0;
    private Integer op = 0;
    private String cost;
    private String income;
    private Integer caughtUid = 0;
    private String caughtNickname;
    private String systemFishId;
    private String systemFishName;
    private Integer systemFishState = 0;
    private String systemFishValue;
}
