package framework.mybatis.domain;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class FunFishRecord {

  @TableId(type = IdType.AUTO)
  private Integer id = 0;
  private String createTime;
  private Integer uid = 0;
  private String nickname;
  private Integer vipLevel = 0;
  private String fishType;
  private Integer room = 0;
  private Integer gold = 0;
  private Integer bombCoin = 0;
  private Integer rewardSendState = 0;
  private Integer bulletValue = 0;
}
