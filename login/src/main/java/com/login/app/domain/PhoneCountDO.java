package com.login.app.domain;

import lombok.Data;

import java.io.Serializable;


/**
 * 手机号注册限制
 * @author keyking
 * @email keyking@163.com
 * @date 2021-10-09 12:11:41
*/
@Data
public class PhoneCountDO implements Serializable {
	private static final long serialVersionUID = 1L;
	//手机号码
	private String phone;
	//数量
	private Integer count;
}
