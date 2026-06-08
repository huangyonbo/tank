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
public class WarningMojin {
	@TableId(type = IdType.AUTO)
	private Integer id = 0;
	private Integer uid = 0;
	private Integer vipLevel = 0;
	@TableField("nickname")
	private String nickName;
	private Long play = 0L;
	private Long win = 0L;
	private Long cur = 0L;
	private Integer hbomb = 0;
	private Integer hbombDebris = 0;
	private Integer nbomb = 0;
	private Integer nbombDebris = 0;
	private String otherDetail;
	private String createTime;
	private String updateTime;
	private Long dmojin = 0L;
}
