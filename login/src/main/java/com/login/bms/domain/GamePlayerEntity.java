package com.login.bms.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 游戏玩家
 *
 * @author gzc
 */
@Data
public class GamePlayerEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	/**
	 * 游戏玩家uid
	 */
	private Integer id;
	/**
	 * 渠道
	 */
	private Integer channel;
	/**
	 * 安装包名称
	 */
	private String cliName;
	/**
	 * 注册设备型号
	 */
	private String devName;
	/**
	 * 最近登录IP
	 */
	private String loginAddr;
	/**
	 * 最近登录设备号
	 */
	private String loginDevice;
	/**
	 * 登录时间
	 */
	private String loginTime;
	/**
	 * 是否在线
	 */
	private Integer online;
	/**
	 * 属性参数
	 */
	private byte[] param;
	/**
	 * 手机号
	 */
	private String phoneNo;
	/**
	 * 注册IP
	 */
	private String regAddr;
	/**
	 * 注册设备号
	 */
	private String regDevice;
	/**
	 * 性别
	 */
	private Integer sex;
	/**
	 * 账号状态
	 */
	private Integer status;
	/**
	 * 用户名
	 */
	private String username;
	/**
	 * 身份证
	 */
	private String idcard;
	/**
	 * 真实姓名
	 */
	private String realname;
	/**
	 * 品牌
	 */
	private String phoneBrand;
	/**
	 * 型号
	 */
	private String phoneModel;
	/**
	 * 最后一次操作
	 */
	private String lastOpt;
	/**
	 * 代理编号
	 */
	private Integer proxyId;

}
