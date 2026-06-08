/**   
*    
* 描述：   
* 文件：Advice.java
* 创建人：胡中伟
* 创建时间：2018年5月14日 下午1:46:46 
*    
*/
package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * 
 * 描述：
 * 
 */
@Data
public class AdsConfig {
	@TableId()
	private Integer channel;
	private String startTime;
	private String endTime;
}
