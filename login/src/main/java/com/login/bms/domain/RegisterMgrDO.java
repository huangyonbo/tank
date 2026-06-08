package com.login.bms.domain;

import lombok.Data;

import java.io.Serializable;


/**
 * 
 * @author keyking
 * @email keyking@163.com
 * @date 2021-05-10 18:02:45
*/
@Data
public class RegisterMgrDO implements Serializable {
	private static final long serialVersionUID = 1L;
	//
	private Integer id;
	//渠道id
	private Integer channelId;
	//手机注册开关
	private Integer phoneRegister;
	//账号注册开关
	private Integer accountRegister;
	//账号注册上限
	private Integer registerLimit;
}
