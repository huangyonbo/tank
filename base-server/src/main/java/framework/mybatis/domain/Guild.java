package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class Guild {
	@TableId
	private Integer id = 0;
	private String name;
	private String declaration;
	private Integer creator = 0;
	private String creatorNickName;
	private Integer guildLevel = 0;
	private Integer memberMax = 0;
	private Integer curMemberNum = 0;
	private Integer reqVipLimit = 0;
	private Integer reqLevelLimit = 0;
	private Integer needApprove = 0;
	private Integer guildStatus = 0;
	private String createTime;
	private String powerConfig;
	private Integer repoCapacity = 0;
	private Integer guildRank = 0;
	private Long totalWealthVal = 0L;
	@TableField("total_bomb_coin")
	private Long totalMojin = 0L;
	private Integer refuseReq = 0;
}