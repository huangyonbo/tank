package framework;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JsonUtil {

	private static Map<String,Gson> gsonMap = new ConcurrentHashMap<>();

	private static Gson getGson(){
		String name = Thread.currentThread().getName();
		Gson gson = gsonMap.get(name);
		if (gson == null){
			gson = new Gson();
			gsonMap.put(name,gson);
		}
		return gson;
	}

	public static <T> List<T> decodeToList(JsonElement jsonElement , Class<T> clazz){
		if (jsonElement != null){
			List<T> result = new ArrayList<>();
			Gson gson = getGson();
			JsonArray array = gson.fromJson(jsonElement,JsonArray.class);
			for (int i = 0 ; i < array.size() ; i++){
				JsonElement jo = array.get(i);
				T t = gson.fromJson(jo,clazz);
				result.add(t);
			}
			return result;
		}
		return null;
	}

	public static <T> List<T> decodeToList(String str , Class<T> clazz){
		if (!StringUtils.isEmpty(str)){
			List<T> result = new ArrayList<>();
			Gson gson = getGson();
			JsonArray array = gson.fromJson(str,JsonArray.class);
			for (int i = 0 ; i < array.size() ; i++){
				JsonElement jo = array.get(i);
				T t = gson.fromJson(jo,clazz);
				result.add(t);
			}
			return result;
		}
		return Collections.emptyList();
	}

	public static <T> List<List<T>> decodeToListList(JsonElement element , Class<T> clazz){
		if (element != null){
			List<List<T>> result = new ArrayList<>();
			Gson gson = getGson();
			JsonArray array = gson.fromJson(element,JsonArray.class);
			for (int i = 0 ; i < array.size() ; i++){
				JsonElement jo = array.get(i);
				List<T> list = decodeToList(jo,clazz);
				result.add(list);
			}
			return result;
		}
		return null;
	}

	public static <T> List<List<T>> decodeToListList(String str , Class<T> clazz){
		if (!StringUtils.isEmpty(str)){
			List<List<T>> result = new ArrayList<>();
			Gson gson = getGson();
			JsonArray array = gson.fromJson(str,JsonArray.class);
			for (int i = 0 ; i < array.size() ; i++){
				JsonElement jo = array.get(i);
				List<T> list = decodeToList(jo,clazz);
				result.add(list);
			}
			return result;
		}
		return null;
	}

	public static <K,V> List<Map<K,V>> decodeToListMap(JsonElement element , Class<K> clazz1, Class<V> clazz2){
		if (element != null){
			List<Map<K,V>> result = new ArrayList<>();
			Gson gson = getGson();
			JsonArray array = gson.fromJson(element,JsonArray.class);
			for (int i = 0 ; i < array.size() ; i++){
				JsonElement jo = array.get(i);
				Map<K,V> map = decodeToMap(jo,clazz1,clazz2);
				result.add(map);
			}
			return result;
		}
		return null;
	}

	public static <K,V> List<Map<K,V>> decodeToListMap(String str , Class<K> clazz1, Class<V> clazz2){
		if (!StringUtils.isEmpty(str)){
			List<Map<K,V>> result = new ArrayList<>();
			Gson gson = getGson();
			JsonArray array = gson.fromJson(str,JsonArray.class);
			for (int i = 0 ; i < array.size() ; i++){
				JsonElement jo = array.get(i);
				Map<K,V> map = decodeToMap(jo,clazz1,clazz2);
				result.add(map);
			}
			return result;
		}
		return null;
	}

	public static <T> void decodeToList(List<T> list,String str , Class<T> clazz){
		if (!StringUtils.isEmpty(str)){
			Gson gson = getGson();
			JsonArray array = gson.fromJson(str,JsonArray.class);
			for (int i = 0 ; i < array.size() ; i++){
				JsonElement jo = array.get(i);
				T t = gson.fromJson(jo,clazz);
				list.add(t);
			}
		}
	}

	public static <K,V> void decodeToMap(Map<K,V> result,JsonElement element , Class<K> clazz1 , Class<V> clazz2){
		if (element != null){
			Gson gson = getGson();
			Type type = new TypeToken<Map<JsonElement,JsonElement>>(){}.getType();
			Map<JsonElement,JsonElement> temp = gson.fromJson(element,type);
			for (JsonElement key : temp.keySet()){
				JsonElement value = temp.get(key);
				K k = gson.fromJson(key,clazz1);
				V v = gson.fromJson(value,clazz2);
				result.put(k,v);
			}
		}
	}

	public static <K,V> void decodeToMap(Map<K,V> result,String str , Class<K> clazz1 , Class<V> clazz2){
		if (StringUtils.isNotEmpty(str)){
			Gson gson = getGson();
			Type type = new TypeToken<Map<JsonElement,JsonElement>>(){}.getType();
			Map<JsonElement,JsonElement> temp = gson.fromJson(str,type);
			for (JsonElement key : temp.keySet()){
				JsonElement value = temp.get(key);
				K k = gson.fromJson(key,clazz1);
				V v = gson.fromJson(value,clazz2);
				result.put(k,v);
			}
		}
	}

	public static <K,V> Map<K,V> decodeToMap(JsonElement element , Class<K> clazz1 , Class<V> clazz2){
		if (element != null){
			Map<K,V> result = new HashMap<K,V>();
			decodeToMap(result,element,clazz1,clazz2);
			return result;
		}
		return null;
	}

	public static <K,V> Map<K,V> decodeToMap(String str , Class<K> clazz1 , Class<V> clazz2){
		if (StringUtils.isNotEmpty(str)){
			Map<K,V> result = new HashMap<K,V>();
			decodeToMap(result,str,clazz1,clazz2);
			return result;
		}
		return null;
	}

	public static <K,K1,V> Map<K,Map<K1,V>> decodeToMapMap(JsonElement jsonElement , Class<K> clazz1 ,Class<K1> clazz2 , Class<V> clazz3){
		if (jsonElement != null){
			Map<K,Map<K1,V>> result = new HashMap<>();
			Gson gson = getGson();
			Type type = new TypeToken<Map<JsonElement,Map<JsonElement,JsonElement>>>(){}.getType();
			Map<JsonElement,Map<JsonElement,JsonElement>> temp = gson.fromJson(jsonElement,type);
			for (JsonElement key : temp.keySet()){
				Map<JsonElement,JsonElement> map = temp.get(key);
				Map<K1,V> vs = new HashMap<>();
				for (Map.Entry<JsonElement,JsonElement> entry :  map.entrySet()){
					K1 k1 = gson.fromJson(entry.getKey(),clazz2);
					V v   = gson.fromJson(entry.getValue(),clazz3);
					vs.put(k1,v);
				}
				K k = gson.fromJson(key,clazz1);
				result.put(k,vs);
			}
			return result;
		}
		return null;
	}

	public static <K,K1,V> Map<K,Map<K1,V>> decodeToMapMap(String str , Class<K> clazz1 ,Class<K1> clazz2 , Class<V> clazz3){
		if (!StringUtils.isEmpty(str)){
			Gson gson = getGson();
			Map<K,Map<K1,V>> result = new HashMap<>();
			Type type = new TypeToken<Map<JsonElement,Map<JsonElement,JsonElement>>>(){}.getType();
			Map<JsonElement,Map<JsonElement,JsonElement>> temp = gson.fromJson(str,type);
			for (JsonElement key : temp.keySet()){
				Map<JsonElement,JsonElement> map = temp.get(key);
				Map<K1,V> vs = new HashMap<>();
				for (Map.Entry<JsonElement,JsonElement> entry :  map.entrySet()){
					K1 k1 = gson.fromJson(entry.getKey(),clazz2);
					V v   = gson.fromJson(entry.getValue(),clazz3);
					vs.put(k1,v);
				}
				K k = gson.fromJson(key,clazz1);
				result.put(k,vs);
			}
			return result;
		}
		return null;
	}

	public static <K,V> Map<K,List<V>> decodeToMapList(JsonElement jsonElement , Class<K> clazz1 , Class<V> clazz2){
		if (jsonElement != null){
			Gson gson = getGson();
			Map<K,List<V>> result = new HashMap<>();
			Type type = new TypeToken<Map<JsonElement,List<JsonElement>>>(){}.getType();
			Map<JsonElement,List<JsonElement>> temp = gson.fromJson(jsonElement,type);
			for (JsonElement key : temp.keySet()){
				List<JsonElement> lis = temp.get(key);
				List<V> vs = new ArrayList<V>();
				for (int i = 0 ; i < lis.size() ; i++){
					JsonElement value = lis.get(i);
					V v = gson.fromJson(value,clazz2);
					vs.add(v);
				}
				K k = gson.fromJson(key,clazz1);
				result.put(k,vs);
			}
			return result;
		}
		return null;
	}

	public static <K,V> Map<K,List<V>> decodeToMapList(String str , Class<K> clazz1 , Class<V> clazz2){
		if (!StringUtils.isEmpty(str)){
			Gson gson = getGson();
			Map<K,List<V>> result = new HashMap<>();
			Type type = new TypeToken<Map<JsonElement,List<JsonElement>>>(){}.getType();
			Map<JsonElement,List<JsonElement>> temp = gson.fromJson(str,type);
			for (JsonElement key : temp.keySet()){
				List<JsonElement> lis = temp.get(key);
				List<V> vs = new ArrayList<V>();
				for (int i = 0 ; i < lis.size() ; i++){
					JsonElement value = lis.get(i);
					V v = gson.fromJson(value,clazz2);
					vs.add(v);
				}
				K k = gson.fromJson(key,clazz1);
				result.put(k,vs);
			}
			return result;
		}
		return null;
	}

	public static <T> T decodeToObj(JsonElement element , Class<T> clazz){
		if (element == null){
			return null;
		}
		return getGson().fromJson(element,clazz);
	}

	public static <T> T decodeToObj(String str , Class<T> clazz){
		if (!StringUtils.isEmpty(str)){
			return getGson().fromJson(str,clazz);
		}
		return null;
	}

	public static String encodeToStr(Object obj) {
		//做修改
		if (obj == null){
			return "";
		}
		String str = getGson().toJson(obj);
		return str;
	}

	public static JsonElement encodeToElement(Object obj) {
		if (obj == null) {
			return null;
		}
		return getGson().toJsonTree(obj);
	}
}
