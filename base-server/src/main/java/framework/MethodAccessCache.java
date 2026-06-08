package framework;

import com.esotericsoftware.reflectasm.MethodAccess;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MethodAccessCache {

    private static Map<Class<?>, MethodAccess> caches = new ConcurrentHashMap<>();

    public static MethodAccess tryToGet(Class<?> type) {
        if (caches.containsKey(type)) {
            return caches.get(type);
        }
        MethodAccess access = MethodAccess.get(type);
        caches.put(type, access);
        return access;
    }
}
