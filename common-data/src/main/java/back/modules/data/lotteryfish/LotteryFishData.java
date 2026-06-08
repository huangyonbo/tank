package back.modules.data.lotteryfish;

import lombok.Data;

import java.io.Serializable;

@Data
public class LotteryFishData implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer id;//编号
    private Integer roomType;//房间类型
    private String startTime;//开始时间
    private String endTime;//结束时间
    private Integer lotteryLimit;//奖券上限
    private Integer lotteryValue;//已掉落奖券
    private Integer refreshTime;//刷新间隔
    private Integer dropMin;//掉落最小值
    private Integer dropMax;//掉落最大值
    private Integer fishLimit;//奖券鱼上限
    private Integer fishValue;//已出奖券鱼数量
    private Integer outRatio;//出鱼概率
}
