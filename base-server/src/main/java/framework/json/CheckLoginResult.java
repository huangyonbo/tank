package framework.json;

import lombok.Data;
@Data
public class CheckLoginResult {
	private int code;
	private String message;
	private String name;
	private Integer sex;
	private String headUrl;
	private Integer channelId;
	private String phone;
	private String recruiter;
	private String payInfo;
	private String extend;//扩展参数
	private String ip;//扩展参数
}
