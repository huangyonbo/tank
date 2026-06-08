package com.login.prop.domain;

import lombok.Data;

import java.io.Serializable;


/**
 * 系统配置
 * @author keyking
 * @email keyking@163.com
 * @date 2021-05-08 18:04:02
*/
@Data
public class PropConfigDO implements Serializable {
	private static final long serialVersionUID = 1L;
	//配置key
	private String id;
	//配置具体参数
	private String data;
}
