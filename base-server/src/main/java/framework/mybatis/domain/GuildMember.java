package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class GuildMember {
	@TableId
	private Integer uid = 0;
	private Integer guildId = 0;
	@TableField("headid")
	private Integer headId = 0;
	private Integer vipLevel = 0;
	private Integer level = 0;
	private Integer contributionVal = 0;
	private Long wealthVal = 0L;
	private Integer guildPosition = 0;
	@TableField("nickname")
	private String nickName;
	private String reqTime;
	private String joinTime;
	private Integer offlineTime = 0;
	private Long offlineTimestamp = 0L;
	@TableField("bomb_coin")
	private Long mojin = 0L;
}