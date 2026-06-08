package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;


@Data
public class ArenaRecord {

	@TableId(type = IdType.AUTO)
	protected Integer id = 0;
	@TableField("gameid")
	protected Integer gameId = 0;
	@TableField("turnid")
	protected Integer turnId = 0;
	protected Integer signPop = 0;
	protected Integer signCount = 0;
	protected Integer joinPop = 0;
	protected Integer joinCount = 0;
	protected Integer robotCount = 0;
	protected String startTime;
	protected String endTime;
	protected String signs;
	protected String rewards;
	private byte[] ranks;
}
