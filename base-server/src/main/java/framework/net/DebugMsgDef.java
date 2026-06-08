package framework.net;

import lombok.Getter;

/**
 * 
 * 描述：
 * 
 */
@Getter
public enum DebugMsgDef {

    DEBUG_REQUEST_SER_LIST(0, "请求服务器列表"),
    DEBUG_REFRESH_SERVER(1, "刷新服务器"),
    DEBUG_LOAD_OBJECT(2, "加载对象"),

    DEBUG_RES_SER_LIST(3, "服务器列表响应"),
    DEBUG_RES_SERVER(4, "服务器信息响应"),
    DEBUG_RES_OBJ(5, "对象加载响应"),

    DEBUG_PUB_SER(6, "发布服务器"),
    DEBUG_PUB_SER_RES(7, "发布服务器响应"),
    DEBUG_PUB_SPACE(8, "发布空间"),
    DEBUG_PUB_SPACE_RES(9, "发布空间响应"),
    DEBUG_PUB_DATA(10, "发布数据"),
    DEBUG_PUB_DATA_RES(11, "发布数据响应");

    private final int id;
    private final String desc;

    DebugMsgDef(int id, String desc) {
        this.id = id;
        this.desc = desc;
    }

    // 根据ID获取枚举
    public static DebugMsgDef getById(int id) {
        for (DebugMsgDef def : values()) {
            if (def.id == id) {
                return def;
            }
        }
        return null;
    }
}