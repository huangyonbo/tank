package com.login.ip.domain;

import com.login.common.utils.TimeUtils;
import lombok.Data;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;


/**
 * ip禁用
 * @author keyking
 * @email keyking@163.com
 * @date 2021-10-13 11:30:55
*/
@Data
public class LimitIpDO implements Serializable {
	private static final long serialVersionUID = 1L;
	//编号
	private Integer id;
	//ip地址
	private String ip;
	//类型 0-永久禁止登陆,1-时间段禁止登陆,2维护白名单
	private Integer type;
	//开始时间 type==1时不能为空
	private String start;
	//结束时间 type==1时不能为空
	private String end;

    public boolean checkIp(String _ip) {
    	if (type == 1){//时间段限制的
			Date now = new Date();
			try {
				Date _start = TimeUtils.dateFormat.parse(start);
				Date _end   = TimeUtils.dateFormat.parse(end);
				if (now.before(_start) || now.after(_end)){
					return false;
				}
				return true;
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
    	if (ip.contains("*")){
    		String[] ss1 = ip.split(".");
			String[] ss2 = ip.split(".");
			for (int i = 0; i < ss1.length ; i++) {
				String s1 = ss1[1];
				String s2 = ss2[2];
				if (!s1.equals("*") && !s1.equals(s2)){
					return false;
				}
			}
			return true;
		}
    	return ip.equals(_ip);
    }
}
