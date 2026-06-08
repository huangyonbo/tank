package back.modules.dataenum;

import java.util.HashMap;
import java.util.Map;


public enum AdminType {
    SUPER_ADMIN(0,"超级管理员"),
    FIRST_ADMIN(1,"一级管理员"),
    SECOND_ADMIN(2,"二级管理员"),
    THIRD_ADMIN(3,"三级管理员"),
    FOURTH_ADMIN(4,"四级管理员"),
    PROXY_ALL(5,"总代理"),
    PROXY_ONE(6,"一级代理"),
    PROXY_TWO(7,"二级代理"),
    CONOLY_ADMIN(8,"二级代理"),
    ;

    private int type;
    private String chName;
    
    private AdminType(int type,String chName){
        this.type = type;
        this.chName = chName;
    }

    public int getType(){
        return type;
    }
    
    public String getChName() {
		return chName;
	}

	public static String getLevel(int type){
        AdminType[] values = values();
        for (AdminType at : values){
        	if (at.getType() == type){
        		return at.getChName();
        	}
        }
        return "NULL";
    }

    public static Map<String, Integer> getTypeId(){
        Map<String, Integer> map = new HashMap<>();
        map.put("Super", SUPER_ADMIN.getType());
        map.put("First", FIRST_ADMIN.getType());
        map.put("Second", SECOND_ADMIN.getType());
        map.put("Third", THIRD_ADMIN.getType());
        map.put("Fourth", FOURTH_ADMIN.getType());
        return map;
    }
}
