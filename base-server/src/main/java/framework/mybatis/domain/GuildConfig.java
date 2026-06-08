package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class GuildConfig {
//	@TableId(type = IdType.AUTO)
	private Integer id = 0;
	private Integer visitLvLimit = 0;
	private Integer visitVipLimit = 0;
	private Integer createCost = 0;
	private Integer createVipLimit = 0;
	private Integer memberMax = 0;
	private Integer timeLimit = 0;
	private Integer modifyNameCost = 0;
	private Integer guildRepoVip = 0;
	private Integer repoCapatory = 0;
	private Integer guildSwitch = 0;
}