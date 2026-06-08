package com.login.common.utils;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * @Author bootdo 1992lcg@163.com
 */
@Data
@ToString
public class PageData implements Serializable {
	private static final long serialVersionUID = 1L;
	private int total;
	private List<?> rows;

	public PageData() {

	}

	public PageData(List<?> list, int total) {
		this.rows = list;
		this.total = total;
	}
}
