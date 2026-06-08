package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class Activity {
	@TableId(type = IdType.AUTO)
	protected Integer id = 0;
	private Integer type = 0;
	private String channel;
	private Integer status = 0;
	private String showStartTime;
	private String showEndTime;
	private String startTime;
	private String endTime;
	private Boolean send = false;
	private Integer weight = 0;
	private byte[] param;
	private String description;
}
