package back.modules.data.guild;

import lombok.Data;

import java.io.Serializable;


/**
 * 公会信息
 *
 * @author gzc
 */
@Data
public class GuildBO implements Serializable {

	private static final long serialVersionUID = -2162381088298787490L;
	/**
	 * 公会ID
	 */
	private Integer id;

	/**
	 * 公会名称
	 */
	private String name;

	/**
	 * 公会公告
	 */
	private String declaration;

	/**
	 * 公会创建者
	 */
	private Integer creator;

	/**
	 * 创建者昵称
	 */
	private String creatorNickName;

	/**
	 * 公会等级
	 */
	private Integer guildLevel;

	/**
	 * 最大成员数
	 */
	private Integer memberMax;

	/**
	 * 当前成员数
	 */
	private Integer curMemberNum;

	/**
	 * 入会等级限制
	 */
	private Integer reqLevelLimit;

	/**
	 * 入会vip限制
	 */
	private Integer reqVipLimit;

	/**
	 * 是否需要审核
	 */
	private Integer needApprove;

	/**
	 * 公会状态
	 */
	private Integer guildStatus;


	/**
	 * 职位权限配置
	 */
	private String powerConfig;

	/**
	 * 仓库背包容量
	 */
	private Integer repoCapacity;

	/**
	 * 排名
	 */
	private Integer guildRank;

	/**
	 * 总财富(金币)
	 */
	private Long totalWealthVal;

	/**
	 * 不接受申请
	 */
	private Integer refuseReq;

}
