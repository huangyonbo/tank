package com.login.app.domain;

import lombok.Data;

import java.io.Serializable;


/**
 * 游戏用户
 * @author keyking
 * @email keyking@163.com
 * @date 2021-11-23 11:10:57
*/
@Data
public class RolesDO implements Serializable {

	private static final long serialVersionUID = 1L;
	//
	private Integer id;
	//渠道
	private Integer channel;
	//创建时间
	private String createTime;
	//登录时间
	private String loginTime;
	//手机号
	private String phoneNo;
    //状态
    private Integer status;//0正常；1封禁

}
