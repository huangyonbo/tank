package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class LotteryRecorder {
    @TableId(type = IdType.AUTO)
    private Integer id = 0;//编号
    private Integer uid = 0;//玩家编号
    private Integer roomType = 0;//房间类型
    private Integer bulletValue = 0;//炮值
    private Integer gotValue = 0;//获得奖券数量
    private String createTime;//时间
}
