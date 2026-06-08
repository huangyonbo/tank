package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class MojinRoomRecord {
	@TableId(type = IdType.AUTO)
	private Integer id = 0;
	private String date;
	protected Integer roomType = 0;
	protected Integer roomDf = 0;
	protected Integer totalPlayerCount = 0;
	protected Integer todayActive = 0;
	protected Integer maxPlayerCount = 0;
	protected String avgPlayTime;
	protected String avgEnterTimes;
	protected Long todayPlay = 0L;
	protected Long todayWin = 0L;
}