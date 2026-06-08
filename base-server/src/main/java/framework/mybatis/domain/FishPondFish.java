package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;


@Data
public class FishPondFish {
	@TableId(type = IdType.AUTO)
	protected Integer id = 0;
	private Integer fishId = 0;
	private Integer size = 0;
	private Integer lifeTime = 0;
	private Integer curValue = 0;
	private Integer catchCost= 0;
	private Integer catchFailTimes = 0;
	private Integer costType = 0;
	private Integer pusherUid = 0;
	private String pusherName;
	private Long   pushTime = 0L;
	private Integer isRobotFish = 0;
	private Integer bindPlayerNum = 0;
	@TableField(exist = false)
	private Integer maxCatchTimes = 0;
	@TableField(exist = false)
	private Integer path = 0;
	@TableField(exist = false)
	private String fishName;
	@TableField(exist = false)
	private Integer canGetReward = 0;
}