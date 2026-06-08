/**   
*    
* 描述：   
* 文件：Mails.java
* 创建人：胡中伟
* 创建时间：2018年4月12日 上午10:51:07 
*    
*/
package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * 
 * 描述：
 * 
 */
@Data
public class SendItems {
	@TableId
	private String id;
	private Integer type = 0;
	private Integer state = 0;
	private Integer channel = 0;
	@TableField("senderuid")
	private Integer senderUid = 0;
	@TableField("sendername")
	private String senderName;
	@TableField("recvuid")
	private Integer recUid = 0;
	@TableField("recvname")
	private String recName;
	private String createTime;
	private String updateTime;

	private String appendix;
	@TableField("systemtype")
	private Integer systemType = 0;
	private String senderHas;
	@TableField("recv_has")
	private String recHas;
}
