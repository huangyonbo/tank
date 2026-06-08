package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

// 兑换记录
@Data
public class RedeemCodeRecord  {
	@TableId
	private String id;
	@TableField("deviceid")
	private String deviceId;
	private String createTime;
}
