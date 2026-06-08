package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class DailyReward {
	@TableId(type = IdType.AUTO)
	protected Integer id;
	private Integer status = 0;
	@TableField("recv_uid")
	private Integer recUid = 0;
	private String channel;
	private String startTime;
	private String endTime;
	private String appendix;
	private String vipLevel;
	private String level;
	private String bulletVal;
}
