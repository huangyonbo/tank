package com.login.bms.domain;

import lombok.Data;

import java.io.Serializable;


/**
 * 
 * @author keyking
 * @email keyking@163.com
 * @date 2021-05-17 10:17:44
*/
@Data
public class DeviceInfoDO implements Serializable {
	private static final long serialVersionUID = 1L;
	//
	private Integer id;
	//
	private String date;
	//
	private String deviceId;
	//
	private Integer channel;
}
