package com.login.app.domain;

import lombok.Builder;
import lombok.Data;

/**
 * @Author: Lambda
 * @Description: 实名验证上报游戏数据 参数详见官方文档
 */

@Data
@Builder
public class ReportDataDTO {

    /**
     * 已通过实名认证用户的唯一标志
     * 已认证通过的用户必填
     */
    private String pi;
    /**
     * 条目编码
     */
    private int no;
    /**
     * 游戏内部会话标识
     */
    private String si;
    /**
     * 用户行为类型 0下线 1上线
     */
    private int bt;
    /**
     * 行为发生时间戳
     */
    private Long ot;
    /**
     *  上报类型 0已认证通过用户 2游客用户
     */
    private int ct;
    /**
     * 设备标识
     */
    private String di;
}
