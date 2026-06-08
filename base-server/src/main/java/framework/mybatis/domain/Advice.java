/**   
*    
* 描述：   
* 文件：Advice.java
* 创建人：胡中伟
* 创建时间：2018年5月14日 下午1:46:46 
*    
*/
package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

/**
 * 
 * 描述：
 * 
 */
@Data
public class Advice {
	@TableId(type = IdType.AUTO)
	private Integer id;
	private Integer uid;
	private Integer channel;
	private Integer type;
	private Integer replyState;
	private String cliVer;
	private String context;
	private String reply;
	private String updateBy;
	private String createBy;
	private Date createTime;
	private Date updateTime;
}
