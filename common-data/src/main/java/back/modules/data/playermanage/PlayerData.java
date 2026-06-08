package back.modules.data.playermanage;


import lombok.Data;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/4/23.
 */
@Data
public class PlayerData implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id; //玩家ID
    private String name;    //玩家账号
    private int level;  //等级
    private int vipLevel;   //VIP等级[详见VIPLevel]
    private int channelId;    //所属渠道[详见配置]
    private int totalCharge;   //累计充值
    private long diamond;    //钻石
    private long coin;   //金币
    private long lottery;    //彩券
    private String prop;    //道具
    private int status; //状态[详见PlayerStatus]
    private long shutUp; //解除禁言时间
    private int sex;    //性别
    private String reg;   //注册时间
    private String login; //最近登录时间
    private int maxGun; //解锁炮值
    private int online; //在线状态[详见OnlineStatus]
    private String regIp;   //注册IP
    private String regDevice;   //注册设备号
    private String regModel;    //注册设备型号
    private String loginIp; //最近登录IP
    private String loginDevice; //最近登录设备号
    private String phone; //手机号
    private String appName; //安装包名称
    private String bank;    //银行信息
    private String idcard;  //身份证
    private String realname;    //真实姓名
    private String phoneBrand;//品牌
    private String phoneModel;//型号
    private String lastOpt; //最后一次操作
    private int itemScore;//道具积分
    private int extraItemScoreLimit;//充值额外获得的道具积分
    private String tempCellphone;//登记的手机号
    private long mojin;  // 魔晶
    private int nMaxGun; // 魔晶炮等级
    private String proxyName; //代理Id
    private int proxyType;   // 代理权限等级
    private long goldTotalPlay;//金币场累计消耗
    private long goldTotalWin;//金币场累计获得
    private long bombTotalPlay;//海神殿累计消耗
    private long bombTotalWin;//海神殿累计获得
    private long miniGame;
}
