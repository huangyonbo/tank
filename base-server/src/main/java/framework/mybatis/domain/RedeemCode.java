package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class RedeemCode {
	@TableId
	private String id;
	private Integer uid = 0;
	private Integer channel = 0;
	@TableField("binddevice")
	private Integer bindDevice = 0;
	@TableField("limitcount")
	private Integer limitCount = 0;
	private Integer used = 0;
	@TableField("lifetime")
	private Long lifeTime = 0L;
	private String context;
	private String createTime;
	private String endTime;
}
