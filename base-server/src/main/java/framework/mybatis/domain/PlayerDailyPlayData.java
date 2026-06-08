package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class PlayerDailyPlayData {
	@TableId(type = IdType.AUTO)
	private Integer id = 0;
	private Integer uid = 0;
	private Integer vipLevel = 0;
	private Integer roomId = 0;
	private Integer roomDf = 0;
	private Integer dayEnterTimes = 0;
	private Integer proxyId = 0;
	private Long mojin = 0L;
	private Long dayCost = 0L;
	private Long dayWin = 0L;
	private Long dayPlayTime = 0L;
	@TableField("nickname")
	private String nickName;
	private String itemHas;
	private String date;
}