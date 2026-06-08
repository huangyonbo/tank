/**   
*    
* 描述：   
* 文件：OnlinePeak.java
* 创建人：胡中伟
* 创建时间：2018年7月11日 上午9:47:34 
*    
*/
package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
@Data
public class OnlinePeak {
	@TableId(type = IdType.AUTO)
	private Integer id = 0;
	private Integer channel = 0;
	private Integer count = 0;
	private String date;
}
