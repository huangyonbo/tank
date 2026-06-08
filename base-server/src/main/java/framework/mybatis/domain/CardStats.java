/**   
*    
* 描述：   
* 文件：ChangeName.java
* 创建人：胡中伟
* 创建时间：2018年5月17日 上午9:17:17 
*    
*/
package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * 
 * 描述：
 * 
 */
@Data
public class CardStats {
	@TableId(type = IdType.AUTO)
	private Integer id = 0;
	private Integer uid = 0;
	private String statsTime;
	private Integer friendAmount = 0;
	private Integer friendRecharge = 0;
	private Integer fansRecharge = 0;
	private Integer cardAmount = 0;
}
