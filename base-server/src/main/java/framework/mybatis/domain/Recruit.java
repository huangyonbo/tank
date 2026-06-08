package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * 好友招募 Created by 赵俊 on 2019/7/9.
 */
@Data
public class Recruit {
	@TableId
	private Integer id = 0;
	private String openDate;// 开启好友招募时间
}
