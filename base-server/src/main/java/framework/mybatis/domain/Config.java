package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;


@Data
public class Config {
	@TableId
	private String id;
	private String value;
}
