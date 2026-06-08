package framework.net;

public enum InnerMsgDef {

    INNER_MSG_UNKNOW(0, "未知"),
    INNER_MSG_INFO(1, "服务器信息"),
    INNER_MSG_CLOSE(2, "关闭服务"),
    INNER_MSG_CHECK_LOGIN(3, "检测登录"),
    INNER_MSG_CHECK_LOGIN_RES(4, "登录检测结果"),
    INNER_MSG_SYNC_LOAD(5, "同步负载"),
    INNER_MSG_LOAD_PLAYER(6, "加载玩家"),
    INNER_MSG_REQUEST_ROLE_DATA(7, "请求角色数据"),
    INNER_MSG_STORE_ROLE_DATA(8, "存储角色数据"),
    INNER_MSG_REG_ROLE_DATA(9, "注册角色数据"),
    INNER_MSG_FORWARD(10, "转发消息"),
    INNER_MSG_CLIENT_DISCONNECT(11, "客户端断开"),
    INNER_MSG_ADD_PLAYER(12, "增加玩家"),
    INNER_MSG_DEL_PLAYER(13, "删除玩家"),
    INNER_MSG_PLAYER_CHANGE_SER(14, "玩家切服"),
    INNER_MSG_COMMAND(15, "命令"),
    INNER_MSG_BROADCAST(16, "广播指定玩家"),
    INNER_MSG_BROADCAST_ALL(17, "广播全部玩家"),
    INNER_MSG_SYNC_PUBDATA(18, "同步公共数据"),
    INNER_MSG_NOTIFY_NEXT_READY(19, "通知下个节点ready"),
    INNER_MSG_CHANGE_SERVER(20, "切换服务器"),
    INNER_MSG_CHANGE_BACK(21, "切换后端"),
    INNER_MSG_CHANGE_RESULT(22, "切服结果"),
    INNER_MSG_REQUEST(23, "请求"),
    INNER_MSG_RESPONSE(24, "响应"),
    INNER_MSG_CUSTOM_MSG(25, "自定义消息"),
    INNER_MSG_CUSTOM_REQUEST(26, "自定义请求"),
    INNER_MSG_CUSTOM_RESPONSE(27, "自定义响应"),
    INNER_MSG_REQ_OBJ_LIST(28, "请求对象列表"),
    INNER_MSG_REQ_OBJ_DATA(29, "请求对象数据"),
    INNER_MSG_KICK_PLAYER(30, "踢玩家"),
    INNER_MSG_GAME_LOG(31, "游戏日志"),
    INNER_MSG_ITEM_LOG(32, "道具日志"),
    INNER_MSG_PROP_LOG(33, "属性日志"),
    INNER_MSG_MAIL_LOG(34, "邮件日志"),
    INNER_MSG_PLAY_LOG(35, "游玩日志"),
    INNER_MSG_PLAYER_LOG(36, "玩家日志"),

    INNER_MSG_PS_LIST(37, "PS列表"),
    INNER_MSG_PD_LIST(38, "PD列表"),
    INNER_MSG_PD(39, "PD数据"),
    INNER_MSG_OFFLINEDATA(40, "离线数据"),

    INNER_MSG_REQ_OFFLINE(41, "请求离线数据"),
    INNER_MSG_DEL_OFFLINE(42, "删除离线数据"),
    INNER_MSG_REQ_READ_ROLE(43, "只读角色"),
    INNER_MSG_REQ_OFFLINE_ROLE(44, "离线角色"),
    INNER_MSG_STORE_OFFROLE(45, "存离线角色"),

    INNER_MSG_FROZEN(46, "冻结"),
    INNER_MSG_UNFROZEN(47, "解冻"),
    INNER_MSG_CHANGE_NAME(48, "改名"),

    INNER_MSG_ADVICE(49, "建议"),
    INNER_MSG_WARNING_ITEMSCORE(50, "道具预警"),
    INNER_MSG_ADD_BLACKLIST(51, "加黑名单"),
    INNER_MSG_DEL_BLACKLIST(52, "删黑名单"),
    INNER_MSG_USE_WHITELIST(53, "白名单开关"),
    INNER_MSG_UPDATE_DAU(54, "更新DAU"),
    INNER_MSG_UPDATE_ONLINEPEAK(55, "更新在线峰值"),

    INNER_REQ_REDEEM_CODE(56, "兑换码"),
    INNER_CLOSE_ARENA(57, "关闭竞技场"),
    INNER_REQ_CARD_ITEM(58, "兑换券"),
    INNER_REQ_ACTIVITY_DATA(59, "活动数据"),
    INNER_REQ_VERSION_DATA(60, "版本数据"),
    INNER_REQ_DAILY_DATA(61, "每日奖励"),
    INNER_MSG_REALNAME(62, "实名认证"),

