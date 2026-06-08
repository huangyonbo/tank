package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class ActivityDebrisPkgRecord {

  @TableId(type = IdType.AUTO)
  private Long id = 0L;
  private String createTime;
  private Integer uid = 0;
  @TableField("nickname")
  private String nickName;
  private Integer vipLevel = 0;
  private Long debrisCount = 0L;
  private Long accumulateDebrisCount = 0L;
  private Long debrisLimit = 0L;
  private String item;
}
