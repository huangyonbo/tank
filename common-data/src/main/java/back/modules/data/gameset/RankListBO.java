package back.modules.data.gameset;

import back.modules.data.Item;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 排行榜奖励配置
 *
 */
@Data
@NoArgsConstructor
public class RankListBO implements Serializable {
	private static final long serialVersionUID = 1503871598001317483L;

	/**
	 * 排行榜属性名
	 */
	private String proName;
	/**
	 * 排名
	 */
	private Integer rank;
	/**
	 * 奖励
	 */
	private List<Item> rewardList;
}