    INNER_MSG_ADD_PUBSPACE(63, "加公共区"),
    INNER_MSG_ADD_PUBDATA(64, "加公共数据"),
    INNER_MSG_ADD_PUBPRO(65, "加公共属性"),
    INNER_MSG_ADD_PUBREC(66, "加公共表"),
    INNER_MSG_DEL_PUBSPACE(67, "删公共区"),
    INNER_MSG_DEL_PUBDATA(68, "删公共数据"),
    INNER_MSG_DEL_PUBPRO(69, "删公共属性"),
    INNER_MSG_DEL_PUBREC(70, "删公共表"),
    INNER_MSG_SET_PUBPRO(71, "设公共属性"),
    INNER_MSG_SET_PUBREC(72, "设公共表"),
    INNER_MSG_ADDR_PUBREC(73, "加一行"),
    INNER_MSG_SET_COL_TYPE(74, "设列类型"),
    INNER_MSG_DELR_PUBREC(75, "删一行"),
    INNER_MSG_CLEAR_PUBREC(76, "清表"),
    INNER_MSG_SYNC_PUB_COMP(77, "公共同步完成"),

    INNER_REQ_SEND_MAIL(78, "发送邮件请求"),
    INNER_MSG_SEND_MAIL(79, "发送邮件"),
    INNER_MSG_QUERY_MAIL(80, "查邮件"),
    INNER_MSG_READ_MAIL(81, "读邮件"),
    INNER_MSG_DEL_MAIL(82, "删邮件"),

    INNER_MSG_EXCHANGE_CARD(83, "兑换卡"),
    INNER_MSG_CARD_STATS(84, "兑换统计"),
    INNER_MSG_ACTIVITY_LOG(85, "活动日志"),
    INNER_MSG_GM_LOG(86, "GM日志"),
    INNER_MSG_RECRUIT(87, "招募"),
    INNER_MSG_AREA_ROOM_LOG(88, "竞技场日志"),

    INNER_MSG_BIND_PROXYID(89, "绑定代理"),
    INNER_MSG_EXECUTE_SQL_METHOD(90, "执行SQL"),
    INNER_MSG_NOTIFY_PRE_ME_READY(91, "通知前节点"),
    INNER_MSG_NOTIFY_ME_CLOSE(92, "通知关闭"),
    INNER_MSG_REQ_SERVER_MODULE(93, "集群请求"),
    INNER_MSG_RESP_SERVER_MODULE(94, "集群响应"),
    INNER_MSG_NOTIFY_SHUTDOWN(95, "通知关闭"),
    INNER_MSG_JUST_SEND_DATA(96, "断线数据"),

    INNER_MSG_CMD_UPDATE_PRO(97, "CMD改属性"),
    INNER_MSG_CMD_UPDATE_REC(98, "CMD改记录"),
    INNER_MSG_RELOAD_CONFIG(99, "重载配置"),

    INNER_MSG_WARNING_MOJIN(100, "魔晶预警"),
    INNER_MSG_MOJIN_ROOM_RECORD(101, "魔晶记录"),
    INNER_MSG_UPDATE_MAIL_PROP(102, "更新邮件道具"),
    INNER_MSG_PAY_CALL_BACK_ERROR(103, "支付失败"),

    INNER_MSG_CREATE_GUILD(104, "创建公会"),
    INNER_MSG_DELETE_GUILD(105, "解散公会"),

    INNER_MSG_ACTIVITY_LUCKY_PUZZLE_LOG(106, "拼图日志"),
    INNER_MSG_FUN_FISH_RECORD(107, "玩法鱼记录"),

    INNER_MSG_ACTIVITY_FISH_POND_RECORD(108, "养鱼池操作"),
    INNER_MSG_ACTIVITY_SYSTEM_FISH_RECORD(109, "系统鱼记录"),
    INNER_MSG_ACTIVITY_FISH_POND_MSG_RECORD(110, "养鱼池消息"),
    INNER_MSG_LOGIN_SUCC(111, "登录成功"),
    INNER_MSG_STORE_MOJIN_DATA(112, "存魔晶"),
    INNER_MSG_SYNC_MYSTERY_LEGEND_PLAY_AND_WIN(113, "秘境同步"),
    INNER_MSG_REQ_ROLE_PARAM(114, "请求角色参数"),
    INNER_MSG_STORE_ROLE_PARAM(115, "存角色参数"),
    INNER_MSG_STORE_ORDER(116, "下单"),
    INNER_MSG_FULL_GAME_ITEMS(117, "控制道具"),
    INNER_MSG_KILL_FISH_LOG(118, "挂机日志"),
    INNER_MSG_KILL_FISH_LOG_MJ(119, "挂机日志MJ"),
    INNER_MSG_SEND_ITEM(120, "发道具"),
    INNER_MSG_DEL_SEND_ITEM_RECORD(121, "删发放记录"),
    INNER_MSG_QUERY_SEND_ITEM_RECORD(122, "查发放记录"),
    INNER_MSG_SEND_BULLET_ADD_SPEED_RECORD(123, "子弹加速记录"),
    INNER_MSG_UPDATE_RATE_LIMIT(124, "更新限流"),
    INNER_MSG_B2G_UPDATE_RATE_LIMIT(125, "后台限流"),
    INNER_MSG_ACCOUNT_STATUS(126, "账号状态");

    private final int id;
    private final String desc;

    InnerMsgDef(int id, String desc) {
        this.id = id;
        this.desc = desc;
    }

    public int getId() { return id; }

    public String getDesc() { return desc; }

    public static InnerMsgDef getById(int id) {
        for (InnerMsgDef def : values()) {
            if (def.id == id) return def;
        }
        return null;
    }
}