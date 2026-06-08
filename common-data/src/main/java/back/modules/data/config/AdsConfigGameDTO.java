package back.modules.data.config;

import lombok.Data;

import java.io.Serializable;

@Data
public class AdsConfigGameDTO implements Serializable {

    private static final long serialVersionUID = -4097693315690794861L;
    /**
     * 渠道
     */
    private Integer channel;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;
}
