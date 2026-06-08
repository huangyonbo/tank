package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class PlayRobot {
	@TableId(type = IdType.AUTO)
	private Integer id = 0;
	private String bag;
	private String battery;
	private Integer bulletLevel = 0;
	private String createTime;
	private Long diamond = 0L;
	private Long gold = 0L;
	private Integer level = 0;
	private String name;
	private Integer open = 0;
	private Integer position = 0;
	private String title;
	private String updateTime;
	private Integer vipLevel = 0;
	private Integer status = 0;
}
