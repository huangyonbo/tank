package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class MermaidTreasureRecord {
	@TableId
	protected Integer id = 0;
	private String date;
	private Integer uid = 0;
	private String userName;
	private String costItemId;
	private Integer costItemCount = 0;
	private Integer boxLevel = 0;
	private Integer type = 0;
	private String itemId;
	private Integer itemCount = 0;
}