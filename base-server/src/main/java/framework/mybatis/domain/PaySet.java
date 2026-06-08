package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class PaySet {
	@TableId
	private Integer channel = 0;
	@TableField("payset")
	protected String paySet;
}