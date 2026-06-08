package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class GuildRepoRecord {
	@TableId(type = IdType.AUTO)
	protected Integer id = 0;
	private Integer uid = 0;
	@TableField("nickname")
	private String nickName;
	private Integer vip = 0;
	private Integer op = 0;
	private Integer takeUid = 0;
	private String takeNickname;
	@TableField("itemid")
	private String itemId;
	private Integer count = 0;
	private String createTime;
	private Integer guildId = 0;
	private String playerHas;
	private String takerPlayerHas;
	private String guildName;
	private String recordId;
}