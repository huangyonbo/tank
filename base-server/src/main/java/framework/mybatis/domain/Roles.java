package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class Roles {
	@TableId
	private Integer id = 0;
	@TableField("username")
	private String userName;
	@TableField("headid")
	private Integer headId = 0;
	/** 炸弹拥有数量*/
	@TableField("bomb_item")
	private Integer bombItem;
	/** 货币数量 */
	@TableField("bombcoin")
	private Long bombcoin;
	private Integer sex = 0;
	private Integer status = 0;
	private Integer channel = 0;
	private Integer totalSec = 0;
	private Integer online = 0;
	private Integer type = 0;
	private String phoneNo;
	@TableField("reg_addr")
	private String regAddress;
	@TableField("login_addr")
	private String loginAddress;
	private String regDevice;
	private String loginDevice;
	private String devName;
	private String cliName;
	private String createTime;
	private String loginTime;
	private String storeTime;
	@TableField("realname")
	private String realName;
	@TableField("idcard")
	private String idCard;
	private String phoneBrand;
	private String phoneModel;
	private String lastOpt;
	private byte[] param;
	private Integer proxyId = 0;
	private Integer vip = 0;
}
