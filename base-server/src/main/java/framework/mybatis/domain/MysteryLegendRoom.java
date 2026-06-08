package framework.mybatis.domain;

import back.modules.data.extremetable.MysteryLegendPlayer;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
public class MysteryLegendRoom {
  /**
   * 主键
   */
  @TableId(type = IdType.AUTO)
  private Integer id = 0;
  /**
   * 最小炮值
   */
  private Integer minBulletValue = 0;

  /**
   * 最大炮值
   */
  private Integer maxBulletValue = 0;
  /**
   * 难度等级
   */
  private Integer level = 0;
  /**
   * 时限
   */
  private Integer timeLimit = 0;
  /**
   * 总玩
   */
  private Long totalPlay = 0L;
  /**
   * 总赢
   */
  private Long totalWin = 0L;
  /**
   * 房间类型
   */
  private Integer type = 0;
  /**
   * 在线玩家数量
   */
  private Integer onlinePlayer = 0;
  /**
   * 创建时间
   */
  private Date createTime;
  /**
   * 创建者
   */
  private String createBy;

  /**
   * 此炮值区间内的玩家数据
   */
  @TableField(exist = false)
  private Map<Integer, MysteryLegendPlayer> playerMap = new HashMap<>();
}
