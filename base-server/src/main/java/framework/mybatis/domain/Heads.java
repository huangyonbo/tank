/**   
*    
* 描述：   
* 文件：Heads.java
* 创建人：胡中伟
* 创建时间：2018年4月24日 下午8:22:21 
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
public class Heads {
	@TableId(type = IdType.AUTO)
	private Integer id = 0;
	private Integer uid = 0;
	private String url;
}
