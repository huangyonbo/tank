package com.login.maintain.domain;

import com.login.common.utils.TimeUtils;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;


/**
 * 维护设置
 * @author keyking
 * @email keyking@163.com
 * @date 2021-10-13 09:45:02
*/
@Data
public class MaintainRecordDO implements Serializable {
	private static final long serialVersionUID = 1L;
	private Integer id;
	//关闭维护时间
	private Date end;
	//维护信息
	private String message;
	//渠道
	private String channel;
	//开启维护时间
	private Date start;
	//维护状态
	private Integer status;
	//维护类型
	private Integer type;
	//客户端版本号
	private String version;
	//发送状态
	private Integer send;
	//更新时间
	private Date updateTime;
	//创建日期
	private Date createTime;
	//是否发送邮件通知玩家
	private Boolean mail;
	//创建者
	private String createBy;
	//更新者
	private String updateBy;

	public String getShowMessage(){
		String _start  = TimeUtils.dateFormat.format(start);
		String _end    = TimeUtils.dateFormat.format(end);
		String showStr = "服务器维护时间：" + _start + "至" + _end;
		showStr += "\n维护内容：" + message;
		return showStr;
	}
}
