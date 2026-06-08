package framework.Store;

import com.google.gson.JsonElement;
import framework.JsonUtil;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ParamData {
    private String[] types;
    private JsonElement element;
    private String method;

    public String encode(Object obj) {
        if (obj == null){
            return null;
        }
        element = JsonUtil.encodeToElement(obj);
        if (List.class.isAssignableFrom(obj.getClass())){
            List<?> list = (List<?>) obj;
            if (list.size() == 0){
                return null;
            }
            Object _obj = list.get(0);
            if (_obj == null){
                return null;
            }
            if (List.class.isAssignableFrom(_obj.getClass())){
                method = "decodeToListList";
                types = new String[1];
                List<?> l2 = (List<?>) _obj;
                if (l2.size() == 0){
                    return null;
                }
                types[0] = l2.get(0).getClass().getName();
            }else if (Map.class.isAssignableFrom(_obj.getClass())){
                Map<?,?> map = (Map<?,?>)_obj;
                if (map.size() == 0){
                    return null;
                }
                Map.Entry<?, ?> entry = map.entrySet().stream().findAny().get();
                method = "decodeToListMap";
                types  = new String[2];
                types[0] = entry.getKey().getClass().getName();
                types[1] = entry.getValue().getClass().getName();
            }else{
                types = new String[1];
                types[0] = _obj.getClass().getName();
            }
        }else if (Map.class.isAssignableFrom(obj.getClass())){
            Map<?,?> map = (Map<?,?>)obj;
            if (map == null || map.size() == 0){
                return null;
            }
            Map.Entry<?, ?> _obj = map.entrySet().stream().findAny().get();
            if (_obj == null){
                return null;
            }
            Object key   = _obj.getKey();
            Object value = _obj.getValue();
            if (value == null){
                return null;
            }
            if (List.class.isAssignableFrom(value.getClass())){
                method   = "decodeToMapList";
                List<?> list = ((List<?>)value);
                if (list == null || list.size() == 0){
                    return null;
                }
                Object o = list.get(0);
                if (o == null){
                    return null;
                }
                types    = new String[2];
                types[0] = key.getClass().getName();
                types[1] = o.getClass().getName();
            }else if (Map.class.isAssignableFrom(value.getClass())){
                Map<?,?> m2 = (Map<?,?>)value;
                if (m2 == null || m2.size() == 0){
                    return null;
                }
                method = "decodeToMapMap";
                Map.Entry<?, ?> o = m2.entrySet().stream().findAny().get();
                types = new String[3];
                types[0] = key.getClass().getName();
                types[1] = o.getKey().getClass().getName();
                types[2] = o.getValue().getClass().getName();
            }else{
                types = new String[2];
                types[0] = key.getClass().getName();
                types[1] = value.getClass().getName();
            }
        }
        return JsonUtil.encodeToStr(this);
    }

    public Object decodeToObject(Class<?> base) {
        try {
            if (List.class.isAssignableFrom(base)){
                if ("decodeToListList".equals(method)){
                    return JsonUtil.decodeToListList(element,Class.forName(types[0]));
                }else if ("decodeToListMap".equals(method)){
                    return JsonUtil.decodeToListMap(element,Class.forName(types[0]),Class.forName(types[1]));
                }else{
                    return JsonUtil.decodeToList(element,Class.forName(types[0]));
                }
            }else if (Map.class.isAssignableFrom(base)){
                if ("decodeToMapList".equals(method)){
                    return JsonUtil.decodeToMapList(element,Class.forName(types[0]),Class.forName(types[1]));
                }else if ("decodeToMapMap".equals(method)){
                    return JsonUtil.decodeToMapMap(element,Class.forName(types[0]),Class.forName(types[1]),Class.forName(types[2]));
                }else{
                    return JsonUtil.decodeToMap(element,Class.forName(types[0]),Class.forName(types[1]));
                }
            }else{
                return JsonUtil.decodeToObj(element,base);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
