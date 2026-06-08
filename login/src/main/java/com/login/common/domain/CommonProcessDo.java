package com.login.common.domain;

import lombok.Data;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

@Data
public class CommonProcessDo {
	private String table;
	private String processDefinitionKey;
	private String taskId;
	private String datas;
	private boolean pass;
	private static final String empty_str = "";
	public Map<String,String> decode(){
		Map<String,String> temp = new HashMap<String,String>();
		if (datas != null) {
			String[] ss = datas.split("&");
			for (String s : ss) {
				String[] sss = s.split("=");
				if (sss.length > 1) {
					try {
						String _s = URLDecoder.decode(sss[1],"UTF-8");
						temp.put(sss[0],_s);
					} catch (UnsupportedEncodingException e) {
					}
				}else {
					temp.put(sss[0],empty_str);
				}
			}
		}
		return temp;
	}
}
