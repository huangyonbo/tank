/**   
*    
* 描述：   
* 文件：OfflineData.java
* 创建人：胡中伟
* 创建时间：2018年5月8日 上午10:20:53 
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
public class OfflineData {
	@TableId(type = IdType.AUTO)
	private Integer id = 0;
	private Integer uid = 0;
	private Integer state = 0;
	private Integer type = 0;
	private String context;
	private String reason;
	private String createTime;
}
