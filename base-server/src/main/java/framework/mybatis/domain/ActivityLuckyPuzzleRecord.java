package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class ActivityLuckyPuzzleRecord {
  @TableId(type = IdType.AUTO)
  private Integer id = 0;
  private Integer uid = 0;
  private String createTime;
  @TableField("nickname")
  private String nickName;
  private Integer op = 0;
  private String items;
}
