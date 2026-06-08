package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class VersionReward {
	@TableId(type = IdType.AUTO)
	private Integer id = 0;
	private Integer status = 0;
	private String channel;
	private String curVer;
	private String tarVer;
	private String startTime;
	private String endTime;
	private String appendix;
}
