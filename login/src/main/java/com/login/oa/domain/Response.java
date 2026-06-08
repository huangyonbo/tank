package com.login.oa.domain;

import lombok.Data;

@Data
public class Response {
	private String responseMessage;
	public Response(String responseMessage) {
		this.responseMessage = responseMessage;
	}
}
