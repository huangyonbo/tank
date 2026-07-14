package game.modules.weapon;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.utils.StringUtils;
import framework.JsonUtil;
import framework.PropertyKey;
import framework.game.IGameObject;
import framework.game.IKernel;
import framework.game.ILogicModule;
import framework.game.KernelEvent;
import framework.mybatis.domain.CustomMapRecord;
import framework.mybatis.service.impl.CustomMapRecordService;
import game.custommsg.CustomMsg;
import game.custommsg.RequestMsgDef;
import game.custommsg.ServerCodeDef;
import game.modules.activities.code.JSONResult;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自定义地图模块
 * 存储分层：
 * 1) MySQL：所有地图长期持久化
 * 2) Redis：1天热地图缓存 + 热度榜
 * 3) 内存：活跃地图短期缓存
 */
public class CustomMapModule implements ILogicModule {
    private static final String KEY_MAP_ID = "mapId";
    private static final String KEY_MAP_DATA = "mapData";
    private static final String KEY_MAP_NAME = "mapName";
    private static final String KEY_PAGE = "page";
    private static final String KEY_TYPE = "type";
    private static final String KEY_COMPLETED = "completed";

    /**
     * 每行地图格串（不含末行可选的 :spawnX:spawnY）的最小/最大长度
     */
    private static final int MIN_WIDTH = 5;
    private static final int MAX_WIDTH = 40;
    private static final int MAX_HEIGHT = 40;
    private static final int MAX_MAP_COUNT_PER_PLAYER = 10;
    private static final int MIN_LEVEL_TO_UPLOAD = 0;
    private static final int LIST_PAGE_SIZE = 20;

    private static final String TEMP_CUSTOM_LEVEL_STARTED = "TempCustomLevelStarted";
    private static final String TEMP_CUSTOM_LEVEL_LAST_FINISH_TIME = "TempCustomLevelLastFinishTime";
    private static final long START_AFTER_FINISH_COOLDOWN_MS = 2000L;

    private static final long REDIS_HOT_TTL_SECONDS = 24 * 60 * 60L; // 1天
    private static final String REDIS_HOT_RANK = "CustomMap::HotRank::1D";
    private static final String REDIS_HOT_MAP_PREFIX = "CustomMap::HotMap::"; // + mapId

    // 周榜/月榜：按自然周/自然月分桶（key 带年月周/年月），到期自动淘汰
    private static final long REDIS_WEEK_RANK_TTL_SECONDS = 8L * 24 * 60 * 60;   // 8天，覆盖跨天延迟
    private static final long REDIS_MONTH_RANK_TTL_SECONDS = 35L * 24 * 60 * 60; // 35天，覆盖跨月延迟
    private static final String REDIS_RANK_PREFIX = "CustomMap::Rank::"; // + metric + ::W::yyyyWww / ::M::yyyyMM
    private static final String METRIC_LIKE = "Like";
    private static final String METRIC_HOT = "Hot";
    private static final String METRIC_HARD = "Hard";

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyyMM");

    /**
     * 每日每图点赞一次
     */
    private static final String REDIS_LIKE_ONCE_PREFIX = "CustomMap::LikeOnce::";
    /**
     * 玩家对某图的对局会话（开始/结束配对）
     */
    private static final String REDIS_PLAY_PREFIX = "CustomMap::Play::";
    private static final int PLAY_SESSION_TTL_SECONDS = 48 * 3600;

    private static final String LUA_DEL_PLAY_IF_EXISTS =
            "if redis.call('exists', KEYS[1]) == 1 then redis.call('del', KEYS[1]) return 1 else return 0 end";

    private static final long ACTIVE_CACHE_TTL_MS = 30 * 60 * 1000L; // 30分钟
    private static final int ACTIVE_CACHE_MAX = 2000;

    // 地图字符白名单（按行校验；f/F 为客户端标准关卡常用边界格）
    private static final String CELL_REGEX = "^[40cdDE81I76abBC9gefF(XYZ!*\\#$%\\^&uvwxyzARSTnopqrstUVW5hijklmOPQ@:]+$";

    /**
     * 仅末行允许附加出生点：整行形如 mapChars:spawnX:spawnY。
     * spawnX/spawnY 可为整数或小数（如 0.8），与客户端格子坐标一致。
     */
    private static final Pattern LAST_ROW_SPAWN_SUFFIX = Pattern.compile(
            "^(.+):(-?(?:\\d+(?:\\.\\d+)?|\\.\\d+)):(-?(?:\\d+(?:\\.\\d+)?|\\.\\d+))$");

    private final Map<Long, ActiveMapCache> m_activeMaps = new ConcurrentHashMap<>();

    @Override
    public boolean onInit(IKernel kernel) {
        kernel.regRequestMessage(RequestMsgDef.REQ_CUSTOM_MAP_SAVE.getId(), this, "OnReqCustomMapSave");
        kernel.regRequestMessage(RequestMsgDef.REQ_CUSTOM_MAP_LIST.getId(), this, "OnReqCustomMapList");
        kernel.regRequestMessage(RequestMsgDef.REQ_CUSTOM_MAP_DELETE.getId(), this, "OnReqCustomMapDelete");
        kernel.regRequestMessage(RequestMsgDef.REQ_CUSTOM_MAP_PUBLIC_LIST.getId(), this, "OnReqCustomMapPublicList");
        kernel.regRequestMessage(RequestMsgDef.REQ_CUSTOM_MAP_GET_BY_ID.getId(), this, "OnReqCustomMapGetById");
        kernel.regRequestMessage(RequestMsgDef.REQ_CUSTOM_MAP_LIKE_BY_ID.getId(), this, "OnReqCustomMapLike");
        kernel.regRequestMessage(RequestMsgDef.REQ_CUSTOM_MAP_START.getId(), this, "OnReqCustomMapStart");
        kernel.regRequestMessage(RequestMsgDef.REQ_CUSTOM_MAP_END.getId(), this, "OnReqCustomMapEnd");

        kernel.regEvent(KernelEvent.KEVENT_OFF_LINE, "Player", this, "OnPlayerOffLine");
        kernel.regEvent(KernelEvent.KEVENT_ON_LINE, "Player", this, "OnPlayerOnLine");
        return true;
    }

