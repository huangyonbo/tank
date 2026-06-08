package framework.mybatis.data;


import lombok.Data;

import java.util.Date;

@Data
public class CustomerServiceMsgBack {
    private Integer id;
    private Date createTime;
    private String channel;
    private Integer level;
    private Integer vipLevel;
    private String receiveId;
    private String attach;
    private String content;
    private Integer expiry;
    private String title;
}
