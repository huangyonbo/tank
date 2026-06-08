package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

@Data
public class CustomGame {

	@TableId(type = IdType.AUTO)
	protected Integer id = 0;
	private Integer type = 0;
	@TableField("roomtype")
	private Integer roomType = 0;
	private Integer createBy;
	private Integer status = 0;
	private Integer online = 0;
	private String enterLimit;
	@TableField("minbv")
	private Integer minBv = 0;
	@TableField("maxbv")
	private Integer maxBv = 0;
	private Integer level = 0;
	@TableField("totalplay")
	private Long totalPlay = 0L;
	@TableField("totalwin")
	private Long totalWin = 0L;
	@TableField("autokick")
	private Integer autoKick = 5 * 60000 ;
	private String passwd;
	private Date createTime;
}
