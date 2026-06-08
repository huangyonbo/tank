/**   
*    
* 描述：   
* 文件：Warning.java
* 创建人：胡中伟
* 创建时间：2018年5月15日 下午2:23:02 
*    
*/
package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * 
 * 描述：
 * 
 */
@Data
public class WarningItemScore {
	@TableId(type = IdType.AUTO)
	private Integer id = 0;
	private Integer uid = 0;
	private Integer vipLevel = 0;
	@TableField("nickname")
	private String nickName;
	private Integer chargeScore = 0;
	private Integer killFishItemScore = 0;
	private Integer drawAwardItemScore = 0;
	private Integer curItemScore = 0;
	private Integer maxItemScore = 0;
	private Long bombCoin = 0L;
	private Long gold = 0L;
	private String createTime;
	private String updateTime;
}