    @Override
    public void onDestroy() {
        m_activeMaps.clear();
    }

    /**
     * 接口2：玩家上传/更新地图
     */
    void OnReqCustomMapSave(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
            throws InvalidProtocolBufferException {
        JSONObject req = parseReq(msg);
        if (req == null) {
            responseSimple(kernel, player, reqid, ServerCodeDef.CODE_PARAM_ERR);
            return;
        }

        int uid = player.getInt(PropertyKey.PLAYER_PROPERTY_UID);
        if (uid <= 0) {
            responseSimple(kernel, player, reqid, ServerCodeDef.CODE_PARAM_ERR);
            return;
        }
        int completedLevel = player.getInt(PropertyKey.PLAYER_LEVELS_COMPLETED);
        if (completedLevel < MIN_LEVEL_TO_UPLOAD) {
            responseSimple(kernel, player, reqid, ServerCodeDef.CODE_CON_LIMIT);
            return;
        }

        MapShape shape = validateAndParseMap(req.getString(KEY_MAP_DATA));
        if (!shape.ok) {
            responseSimple(kernel, player, reqid, ServerCodeDef.CODE_PARAM_ERR);
            return;
        }

        String tmpMapName = req.getString(KEY_MAP_NAME);
        if (tmpMapName == null) {
            tmpMapName = "";
        } else {
            tmpMapName = tmpMapName.trim();
        }
        final String mapName = tmpMapName;

        Long reqMapId = req.getLong(KEY_MAP_ID);
        JSONResult resp = new JSONResult();

        if (reqMapId == null || reqMapId <= 0L) {
            // 新建：先查玩家数量上限
            List<Object> cntParams = new ArrayList<>();
            cntParams.add(uid);
            kernel.executeSomeToStore(CustomMapRecordService.class, "countByOwner", cntParams, cntRes -> {
                long count = StringUtils.parseLongSafe(cntRes);
                if (count >= MAX_MAP_COUNT_PER_PLAYER) {
                    responseSimple(kernel, player, reqid, ServerCodeDef.CODE_TIMES_LIMIT);
                    return;
                }
                CustomMapRecord record = new CustomMapRecord();
                record.setOwnerUid(uid);
                record.setMapName(mapName);
                record.setMapData(shape.normalizedData);
                record.setWidth(shape.width);
                record.setHeight(shape.height);
                record.setHeat(0L);
                kernel.executeSomeToStore(CustomMapRecordService.class, "createMap", Collections.singletonList(record), saveRes -> {
                    if (saveRes == null) {
                        responseSimple(kernel, player, reqid, ServerCodeDef.CODE_FAILED);
                        return;
                    }
                    // store 层使用 gson 序列化，避免 fastjson 对 Date 格式解析失败
                    CustomMapRecord created = JsonUtil.decodeToObj(saveRes, CustomMapRecord.class);
                    if (created == null || created.getMapId() == null || created.getMapId() <= 0L) {
                        responseSimple(kernel, player, reqid, ServerCodeDef.CODE_FAILED);
                        return;
                    }
                    cacheHotMap(kernel, created);
                    cacheActiveMap(created);
                    resp.setDesc(ServerCodeDef.CODE_SUCCESS);
                    resp.put(KEY_MAP_ID, created.getMapId());
                    resp.put("created", true);
                    kernel.response(player, reqid, CustomMsg.String.newBuilder().setValue(resp.toJSONString()).build().toByteArray());
                });
            });
            return;
        }

        // 更新：先校验归属
        List<Object> getParams = new ArrayList<>();
        getParams.add(reqMapId);
        kernel.executeSomeToStore(CustomMapRecordService.class, "getByMapId", getParams, oneRes -> {
            if (oneRes == null) {
                responseSimple(kernel, player, reqid, ServerCodeDef.CODE_NOT_EXIST);
                return;
            }
            // store 层使用 gson 序列化，避免 fastjson 对 Date 格式解析失败
            CustomMapRecord old = JsonUtil.decodeToObj(oneRes, CustomMapRecord.class);
            if (old == null || old.getMapId() == null) {
                responseSimple(kernel, player, reqid, ServerCodeDef.CODE_NOT_EXIST);
                return;
            }
            if (old.getOwnerUid() == null || old.getOwnerUid() != uid) {
                responseSimple(kernel, player, reqid, ServerCodeDef.CODE_CON_LIMIT);
                return;
            }

            CustomMapRecord toUpdate = new CustomMapRecord();
            toUpdate.setMapId(reqMapId);
            toUpdate.setOwnerUid(uid);
            toUpdate.setMapName(mapName);
            toUpdate.setMapData(shape.normalizedData);
            toUpdate.setWidth(shape.width);
            toUpdate.setHeight(shape.height);

            kernel.executeSomeToStore(CustomMapRecordService.class, "updateMap", Collections.singletonList(toUpdate), updRes -> {
                if (!StringUtils.parseBooleanSafe(updRes)) {
                    responseSimple(kernel, player, reqid, ServerCodeDef.CODE_FAILED);
                    return;
                }
                // 刷新缓存
                old.setMapName(mapName);
                old.setMapData(shape.normalizedData);
                old.setWidth(shape.width);
                old.setHeight(shape.height);
                cacheHotMap(kernel, old);
                cacheActiveMap(old);
                resp.setDesc(ServerCodeDef.CODE_SUCCESS);
                resp.put(KEY_MAP_ID, reqMapId);
                resp.put("created", false);
                kernel.response(player, reqid, CustomMsg.String.newBuilder().setValue(resp.toJSONString()).build().toByteArray());
            });
        });
    }

    /**
     * 接口1：玩家查看自己的地图
     */
    void OnReqCustomMapList(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg) {
        int uid = player.getInt(PropertyKey.PLAYER_PROPERTY_UID);
        if (uid <= 0) {
            responseSimple(kernel, player, reqid, ServerCodeDef.CODE_PARAM_ERR);
            return;
        }
        List<Object> params = new ArrayList<>();
        params.add(uid);
        params.add(0);
        params.add(MAX_MAP_COUNT_PER_PLAYER);
        JSONResult resp = new JSONResult();
        kernel.executeSomeToStore(CustomMapRecordService.class, "listByOwner", params, listRes -> {
            JSONArray list = new JSONArray();
            if (listRes != null) {

                List<CustomMapRecord> maps = JsonUtil.decodeToList(listRes, CustomMapRecord.class);
                if (maps != null) {
                    for (CustomMapRecord one : maps) {
                        if (one != null) {
                            list.add(toJson(one, true));
                            cacheActiveMap(one);
                        }
                    }
                }
            }
            resp.setDesc(ServerCodeDef.CODE_SUCCESS);
            resp.put("count", list.size());
            resp.put("list", list);
            kernel.response(player, reqid, CustomMsg.String.newBuilder().setValue(resp.toJSONString()).build().toByteArray());
        });
    }

    /**
     * 接口3：删除自己的某张地图
     */
    void OnReqCustomMapDelete(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
            throws InvalidProtocolBufferException {
        JSONObject req = parseReq(msg);
        if (req == null) {
            responseSimple(kernel, player, reqid, ServerCodeDef.CODE_PARAM_ERR);
            return;
        }
        Long mapId = req.getLong(KEY_MAP_ID);
        if (mapId == null || mapId <= 0L) {
            responseSimple(kernel, player, reqid, ServerCodeDef.CODE_PARAM_ERR);
            return;
        }
        int uid = player.getInt(PropertyKey.PLAYER_PROPERTY_UID);
        if (uid <= 0) {
            responseSimple(kernel, player, reqid, ServerCodeDef.CODE_PARAM_ERR);
            return;
        }

        List<Object> params = new ArrayList<>();
        params.add(mapId);
        params.add(uid);
        JSONResult resp = new JSONResult();
        kernel.executeSomeToStore(CustomMapRecordService.class, "removeByMapIdAndOwner", params, delRes -> {
            ServerCodeDef code = StringUtils.parseBooleanSafe(delRes) ? ServerCodeDef.CODE_SUCCESS : ServerCodeDef.CODE_NOT_EXIST;
            if (code == ServerCodeDef.CODE_SUCCESS) {
                removeHotMap(kernel, mapId);
                m_activeMaps.remove(mapId);
            }
            resp.setDesc(code);
            resp.put(KEY_MAP_ID, mapId);
            kernel.response(player, reqid, CustomMsg.String.newBuilder().setValue(resp.toJSONString()).build().toByteArray());
        });
    }

    /**
     * 接口4：公共地图列表（newest/hot）
     */
    void OnReqCustomMapPublicList(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
            throws InvalidProtocolBufferException {
        JSONObject req = parseReqAllowEmpty(msg);
        int page = Math.max(1, req.getIntValue(KEY_PAGE));
        String reqType = StringUtils.parseStringSafe(req.getString(KEY_TYPE)).toLowerCase();
        // 兼容旧值：hot => hot_day
        if ("hot".equals(reqType)) {
            reqType = "hot_day";
        }
        final String type = reqType;
        int offset = (page - 1) * LIST_PAGE_SIZE;

        if ("random".equals(type)) {
            clearExpiredActiveCacheIfNeeded();
            List<ActiveMapCache> actives = new ArrayList<>(m_activeMaps.values());
            JSONArray list = new JSONArray();
            long total = actives.size();
            if (!actives.isEmpty()) {
                Collections.shuffle(actives);
                int take = Math.min(LIST_PAGE_SIZE, actives.size());
                for (int i = 0; i < take; i++) {
                    ActiveMapCache one = actives.get(i);
                    if (one != null && one.record != null) {
                        list.add(toJson(one.record, false));
                    }
                }
            }
            JSONResult resp = new JSONResult();
            resp.setDesc(ServerCodeDef.CODE_SUCCESS);
            resp.put(KEY_PAGE, page);
            resp.put("pageSize", LIST_PAGE_SIZE);
            resp.put("type", type);
            resp.put("total", total);
            resp.put("list", list);
            kernel.response(player, reqid, CustomMsg.String.newBuilder().setValue(resp.toJSONString()).build().toByteArray());
            return;
        }

        String rankKey = resolveRankKey(type);
        if (rankKey != null) {
            // Redis 榜单（热/赞/难 + 周/月；以及历史兼容的 hot_day）
            Jedis jedis = kernel.getJedis();
            JSONArray list = new JSONArray();
            long total = 0L;
            if (jedis != null) {
                total = jedis.zcard(rankKey);
                int end = offset + LIST_PAGE_SIZE - 1;
                List<String> ids = jedis.zrevrange(rankKey, offset, end);
                if (ids != null) {
                    for (String idStr : ids) {
                        long mapId = StringUtils.parseLongSafe(idStr);
                        if (mapId <= 0L) {
                            continue;
                        }
                        JSONObject one = loadMapForRead(kernel, mapId, false, 0);
                        if (one != null) {
                            list.add(one);
                        }
                    }
                }
            }
            JSONResult resp = new JSONResult();
            resp.setDesc(ServerCodeDef.CODE_SUCCESS);
            resp.put(KEY_PAGE, page);
            resp.put("pageSize", LIST_PAGE_SIZE);
            resp.put("type", type);
            resp.put("rankKey", rankKey);
            resp.put("total", total);
            resp.put("list", list);
            kernel.response(player, reqid, CustomMsg.String.newBuilder().setValue(resp.toJSONString()).build().toByteArray());
            return;
        }

        // newest 从 MySQL 查，保证全量
        List<Object> listParams = new ArrayList<>();
        listParams.add(offset);
        listParams.add(LIST_PAGE_SIZE);
        kernel.executeSomeToStore(CustomMapRecordService.class, "listNewest", listParams, listRes -> {
            JSONArray list = new JSONArray();
            if (listRes != null) {
                List<CustomMapRecord> maps = JsonUtil.decodeToList(listRes, CustomMapRecord.class);
                if (maps != null) {
                    for (CustomMapRecord one : maps) {
                        if (one != null) {
                            list.add(toJson(one, false));
                            cacheActiveMap(one);
                        }
                    }
                }
            }
            kernel.executeSomeToStore(CustomMapRecordService.class, "countAll", null, cntRes -> {
                long total = StringUtils.parseLongSafe(cntRes);
                JSONResult resp = new JSONResult();
                resp.setDesc(ServerCodeDef.CODE_SUCCESS);
                resp.put(KEY_PAGE, page);
                resp.put("pageSize", LIST_PAGE_SIZE);
                resp.put("type", type);
                resp.put("total", total);
                resp.put("list", list);
                kernel.response(player, reqid, CustomMsg.String.newBuilder().setValue(resp.toJSONString()).build().toByteArray());
            });
        });
    }

    /**
     * 接口5：按 mapId 获取地图
     */
    void OnReqCustomMapGetById(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
            throws InvalidProtocolBufferException {
        JSONObject req = parseReq(msg);
        if (req == null) {
            responseSimple(kernel, player, reqid, ServerCodeDef.CODE_PARAM_ERR);
            return;
        }
        long mapId = req.getLongValue(KEY_MAP_ID);
        if (mapId <= 0L) {
            responseSimple(kernel, player, reqid, ServerCodeDef.CODE_PARAM_ERR);
            return;
        }

        int viewerUid = player.getInt(PropertyKey.PLAYER_PROPERTY_UID);
        JSONObject map = loadMapForRead(kernel, mapId, true, viewerUid);
        if (map != null) {
            JSONResult resp = new JSONResult();
            resp.setDesc(ServerCodeDef.CODE_SUCCESS);
            resp.put(KEY_MAP_ID, mapId);
            resp.put("map", map);
            kernel.response(player, reqid, CustomMsg.String.newBuilder().setValue(resp.toJSONString()).build().toByteArray());
            return;
        }

        // Redis/内存未命中，回源 MySQL
        List<Object> params = new ArrayList<>();
        params.add(mapId);
        kernel.executeSomeToStore(CustomMapRecordService.class, "getByMapId", params, oneRes -> {
            ServerCodeDef code = ServerCodeDef.CODE_SUCCESS;
            JSONObject outMap = null;
            if (oneRes == null) {
                code = ServerCodeDef.CODE_NOT_EXIST;
            } else {
                // store 层使用 gson 序列化，避免 fastjson 对 Date 格式解析失败
                CustomMapRecord one = JsonUtil.decodeToObj(oneRes, CustomMapRecord.class);
                if (one == null || one.getMapId() == null) {
                    code = ServerCodeDef.CODE_NOT_EXIST;
                } else {
                    cacheHotMap(kernel, one);
                    cacheActiveMap(one);
                    touchHeat(kernel, one, viewerUid);
                    outMap = toJson(one, true);
                }
            }
            JSONResult resp = new JSONResult();
            resp.setDesc(code);
            resp.put(KEY_MAP_ID, mapId);
            if (outMap != null) {
                resp.put("map", outMap);
            }
            kernel.response(player, reqid, CustomMsg.String.newBuilder().setValue(resp.toJSONString()).build().toByteArray());
        });
    }

    /**
     * 接口6：按 mapId 点赞（同一玩家同一地图每天最多 1 次）
     */
    void OnReqCustomMapLike(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
            throws InvalidProtocolBufferException {
        JSONObject req = parseReq(msg);
        if (req == null) {
            responseSimple(kernel, player, reqid, ServerCodeDef.CODE_PARAM_ERR);
            return;
        }
        long mapId = req.getLongValue(KEY_MAP_ID);
        if (mapId <= 0L) {
            responseSimple(kernel, player, reqid, ServerCodeDef.CODE_PARAM_ERR);
            return;
        }
        int uid = player.getInt(PropertyKey.PLAYER_PROPERTY_UID);
        if (uid <= 0) {
            responseSimple(kernel, player, reqid, ServerCodeDef.CODE_PARAM_ERR);
            return;
        }
        Jedis jedis = kernel.getJedis();
        if (jedis == null) {
            responseSimple(kernel, player, reqid, ServerCodeDef.CODE_FAILED);
            return;
        }

        List<Object> params = new ArrayList<>();
        params.add(mapId);
        kernel.executeSomeToStore(CustomMapRecordService.class, "getByMapId", params, oneRes -> {
            CustomMapRecord one = JsonUtil.decodeToObj(oneRes, CustomMapRecord.class);
            if (one == null || one.getMapId() == null) {
                responseSimple(kernel, player, reqid, ServerCodeDef.CODE_NOT_EXIST);
                return;
            }
            if (one.getOwnerUid() != null && one.getOwnerUid() == uid) {
                responseSimple(kernel, player, reqid, ServerCodeDef.CODE_CON_LIMIT);
                return;
            }
            String likeKey = likeOnceKey(uid, mapId);
            String setRes = jedis.set(likeKey, "1", SetParams.setParams().nx().ex(secondsUntilEndOfDay()));
            if (!"OK".equals(setRes)) {
                responseSimple(kernel, player, reqid, ServerCodeDef.CODE_TIMES_LIMIT);
                return;
            }
            List<Object> incLike = new ArrayList<>();
            incLike.add(mapId);
            kernel.executeSomeToStore(CustomMapRecordService.class, "recordLikeIncrement", incLike, incRes -> {
                CustomMapRecord updated = JsonUtil.decodeToObj(incRes, CustomMapRecord.class);
                if (updated == null || updated.getMapId() == null) {
                    jedis.del(likeKey);
                    responseSimple(kernel, player, reqid, ServerCodeDef.CODE_NOT_EXIST);
                    return;
                }
                touchLike(kernel, mapId, 1D);
                cacheHotMap(kernel, updated);
                cacheActiveMap(updated);
                JSONResult resp = new JSONResult();
                resp.setDesc(ServerCodeDef.CODE_SUCCESS);
                resp.put(KEY_MAP_ID, mapId);
                resp.put("liked", true);
                resp.put("likeCount", countLong(updated.getLikeCount()));
                kernel.response(player, reqid, CustomMsg.String.newBuilder().setValue(resp.toJSONString()).build().toByteArray());
            });
        });
    }

    /**
     * 接口7：开始挑战某张自定义地图（同一玩家同一地图不可重复开始，直到结束或会话过期）
     */
    void OnReqCustomMapStart(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
            throws InvalidProtocolBufferException {
        JSONObject req = parseReq(msg);
        if (req == null) {
            responseSimple(kernel, player, reqid, ServerCodeDef.CODE_PARAM_ERR);
            return;
        }
        long mapId = req.getLongValue(KEY_MAP_ID);
        if (mapId <= 0L) {
            responseSimple(kernel, player, reqid, ServerCodeDef.CODE_PARAM_ERR);
            return;
        }
        int uid = player.getInt(PropertyKey.PLAYER_PROPERTY_UID);
        if (uid <= 0) {
            responseSimple(kernel, player, reqid, ServerCodeDef.CODE_PARAM_ERR);
            return;
        }
        // 已开始则不能重复开始
        if (player.getTempBool(TEMP_CUSTOM_LEVEL_STARTED)) {
            responseSimple(kernel, player, reqid, ServerCodeDef.CODE_WRONG_STATE);
            return;
        }
        // 不能刷接口
        if (kernel.getServerTime() - player.getTempLong(TEMP_CUSTOM_LEVEL_LAST_FINISH_TIME) <= START_AFTER_FINISH_COOLDOWN_MS) {
            responseSimple(kernel, player, reqid, ServerCodeDef.CODE_WRONG_STATE);
            return;
        }
        Jedis jedis = kernel.getJedis();
        if (jedis == null) {
            responseSimple(kernel, player, reqid, ServerCodeDef.CODE_FAILED);
            return;
        }

        List<Object> params = new ArrayList<>();
        params.add(mapId);
        kernel.executeSomeToStore(CustomMapRecordService.class, "getByMapId", params, oneRes -> {
            CustomMapRecord one = JsonUtil.decodeToObj(oneRes, CustomMapRecord.class);
            if (one == null || one.getMapId() == null) {
                responseSimple(kernel, player, reqid, ServerCodeDef.CODE_NOT_EXIST);
                return;
            }
            String playKey = playSessionKey(uid, mapId);
            String setRes = jedis.set(playKey, String.valueOf(System.currentTimeMillis()),
                    SetParams.setParams().nx().ex(PLAY_SESSION_TTL_SECONDS));
            if (!"OK".equals(setRes)) {
                responseSimple(kernel, player, reqid, ServerCodeDef.CODE_WRONG_STATE);
                return;
            }
            List<Object> startParams = new ArrayList<>();
            startParams.add(mapId);
            kernel.executeSomeToStore(CustomMapRecordService.class, "recordPlayStart", startParams, startRes -> {
                CustomMapRecord updated = JsonUtil.decodeToObj(startRes, CustomMapRecord.class);
                if (updated == null || updated.getMapId() == null) {
                    jedis.del(playKey);
                    responseSimple(kernel, player, reqid, ServerCodeDef.CODE_NOT_EXIST);
                    return;
                }
                cacheHotMap(kernel, updated);
                cacheActiveMap(updated);
                JSONResult resp = new JSONResult();
                resp.setDesc(ServerCodeDef.CODE_SUCCESS);
                resp.put(KEY_MAP_ID, mapId);
                resp.put("started", true);
                resp.put("playCount", countLong(updated.getPlayCount()));
                kernel.response(player, reqid, CustomMsg.String.newBuilder().setValue(resp.toJSONString()).build().toByteArray());
                player.setTempData(TEMP_CUSTOM_LEVEL_STARTED, true);
                player.setTempData(TEMP_CUSTOM_LEVEL_LAST_FINISH_TIME, kernel.getServerTime());
            });
        });
    }

    public void OnPlayerOnLine(IKernel kernel, IGameObject player) {
        player.removeTempData(TEMP_CUSTOM_LEVEL_LAST_FINISH_TIME);
        player.removeTempData(TEMP_CUSTOM_LEVEL_STARTED);
    }

    public void OnPlayerOffLine(IKernel kernel, IGameObject player) {
        player.removeTempData(TEMP_CUSTOM_LEVEL_LAST_FINISH_TIME);
        player.removeTempData(TEMP_CUSTOM_LEVEL_STARTED);
    }

    /**
     * 接口8：结束挑战。必须先开始；未完成则地图 difficulty+1，并刷新最难榜（按 difficulty 分值排序）。
     */
    void OnReqCustomMapEnd(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
            throws InvalidProtocolBufferException {
        JSONObject req = parseReq(msg);
        if (req == null) {
            responseSimple(kernel, player, reqid, ServerCodeDef.CODE_PARAM_ERR);
            return;
        }
        long mapId = req.getLongValue(KEY_MAP_ID);
        if (mapId <= 0L) {
            responseSimple(kernel, player, reqid, ServerCodeDef.CODE_PARAM_ERR);
            return;
        }
        boolean completed = req.getBooleanValue(KEY_COMPLETED);
        int uid = player.getInt(PropertyKey.PLAYER_PROPERTY_UID);
        if (uid <= 0) {
            responseSimple(kernel, player, reqid, ServerCodeDef.CODE_PARAM_ERR);
            return;
        }
        Jedis jedis = kernel.getJedis();
        if (jedis == null) {
            responseSimple(kernel, player, reqid, ServerCodeDef.CODE_FAILED);
            return;
        }
        String playKey = playSessionKey(uid, mapId);
        Object delRes = jedis.eval(LUA_DEL_PLAY_IF_EXISTS, 1, playKey);
        if (StringUtils.luaLong(delRes) != 1L) {
            responseSimple(kernel, player, reqid, ServerCodeDef.CODE_WRONG_STATE);
            return;
        }

        if (completed) {
            List<Object> okParams = new ArrayList<>();
            okParams.add(mapId);
            kernel.executeSomeToStore(CustomMapRecordService.class, "recordPlaySuccess", okParams, okRes -> {
                CustomMapRecord updated = JsonUtil.decodeToObj(okRes, CustomMapRecord.class);
                if (updated == null || updated.getMapId() == null) {
                    responseSimple(kernel, player, reqid, ServerCodeDef.CODE_NOT_EXIST);
                    return;
                }
                cacheHotMap(kernel, updated);
                cacheActiveMap(updated);
                JSONResult resp = new JSONResult();
                resp.setDesc(ServerCodeDef.CODE_SUCCESS);
                resp.put(KEY_MAP_ID, mapId);
                resp.put(KEY_COMPLETED, true);
                resp.put("successCount", countLong(updated.getSuccessCount()));
                kernel.response(player, reqid, CustomMsg.String.newBuilder().setValue(resp.toJSONString()).build().toByteArray());
            });
            player.setTempData(TEMP_CUSTOM_LEVEL_LAST_FINISH_TIME, kernel.getServerTime());
            player.removeTempData(TEMP_CUSTOM_LEVEL_STARTED);
            return;
        }

        List<Object> bumpParams = new ArrayList<>();
        bumpParams.add(mapId);
        kernel.executeSomeToStore(CustomMapRecordService.class, "recordPlayFail", bumpParams, bumpRes -> {
            CustomMapRecord updated = JsonUtil.decodeToObj(bumpRes, CustomMapRecord.class);
            if (updated == null || updated.getMapId() == null) {
                responseSimple(kernel, player, reqid, ServerCodeDef.CODE_NOT_EXIST);
                return;
            }
            cacheHotMap(kernel, updated);
            cacheActiveMap(updated);
            Jedis j = kernel.getJedis();
            if (j != null) {
                publishHardRankByDifficulty(j, mapId, difficultyOf(updated));
            }
            JSONResult resp = new JSONResult();
            resp.setDesc(ServerCodeDef.CODE_SUCCESS);
            resp.put(KEY_MAP_ID, mapId);
            resp.put(KEY_COMPLETED, false);
            resp.put("difficulty", difficultyOf(updated));
            resp.put("failCount", countLong(updated.getFailCount()));
            kernel.response(player, reqid, CustomMsg.String.newBuilder().setValue(resp.toJSONString()).build().toByteArray());
            player.setTempData(TEMP_CUSTOM_LEVEL_LAST_FINISH_TIME, kernel.getServerTime());
            player.removeTempData(TEMP_CUSTOM_LEVEL_STARTED);
        });
    }

    private JSONObject loadMapForRead(IKernel kernel, long mapId, boolean addHeat, int viewerUid) {
        clearExpiredActiveCacheIfNeeded();

        // 1) 内存活跃缓存
        ActiveMapCache active = m_activeMaps.get(mapId);
        if (active != null && active.expireAt > System.currentTimeMillis()) {
            if (addHeat) {
                touchHeat(kernel, active.record, viewerUid);
            }
            return toJson(active.record, true);
        }

        // 2) Redis热地图
        Jedis jedis = kernel.getJedis();
        if (jedis != null) {
            String hotKey = REDIS_HOT_MAP_PREFIX + mapId;
            if (jedis.exists(hotKey)) {
                CustomMapRecord fromRedis = new CustomMapRecord();
                fromRedis.setMapId(mapId);
                fromRedis.setOwnerUid(StringUtils.parseIntSafe(jedis.hget(hotKey, "ownerUid")));
                fromRedis.setMapName(StringUtils.parseStringSafe(jedis.hget(hotKey, KEY_MAP_NAME)));
                fromRedis.setMapData(StringUtils.parseStringSafe(jedis.hget(hotKey, KEY_MAP_DATA)));
                fromRedis.setWidth(StringUtils.parseIntSafe(jedis.hget(hotKey, "width")));
                fromRedis.setHeight(StringUtils.parseIntSafe(jedis.hget(hotKey, "height")));
                fromRedis.setHeat(StringUtils.parseLongSafe(jedis.hget(hotKey, "heat")));
                fromRedis.setDifficulty(StringUtils.parseIntSafe(jedis.hget(hotKey, "difficulty")));
                fromRedis.setPlayCount(StringUtils.parseLongSafe(jedis.hget(hotKey, "playCount")));
                fromRedis.setSuccessCount(StringUtils.parseLongSafe(jedis.hget(hotKey, "successCount")));
                fromRedis.setFailCount(StringUtils.parseLongSafe(jedis.hget(hotKey, "failCount")));
                fromRedis.setLikeCount(StringUtils.parseLongSafe(jedis.hget(hotKey, "likeCount")));
                cacheActiveMap(fromRedis);
                if (addHeat) {
                    touchHeat(kernel, fromRedis, viewerUid);
                }
                return toJson(fromRedis, true);
            }
        }

        // 3) MySQL 回源在 OnReqCustomMapGetById 中异步处理
        return null;
    }

    private void touchHeat(IKernel kernel, CustomMapRecord one, int viewerUid) {
        if (one == null || one.getMapId() == null || one.getOwnerUid() == null) {
            return;
        }
        if (viewerUid <= 0 || viewerUid == one.getOwnerUid()) {
            return;
        }
        long newHeat = (one.getHeat() == null ? 0L : one.getHeat()) + 1L;
        one.setHeat(newHeat);
        cacheActiveMap(one);

        Jedis jedis = kernel.getJedis();
        if (jedis != null) {
            String hk = REDIS_HOT_MAP_PREFIX + one.getMapId();
            jedis.hset(hk, "heat", String.valueOf(newHeat));
            jedis.hset(hk, "difficulty", String.valueOf(difficultyOf(one)));
            jedis.hset(hk, "playCount", String.valueOf(countLong(one.getPlayCount())));
            jedis.hset(hk, "successCount", String.valueOf(countLong(one.getSuccessCount())));
            jedis.hset(hk, "failCount", String.valueOf(countLong(one.getFailCount())));
            jedis.hset(hk, "likeCount", String.valueOf(countLong(one.getLikeCount())));
            jedis.expire(hk, REDIS_HOT_TTL_SECONDS);
            // 兼容：1天热榜（历史实现为累计 heat + TTL）
            jedis.zadd(REDIS_HOT_RANK, newHeat, String.valueOf(one.getMapId()));
            jedis.expire(REDIS_HOT_RANK, REDIS_HOT_TTL_SECONDS);

            // 新增：自然周/自然月热榜（按本周/本月浏览增量计分）
            zincrRank(jedis, rankKeyWeek(METRIC_HOT), one.getMapId(), 1D, REDIS_WEEK_RANK_TTL_SECONDS);
            zincrRank(jedis, rankKeyMonth(METRIC_HOT), one.getMapId(), 1D, REDIS_MONTH_RANK_TTL_SECONDS);
        }

        // 异步落库，不阻塞主流程
        List<Object> params = new ArrayList<>();
        params.add(one.getMapId());
        params.add(1L);
        kernel.executeSomeToStore(CustomMapRecordService.class, "addHeat", params, null);
    }

    private void cacheHotMap(IKernel kernel, CustomMapRecord one) {
        if (one == null || one.getMapId() == null) {
            return;
        }
        Jedis jedis = kernel.getJedis();
        if (jedis == null) {
            return;
        }
        String key = REDIS_HOT_MAP_PREFIX + one.getMapId();
        jedis.hset(key, "mapId", String.valueOf(one.getMapId()));
        jedis.hset(key, "ownerUid", String.valueOf(one.getOwnerUid() == null ? 0 : one.getOwnerUid()));
        jedis.hset(key, KEY_MAP_NAME, StringUtils.parseStringSafe(one.getMapName()));
        jedis.hset(key, KEY_MAP_DATA, StringUtils.parseStringSafe(one.getMapData()));
        jedis.hset(key, "width", String.valueOf(one.getWidth() == null ? 0 : one.getWidth()));
        jedis.hset(key, "height", String.valueOf(one.getHeight() == null ? 0 : one.getHeight()));
        jedis.hset(key, "heat", String.valueOf(one.getHeat() == null ? 0L : one.getHeat()));
        jedis.hset(key, "difficulty", String.valueOf(difficultyOf(one)));
        jedis.hset(key, "playCount", String.valueOf(countLong(one.getPlayCount())));
        jedis.hset(key, "successCount", String.valueOf(countLong(one.getSuccessCount())));
        jedis.hset(key, "failCount", String.valueOf(countLong(one.getFailCount())));
        jedis.hset(key, "likeCount", String.valueOf(countLong(one.getLikeCount())));
        jedis.expire(key, REDIS_HOT_TTL_SECONDS);
        jedis.zadd(REDIS_HOT_RANK, one.getHeat() == null ? 0D : one.getHeat(), String.valueOf(one.getMapId()));
        jedis.expire(REDIS_HOT_RANK, REDIS_HOT_TTL_SECONDS);
        publishHardRankByDifficulty(jedis, one.getMapId(), difficultyOf(one));
    }

    private void removeHotMap(IKernel kernel, long mapId) {
        Jedis jedis = kernel.getJedis();
        if (jedis == null) {
            return;
        }
        jedis.del(REDIS_HOT_MAP_PREFIX + mapId);
        jedis.zrem(REDIS_HOT_RANK, String.valueOf(mapId));
        // 周/月榜：只移除“当前分桶”的成员；历史分桶会随 TTL 自然过期
        jedis.zrem(rankKeyWeek(METRIC_HOT), String.valueOf(mapId));
        jedis.zrem(rankKeyMonth(METRIC_HOT), String.valueOf(mapId));
        jedis.zrem(rankKeyWeek(METRIC_LIKE), String.valueOf(mapId));
        jedis.zrem(rankKeyMonth(METRIC_LIKE), String.valueOf(mapId));
        jedis.zrem(rankKeyWeek(METRIC_HARD), String.valueOf(mapId));
        jedis.zrem(rankKeyMonth(METRIC_HARD), String.valueOf(mapId));
    }

    /**
     * 供后续扩展：点赞计分（周/月榜）
     */
    private void touchLike(IKernel kernel, long mapId, double delta) {
        if (mapId <= 0 || delta == 0) {
            return;
        }
        Jedis jedis = kernel.getJedis();
        if (jedis == null) {
            return;
        }
        zincrRank(jedis, rankKeyWeek(METRIC_LIKE), mapId, delta, REDIS_WEEK_RANK_TTL_SECONDS);
        zincrRank(jedis, rankKeyMonth(METRIC_LIKE), mapId, delta, REDIS_MONTH_RANK_TTL_SECONDS);
    }

    /**
     * 最难榜：按地图当前 difficulty 作为分值（zrevrange 分数高在前）
     */
    private static void publishHardRankByDifficulty(Jedis jedis, long mapId, int difficulty) {
        if (jedis == null || mapId <= 0L) {
            return;
        }
        double score = Math.max(0, difficulty);
        String id = String.valueOf(mapId);
        String wk = rankKeyWeek(METRIC_HARD);
        String mk = rankKeyMonth(METRIC_HARD);
        jedis.zadd(wk, score, id);
        jedis.expire(wk, (int) REDIS_WEEK_RANK_TTL_SECONDS);
        jedis.zadd(mk, score, id);
        jedis.expire(mk, (int) REDIS_MONTH_RANK_TTL_SECONDS);
    }

    private static int difficultyOf(CustomMapRecord one) {
        if (one == null || one.getDifficulty() == null) {
            return 0;
        }
        return Math.max(0, one.getDifficulty());
    }

    private static long countLong(Long v) {
        if (v == null) {
            return 0L;
        }
        return v < 0L ? 0L : v;
    }

    private static void zincrRank(Jedis jedis, String rankKey, long mapId, double delta, long ttlSeconds) {
        if (jedis == null || rankKey == null || rankKey.isEmpty()) {
            return;
        }
        jedis.zincrby(rankKey, delta, String.valueOf(mapId));
        jedis.expire(rankKey, (int) ttlSeconds);
    }

    private static String resolveRankKey(String type) {
        if (type == null) {
            return null;
        }
        switch (type) {
            case "hot_day":
                return REDIS_HOT_RANK;
            case "hot_week":
                return rankKeyWeek(METRIC_HOT);
            case "hot_month":
                return rankKeyMonth(METRIC_HOT);
            case "like_week":
                return rankKeyWeek(METRIC_LIKE);
            case "like_month":
                return rankKeyMonth(METRIC_LIKE);
            case "hard_week":
                return rankKeyWeek(METRIC_HARD);
            case "hard_month":
                return rankKeyMonth(METRIC_HARD);
            default:
                return null;
        }
    }

    private static String rankKeyWeek(String metric) {
        LocalDate d = LocalDate.now(ZoneId.systemDefault());
        WeekFields wf = WeekFields.ISO;
        int week = d.get(wf.weekOfWeekBasedYear());
        int year = d.get(wf.weekBasedYear());
        String suffix = String.format("%dW%02d", year, week);
        return REDIS_RANK_PREFIX + metric + "::W::" + suffix;
    }

    private static String rankKeyMonth(String metric) {
        LocalDate d = LocalDate.now(ZoneId.systemDefault());
        String suffix = MONTH_FMT.format(d);
        return REDIS_RANK_PREFIX + metric + "::M::" + suffix;
    }

    private void cacheActiveMap(CustomMapRecord one) {
        if (one == null || one.getMapId() == null) {
            return;
        }
        if (m_activeMaps.size() >= ACTIVE_CACHE_MAX) {
            clearExpiredActiveCacheIfNeeded();
            if (m_activeMaps.size() >= ACTIVE_CACHE_MAX) {
                Long first = m_activeMaps.keySet().stream().findFirst().orElse(null);
                if (first != null) {
                    m_activeMaps.remove(first);
                }
            }
        }
        ActiveMapCache cache = new ActiveMapCache();
        cache.record = one;
        cache.expireAt = System.currentTimeMillis() + ACTIVE_CACHE_TTL_MS;
        m_activeMaps.put(one.getMapId(), cache);
    }

    private void clearExpiredActiveCacheIfNeeded() {
        if (m_activeMaps.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        List<Long> removeKeys = new ArrayList<>();
        for (Map.Entry<Long, ActiveMapCache> entry : m_activeMaps.entrySet()) {
            if (entry.getValue() == null || entry.getValue().expireAt <= now) {
                removeKeys.add(entry.getKey());
            }
        }
        for (Long key : removeKeys) {
            m_activeMaps.remove(key);
        }
    }

    private JSONObject parseReq(byte[] msg) throws InvalidProtocolBufferException {
        if (msg == null || msg.length == 0) {
            return null;
        }
        String jsonStr = CustomMsg.String.parseFrom(msg).getValue();
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return null;
        }
        return JSON.parseObject(jsonStr);
    }

    private JSONObject parseReqAllowEmpty(byte[] msg) throws InvalidProtocolBufferException {
        if (msg == null || msg.length == 0) {
            return new JSONObject();
        }
        String jsonStr = CustomMsg.String.parseFrom(msg).getValue();
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return new JSONObject();
        }
        JSONObject req = JSON.parseObject(jsonStr);
        return req == null ? new JSONObject() : req;
    }

    private void responseSimple(IKernel kernel, IGameObject player, int reqid, ServerCodeDef code) {
        JSONResult resp = new JSONResult();
        resp.setDesc(code);
        kernel.response(player, reqid, CustomMsg.String.newBuilder().setValue(resp.toJSONString()).build().toByteArray());
    }

    private JSONObject toJson(CustomMapRecord one, boolean withData) {
        JSONObject obj = new JSONObject();
        obj.put(KEY_MAP_ID, one.getMapId());
        obj.put("userId", one.getOwnerUid() == null ? 0 : one.getOwnerUid());
        obj.put(KEY_MAP_NAME, one.getMapName());
        //obj.put("heat", one.getHeat() == null ? 0L : one.getHeat());
        obj.put("playCount", countLong(one.getPlayCount()));
        obj.put("winCount", countLong(one.getSuccessCount()));
        obj.put("failCount", countLong(one.getFailCount()));
        obj.put("likeCount", countLong(one.getLikeCount()));
        if (withData) {
            obj.put(KEY_MAP_DATA, StringUtils.parseStringSafe(one.getMapData()));
        }
        return obj;
    }

    private static String likeOnceKey(int uid, long mapId) {
        String day = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.BASIC_ISO_DATE);
        return REDIS_LIKE_ONCE_PREFIX + uid + "::" + mapId + "::" + day;
    }

    private static String playSessionKey(int uid, long mapId) {
        return REDIS_PLAY_PREFIX + uid + "::" + mapId;
    }

    private static int secondsUntilEndOfDay() {
        ZoneId z = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(z);
        ZonedDateTime next = now.toLocalDate().plusDays(1).atStartOfDay(z);
        int sec = (int) Duration.between(now.toInstant(), next.toInstant()).getSeconds();
        return Math.max(1, sec);
    }


    private static MapShape validateAndParseMap(String mapData) {
        MapShape res = new MapShape();
        if (mapData == null || mapData.trim().isEmpty()) {
            return res;
        }
        // 约定：mapData 内的“每一行”使用逗号分隔
        String[] lines = mapData.split(",");
        if (lines.length <= 0 || lines.length > MAX_HEIGHT) {
            return res;
        }
        int width = -1;
        List<String> validRows = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String rawRow = lines[i];
            if (rawRow == null) {
                return res;
            }
            String row = rawRow.trim();
            if (row.isEmpty()) {
                return res;
            }
            boolean lastRow = (i == lines.length - 1);
            String logicalRow = logicalMapRowForWidth(row, lastRow);
            if (logicalRow.isEmpty()) {
                return res;
            }
            if (!logicalRow.matches(CELL_REGEX)) {
                return res;
            }
            int rowLen = logicalRow.length();
            if (rowLen < MIN_WIDTH || rowLen > MAX_WIDTH) {
                return res;
            }
            if (width < 0) {
                width = rowLen;
            } else if (rowLen != width) {
                return res;
            }
            validRows.add(row);
        }
        if (width <= 0) {
            return res;
        }
        res.ok = true;
        res.width = width;
        res.height = validRows.size();
        res.normalizedData = mapData;
        return res;
    }

    /**
     * 计算用于宽度校验的“地图格串”：仅最后一行可带 {@code :spawnX:spawnY}，其它行必须整行都是格字符。
     */
    private static String logicalMapRowForWidth(String row, boolean lastRow) {
        if (!lastRow) {
            return row;
        }
        Matcher m = LAST_ROW_SPAWN_SUFFIX.matcher(row);
        return m.matches() ? m.group(1) : row;
    }


    static class MapShape {
        boolean ok = false;
        int width = 0;
        int height = 0;
        String normalizedData = "";
    }

    static class ActiveMapCache {
        CustomMapRecord record;
        long expireAt;
    }
}
