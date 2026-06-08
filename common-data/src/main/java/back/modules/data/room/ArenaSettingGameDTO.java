package back.modules.data.room;

import lombok.Data;

import java.io.Serializable;


/**
 * 竞技场设置
 *
 * @author gzc
 */
@Data
public class ArenaSettingGameDTO implements Serializable {

	private static final long serialVersionUID = -8670595464314803643L;
	/**
	 * ID
	 */
	private Integer id;

	/**
	 * 比赛类型
	 */
	private Integer type;

	/**
	 * 比赛名称
	 */
	private String name;

	/**
	 * 渠道
	 */
	private String channel;

	/**
	 * 状态
	 */
	private Integer status;

	/**
	 * 房间分类
	 */
	private Integer roomCategory;

	/**
	 * 房间类型
	 */
	private Integer roomType;

	/**
	 * 比赛奖励
	 */
	private String award;

	/**
	 * 比赛图标
	 */
	private Integer icon;

	/**
	 * 比赛时长（分钟）
	 */
	private Integer keepTime;

	/**
	 * 开启时间
	 */
	private Long startTime;

	/**
	 * 关闭时间
	 */
	private Long endTime;

	/**
	 * 结算时间（分钟）
	 */
	private Integer clearingTime;

	/**
	 * 间隔时间（分钟）
	 */
	private Integer intervalTime;

	/**
	 * 保护时间（分钟）
	 */
	private Integer protectTime;

	/**
	 * 参赛消耗
	 */
	private String sign;

	/**
	 * 默认炮值
	 */
	private Integer defaultBullet;

	/**
	 * 炮值变化
	 */
	private Integer bulletChange;

	/**
	 * 最大炮值
	 */
	private Integer maxBullet;

	/**
	 * 最小炮值
	 */
	private Integer minBullet;

	/**
	 * 进入条件
	 */
	private String enterLimit;

	/**
	 * 参与次数
	 */
	private Integer joinTimes;

	/**
	 * 循环次数
	 */
	private Integer maxRound;

	/**
	 * 代币数量
	 */
	private Integer moneyCount;

	/**
	 * 最多容纳人数
	 */
	private Integer maxCapacity;

	/**
	 * 最少容纳人数
	 */
	private Integer minCapacity;

	/**
	 * 第一名跑马灯公告
	 */
	private Boolean marquee;

	/**
	 * 机器人占榜
	 */
	private String occupy;

}
