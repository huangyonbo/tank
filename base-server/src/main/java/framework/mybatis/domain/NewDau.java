/**   
*    
* 描述：   
* 文件：NewDau.java
* 创建人：胡中伟
* 创建时间：2018年7月11日 上午9:45:23 
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
public class NewDau {
	@TableId(type = IdType.AUTO)
	protected Integer id = 0;
	private Integer uid = 0;
	private Integer channel = 0;
	private Integer onlineTime = 0;
	private Integer loginCount = 0;
	private String date;
}
