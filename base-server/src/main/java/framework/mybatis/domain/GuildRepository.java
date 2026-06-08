package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class GuildRepository {
	@TableId(type = IdType.AUTO)
	protected Integer id = 0;
	private Integer guildId = 0;
	private String recordId;
	private String storeTime;
	private Integer uid = 0;
	private Integer vip = 0;
	private String nickname;
	@TableField("itemid")
	private String itemId;
	private Integer count = 0;
	private String password;
}