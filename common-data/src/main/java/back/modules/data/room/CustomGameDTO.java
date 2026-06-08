package back.modules.data.room;

import lombok.Data;

import java.io.Serializable;

@Data
public class CustomGameDTO implements Serializable {

	private static final long serialVersionUID = -3729283056502971457L;
	/**
	 * ID
	 */
	private Integer id;
	/**
	 * 桌类别
	 */
	private Integer type;
	/**
	 * 房间类别
	 */
	private Integer roomType;
	/**
	 * 桌状态
	 */
	private Integer status;
	/**
	 * 难度
	 */
	private Integer level;
	/**
	 * 最大炮值
	 */
	private Integer maxBv;
	/**
	 * 最小炮值
	 */
	private Integer minBv;
	/**
	 * 自动踢出
	 */
	private Integer autoKick;
	/**
	 * 桌数量
	 */
	private Integer count;
	/**
	 * 创建者
	 */
	private Integer createBy;
	/**
	 * 进入限制
	 */
	private String enterLimit;
	/**
	 * 密码桌密码
	 */
	private String password;

}
