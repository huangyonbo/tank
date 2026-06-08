package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.util.Date;

/**
 * 排行榜奖励配置
 *
 * @author gzc
 */
@Data
public class RankList {
	/**
	 * ID
	 */
	@TableId(type = IdType.AUTO)
	private Long id = 0L;
	/**
	 * 排行榜属性名
	 */
	private String proName;
	/**
	 * 排名
	 */
	private Integer rankIndex = 0;
	/**
	 * 奖励
	 */
	private String reward;
	/**
	 * 创建时间
	 */
	private Date createTime;

}
