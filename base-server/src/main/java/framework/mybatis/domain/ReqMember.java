package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class ReqMember {

	@TableId(type = IdType.AUTO)
	private Integer id = 0;
	private Integer uid = 0;
	private Integer reqGuildId = 0;
	@TableField("headid")
	private Integer headId = 0;
	private Integer level = 0;
	private String nickName;
	private Integer vipLevel = 0;
	private Long mojin = 0L;
	private String reqTime;
}