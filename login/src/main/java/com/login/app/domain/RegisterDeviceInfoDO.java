package com.login.app.domain;

import lombok.Data;

import java.io.Serializable;


/**
 * 
 * @author keyking
 * @email keyking@163.com
 * @date 2021-05-11 08:12:04
*/
@Data
public class RegisterDeviceInfoDO implements Serializable {
	private static final long serialVersionUID = 1L;
	//
	private Integer id = 0;
	//设备id
	private String deviceId;
	//注册账号数量
	private Integer accountCount = 0;
	//渠道id
	private Integer channel;
}
