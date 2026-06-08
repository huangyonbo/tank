/**   
*    
* 描述：   
* 文件：Dau.java
* 创建人：胡中伟
* 创建时间：2018年7月9日 上午10:36:56 
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
public class Dau {
	@TableId(type = IdType.AUTO)
	protected Integer id = 0;
	private Integer uid = 0;
	private Integer channel = 0;
	private Integer onlineTime = 0;
	private Integer loginCount = 0;
	private String date;
}
