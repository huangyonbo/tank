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
public class ListHistory {
	@TableId(type = IdType.AUTO)
	private Integer id = 0;
	private String type;
	private String name;
	private byte[] data;
	private String createTime;
}
