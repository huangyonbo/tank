package back.modules.dataenum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2018/4/18.
 */
public enum NoticeType {
    NULL(0, ""),
    LOGIN(1, "登录公告"),
    GAME(2, "游戏公告");

    private final int id;
    private final String name;

    private NoticeType(final int id, final String name){
        this.id = id;
        this.name = name;
    }

    public int getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public static List<Object> getNoticeType(){
        List<Object> list = new ArrayList<>();
        for (NoticeType noticeType : values()){
            if (noticeType == NULL)
                continue;
            Map<String, Object> map = new HashMap<>();
            map.put("id", noticeType.getId());
            map.put("name", noticeType.getName());
            list.add(map);
        }
        return list;
    }

    public static NoticeType getNoticeType(int type){
        for (NoticeType noticeType : values()){
            if (type == noticeType.getId())
                return noticeType;
        }
        return NULL;
    }

    public static Map<String, Integer> getTypeId(){
        Map<String, Integer> map = new HashMap<>();
        map.put("Login", LOGIN.getId());
        map.put("Game", GAME.getId());
        return map;
    }
}
