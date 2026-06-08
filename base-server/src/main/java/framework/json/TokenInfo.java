package framework.json;

import lombok.Data;

@Data
public class TokenInfo {
	private int uid;
	private String token;
	private String extend;//扩展参数
}
