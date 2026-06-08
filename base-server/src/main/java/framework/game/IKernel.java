package framework.game;

import com.alibaba.fastjson.JSONObject;
import framework.BaseServer;
import framework.IRequestCallback;
import framework.MailLogType;
import framework.mybatis.domain.MojinRoomRecord;
import framework.mybatis.domain.PlayerDailyPlayData;
import framework.mybatis.service.AbstractService;
import framework.net.http.HttpClientApi;
import framework.pub.IPubData;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public interface IKernel {


    public class Appendix {
        public String item;
        public int count;
    }

    public class MailData {
        public String id;
        public int type;
        public int senduid;
        public int recvuid;
        public String sendName;
        public String recvName;
        public String title;
        public String context;
        public String sendTime;
        public String endTime;
        public String appendix;
        public int system;
    }

    public class CardData {
        public String id;
        public String passwd;
        public String endDate;
        public String itemid;
    }


    public enum LogKind {
        SYSTEM_GIVE, // 系统赠送
        BIG_REWARD, // 大奖
        OTHERS, // 其他
        WARN, // 预警
        END
    }

    public enum LogType {
        GET, // 获得
        USED, // 消耗
        PLAY, // 游玩

        END
    }

    public enum ItemLogType {
        UNKNOW,

        ADDNEW, USE, GIVE, DEL,

        END
    }

    public enum PlayLogType {
        UNKNOW,

        ENTER_DESK, LEAVE_DESK, ONLINE, OFFLINE, CLEAR_PW, BIG_FISH, PAY_ORDER,

        END
    }

    public enum PlayerState {
        STATE_NORMAL, STATE_DISCONNECT, STATE_CHANGESER, STATE_CHANGESCENE,
    }

    // * 日志类型：0-道具获得，1-道具消耗，2-进入房间，3-退出房间，4-清除总玩总赢，5-击杀大鱼，6-定时游玩，7-属性变化,8-公共道具积分消耗
    public enum PlayerLogType {
        GET_ITEM, DEL_ITEM, ENTER_ROOM, LEAVE_ROOM, CLEAR_PW, KILL_FISH, GAME_TIMER, PROP_CHANGE,PUB_SCORE,
        END
    }

    /**
     * 添加脚本类
     *
     * @param name   脚本名
     * @param parent 父对象名
     * @return 是否成功
     */
    boolean addClass(String name, String parent);

    /**
     * 声明属性
     *
     * @param logicClass 逻辑类
     * @param name       属性名
     * @param type       属性类型
     * @param pubVisible 是否公共可视
     * @param priVisible 是否私有可视
     * @param save       是否需要保存
     */
    void declareProperty(String logicClass, String name, ValueType type, boolean pubVisible, boolean priVisible,
                         boolean save);

    /**
     * 修改属性配置
     *
     * @param script     逻辑类
     * @param name       属性名
     * @param pubVisible 是否公共可视
     * @param priVisible 是否私有可视
     * @param save       是否需要保存
     */
    void setVisible(String script, String name, boolean pubVisible, boolean priVisible, boolean save);

    /**
     * 声明表
     *
     * @param script     目标脚本
     * @param name       表名
     * @param cols       总列数
     * @param maxRow     最大行数
     * @param pubVisible 是否公共可视
     * @param priVisible 是否私有可视
     * @param save       是否保存
     * @return 表对象
     */
    IRecord declareRecord(String script, String name, int cols, int maxRow, boolean pubVisible, boolean priVisible, boolean save);

    /**
     * 声明心跳
     *
     * @param name       心跳名称
     * @param listener   监听对象
     * @param methodName 回调函数
     * @return 是否成功
     */
    boolean declareHeartBeat(String name, Object listener, String methodName);

    /**
     * 预加载配置
     *
     * @param path 配置路径
     */
    void preLoadConfig(String path);

    /**
     * 更新配置
     *
     * @param path 配置路径
     * @return 是否成功
     */
    boolean updateConfig(String path);

    /**
     * 预加载对象
     *
     * @param cfg 配置id
     */
    void preLoadObject(String cfg);

    /**
     * 根据配置创建对象
     *
     * @param cfg 配置名
     * @return 创建的对象，失败则返回null
     */
    IGameObject createObjectByConfig(String cfg, IGameObject parent);

    IGameObject createObjectByConfig(String cfg, IGameObject parent, Object... params);

    /**
     * 根据id获取预加载的对象
     *
     * @param id 配置id
     * @return 对象
     */
    IGameObject getPreloadObject(String id);

    /**
     * 格局对象号获取对象
     *
     * @param objectId 对象号
     * @return 对象，失败返回null
     */
    IGameObject getGameObject(long objectId);

    /**
     * 获取世界对象
     *
     * @return
     */
    IGameObject getWorld();

    /**
     * 销毁对象
     *
     * @param object 对象
     */
    void destroyGameObject(IGameObject object);

    /**
     * 注册事件
     *
     * @param event      事件ID
     * @param script     目标脚本
     * @param listener   监听对象
     * @param methodName 回调方法
     */
    void regEvent(KernelEvent event, String script, Object listener, String methodName);

    /**
     * 注册客户端消息回调
     *
     * @param msgid      消息id
     * @param listener   监听对象
     * @param methodName 回调方法
     */
    void regClientMessage(int msgid, Object listener, String methodName);

    /**
     * 注册客户端请求
     *
     * @param msgid      消息id
     * @param listener   监听对象
     * @param methodName 回调方法
     */
    void regRequestMessage(int msgid, Object listener, String methodName);

    /**
     * 响应客户端请求
     *
     * @param player 玩家对象
     * @param reqid  请求id
     * @param data   返回消息
     */
    void response(IGameObject player, int reqid, byte[] data);


    /**
     * 响应客户端请求
     *
     * @param player 玩家对象
     * @param reqid  请求id
     * @param jsonObject   返回消息
     */
    void response(IGameObject player, int reqid, JSONObject jsonObject);


    /**
     * 注册命令
     *
     * @param cmdid      命令id
     * @param script     目标脚本
     * @param listener   监听对象
     * @param methodName 回调方法
     */
    void regCommand(int cmdid, String script, Object listener, String methodName);

    /**
     * 监听属性变化
     *
     * @param proName    属性名
     * @param script     目标脚本
     * @param listener   监听对象
     * @param methodName 回调方法
     */
    void listenPropertyChange(String proName, String script, Object listener, String methodName);

    /**
     * 监听操作属性事件
     *
     * @param proName    属性名
     * @param script     目标脚本
     * @param listener   监听对象
     * @param methodName 回调方法
     */
    void listenSetProperty(String proName, String script, Object listener, String methodName);

    /**
     * 监听表格变化
     * @param recName    表名
     * @param script     目标脚本
     * @param listener   监听对象
     * @param methodName 回调方法
     */
    void listenRecordChange(String recName, String script, Object listener, String methodName);

    /**
     * 向客户端发送消息
     *
     * @param player 玩家对象
     * @param msgid  消息id
     * @param data   消息数据
     */
    void sendMessage(IGameObject player, int msgid, byte[] data);

    /**
     * 房间内广播
     *
     * @param room  房间对象
     * @param msgid 消息id
     * @param data  消息数据
     */
    void broadCastByRoom(IGameObject room, int msgid, byte[] data);

    /**
     * 桌子内广播
     *
     * @param desk  桌子对象
     * @param msgid 消息id
     * @param data  消息数据
     */
    void broadCastByDesk(IGameObject desk, int msgid, byte[] data);

    /**
     * 视野范围内广播
     *
     * @param player 玩家对象
     * @param msgid  消息id
     * @param data   消息数据
     */
    void broadCastByKen(IGameObject player, int msgid, byte[] data);

    /**
     * 视野范围内广播，但不包括自己
     *
     * @param player 玩家对象
     * @param msgid  消息id
     * @param data   消息数据
     */
    void broadCastByKenWithOutSelf(IGameObject player, int msgid, byte[] data);

    /**
     * 按渠道广播
     *
     * @param channel 渠道id
     * @param msgid   消息id
     * @param data    消息数据
     */
    void broadCastByChannel(int channel, int msgid, byte[] data);

    /**
     * 按玩家id来广播
     *
     * @param msgid 消息id
     * @param data  消息数据
     */
    void broadCastByUids(List<Integer> uids, int msgid, byte[] data);

    /**
     * 当前服务器广播
     *
     * @param msgid 消息id
     * @param data  消息数据
     */
    void broadCastCurServer(int msgid, byte[] data);

    /**
     * 全服广播
     *
     * @param msgid 消息id
     * @param data  消息数据
     */
    void broadCastAllServer(int msgid, byte[] data);

    /**
     * 发送命令
     *
     * @param object 目标对象
     * @param cmdid  命令id
     * @param args   参数
     */
    void command(IGameObject object, int cmdid, Object... args);

    /**
     * 发送命令
     *
     * @param objectid 对象号
     * @param cmdid    命令id
     * @param args     参数
     */
    void command(long objectid, int cmdid, Object... args);

    /**
     * 向指定玩家发送命令
     *
     * @param uid   玩家id
     * @param cmdid 命令id
     * @param args  参数
     */
    void commandPlayer(int uid, int cmdid, Object... args);

    /**
     * 向所有玩家发送命令
     *
     * @param cmdid
     * @param args
     */
    void commandAllPlayer(int cmdid, Object... args);

    /**
     * 增加功能模块
     *
     * @param name   模块名
     * @param module 模块对象
     */
    void addModule(String name, ILogicModule module);

    /**
     * 获取模块
     *
     * @param name 模块名
     * @return 模块对象，没有则返回null
     */
    ILogicModule getModule(String name);
    public <T> T getModule(Class<T> name);
    /**
     * 添加心跳
     *
     * @param name   心跳名，需要全局唯一
     * @param target 目标对象
     * @param ms     心跳时间
     * @param repeat 重复次数
     * @return 是否成功
     */
    boolean addHeartBeat(String name, IGameObject target, int ms, int repeat);

    /**
     * 检测心跳
     *
     * @param target 目标对象
     * @param name   心跳名
     * @return 是否存在
     */
    boolean haveHeartBeat(IGameObject target, String name);

    /**
     * 删除心跳
     *
     * @param target 目标对象
     * @param name   心跳名
     */
    void removeHeartBeat(IGameObject target, String name);

    /**
     * 坐下
     *
     * @param player 玩家对象
     * @param desk   桌子对象
     * @return 是否成功
     */
    boolean sitDown(IGameObject player, IGameObject desk);

    /**
     * 坐下
     *
     * @param player 玩家对象
     * @param desk   桌子对象
     * @param seatid 座位号
     * @return 是否成功
     */
    boolean sitDown(IGameObject player, IGameObject desk, int seatid);

    /**
     * 坐下
     *
     * @param player 玩家对象
     * @param deskid 桌子对象号
     * @param seatid 座位号
     * @return 是否成功
     */
    boolean sitDown(IGameObject player, long deskid, int seatid);

    /**
     * 坐下
     *
     * @param player 玩家对象
     * @param deskid 桌子对象号
     * @return 是否成功
     */
    boolean sitDown(IGameObject player, long deskid);

    /**
     * 起立
     *
     * @param player 玩家对象
     * @return 是否成功
     */
    boolean standUp(IGameObject player);

    /**
     * 监听断开连接事件（注意：业务上只允许注册一次，回调返回值表示断线重连的时间）
     *
     * @param listener   监听对象
     * @param methodName 回调方法
     */
    void addDisconnectEvent(Object listener, String methodName);

    /**
     * 获取redis连接
     * @return
     */
    Jedis getJedis();

    /**
     * 根据uid获取玩家对象
     *
     * @param uid 玩家id
     * @return 玩家对象
     */
    IGameObject getPlayer(int uid);

    /**
     * 获取所有在线玩家列表
     */
    List<IGameObject> getAllOnlinePlayer();


    /**
     * 获取当前服务器玩家人数
     *
     * @return 当前服务器玩家人数
     */
    int getPlayerCount();

    /**
     * 获取全服总玩家数
     *
     * @return 全服总玩家数
     */
    int getAllPlayerCount();

    /**
     * 加载通用xml配置
     *
     * @param path 配置文件路径
     * @return 失败返回null
     */
    ICfgReader loadXmlConfig(String path);

    /**
     * 遍历文件
     *
     * @param dir 文件夹目录
     * @return 文件列表
     */
    File[] listFiles(String dir);

    /**
     * 获取服务器事件
     *
     * @return 服务器事件
     */
    long getServerTime();

    /**
     * 获取服务id
     *
     * @return 服务id
     */
    int getSerID();

    /**
     * 获取服务器名
     *
     * @return 服务器名
     */
    String getSerName();

    /**
     * 是否是当前服务器内对象
     *
     * @param objid 对象号
     * @return 是否是
     */
    boolean isCurSerObject(long objid);

    /**
     * 注册子服务消息
     *
     * @param msgid      消息id
     * @param listener   作用域
     * @param methodName 回调
     */
    void regServerMsg(int msgid, Object listener, String methodName);

    /**
     * 注册子服务请求
     *
     * @param msgid      请求id
     * @param listener   作用域
     * @param methodName 回调
     */
    void regServerRequest(int msgid, Object listener, String methodName);

    /**
     * 根据id向子服务发送消息
     *
     * @param serid 子服务id
     * @param msgid 消息id
     * @param data  发送参数数据
     */
    void sendServerMsg(int serid, int msgid, byte[] data);

    /**
     * 根据name向子服务发送消息
     *
     * @param sername 子服务名称
     * @param msgid   消息id
     * @param data    发送参数数据
     */
    void sendServerMsg(String sername, int msgid, byte[] data);

    /**
     * 根据Id向子服务请求数据
     *
     * @param serid 子服务id
     * @param msgid 消息id
     * @param data  请求参数数据
     * @param cb    回调
     */
    void requestServer(int serid, int msgid, byte[] data, IRequestCallback cb);

    /**
     * 根据name向子服务请求数据
     *
     * @param sername 子服务名称
     * @param msgid   消息id
     * @param data    请求参数数据
     * @param cb      回调
     */
    void requestServer(String sername, int msgid, byte[] data, IRequestCallback cb);

    /**
     * 回复子服务
     *
     * @param reqid 请求Id
     * @param data  回复数据
     */
    void responseServer(int reqid, byte[] data);

    /**
     * 创建容器
     *
     * @param name     容器名
     * @param script   容器脚本
     * @param capacity 容量
     * @param target   目标对象
     * @return 容器对象
     */
    IGameObject createContainer(String name, String script, int capacity, IGameObject target);

    /**
     * 获取公共数据区
     *
     * @param name
     *            数据区名
     * @return IPubSpace
     */
    //IPubSpace GetPubSpace(String name);

    /**
     * 获取公共数据
     *
     * @param name 数据名
     * @return PubData
     */
    IPubData getPubData(String name);

    /**
     * 根据uid获取用户名
     *
     * @param uid 用户uid
     * @return 用户名，用户不存在则返回 null
     */
    String getUserName(int uid);

    /**
     * 获取用户头像id
     *
     * @param uid
     * @return
     */
    int getUserHeadid(int uid);
    int getProxyId(int uid);

    /**
     * 获取头像url
     *
     * @param headid
     * @return
     */
    String getHeadUrl(int headid);

    /**
     * 发送系统邮件
     *
     * @param recvuid  收件人uid，-1则全服邮件
     * @param channel  收件人渠道，-1则全渠道
     * @param title    邮件标题
     * @param context  邮件内容
     * @param lifetime 邮件时效(-1永久有效)
     * @param appendix 附件
     * @return 是否发送成功
     */
    boolean sendSystemMail(int recvuid, int channel, String title, String context, long lifetime, String appendix);

    /**
     * 发送指定系统邮件
     *
     * @param recvuid  收件人uid，-1则全服邮件
     * @param channel  收件人渠道，-1则全渠道
     * @param title    邮件标题
     * @param context  邮件内容
     * @param lifetime 邮件时效(-1永久有效)
     * @param appendix 附件
     * @return 是否发送成功
     */
    boolean sendSystemMail(int recvuid, int channel, String title, String context, long lifetime, String appendix,
                           MailSystemDef system);

    /**
     * 发送支付邮件
     *
     * @param recvuid
     * @param channel
     * @param title
     * @param context
     * @param lifetime
     * @param appendix
     * @return
     */
    boolean sendPayMail(int recvuid, int channel, String title, String context, long lifetime, String appendix);

    /**
     * 发送普通邮件
     *
     * @param player   发送者
     * @param recvuid  收件人uid，-1则全服邮件
     * @param title    邮件标题
     * @param context  邮件内容
     * @param lifetime 邮件时效(-1永久有效)
     * @param appendix 附件
     * @return 是否发送成功
     */
    boolean sendNormalMail(IGameObject player, int recvuid, String title, String context, long lifetime,
                           String appendix);

    /**
     * 发送道具邮件
     *
     * @param player   发送者
     * @param recvuid  收件人uid，-1则全服邮件
     * @param title    邮件标题
     * @param context  邮件内容
     * @param lifetime 邮件时效(-1永久有效)
     * @param appendix 附件
     * @return 是否发送成功
     */
    boolean sendItemMail(IGameObject player, int recvuid, String title, String context, long lifetime, String appendix);

    /**
     * 查询邮件列表
     *
     * @param uid        用户uid
     * @param lastMailid 上次邮件id
     * @param cb         回调
     */
    void queryMail(int uid, int channel, String lastMailid, Consumer<List<MailData>> cb);

    /**
     * 读取邮件
     *
     * @param mailid 邮件id
     * @param cb     回调
     */
    void readMail(String mailid, Consumer<MailData> cb);

    /**
     * 删除邮件
     *
     * @param mailid 邮件id
     */
    void delMail(String mailid);

    /**
     * 踢玩家下线
     *
     * @param player 玩家对象
     */
    void kickPlayer(IGameObject player);

    /**
     * 没有提示的剔玩家下线
     * @param player
     */
    void kickPlayerNoTip(IGameObject player);

    /**
     * 按渠道踢玩家下线
     *
     * @param channel 渠道id
     */
    void kickPlayerByChannel(int channel);

    /**
     * 维护的时候按渠道踢人
     *
     * @param channel
     */
    void kickPlayerByChannelWhenMaintain(int channel);

    /**
     * 按ip地址踢玩家下线
     *
     * @param addr ip
     */
    void kickPlayerByIp(String addr);

    /**
     * 按设备id踢玩家下线
     *
     * @param devid 设备id
     */
    void kickPlayerByDevID(String devid);

    /**
     * 踢本服务器所有玩家下线
     */
    void kickAllPlayer();

    /**
     * 维护踢出所有人玩家
     */
    void kickAllPlayerWhenMaintain();

    /**
     * 记录游戏日志
     *
     * @param player  玩家
     * @param type    日志类型
     * @param context 日志内容
     * @param reason  日志原因
     * @param target  目标对象uid
     */
    void addGameLog(IGameObject player, LogKind kind, LogType type, String context, String system, String reason,
                    int target);

    /**
     * 道具日志
     *
     * @param player 玩家
     * @param itemid 道具编号
     * @param count  数量
     * @param roomName 房间名
     * @param output 产出途径
     * @param useway 消耗途径
     */
    void addItemLog(IGameObject player, String itemid, int count, String roomName, String output, String useway);

    /**
     * 邮件日志
     * @param player
     * @param mailid
     * @param type
     * @param context
     * @param reason
     */
    void addMailLog(IGameObject player, String mailid, MailLogType type, String context, String reason);

    /**
     * 游玩日志
     * @param player
     * @param desk
     * @param type
     * @param context
     * @param reason
     */
    void addPlayLog(IGameObject player, IGameObject desk, PlayLogType type, String context, String reason);

    /**
     * 玩家日志（道具+部分游玩） 日志类型：0-道具获得，1-道具消耗，2-进入房间，3-退出房间，4-清除总玩总赢，5-击杀大鱼，6-定时游玩
     * 日志内容： 0-道具获得，1-道具消耗： 【原数量，变化量，变化后量】 2-进入房间，3-退出房间： 【】 4-清除总玩总赢：
     * 【房间id：总玩，总赢；】 5-击杀大鱼： 【掉落信息】 6-定时游玩： 【】
     * |时间|玩家uid|日志类型|房间id|系统id|道具id|渠道id|登录时间|注册时间|VIP等级|玩家等级|金币|钻石|彩券|总玩|总赢|
     * 日志内容|日志原因
     */
    void addPlayerLog(IGameObject player, IGameObject item, int type, int system, String context, String reason);

    /**
     * @param player
     * @param activity 活动类型
     * @param methodName 方法名称
     * @param data     活动数据
     */
    void addActivityLog(IGameObject player, int activity,String methodName, String data);

    /**
     *  游戏击杀日志
     * @param player
     * @param desk
     */
    void addKillFishLog(IGameObject player, IGameObject desk);

    /**
     * @param player
     * @param option 操作 0: 合成拼图； 1: 分享拼图
     */
    void addActivityLuckyPuzzleLog(IGameObject player, int option, String items);

    /**
     * GM操作日志
     *
     * @param player GM
     * @param target 操作目标
     * @param cmds   命令
     * @param result 结果
     */
    void addGmLog(IGameObject player, IGameObject target, String cmds, String result);

    /**
     * 玩法鱼数据记录
     *
     * @param player          玩家对象
     * @param fish            玩法鱼
     * @param room            所在房间
     * @param gold            金币所得
     * @param bombCoin        魔晶所得
     * @param rewardSendState 奖励发放状态 0:直接发放 1:邮件
     */
    void addFunFishRecord(IGameObject player, String fish, int room, int gold, int bombCoin, int rewardSendState);

    /**
     * 养鱼池玩家操作记录
     *
     * @param player         玩家对象
     * @param option         操作（0：投放, 1：捕捉成功, 2：捕捉失败, 3：收获）
     * @param cost           金币消耗：01000 / 魔晶消耗：11000
     * @param income         金币收益：01000 / 魔晶收益：11000
     * @param caughtUid      被捕获玩家ID
     * @param caughtNickname 被捕获玩家昵称
     */
    void addActivityFishPondRecord(IGameObject player, int option, String cost, String income, int caughtUid, String caughtNickname);

    /**
     * 养鱼池系统鱼记录
     *
     * @param fishId         系统鱼ID
     * @param fishName       系统鱼昵称
     * @param fishState      系统鱼状态 0：生成，1：捕捉成功，2：捕捉失败）
     * @param fishValue      系统鱼价值 金币：01000 / 魔晶：11000
     * @param caughtUid      捕获玩家ID
     * @param caughtNickname 捕获玩家昵称
     */
    void addActivitySystemFishRecord(String fishId, String fishName, int fishState, String fishValue, int caughtUid, String caughtNickname);

    /**
     * 存储养鱼池玩家消息
     *
     * @param cost   鱼的价值（0则无）
     */
    void addFishPondMsgRecord(int type, int uid, String nickname, String cost);

    /**
     * 设置玩家状态
     * @param player
     * @param state
     */
    void setState(IGameObject player, PlayerState state);

    /**
     * 添加离线数据
     *
     * @param uid     目标玩家
     * @param type    类型
     * @param context 内容
     * @param reason  原因
     */
    void addOfflineData(int uid, int type, String context, String reason);

    /**
     * 检测配置是否合法
     *
     * @param cfgid 配置id
     * @return 是否合法
     */
    boolean checkCfgLegal(String cfgid);

    /**
     * 获取配置属性
     *
     * @param cfgid 配置id
     * @param name  属性名
     * @return 属性值
     */
    String getCfgProperty(String cfgid, String name);

    /**
     * 获取配置属性
     *
     * @param cfgId 配置id
     * @param name  属性名
     * @param defaultValue 默认值
     * @return 属性值
     */
    <T> T getCfgDetailProperty(String cfgId, String name, Object defaultValue);

    /**
     * 改名
     *
     * @param player  玩家
     * @param newName 新名字
     * @param cb      回调
     */
    void changeName(IGameObject player, String newName, Consumer<Boolean> cb);

    /**
     * 投诉建议
     *
     * @param player  玩家
     * @param context 内容
     * @param type    类型
     */
    void advice(IGameObject player, String context, int type);

    /**
     * 增加道具积分预警日志
     *
     * @param player             玩家对象
     * @param chargeScore        玩家充值积分
     * @param killFishItemScore  玩家击杀掉落的道具积分
     * @param drawAwardItemScore 玩家抽奖的道具积分
     * @param maxItemScore       玩家道具积分上限
     */
    void addWarningItemScore(IGameObject player, int chargeScore, int killFishItemScore, int drawAwardItemScore, int maxItemScore);

    /**
     * 添加魔晶预警日志
     *
     * @param player       玩家对象
     * @param play         魔晶场消耗
     * @param win          魔晶场获得
     * @param hbomb        传说三叉戟数量
     * @param hbomb_debris 传说三叉戟碎片数量
     * @param nbomb        至尊
     * @param nbomb_debris 至尊碎片
     * @param other_detail 其他明细
     * @param dmojin       魔晶差值
     */
    void addWarningMoJin(IGameObject player, long play, long win, int hbomb, int hbomb_debris, int nbomb, int nbomb_debris, String other_detail, long dmojin);

    void addMoJinRoomActiveData(MojinRoomRecord record);

    void startPerf(String func);

    void overPerf(String func);

    void addOnlineTime(IGameObject player, int itme);

    void addLoginCount(IGameObject player);

    void updateOnlineCount(int channel, int count);

    /**
     * 使用兑换码
     *
     * @param code 兑换码
     * @param cb   响应，空串表示兑换失败，非空表示获得的道具
     */
    void useRedeemCode(String code, int uid, int channel, String devid, Consumer<String> cb);

    void checkCardItem(IGameObject player, String itemid, int type, Consumer<List<CardData>> cb);

    /**
     * 实名认证
     */
    void realName(IGameObject player, String name, String idnum, Consumer<Boolean> cb);

    /**
     * 绑定代理商
     */
    void bindProxy(IGameObject player, int proxyId, Consumer<Boolean> cb);

    /**
     * 兑换卡兑换记录
     *
     * @param uid   玩家id
     * @param items 消耗物品
     * @param goods 获得物品
     * @param cb    回调
     */
    void exchangeCard(int uid, String items, String goods, Consumer<Boolean> cb);

    /**
     * 招募记录
     *
     * @param uid      玩家id
     * @param openDate 开启招募时间
     */
    void addRecruit(int uid, String openDate);

    long getPlayerObjID(int uid);

    void changeServer(IGameObject player, long tarobj);

    void changeServer(IGameObject player, int serid);

    /**
     * 加载离线玩家数据（不会触发OnLoad、Online等等事件）
     *
     * @param uid
     * @param cb
     */
    void loadPlayerData(int uid, Consumer<IGameObject> cb);


    /**
     * 从db加载数据
     * @param clazz
     * @param method
     * @param objects
     * @param cb
     */
    void executeSomeToStore(Class<? extends AbstractService<?>> clazz, String method, List<Object> objects, Consumer<String> cb);

    /**
     * 集群的时候判断是不是主逻辑
     *
     * @return
     */
    boolean isMain();

    /**
     * 注册功能模块关闭事件
     *
     * @param listener
     * @param order
     * @param methodName
     */
    void regStopListener(Object listener, int order, String methodName);


    /**
     * 向其他logic发送消息
     *
     * @param sername
     * @param msgid
     * @param data
     */
    void sendMsgToServer(String sername, int msgid, byte[] data);

    /**
     * 想其他服务器发送rpc请求
     *
     * @param sername
     * @param msgid
     * @param data
     */
    void requestToServer(String sername, int msgid, byte[] data, Consumer<byte[]> cb);

    ClassSet getClassSet();

    void addOrUpdatePlayerDailyPlayData(List<PlayerDailyPlayData> list);

    /**
     * 根据uid集合获取玩家对象列表
     *
     * @param uidList uid集合
     * @return 玩家对象列表
     */
    List<GamePlayer> listPlayer(List<Integer> uidList);

    HttpClientApi getHttpClient();

    BaseServer getServer();
    default JSONObject getPlayerLog(IGameObject player){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("player",player.getProperty("Uid"));
        return jsonObject;
    }
    default JSONObject getActivityLog(IGameObject player){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("player",player.getProperty("Uid"));
        return jsonObject;
    }
   public static JSONObject getSystemLog(){
        JSONObject jsonObject = new JSONObject();
        return jsonObject;
    }
}
