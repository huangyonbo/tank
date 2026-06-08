package com.login.app.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;


/**
 * 
 * @author keyking
 * @email keyking@163.com
 * @date 2021-05-12 17:23:56
*/
@Data
public class AccountCancelRecordDO implements Serializable {
	private static final long serialVersionUID = 1L;
	//
	private Long id;
	//玩家ID
	private Integer uid;
	//玩家等级
	private Integer level;
	//玩家vip等级
	private Integer vipLevel;
	//申请注销时间
	private Date createTime;
	//所属渠道
	private Integer channel;
	//累计充值
	private Long accumulateRecharge;
	//钻石
	private Long diamond;
	//金币
	private Long gold;
	//核能
	private Long bombCoin;
	//奖券
	private Long colorTicket;
	//道具积分
	private Long itemScore;
	//道具积分上限
	private Long itemScoreLimit;
	//道具
	private byte[] itemParam;
	//类型，0：冻结中，1：已注销
	private Integer status;
}
