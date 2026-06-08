package framework.net;

public enum ColonyType {
    COLONY_TYPE_NONE(0, "占位用"),
    COLONY_TYPE_LOAD_RUN_INFO(1, "获取集群信息"),
    COLONY_TYPE_ONLINE(2, "激活节点"),
    COLONY_TYPE_OFFLINE(3, "下线节点"),
    COLONY_TYPE_HEART(4, "节点心跳");

    private final int id;
    private final String desc;

    ColonyType(int id, String desc) {
        this.id = id;
        this.desc = desc;
    }

    public int getId() {
        return id;
    }

    public String getDesc() {
        return desc;
    }

    // 根据ID获取枚举
    public static ColonyType getById(int id) {
        for (ColonyType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return null;
    }
}