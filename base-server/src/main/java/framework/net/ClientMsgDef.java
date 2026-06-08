package framework.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public enum ClientMsgDef {
    CLIENT_LOGIN(0, "客户端请求登录"),
    CLIENT_LOGIN_RES(1, "登录结果"), //  code - 0：成功，1：冻结，2：异常，3：服务器维护
    CLIENT_LOAD_OBJECT(2, "加载对象"),
    CLIENT_DELETE_OBJECT(3, "销毁对象"),
    CLIENT_CUSTOM(4, "自定义消息"),
    CLIENT_REQUEST(5, ""),
    CLIENT_RESPONSE(6, ""),
    CLIENT_SYNC_PROPERTY(7, "同步属性"),
    CLIENT_SYNC_ONE_PRO(8, "同步单个属性"),
    CLIENT_ADD_VIEWPORT(9, "增加视图"),
    CLIENT_REMOVE_VIEWPORT(10, "删除视图"),
    CLIENT_LOAD_VPITEM(11, "加载视图内对象"),
    CLIENT_REMOVE_VPITEM(12, "移除视图内对象"),
    CLIENT_SYNC_VPITEM_PRO(13, "同步视图内对象属性"),
    CLIENT_REC_ADD_ROW(14, "可视表增加一行"),
    CLIENT_REC_DEL_ROW(15, "可视表删除一行"),
    CLIENT_REC_SET_VAL(16, "可视表设置值"),
    CLIENT_GET_ADDR(17, "获取服务器地址"),
    CLIENT_ADDR_RES(18, "服务器地址信息"),
    CLIENT_HEART_BEAT(19, "心跳"),
    CLIENT_KEY(20, "通信秘钥"), //由RAS私钥加密后的通信秘钥
    CLIENT_KICK(21, "被踢下线"),
    CLIENT_CHECK_CON(22, "检测连接"),
    CLIENT_REC_CLEAR(23, "可视表清空"),
    CLIENT_RESET_UID(24, "重连后恢复session属性"),
    CLIENT_REC_SET_MORE_VAL(25, "可视表设置多行值"),
    ;

    private static final Logger log = LoggerFactory.getLogger(ClientMsgDef.class);

    private final int id;
    private final String desc;

    private static final Map<Integer, ClientMsgDef> MAP = new HashMap<>();

    static {
        for (ClientMsgDef def : values()) {
            if (MAP.containsKey(def.id)) {
                log.error("Repeated client message ID {}", def.id);
            } else {
                MAP.put(def.id, def);
            }
        }
    }

    ClientMsgDef(int id, String desc) {
        this.id = id;
        this.desc = desc;
    }

    public int getId() {
        return id;
    }

    public String getDesc() {
        return desc;
    }

    public static ClientMsgDef getById(int id) {
        return MAP.get(id);
    }
}
