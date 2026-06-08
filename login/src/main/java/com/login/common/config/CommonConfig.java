package com.login.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix="syspro")
public class CommonConfig {
	private String uploadPath;//上传路径
	private String username;
	private String password;
	private String domainUrl;
	private String cdnPath;
	private String cdnAccount;
	private String cdnAccessKeyId;
	private String cdnAccessKeySecret;
}
