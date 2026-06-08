/**   
*    
* 描述：   
* 文件：Order.java
* 创建人：胡中伟
* 创建时间：2018年4月27日 下午4:54:19 
*    
*/
package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

@Data
public class OrderLog {
	@TableId(type = IdType.AUTO)
	private Integer id = 0;
	private Integer uid = 0;
	private Integer channel = 0;
	private String name;
	private String item;
	private String material;
	private String createBy;
	private Integer status = 0;
	private Date createTime;
	private Date updateTime;
	private String compTime;
	private String remarks;
	private Integer type = 0; // 商品类型 0-虚拟物品 1-实物,2-vip成长，3灵石大作战
	private String updateBy;
	private Integer vip;
}
