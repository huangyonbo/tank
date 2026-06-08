package com.login.common.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 文件上传
 *
 * @author chglee
 * @email 1992lcg@163.com
 * @date 2017-09-19 16:02:20
 */
@Data
public class FileDO implements Serializable {
    private static final long serialVersionUID = 1L;
    //
    private Long id;
    //名称
    private String name;
    // 文件类型
    private Integer type;
    // URL地址
    private String url;
    //二维码路径
    private String code;
    // 创建时间
    private Date createDate;

    public FileDO() {
    	
    }


    public FileDO(Integer type,String name,String url, Date createDate) {
        this.type = type;
        this.name = name;
        this.url = url;
        this.createDate = createDate;
        int index = url.lastIndexOf(".");
        this.code = url.substring(0,index) + "_code.png";
    }
    
    @Override
    public String toString() {
        return "FileDO{" +
                "id=" + id +
                ", name=" + name +
                ", type=" + type +
                ", url='" + url + '\'' +
                ", createDate=" + createDate +
                '}';
    }
}
