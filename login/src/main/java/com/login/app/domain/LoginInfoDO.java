package com.login.app.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 
 * @author keyking
 * @email keyking@163.com
 * @date 2021-05-08 17:11:52
*/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginInfoDO implements Serializable {

	private static final long serialVersionUID = 1L;
	//uid
	private Integer id;
	//账号
	private String userName;
	//密码（HASH）
	private String password;
	//游戏类型
	private Integer gameType;
	//手机号码
	private String phone;
	//邮箱
	private String email;
	//盐值
	private String salt;
	//性别
	private Integer sex;
	//最后一次登录ip地址
	private String ip;
	//账号注册时间
	private Date registerTime;
	//最后一次登录时间
	private Date loginTime;
	//注册方式
	private Integer registerType;
	//渠道ID
	private Integer channelId;
	//注册时设备ID
	private String registerDeviceId;
	//注册设备型号
	private String model;
	//是否绑定
	private Integer bind;
	//招募者分享码
	private String recruiter;
	//实名认证
	private String pi;
	//第三方昵称
	private String nickname;
	//省份
	private String province;
	//城市
	private String city;
	//国家
	private String country;
	//状态
	private Integer status;//0正常；1封禁
}
