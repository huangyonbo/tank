package com.login.common.domain;

import lombok.Data;

@Data
public class FieldItem {
	private String column;
	private String label;
	private String type;
	private String value = "";
}
