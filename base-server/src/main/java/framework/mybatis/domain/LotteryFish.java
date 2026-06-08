package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class LotteryFish {
    @TableId(type = IdType.AUTO)
    private Integer id = 0;//编号
    private Integer roomType = 0;//房间类型
    private String startTime;//开始时间
    private String endTime;//结束时间
    private Integer lotteryLimit = 0;//奖券上限
    private Integer lotteryValue = 0;//已掉落奖券
    private Integer refreshTime = 0;//刷新间隔
    private Integer dropMin = 0;//掉落最小值
    private Integer dropMax = 0;//掉落最大值
    private Integer fishLimit = 0;//奖券鱼上限
    private Integer fishValue = 0;//已出奖券鱼数量
    private Integer outRatio;//出鱼概率
}
