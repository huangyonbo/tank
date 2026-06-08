package framework.mybatis.domain;

import lombok.Data;


/**
 * 支付渠道
 * @author keyking
 * @email keyking@163.com
 * @date 2021-10-11 14:11:41
*/
@Data
public class PayChannel {
	//渠道编号
	private Integer id;
	//渠道名称
	private String name;
	//支付路由
	private String route;
	//道具积分
	private Integer itemScore;
	//实名认证
	private Integer certification;
	//奖券掉落率
	private Integer dropLotteryRatio;
	//支付包名
	private String packageName;
	//公共积分比例
	private int pubRatio;
	//当前公共积分
	private long curPubScore;
	//最大公共积分
	private long maxPubScore;
}
