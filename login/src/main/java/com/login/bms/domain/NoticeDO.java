package com.login.bms.domain;


import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class NoticeDO implements Serializable {
    private static final long serialVersionUID = 7477108207564061096L;

    private Integer id;
    /**
     * 内容
     */
    private String content;
    /**
     * 结束时间
     */
    private Date endTime;
    /**
     * 图片
     */
    private String picture;
    /**
     * 渠道
     */
    private String channel;
    /**
     * 开始时间
     */
    private Date start;
    /**
     * 标题
     */
    private String title;
    /**
     * 类型（1：登录公告；2：跑马灯公告）
     */
    private Integer type;
    /**
     * 发送状态
     */
    private Integer send;
    /**
     * 标签
     */
    private String tag;
    /**
     * 邮件（仅跑马灯公告有效）
     */
    private Boolean mail;

}
