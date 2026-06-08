package framework.pub;

import redis.clients.jedis.Jedis;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PubUtils {

    private static final String PUB_HEAD_KEY = "lp_mry_";
    public static final String PUB_FILED_KEY_STR = "pubdata";
    private static final byte[] PUB_FILED_KEY = PUB_FILED_KEY_STR.getBytes();
    private static final String SYNC_LOCK_KEY = "sync_lock_key";
    private static Map<String,PubData> cache = new ConcurrentHashMap<>();//单节点的时候的缓存

    public static void tick(){
        long time = System.currentTimeMillis();
        for (Map.Entry<String,PubData> entry : cache.entrySet()){
            if (entry.getValue().tick(time)){
                cache.remove(entry.getKey());
            }
        }
    }

    /**
     * 异步是否可以读取
     * @param jedis
     * @return
     */
    private static boolean syncCouldRead(Jedis jedis){
        String key   = getKey(SYNC_LOCK_KEY);
        String flag  = jedis.hget(key,PubUtils.PUB_FILED_KEY_STR);
        if ("1".equals(flag)){
            return false;
        }
        return true;
    }

    /**
     * PublicLogic 和 GameLogic不在一个jvm的时候
     * @param jedis
     * @param pubName
     * @return
     */
    public static PubData loadSyncData(Jedis jedis, String pubName){
        try {
            while (!syncCouldRead(jedis)){
                Thread.sleep(1);
            }
            String key   = getKey(pubName);
            byte[] bytes = jedis.hget(key.getBytes(),PUB_FILED_KEY);
            if (bytes == null) {
                return null;
            }
            return new PubData(pubName).load(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 锁定
     * @param jedis
     */
    private static void syncLock(Jedis jedis){
        String key   = getKey(SYNC_LOCK_KEY);
        jedis.hset(key,PUB_FILED_KEY_STR,"1");
    }

    /**
     * 解锁
     * @param jedis
     */
    public static void syncUnLock(Jedis jedis){
        String key   = getKey(SYNC_LOCK_KEY);
        jedis.hset(key,PUB_FILED_KEY_STR,"0");
    }

    /**
     * PublicLogic 和 GameLogic在一个jvm的时候
     * @param jedis
     * @param pubName
     * @return
     */
    public static PubData loadData(Jedis jedis, String pubName){
        try {
            String key   = getKey(pubName);
            PubData data = cache.get(key);
            if (data == null){
                byte[] bytes = jedis.hget(key.getBytes(),PUB_FILED_KEY);
                if (bytes == null) {
                    return null;
                }
                data = new PubData(pubName);
                data.load(bytes);
                cache.put(key,data);
            }else{
                data.onUse();
            }
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 存档公共数据
     * @param jedis
     * @param data
     * @return
     */
    public static boolean storeData(Jedis jedis, PubData data){
        try {
            syncLock(jedis);
            byte[] bytes = data.getStoreData();
            String key   = getKey(data.getName());
            jedis.hset(key.getBytes(),PUB_FILED_KEY,bytes);
            syncUnLock(jedis);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getKey(String pubName){
        return PUB_HEAD_KEY  + pubName;
    }

    /**
     * 删除公共数据
     * @param jedis
     * @param pubName
     * @return
     */
    public static boolean deleteData(Jedis jedis, String pubName) {
        try {
            syncLock(jedis);
            String key  = getKey(pubName);
            jedis.hdel(key,PUB_FILED_KEY_STR);
            cache.remove(key);
            syncUnLock(jedis);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
