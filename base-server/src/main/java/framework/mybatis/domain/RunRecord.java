/**   
*    
* 描述：   
* 文件：RunRecord.java
* 创建人：胡中伟
* 创建时间：2018年5月30日 下午3:21:35 
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
public class RunRecord {
	@TableId(type = IdType.AUTO)
	private Integer id = 0;
	private String serName;
	private Integer roomType = 0;
	private Long totalPlay = 0L;
	private Long totalWin = 0L;
	private String createDate;
}
