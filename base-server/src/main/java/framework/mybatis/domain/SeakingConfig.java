package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class SeakingConfig {
	@TableId
	private Integer id = 0;
	private String value;
}
