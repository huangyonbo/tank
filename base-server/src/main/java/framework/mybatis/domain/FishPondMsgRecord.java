package framework.mybatis.domain;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class FishPondMsgRecord {

  @TableId(type = IdType.AUTO)
  private Integer id = 0;
  private String createTime;
  private Integer type = 0;
  private Integer uid = 0;
  @TableField("nickname")
  private String nickName;
  private String cost;
}
