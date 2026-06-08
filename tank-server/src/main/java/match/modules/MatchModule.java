package match.modules;

import back.modules.data.room.CustomGameDTO;
import back.modules.dataenum.RoomType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.InvalidProtocolBufferException;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.ByteUtils;
import framework.JsonUtil;
import framework.game.ClassSet;
import framework.game.ICfgReader;
import framework.match.IMatchModule;
import framework.match.MatchKernel;
import framework.mybatis.domain.Config;
import framework.mybatis.domain.CustomGame;
import framework.mybatis.domain.PlayRobot;
import framework.mybatis.service.impl.ConfigService;
import framework.mybatis.service.impl.CustomGameService;
import framework.mybatis.service.impl.PlayRobotService;
import game.modules.RoomModule;
import lombok.ToString;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MatchModule implements IMatchModule {
    enum SeatState {
        FREE, ORDER, HAVE,
    }

    enum RobotState {
        FREE, INUSE,
    }

    class SeatData {
        SeatState state;
        long playerid;
        long orderTime;

        int uid;
        int level;
        int head;
        int vip;
        String name;

        boolean isEmpty(long time) {
            if (state == SeatState.FREE) {
                return true;
            }
            if (state == SeatState.ORDER) {
                return time - orderTime >= ORDER_MAX_TIME;
            }
            return false;
        }
    }

    class  DeskData {
        long objid;
        int gameid = -1; // 竞技场 游戏id
        int deskid = -1; // 至尊选座 桌子id
        int seatCount;
        int minBv;
        int maxBv;
        SeatData[] seats;
        int deskType;
    }

    class RoomData {
        int type;
        Map<Long, DeskData> mapDesks = new HashMap<>();
    }

    class WaitEnterData {
        int type;
        long playerid;
        int reqid;
        int gameid;
        int bulletVal;
        int deskType;
    }

    class WaitCustomDesk {
        long playerid;
        int reqid;
        int deskid;
        int seatid;
    }

    static class WaitMysteryLegendDesk {
        Long playerId;
        Integer reqId;
        Integer roomId;
    }


    LinkedList<WaitCustomDesk> m_waitCustom = new LinkedList<>();

    class RobotBagInfo {
        public int item_skill_frozen;
        public int item_skill_lock;
        public int item_skill_speed;
    }

    class RobotData {
        boolean open;

        int uid;
        int bulletLevel;
        int level;
        int vipLevel;
        int position;
        int state;

        long deskid;

        long diamond;
        long gold;

        String name;

        Map<String, Integer> mapItems;
    }

    @ToString
    class CustomGameData {
        int id;
        int type; // 0普通桌，1密码桌
        int roomType; // 9神秘河流, 10贵族海域, 11地心深渊
        int minBv;
        int maxBv;
        int level;
        int autoKick;
        long totalPlay;
        long totalWin;
        boolean bSwitch;
        String enterLimit; // 进入限制

        int online = 0;
        long deskid; // 实体桌id，可根究mapDeskById获取桌子数据
        int creator; // 创建者uid
        long endTime; // 结束时间
        String passwd; // 进入密码

    }

    Map<Integer, CustomGameData> m_mapCustomGameData = new HashMap<>();

    Map<Integer, RobotData> m_mapRobotByUid = new HashMap<>();
    Map<Integer, List<RobotData>> m_mapRobotByRoom = new HashMap<>();

    static final int ORDER_MAX_TIME = 10000; // 座位预定保留时间

    List<WaitEnterData> m_waitEnter = new ArrayList<>();
    Map<Integer, RoomData> mapRooms = new HashMap<>();
    Map<String, Integer> mapGameSer = new HashMap<>();
    Map<Long, DeskData> mapDeskById = new HashMap<>();

    Random m_rand = new Random();
    // pw cfg
    String m_pwCfg;
    int m_maxCount;
    int m_minBv;
    int m_maxBv;
    int m_level;
    int m_deskbg = -1;
    String m_limit;
    long m_lifetime = -1;
    Map<String, Integer> m_mapLimit = new HashMap<>();
    Set<Integer> m_setSupreme = new HashSet<>();
    private static Logger logger = LoggerFactory.getLogger(MatchModule.class);

    class DragonBv {
        int id;
        int min;
        int max;
    }

    Map<Integer, DragonBv> m_mapDragonBvCfg = new HashMap<>();

    /**
     * 秘境传说活动专属房间类型
     */
    private final Set<Integer> mysteryLegendRoomSet = new HashSet<>();

    private final List<WaitMysteryLegendDesk> waitMysteryLegendDesks = new ArrayList<>();


    @Override
    public boolean onInit(MatchKernel kernel) {
        kernel.regServerMsg(ServerMsgDef.MMSG_ADD_DESK.ordinal(), this, "OnAddDesk");
        kernel.regServerMsg(ServerMsgDef.MMSG_DEL_DESK.ordinal(), this, "OnDelDesk");
        kernel.regServerMsg(ServerMsgDef.MMSG_SIT_DOWN.ordinal(), this, "OnSitDown");
        kernel.regServerMsg(ServerMsgDef.MMSG_STAND_UP.ordinal(), this, "OnStandUp");
        kernel.regServerMsg(ServerMsgDef.B2M_UPDATE_ROBOT.ordinal(), this, "OnUpdateRobot");
        kernel.regServerMsg(ServerMsgDef.G2M_ROBOT_UPDATE.ordinal(), this, "OnSaveRobot");

        kernel.regServerMsg(ServerMsgDef.G2M_UPDATE_BG.ordinal(), this, "OnUpdateBg");
        kernel.regServerMsg(ServerMsgDef.G2M_UPDATE_PLAY_WIN.ordinal(), this, "OnUpdatePlayWin");
        kernel.regServerMsg(ServerMsgDef.G2M_DELETE_MYSTERY_LEGEND_ROOM_CONFIG.ordinal(), this, "OnDeleteMysteryLegendRoomConfig");

//		kernel.RegServerMsg(ServerMsgDef.B2M_UPDATE_GAME.ordinal(), this, "OnUpdateCustomGame");
//		kernel.RegServerMsg(ServerMsgDef.B2M_CLOSE_GAME.ordinal(), this, "OnCloseCustomGame");
        kernel.regServerMsg(ServerMsgDef.B2M_UPDATE_CFG.ordinal(), this, "OnUpdateCfg");

        kernel.regServerRequest(ServerMsgDef.MMSG_ALLOC_ROBOT.ordinal(), this, "OnAllocRobot");
        kernel.regServerRequest(ServerMsgDef.MMSG_FREE_ROBOT.ordinal(), this, "OnFreeRobot");

        kernel.regServerRequest(ServerMsgDef.G2M_REQ_DESK_LIST.ordinal(), this, "OnReqDeskList");
        kernel.regServerRequest(ServerMsgDef.G2M_REQ_SIT_DOWN.ordinal(), this, "OnReqSitDown");
        kernel.regServerRequest(ServerMsgDef.G2M_CREATE_PW.ordinal(), this, "OnReqCreatePw");
        kernel.regServerRequest(ServerMsgDef.G2M_ENTER_PW.ordinal(), this, "OnReqEnterPw");
        kernel.regServerRequest(ServerMsgDef.G2M_GET_CUSTOM_PW_DESK_DATA.ordinal(), this, "OnReqGetDeskData");
        kernel.regServerRequest(ServerMsgDef.B2M_ADD_CUSTOM_GAME.ordinal(), this, "OnAddCustomGame");
        kernel.regServerRequest(ServerMsgDef.B2M_UPDATE_CUSTOM_GAME.ordinal(), this, "OnUpdateCustomGame");
        kernel.regServerRequest(ServerMsgDef.B2M_CLOSE_CUSTOM_GAME.ordinal(), this, "OnCloseCustomGame");
        kernel.regServerRequest(ServerMsgDef.B2M_OPEN_CUSTOM_GAME.ordinal(), this, "OnOpenCustomGame");
        kernel.regServerRequest(ServerMsgDef.G2M_REQ_MYSTERY_LEGEND_DESK_OBJ_ID.ordinal(), this, "OnReqMysteryLegendDeskObjId");

        kernel.regServerRequest(ServerMsgDef.MMSG_MATCH.ordinal(), this, "OnMatch");

        RefreshCfg(kernel, "res/Game/FireDragon.xml");

        m_deskbg = m_rand.nextInt(4);
        m_setSupreme.add(RoomType.SUPER.getId()); // 至尊选座
        for (int i = RoomType.ROOM_SUPREME_1.getId(); i <= RoomType.ROOM_ANCIENT_RELICS.getId(); i++) {
            m_setSupreme.add(i);
        }
        m_setSupreme.add(RoomType.ROOM_PERSONAL.getId());//单人模式
        m_setSupreme.add(RoomType.ROOM_ANCIENT_RELICS.getId());//上古遗迹
        mysteryLegendRoomSet.add(RoomType.ROOM_MYSTERY_LEGEND.getId()); //金币场深海秘境
        mysteryLegendRoomSet.add(RoomType.ROOM_N_MYSTERY_LEGEND.getId()); //海神殿深海秘境
        return true;
    }

    @Override
    public void onNetReady(MatchKernel kernel) {
        RefreshPwCfg(kernel, null);
        kernel.executeSomeToStore(PlayRobotService.class, "loadAll", null, (str) -> {
            List<PlayRobot> robots = framework.JsonUtil.decodeToList(str, PlayRobot.class);
            for (int i = 0; i < robots.size(); i++) {
                OnLoadRobot(kernel, robots.get(i));
            }
        });
        kernel.executeSomeToStore(CustomGameService.class, "loadAll", null, (str) -> {
            List<CustomGame> games = framework.JsonUtil.decodeToList(str, CustomGame.class);
            for (int i = 0; i < games.size(); i++) {
                OnLoadCustomGame(kernel, games.get(i));
            }
        });
        kernel.executeSomeToStore(CustomGameService.class, "updateOnline", null, null);
    }

    @Override
    public void onDestroy() {

    }

    void RefreshCfg(MatchKernel kernel, String path) {
        if (path.equals("res/Game/FireDragon.xml")) {
            ICfgReader cfg = kernel.loadXmlConfig(path);
            if (cfg == null) {
                return;
            }
            int count = cfg.getItemCount();
            for (int i = 0; i < count; ++i) {
                DragonBv bv = new DragonBv();
                bv.id = cfg.getInt(i, "Id");
                bv.min = cfg.getInt(i, "Min");
                bv.max = cfg.getInt(i, "Max");
                m_mapDragonBvCfg.put(bv.id, bv);
            }
        }
    }

    void RefreshPwCfg(MatchKernel kernel, Consumer<Boolean> cb) {
        List<Object> params = new ArrayList<Object>();
        params.add("PwDeskCfg");
        kernel.executeSomeToStore(ConfigService.class, "queryById", params, (str) -> {
            if (str == null) {
                if (cb != null) {
                    cb.accept(false);
                }
                return;
            }
            Config config = framework.JsonUtil.decodeToObj(str, Config.class);
            if (!StringUtils.isEmpty(config.getValue())) {
                m_pwCfg = config.getValue();
                try {
                    JsonParser parse = new JsonParser();
                    JsonObject json = (JsonObject) parse.parse(m_pwCfg);
                    m_maxCount = json.get("count").getAsInt();
                    m_minBv = json.get("minBv").getAsInt();
                    m_maxBv = json.get("maxBv").getAsInt();
                    m_level = json.get("level").getAsInt();
                    m_limit = json.get("limit").getAsJsonObject().toString();
                    m_lifetime = json.get("recycle").getAsLong();
                } catch (Exception exp) {
                    exp.printStackTrace();
                }
                if (m_lifetime > 0) {
                    m_lifetime = m_lifetime * 3600000L;
                }
                if (cb != null) {
                    cb.accept(true);
                }
            } else {
                logger.error("The config of PwDeskCfg is empty");
                if (cb != null) {
                    cb.accept(false);
                }
            }
        });
    }

    void OnLoadRobot(MatchKernel kernel, PlayRobot robot) {
        RobotData data = new RobotData();
        data.open = robot.getOpen() == 1;

        data.uid = robot.getId();
        data.level = robot.getLevel();
        data.bulletLevel = robot.getBulletLevel();
        data.vipLevel = robot.getVipLevel();
        data.position = robot.getPosition();

        data.diamond = robot.getDiamond();
        data.gold = robot.getGold();

        data.name = robot.getName();

        data.mapItems = new HashMap<>();

        String battery = robot.getBattery();
        if (!battery.isEmpty()) {
            data.mapItems.put(battery, 1);
        }
        String title = robot.getTitle();
        if (!title.isEmpty()) {
            data.mapItems.put(title, 1);
        }
        String bag = robot.getBag();
        if (!bag.isEmpty()) {
            RobotBagInfo bagInfo = new Gson().fromJson(bag, RobotBagInfo.class);
            if (bagInfo.item_skill_frozen > 0) {
                data.mapItems.put("item_skill_frozen", bagInfo.item_skill_frozen);
            }
            if (bagInfo.item_skill_lock > 0) {
                data.mapItems.put("item_skill_lock", bagInfo.item_skill_lock);
            }
            if (bagInfo.item_skill_speed > 0) {
                data.mapItems.put("item_skill_speed", bagInfo.item_skill_speed);
            }
        }
        m_mapRobotByUid.put(data.uid, data);
        if (m_mapRobotByRoom.containsKey(data.position)) {
            m_mapRobotByRoom.get(data.position).add(data);
        } else {
            List<RobotData> list = new ArrayList<>();
            list.add(data);
            m_mapRobotByRoom.put(data.position, list);
        }
    }

    void OnUpdateBg(MatchKernel kernel, int serid, int msgid, byte[] data) {
        int bg = m_rand.nextInt(3);
        if (bg >= m_deskbg) {
            ++bg;
        }
        m_deskbg = bg;

        ServerMsg.IntSingle.Builder build = ServerMsg.IntSingle.newBuilder();
        build.setIntMember(m_deskbg);
        kernel.broadToServer("game", ServerMsgDef.M2G_UPDATE_BG.ordinal(), build.build().toByteArray());
    }

    void OnSaveRobot(MatchKernel kernel, int serid, int msgid, byte[] data)
            throws InvalidProtocolBufferException, SQLException {
        ServerMsg.UpdateRobot2Match msg = ServerMsg.UpdateRobot2Match.parseFrom(data);
        int uid = msg.getUid();
        int level = msg.getLevel();
        long diamond = msg.getDiamond();
        long gold = msg.getGold();
        RobotBagInfo bagInfo = new RobotBagInfo();
        for (int i = 0; i < msg.getItemCount(); ++i) {
            if (msg.getItem(i).equals("item_skill_lock")) {
                bagInfo.item_skill_lock = msg.getCount(i);
            } else if (msg.getItem(i).equals("item_skill_frozen")) {
                bagInfo.item_skill_frozen = msg.getCount(i);
            } else if (msg.getItem(i).equals("item_skill_speed")) {
                bagInfo.item_skill_speed = msg.getCount(i);
            }
        }
        String bag = new Gson().toJson(bagInfo);
        kernel.updateRobot(uid, level, diamond, gold, bag);
    }

    void OnUpdateRobot(MatchKernel kernel, int serid, int msgid, byte[] data)
            throws InvalidProtocolBufferException, SQLException {
        ServerMsg.UpdateRobot msg = ServerMsg.UpdateRobot.parseFrom(data);
        int uid = msg.getUid();
        List<Object> params = new ArrayList<>();
        params.add(uid);
        kernel.executeSomeToStore(PlayRobotService.class, "queryById", params, (str) -> {
            if (str == null) {
                if (m_mapRobotByUid.containsKey(uid)) {
                    RobotData robot = m_mapRobotByUid.get(uid);
                    if (robot.state == 1) {
                        RobotStandup(kernel, robot);
                    }
                    robot.open = false;
                    int pos = robot.position;
                    if (m_mapRobotByRoom.containsKey(pos)) {
                        List<RobotData> list = m_mapRobotByRoom.get(pos);
                        list.remove(robot);
                    }
                    m_mapRobotByUid.remove(uid);
                }
                return;
            }
            PlayRobot _robot = framework.JsonUtil.decodeToObj(str, PlayRobot.class);
            if (m_mapRobotByUid.containsKey(uid)) {
                RobotData robot = m_mapRobotByUid.get(uid);
                int oldPos = robot.position;
                robot.open = _robot.getOpen() == 1;
                robot.uid = _robot.getId();
                robot.level = _robot.getLevel();
                robot.bulletLevel = _robot.getBulletLevel();
                robot.vipLevel = _robot.getVipLevel();
                robot.position = _robot.getPosition();
                robot.diamond = _robot.getDiamond();
                robot.gold = _robot.getGold();
                robot.name = _robot.getName();
                robot.mapItems.clear();
                String battery = _robot.getBattery();
                if (!battery.isEmpty()) {
                    robot.mapItems.put(battery, 1);
                }
                String title = _robot.getTitle();
                if (!title.isEmpty()) {
                    robot.mapItems.put(title, 1);
                }
                String bag = _robot.getBag();
                if (!bag.isEmpty()) {
                    RobotBagInfo bagInfo = new Gson().fromJson(bag, RobotBagInfo.class);
                    if (bagInfo.item_skill_frozen > 0) {
                        robot.mapItems.put("item_skill_frozen", bagInfo.item_skill_frozen);
                    }
                    if (bagInfo.item_skill_lock > 0) {
                        robot.mapItems.put("item_skill_lock", bagInfo.item_skill_lock);
                    }
                    if (bagInfo.item_skill_speed > 0) {
                        robot.mapItems.put("item_skill_speed", bagInfo.item_skill_speed);
                    }
                }
                if (robot.state == 1) {
                    if (oldPos != robot.position || !robot.open) {
                        RobotStandup(kernel, robot);
                    } else {
                        UpdateRobot2Game(kernel, robot);
                    }
                }
                if (oldPos != robot.position) {
                    if (m_mapRobotByRoom.containsKey(oldPos)) {
                        m_mapRobotByRoom.get(oldPos).remove(robot);
                    }
                    if (m_mapRobotByRoom.containsKey(robot.position)) {
                        m_mapRobotByRoom.get(robot.position).add(robot);
                    } else {
                        List<RobotData> list = new ArrayList<>();
                        list.add(robot);
                        m_mapRobotByRoom.put(robot.position, list);
                    }
                }
            } else {
                OnLoadRobot(kernel, _robot);
            }
        });
    }

    // custom game
    void OnLoadCustomGame(MatchKernel kernel, CustomGame game) {
        CustomGameData data = new CustomGameData();
        data.id = game.getId();
        data.type = game.getType();
        data.roomType = game.getRoomType();
        data.minBv = game.getMinBv();
        data.maxBv = game.getMaxBv();
        data.level = game.getLevel();
        data.autoKick = game.getAutoKick();
        data.totalPlay = game.getTotalPlay();
        data.totalWin = game.getTotalWin();
        data.bSwitch = game.getStatus() == 1;
        data.enterLimit = game.getEnterLimit();
        data.passwd = game.getPasswd() == null ? "" : game.getPasswd();
        data.deskid = 0L;
        m_mapCustomGameData.put(data.id, data);
    }

    void OnAddCustomGame(MatchKernel kernel, int reqId, byte[] data) throws Exception {
        CustomGameDTO customGameDTO = ByteUtils.byteToObject(data);
        List<CustomGame> customGameList = new ArrayList<>();
        for (int i = 0; i < customGameDTO.getCount(); i++) {
            CustomGame customGame = new CustomGame();
            BeanUtils.copyProperties(customGameDTO, customGame);
            customGame.setCreateTime(new Date());
            customGameList.add(customGame);
        }
        List<Object> param = new ArrayList<>();
        param.add(customGameList);
        kernel.executeSomeToStore(CustomGameService.class, "saveList", param, res -> {
            if (res == null) {
                kernel.responseServer(reqId, "新增自定义桌失败".getBytes(StandardCharsets.UTF_8));
                return;
            }
            List<CustomGame> customGames = JsonUtil.decodeToList(res, CustomGame.class);
            for (CustomGame customGame : customGames) {
                if (!m_mapCustomGameData.containsKey(customGame.getId())) {
                    AddCustomGameData(kernel, customGame);
                }
            }
            kernel.responseServer(reqId, new byte[0]);
        });
    }

    private void AddCustomGameData(MatchKernel kernel, CustomGame customGame) {
        CustomGameData customGameData = m_mapCustomGameData.get(customGame.getId());
        boolean isNew = (customGameData == null);
        
        if (isNew) {
            customGameData = new CustomGameData();
            customGameData.id = customGame.getId();
            customGameData.deskid = 0L;
            customGameData.online = 0;
            customGameData.totalPlay = 0;
            customGameData.totalWin = 0;
        } else {
        }
        
        // 更新配置相关字段
        customGameData.type = customGame.getType();
        customGameData.roomType = customGame.getRoomType();
        customGameData.minBv = customGame.getMinBv();
        customGameData.maxBv = customGame.getMaxBv();
        customGameData.level = customGame.getLevel();
        customGameData.autoKick = customGame.getAutoKick();
        customGameData.passwd = ObjectUtils.isNotEmpty(customGame.getPasswd()) ? customGame.getPasswd() : "";
        customGameData.enterLimit = customGame.getEnterLimit();
        customGameData.bSwitch = customGame.getStatus() == 1;
        
        if (isNew) {
            customGameData.totalPlay = customGame.getTotalPlay() != null ? customGame.getTotalPlay() : 0;
            customGameData.totalWin = customGame.getTotalWin() != null ? customGame.getTotalWin() : 0;
        }
        
        m_mapCustomGameData.put(customGameData.id, customGameData);
        ServerMsg.DeskData.Builder deskData = ServerMsg.DeskData.newBuilder();
        deskData.setDeskid(customGameData.id);
        deskData.setMinbv(customGameData.minBv);
        deskData.setMaxbv(customGameData.maxBv);
        deskData.setLimit(customGameData.enterLimit);
        deskData.setRoomType(customGameData.roomType);
        deskData.setType(customGameData.type);
        kernel.broadToServer("game", ServerMsgDef.M2G_UPDATE_DESK.ordinal(), deskData.build().toByteArray());
    }

    void OnUpdateCustomGame(MatchKernel kernel, int reqId, byte[] data) throws Exception {
        CustomGameDTO customGameDTO = ByteUtils.byteToObject(data);
        CustomGameData customGameData = m_mapCustomGameData.get(customGameDTO.getId());
        if (customGameData == null) {
            logger.error("not found from game, id [{}]", customGameDTO.getId());
            kernel.responseServer(reqId, "更新失败".getBytes(StandardCharsets.UTF_8));
        } else {
            customGameData.minBv = customGameDTO.getMinBv();
            customGameData.maxBv = customGameDTO.getMaxBv();
            customGameData.autoKick = customGameDTO.getAutoKick();
            customGameData.bSwitch = customGameDTO.getStatus() == 1;
            customGameData.enterLimit = customGameDTO.getEnterLimit();

            if (customGameData.level != customGameDTO.getLevel()) {
                customGameData.totalPlay = 0;
                customGameData.totalWin = 0;
                customGameData.level = customGameDTO.getLevel();
            }
            if (customGameData.deskid != 0L && mapDeskById.containsKey(customGameData.deskid)) {
                ServerMsg.UpdateDesk.Builder deskMsg = ServerMsg.UpdateDesk.newBuilder();
                deskMsg.setDeskid(customGameData.deskid);
                deskMsg.setMinbv(customGameData.minBv);
                deskMsg.setMaxbv(customGameData.maxBv);
                deskMsg.setAutoKick(customGameData.autoKick);
                if (customGameData.level != customGameDTO.getLevel()) {
                    deskMsg.setLevel(customGameData.level);
                }
                int deskSer = ClassSet.getObjectSerID(customGameData.deskid);
                kernel.sendServerMsg(deskSer, ServerMsgDef.M2G_KICK_PLAYERS.ordinal(), deskMsg.build().toByteArray());
            }
            ServerMsg.DeskData.Builder deskData = ServerMsg.DeskData.newBuilder();
            deskData.setDeskid(customGameDTO.getId());
            deskData.setMinbv(customGameDTO.getMinBv());
            deskData.setMaxbv(customGameDTO.getMaxBv());
            deskData.setLimit(customGameDTO.getEnterLimit());
            deskData.setRoomType(customGameData.roomType);
            deskData.setType(customGameData.type);
            kernel.broadToServer("game", ServerMsgDef.M2G_UPDATE_DESK.ordinal(), deskData.build().toByteArray());
            kernel.responseServer(reqId, new byte[0]);
        }
    }

    void OnOpenCustomGame(MatchKernel kernel, int reqId, byte[] data) {
        int id = Integer.parseInt(new String(data));
        List<Object> list = new ArrayList<>();
        list.add(id);
        kernel.executeSomeToStore(CustomGameService.class, "queryById", list, res -> {
            CustomGame customGame = JsonUtil.decodeToObj(res, CustomGame.class);
            if (customGame != null) {
                AddCustomGameData(kernel, customGame);
                kernel.responseServer(reqId, new byte[0]);
            } else {
                logger.error("not found from game, id [{}]", id);
                kernel.responseServer(reqId, "开放失败".getBytes(StandardCharsets.UTF_8));
            }
        });

    }

//	void OnUpdateCustomGame(MatchKernel kernel, int serid, int msgid, byte[] data)
//			throws InvalidProtocolBufferException {
//		CustomMsg.String tmp = CustomMsg.String.parseFrom(data);
//		String jsonStr = tmp.getValue();
//		JsonObject json = JsonUtil.decodeToObj(jsonStr, JsonObject.class);
//		int id = json.get("id").getAsInt(); // 同时创建多个桌子时 首个桌子的id
//		int tableNum = json.get("tableNum").getAsInt();
//
//		List<Object> params = new ArrayList<>();
//		params.add(id);
//		kernel.ExecuteSomeToStore(CustomGameService.class, "queryById", params, (str) -> {
//			if (str == null) {
//				return;
//			}
//			CustomGame game = framework.JsonUtil.decodeToObj(str, CustomGame.class);
//			// 更新
//			if (m_mapCustomGameData.containsKey(id)) {
//				CustomGameData gameData = m_mapCustomGameData.get(id);
//				gameData.type = game.getType();
//				gameData.roomType = game.getRoomType();
//				gameData.minBv = game.getMinBv();
//				gameData.maxBv = game.getMaxBv();
//				gameData.autoKick = game.getAutoKick();
//				gameData.totalPlay = game.getTotalPlay();
//				gameData.totalWin = game.getTotalWin();
//				gameData.bSwitch = game.getStatus() == 1;
//				gameData.enterLimit = game.getEnterLimit();
//				gameData.passwd = game.getPasswd() == null ? "" : game.getPasswd();
//				boolean levelChanged = false;
//				int level = game.getLevel();
//				if (level != gameData.level) {
//					logger.info("level {}, gameData.level {}", level, gameData.level);
//					levelChanged = true;
//					gameData.level = level;
//					gameData.totalPlay = 0;
//					gameData.totalWin = 0;
//					kernel.UpdatePwGame(id, -1, gameData.type, gameData.roomType, gameData.level, gameData.minBv,
//							gameData.maxBv, gameData.online, gameData.totalPlay, gameData.totalWin, gameData.enterLimit, gameData.passwd);
//				}
//				if (gameData.deskid != 0L) {
//					if (mapDeskById.containsKey(gameData.deskid)) {
//						ServerMsg.UpdateDesk.Builder deskMsg = ServerMsg.UpdateDesk.newBuilder();
//						deskMsg.setDeskid(gameData.deskid);
//						deskMsg.setMinbv(gameData.minBv);
//						deskMsg.setMaxbv(gameData.maxBv);
//						deskMsg.setAutoKick(gameData.autoKick);
//						if (levelChanged) {
//							deskMsg.setLevel(gameData.level);
//						}
//						int deskSer = ClassSet.GetObjectSerID(gameData.deskid);
//						kernel.SendServerMsg(deskSer, ServerMsgDef.M2G_KICK_PLAYERS.ordinal(), deskMsg.build().toByteArray());
//					}
//				}
//			} else {
//				for (int j = 0; j < tableNum; j++) {
//					game.setId(id + j); // id是同时创建多个桌子时 首个桌子的id
//					OnLoadCustomGame(kernel, game);
//				}
//			}
//			for (int j = 0; j < tableNum; j++) {
//				int tmpId = id+j;
//				if (!m_mapCustomGameData.containsKey(tmpId)) {
//					return;
//				}
//				CustomGameData gameData = m_mapCustomGameData.get(tmpId);
//				if (gameData.type == 0) {
//					ServerMsg.DeskData.Builder deskData = ServerMsg.DeskData.newBuilder();
//					deskData.setDeskid(tmpId);
//					deskData.setMinbv(gameData.minBv);
//					deskData.setMaxbv(gameData.maxBv);
//					deskData.setLimit(gameData.enterLimit);
//					deskData.setRoomType(gameData.roomType);
//					kernel.BroadToServer("game", ServerMsgDef.M2G_UPDATE_DESK.ordinal(),
//							deskData.build().toByteArray());
//				}
//			}
//		});
//	}


    //	void OnCloseCustomGame(MatchKernel kernel, int serid, int msgid, byte[] data) throws InvalidProtocolBufferException, SQLException {
//		ServerMsg.IntSingle msg = ServerMsg.IntSingle.parseFrom(data);
//		int id = msg.getIntMember();
//		if (!m_mapCustomGameData.containsKey(id)) {
//			return;
//		}
//		CustomGameData gameData = m_mapCustomGameData.get(id);
//		if (gameData.type == 0) {
//			// delete desk from game
//			ServerMsg.IntSingle.Builder deskMsg = ServerMsg.IntSingle.newBuilder();
//			deskMsg.setIntMember(id);
//			kernel.BroadToServer("game", ServerMsgDef.M2G_REMOVE_CFG.ordinal(), deskMsg.build().toByteArray());
//		}
//		if (gameData.deskid != 0L) {
//			// kick all player and destroy the desk
//			if (mapDeskById.containsKey(gameData.deskid)) {
//				ServerMsg.Int64.Builder deskMsg = ServerMsg.Int64.newBuilder();
//				deskMsg.setValue(gameData.deskid);
//				int deskSer = ClassSet.GetObjectSerID(gameData.deskid);
//				kernel.SendServerMsg(deskSer, ServerMsgDef.M2G_REMOVE_DESK.ordinal(), deskMsg.build().toByteArray());
//			}
//		}
//		m_mapCustomGameData.remove(id);
//	}
    void OnCloseCustomGame(MatchKernel kernel, int reqId, byte[] data) {
        int id = Integer.parseInt(new String(data));
        CustomGameData customGameData = m_mapCustomGameData.get(id);
        if (customGameData != null) {
            if (customGameData.type == 0) {
                // delete desk from game
                ServerMsg.IntSingle.Builder deskMsg = ServerMsg.IntSingle.newBuilder();
                deskMsg.setIntMember(id);
                kernel.broadToServer("game", ServerMsgDef.M2G_REMOVE_CFG.ordinal(), deskMsg.build().toByteArray());
            }
            if (customGameData.deskid != 0L) {
                // kick all player and destroy the desk
                if (mapDeskById.containsKey(customGameData.deskid)) {
                    ServerMsg.Int64.Builder deskMsg = ServerMsg.Int64.newBuilder();
                    deskMsg.setValue(customGameData.deskid);
                    int deskSer = ClassSet.getObjectSerID(customGameData.deskid);
                    kernel.sendServerMsg(deskSer, ServerMsgDef.M2G_REMOVE_DESK.ordinal(), deskMsg.build().toByteArray());
                }
            }
            m_mapCustomGameData.remove(id);
            kernel.responseServer(reqId, new byte[0]);
        } else {
            logger.error("not found from game, id [{}]", id);
            kernel.responseServer(reqId, "关闭失败".getBytes(StandardCharsets.UTF_8));
        }
    }

    void OnUpdateCfg(MatchKernel kernel, int serid, int msgid, byte[] data)
            throws InvalidProtocolBufferException, SQLException {
        ServerMsg.StringSingle val = ServerMsg.StringSingle.parseFrom(data);
        String key = val.getWords();
        if (key.equals("PwDeskCfg")) {
            RefreshPwCfg(kernel, (flag) -> {
                if (!flag) {
                    return;
                }
                ServerMsg.StringSingle.Builder updateCfg = ServerMsg.StringSingle.newBuilder();
                updateCfg.setWords(m_pwCfg);
                kernel.broadToServer("game", ServerMsgDef.M2G_UPDATE_PW_CFG.ordinal(), updateCfg.build().toByteArray());
            });
        }
    }

    public void OnAddDesk(MatchKernel kernel, int serid, int msgid, byte[] data) throws InvalidProtocolBufferException {
        DeskData desk = new DeskData();
        ServerMsg.AddDesk addDesk = ServerMsg.AddDesk.parseFrom(data);
        int type = addDesk.getRoomType();
        desk.objid = addDesk.getDeskid();
        if (addDesk.hasGameid()) {
            desk.gameid = addDesk.getGameid();
        }
        if (addDesk.hasDeskType()) {
            desk.deskType = addDesk.getDeskType();
            desk.minBv = addDesk.getMinBv();
            desk.maxBv = addDesk.getMaxBv();
        }
        desk.seatCount = addDesk.getSeatcount();
        desk.seats = new SeatData[desk.seatCount];
        for (int i = 0; i < desk.seatCount; i++) {
            desk.seats[i] = new SeatData();
            desk.seats[i].playerid = 0l;
            desk.seats[i].state = SeatState.FREE;
        }
        int playerCount = addDesk.getPlayercount();
        for (int i = 0; i < playerCount; i++) {
            int seatid = addDesk.getSeatid(i);
            desk.seats[seatid].playerid = addDesk.getPlayerid(i);
            desk.seats[seatid].state = SeatState.HAVE;
        }
        RoomData room = mapRooms.get(type);
        if (room == null) {
            room = new RoomData();
            mapRooms.put(type, room);
        }
        room.mapDesks.put(desk.objid, desk);
        mapDeskById.put(desk.objid, desk);
        if (m_setSupreme.contains(type)) {
            //至尊选座
            int deskid = addDesk.getDesk();
            if (!m_mapCustomGameData.containsKey(deskid)) {
                return;
            }
            //logger.info("OnAddDesk  ===> {}", deskid);
            desk.deskid = deskid;
            CustomGameData gameData = m_mapCustomGameData.get(deskid);
            gameData.deskid = desk.objid;
            CheckCustomList(kernel, deskid);
        } else if (mysteryLegendRoomSet.contains(type)) {
            checkMysteryLegendWaitList(kernel, addDesk.getObjId(), desk.objid);
        } else {
            CheckWaitList(kernel);
        }
    }

    void checkMysteryLegendWaitList(MatchKernel kernel, long objId, long deskId) {

        DeskData deskData = mapDeskById.get(deskId);
        if (deskData == null) {
            return;
        }
        Optional<WaitMysteryLegendDesk> waitDeskOptional = waitMysteryLegendDesks.stream().
                filter(waitDesk -> waitDesk.playerId.equals(objId)).findAny();

        if (waitDeskOptional.isPresent()) {
            WaitMysteryLegendDesk waitDesk = waitDeskOptional.get();
            ServerMsg.MysteryLegendMatchRes.Builder build = ServerMsg.MysteryLegendMatchRes.newBuilder();
            for (int i = 0; i < deskData.seatCount; i++) {
                SeatData seatData = deskData.seats[i];
                if (seatData.isEmpty(kernel.getServerTime())) {
                    build.setDeskId(deskId);
                    build.setSeatId(i);
                    build.setRoomId(waitDesk.roomId);
                    seatData.state = SeatState.ORDER;
                    seatData.orderTime = kernel.getServerTime();
                    waitMysteryLegendDesks.remove(waitDesk);
                    kernel.responseServer(waitDesk.reqId, build.build().toByteArray());
                    return;
                }
            }
        }
    }

    public void OnDelDesk(MatchKernel kernel, int serid, int msgid, byte[] data) throws InvalidProtocolBufferException {
        ServerMsg.DelDesk delDesk = ServerMsg.DelDesk.parseFrom(data);
        long deskid = delDesk.getDeskid();

        for (Entry<Integer, RoomData> entry : mapRooms.entrySet()) {
            if (entry.getValue().mapDesks.containsKey(deskid)) {
                entry.getValue().mapDesks.remove(deskid);
                break;
            }
        }
    }

    public void OnSitDown(MatchKernel kernel, int serid, int msgid, byte[] data) throws InvalidProtocolBufferException {
        ServerMsg.SitDown sitDown = ServerMsg.SitDown.parseFrom(data);
        long deskid = sitDown.getDeskid();
        int seatid = sitDown.getSeatid();
        long playerid = sitDown.getPlayerid();
        int uid = sitDown.getUid();
        int vip = sitDown.getVip();
        int level = sitDown.getLevel();
        int head = sitDown.getHead();
        String name = sitDown.getName();
        //logger.info("Match OnSitDown {} {} {}",deskid,uid,seatid);
        for (Entry<Integer, RoomData> entry : mapRooms.entrySet()) {
            DeskData desk = entry.getValue().mapDesks.get(deskid);
            if (desk == null) {
                continue;
            }
            SeatData seatData = desk.seats[seatid];
            seatData.playerid = playerid;
            seatData.state = SeatState.HAVE;
            seatData.uid = uid;
            seatData.vip = vip;
            seatData.name = name;
            seatData.level = level;
            seatData.head = head;
            // 至尊选座，则同步座位信息到所有Game
            if (m_setSupreme.contains(entry.getKey()) && m_mapCustomGameData.containsKey(desk.deskid)) {
                CustomGameData gameData = m_mapCustomGameData.get(desk.deskid);
                ++gameData.online;
                kernel.updatePwGame(desk.deskid, -1, gameData.type, gameData.roomType, gameData.level,
                        gameData.minBv, gameData.maxBv, gameData.online, gameData.totalPlay, gameData.totalWin, gameData.enterLimit,
                        gameData.passwd);
                // 如果房间是魔晶单人桌 则不显示在线人数 显示的的是在线玩家 X
//				if (gameData.roomType == RoomType.ROOM_PERSONAL.ordinal()) {
//					kernel.UpdateOneOnline(desk.deskid, uid, gameData.totalPlay, gameData.totalWin);
//				} else {
                kernel.updateOneOnline(desk.deskid, gameData.online, gameData.totalPlay, gameData.totalWin);
//				}

                ServerMsg.BroadSeat.Builder msg = ServerMsg.BroadSeat.newBuilder();
                msg.setDeskid(desk.deskid);
                msg.setSeatid(seatid);
                msg.setUid(uid);
                msg.setVip(vip);
                msg.setName(name);
                msg.setLevel(level);
                msg.setHead(head);
                kernel.broadToServer("game", ServerMsgDef.M2G_BROAD_SEAT.ordinal(), msg.build().toByteArray());
            }
            break;
        }
    }

    public void OnStandUp(MatchKernel kernel, int serid, int msgid, byte[] data) throws InvalidProtocolBufferException {
        ServerMsg.StandUp standUp = ServerMsg.StandUp.parseFrom(data);
        long deskid = standUp.getDeskid();
        int seatid = standUp.getSeatid();
        if (standUp.hasUid()) {
            int uid = standUp.getUid();
            if (m_mapRobotByUid.containsKey(uid)) {
                RobotData robot = m_mapRobotByUid.get(uid);
                robot.state = RobotState.FREE.ordinal();
                kernel.changeRobotState(robot.uid, robot.state);
            }
        }
        for (Entry<Integer, RoomData> entry : mapRooms.entrySet()) {
            DeskData desk = entry.getValue().mapDesks.get(deskid);
            if (desk == null) {
                continue;
            }
            SeatData seatData = desk.seats[seatid];
            //logger.info("Match OnStandUp {} {} {}",deskid,seatData.uid,seatid);
            seatData.playerid = 0L;
            seatData.uid = 0;
            seatData.state = SeatState.FREE;
            // 至尊选座
            Integer roomType = entry.getKey();
            if (m_setSupreme.contains(roomType) && m_mapCustomGameData.containsKey(desk.deskid)) {
                CustomGameData gameData = m_mapCustomGameData.get(desk.deskid);
                --gameData.online;
                // 存储总玩总赢
                long totalp = standUp.getTotalplay();
                long totalw = standUp.getTotalwin();
                gameData.totalPlay = totalp;
                gameData.totalWin = totalw;
                kernel.updatePwGame(desk.deskid, -1, gameData.type, gameData.roomType, gameData.level,
                        gameData.minBv, gameData.maxBv, gameData.online, gameData.totalPlay, gameData.totalWin,
                        gameData.enterLimit, gameData.passwd);
                // 更新人数、总玩总赢
                // 如果房间是魔晶单人桌 则玩家起身之后 就没人
                if (gameData.roomType == RoomType.ROOM_PERSONAL.ordinal()) {
                    kernel.updateOneOnline(desk.deskid, 0, gameData.totalPlay, gameData.totalWin);
                } else {
                    kernel.updateOneOnline(desk.deskid, gameData.online, gameData.totalPlay, gameData.totalWin);
                }

                // 同步座位信息到所有Game
                ServerMsg.BroadSeat.Builder msg = ServerMsg.BroadSeat.newBuilder();
                msg.setDeskid(desk.deskid);
                msg.setSeatid(seatid);
                kernel.broadToServer("game", ServerMsgDef.M2G_BROAD_SEAT.ordinal(), msg.build().toByteArray());
                if (m_mapCustomGameData.containsKey(desk.deskid) && m_mapCustomGameData.get(desk.deskid).type == 1) {
                    // 密码桌
                    boolean have = false;
                    for (int i = 0; i < desk.seatCount; ++i) {
                        if (desk.seats[i].state == SeatState.HAVE) {
                            have = true;
                            break;
                        }
                    }
                    if (!have) {
                        // 释放密码桌
                        logger.info("桌子没有人了 {}  释放密码 {}",desk.deskid,gameData.passwd);
                        if (m_mapCustomGameData.get(desk.deskid).endTime != -1
                                && kernel.getServerTime() >= m_mapCustomGameData.get(desk.deskid).endTime) {
                            m_mapCustomGameData.get(desk.deskid).passwd = "";
                            // 清空数据库密码桌密码 20200603 by Lambda
                            kernel.updatePwGame(desk.deskid, -1, gameData.type, gameData.roomType, gameData.level,
                                    gameData.minBv, gameData.maxBv, gameData.online, gameData.totalPlay,
                                    gameData.totalWin, gameData.enterLimit, "");
                        }
                    }
                }
            }
        }
    }

    // 创建密码房
    public void OnReqCreatePw(MatchKernel kernel, int reqid, byte[] data) throws InvalidProtocolBufferException {
        ServerMsg.CreatePw create = ServerMsg.CreatePw.parseFrom(data);
        long playerid = create.getPlayerid();
        int seatid = create.getSeatid();
        int uid = create.getUid();
        String passwd = create.getPasswd();
        int cmax = create.getCreatemax();
        int roomType = create.getRoomType();

        //logger.info("Match OnReqCreatePw {}", create);
        int haveCount = 0;
        int ownCount = 0;
        for (Entry<Integer, CustomGameData> entry : m_mapCustomGameData.entrySet()) {
            CustomGameData value = entry.getValue();
            if (value.type == 1) {
                if (passwd.equals(value.passwd)) {
                    // 密码重复
                    ServerMsg.MatchRes.Builder build = ServerMsg.MatchRes.newBuilder();
                    build.setCode(1);
                    kernel.responseServer(reqid, build.build().toByteArray());
                    return;
                } else if (value.deskid != 0L && value.passwd != null && value.passwd.length() > 0) {
                    ++haveCount;
                    if (value.creator == uid) {
                        ++ownCount;
                    }
                }
            }
        }
        if (haveCount >= m_maxCount) {
            // 数量上限
            ServerMsg.MatchRes.Builder build = ServerMsg.MatchRes.newBuilder();
            build.setCode(2);
            kernel.responseServer(reqid, build.build().toByteArray());
            return;
        }
        if (ownCount >= cmax) {
            // 单人可创建数量上线
            ServerMsg.MatchRes.Builder build = ServerMsg.MatchRes.newBuilder();
            build.setCode(3);
            kernel.responseServer(reqid, build.build().toByteArray());
            return;
        }
        // 分配Game创建桌子
        String min = "";
        int minVal = 9999999;
        Object[] games = kernel.getServersByType("game");
        for (int i = 0; i < games.length; ++i) {
            String name = games[i].toString();
            if (!mapGameSer.containsKey(name)) {
                mapGameSer.put(name, 0);
                min = name;
                break;
            } else {
                if (mapGameSer.get(name) < minVal) {
                    minVal = mapGameSer.get(name);
                    min = name;
                }
            }
        }
        if (!min.isEmpty()) {
            int deskid = 0;
            int max = 1;
            for (Entry<Integer, CustomGameData> entry : m_mapCustomGameData.entrySet()) {
                if (entry.getValue().bSwitch && entry.getValue().type == 1 && entry.getValue().roomType == roomType
                        && entry.getValue().passwd.isEmpty()) {
                    deskid = entry.getKey();
                    break;
                }
                if (entry.getKey() >= max) {
                    max = entry.getKey() + 1;
                }
            }
            ServerMsg.CreateCustomDesk.Builder cdesk = ServerMsg.CreateCustomDesk.newBuilder();
            boolean levelChanged = false;
            CustomGameData gameData = null;
            if (deskid == 0) {
                deskid = max;
                gameData = new CustomGameData();
                gameData.totalPlay = 0;
                gameData.totalWin = 0;
                gameData.deskid = 0L;
            } else {
                gameData = m_mapCustomGameData.get(deskid);
                if (gameData.level != m_level) {
                    levelChanged = true;
                    gameData.totalPlay = 0;
                    gameData.totalWin = 0;
                }
            }

            gameData.bSwitch = true;
            gameData.level = m_level;
            gameData.minBv = m_minBv;
            gameData.maxBv = m_maxBv;
            gameData.type = 1;
            gameData.roomType = roomType;
            gameData.enterLimit = m_limit;
            gameData.autoKick = 5 * 60 * 1000;
            gameData.online = 0;
            gameData.creator = uid;
            gameData.passwd = passwd;
            gameData.id = deskid;

            if (m_lifetime >= 0) {
                gameData.endTime = m_lifetime + kernel.getServerTime();
            } else {
                gameData.endTime = -1;
            }

            m_mapCustomGameData.put(deskid, gameData);

            logger.info("Match CustomGameData {} {}", deskid, gameData);

            // 新增密码桌
            kernel.updatePwGame(deskid, uid, gameData.type, gameData.roomType, gameData.level, gameData.minBv,
                    gameData.maxBv, gameData.online, gameData.totalPlay, gameData.totalWin, gameData.enterLimit, gameData.passwd);

            if (gameData.deskid != 0L) {
                // 更新密码桌配置
                ServerMsg.UpdateDesk.Builder deskMsg = ServerMsg.UpdateDesk.newBuilder();
                deskMsg.setDeskid(gameData.deskid);
                deskMsg.setMinbv(gameData.minBv);
                deskMsg.setMaxbv(gameData.maxBv);
                deskMsg.setAutoKick(gameData.autoKick);
                if (levelChanged) {
                    deskMsg.setLevel(gameData.level);
                }

                int deskSer = ClassSet.getObjectSerID(gameData.deskid);
                kernel.sendServerMsg(deskSer, ServerMsgDef.M2G_UPDATE_PARAM.ordinal(), deskMsg.build().toByteArray());

                // 直接进入
                ServerMsg.MatchRes.Builder build = ServerMsg.MatchRes.newBuilder();
                build.setRoomType(roomType);
                build.setDeskid(m_mapCustomGameData.get(deskid).deskid);
                build.setSeatid(seatid);
                build.setPlayerid(playerid);
                kernel.responseServer(reqid, build.build().toByteArray());
                return;
            }

            cdesk.setTotalPlay(m_mapCustomGameData.get(deskid).totalPlay);
            cdesk.setTotalWin(m_mapCustomGameData.get(deskid).totalWin);
            cdesk.setMinbv(m_mapCustomGameData.get(deskid).minBv);
            cdesk.setMaxbv(m_mapCustomGameData.get(deskid).maxBv);
            cdesk.setLevel(m_mapCustomGameData.get(deskid).level);
            cdesk.setLimit(m_mapCustomGameData.get(deskid).enterLimit);
            cdesk.setAutoKick(m_mapCustomGameData.get(deskid).autoKick);
            cdesk.setRoomType(m_mapCustomGameData.get(deskid).roomType);
            cdesk.setDeskid(deskid);
            cdesk.setDeskbg(m_deskbg);
            cdesk.setType(1);
            kernel.sendServerMsg(min, ServerMsgDef.M2G_CREATE_CUSTOM_DESK.ordinal(), cdesk.build().toByteArray());

            WaitCustomDesk wait = new WaitCustomDesk();
            wait.playerid = playerid;
            wait.reqid = reqid;
            wait.deskid = deskid;
            wait.seatid = seatid;
            m_waitCustom.add(wait);
        }
    }

    void OnReqEnterPw(MatchKernel kernel, int reqid, byte[] data) throws InvalidProtocolBufferException {
        ServerMsg.EnterPw create = ServerMsg.EnterPw.parseFrom(data);
        long playerid = create.getPlayerid();
        int seatid = create.getSeatid();
        String passwd = create.getPasswd();
        int maxbv = create.getMaxbv();
        int roomType = create.getRoomType();
        
        logger.info("OnReqEnterPw 玩家{} 请求进入房间 roomType={} seatid={} passwd={} maxbv={}", 
                playerid, roomType, seatid, passwd != null ? passwd : "null", maxbv);

        ServerMsg.MatchRes.Builder build = ServerMsg.MatchRes.newBuilder();
        build.setDeskid(0L);
        build.setSeatid(-1);
        build.setPlayerid(playerid);
        build.setCode(2); // 桌子不存在
        boolean bFound = false;
        
        if (passwd == null || passwd.isEmpty()) {
            logger.info("OnReqEnterPw 密码为空，查找普通桌 roomType={}", roomType);
            List<Object[]> availableDesks = new ArrayList<>(); // [deskid, seatid, CustomGameData]
            long now = kernel.getServerTime();
            
            for (Entry<Integer, CustomGameData> entry : m_mapCustomGameData.entrySet()) {
                CustomGameData customGameData = entry.getValue();
                // 检查type=0且roomType相同
                if (customGameData.type == 0 && customGameData.roomType == roomType) {
                    if (customGameData.minBv > maxbv) {
                        continue;
                    }
                    
                    long desk = customGameData.deskid;
                    // 检查桌子是否存在
                    if (!mapDeskById.containsKey(desk)) {
                        // 如果不存在则创建一个
                        if (desk == 0L) {
                            logger.info("OnReqEnterPw 普通桌不存在，准备创建 deskid={} roomType={}", customGameData.id, roomType);
                            // 分配Game创建桌子
                            String min = "";
                            int minVal = 9999999;
                            Object[] games = kernel.getServersByType("game");
                            for (int i = 0; i < games.length; ++i) {
                                String name = games[i].toString();
                                if (!mapGameSer.containsKey(name)) {
                                    mapGameSer.put(name, 0);
                                    min = name;
                                    break;
                                } else {
                                    if (mapGameSer.get(name) < minVal) {
                                        minVal = mapGameSer.get(name);
                                        min = name;
                                    }
                                }
                            }
                            if (!min.isEmpty()) {
                                ServerMsg.CreateCustomDesk.Builder cdesk = ServerMsg.CreateCustomDesk.newBuilder();
                                int deskid = customGameData.id;
                                cdesk.setDeskid(deskid);
                                cdesk.setTotalPlay(customGameData.totalPlay);
                                cdesk.setTotalWin(customGameData.totalWin);
                                cdesk.setMinbv(customGameData.minBv);
                                cdesk.setMaxbv(customGameData.maxBv);
                                cdesk.setLevel(customGameData.level);
                                cdesk.setLimit(customGameData.enterLimit);
                                cdesk.setAutoKick(customGameData.autoKick);
                                cdesk.setDeskbg(3);
                                cdesk.setRoomType(customGameData.roomType);
                                cdesk.setType(0);
                                kernel.sendServerMsg(min, ServerMsgDef.M2G_CREATE_CUSTOM_DESK.ordinal(),
                                        cdesk.build().toByteArray());
                                WaitCustomDesk wait = new WaitCustomDesk();
                                wait.playerid = playerid;
                                wait.reqid = reqid;
                                wait.deskid = deskid;
                                wait.seatid = seatid;
                                m_waitCustom.add(wait);
                                logger.info("OnReqEnterPw 普通桌创建请求已发送 deskid={} gameServer={} seatid={}", deskid, min, seatid);
                                return;
                            } else {
                                logger.warn("OnReqEnterPw 没有可用的game服务器创建普通桌");
                            }
                        }
                    } else {
                        DeskData deskData = mapDeskById.get(desk);
                        // 查找空闲座位
                        if (seatid == -1) {
                            // 完全随机选择座位
                            int availableCount = 0;
                            for (int i = 0; i < deskData.seats.length; i++) {
                                if (deskData.seats[i].state == SeatState.FREE
                                        || (deskData.seats[i].state == SeatState.ORDER
                                        && now - deskData.seats[i].orderTime >= ORDER_MAX_TIME)) {
                                    availableDesks.add(new Object[]{desk, i, customGameData});
                                    availableCount++;
                                }
                            }
                            if (availableCount > 0) {
                                logger.debug("OnReqEnterPw 普通桌有空闲座位 desk={} 空闲座位数={}", desk, availableCount);
                            }
                        } else {
                            // 按照指定的座位id
                            if (seatid < deskData.seats.length) {
                                if (deskData.seats[seatid].state == SeatState.FREE
                                        || (deskData.seats[seatid].state == SeatState.ORDER
                                        && now - deskData.seats[seatid].orderTime >= ORDER_MAX_TIME)) {
                                    availableDesks.add(new Object[]{desk, seatid, customGameData});
                                    logger.debug("OnReqEnterPw 普通桌指定座位可用 desk={} seatid={}", desk, seatid);
                                } else {
                                    logger.debug("OnReqEnterPw 普通桌指定座位不可用 desk={} seatid={} state={}", 
                                            desk, seatid, deskData.seats[seatid].state);
                                }
                            }
                        }
                    }
                }
            }
            
            // 随机选择一个符合条件的桌子
            if (!availableDesks.isEmpty()) {
                Object[] selected = availableDesks.get(m_rand.nextInt(availableDesks.size()));
                long selectedDesk = (Long) selected[0];
                int selectedSeatid = (Integer) selected[1];
                CustomGameData selectedCustomGameData = (CustomGameData) selected[2];
                
                logger.info("OnReqEnterPw 普通桌随机选择成功 玩家={} desk={} seatid={} 候选数={}", 
                        playerid, selectedDesk, selectedSeatid, availableDesks.size());
                
                bFound = true;
                build.setRoomType(selectedCustomGameData.roomType);
                build.setCode(0);
                build.setDeskid(selectedDesk);
                build.setSeatid(selectedSeatid);
                
                DeskData deskData = mapDeskById.get(selectedDesk);
                deskData.seats[selectedSeatid].state = SeatState.ORDER;
                deskData.seats[selectedSeatid].orderTime = now;
                
                kernel.responseServer(reqid, build.build().toByteArray());
                return;
            } else {
                logger.info("OnReqEnterPw 普通桌没有找到可用桌子 roomType={} seatid={}", roomType, seatid);
            }
        }
        
        // 原有逻辑：密码不为空时，查找type=1且密码匹配的房间
        if (passwd != null && !passwd.isEmpty()) {
            logger.info("OnReqEnterPw 密码不为空，查找密码桌 roomType={} passwd={}", roomType, passwd);
        }
        for (Entry<Integer, CustomGameData> entry : m_mapCustomGameData.entrySet()) {
            if(entry.getValue().roomType != roomType){
                continue;
            }
            if (entry.getValue().type == 1 && entry.getValue().passwd.equals(passwd)) {
                logger.info("OnReqEnterPw 找到相同密码的房间 玩家={} deskid={} 房间密码={}", 
                        playerid, entry.getValue().id, entry.getValue().passwd);
                bFound = true;
                build.setRoomType(entry.getValue().roomType);
                if (entry.getValue().minBv > maxbv) {
                    logger.warn("OnReqEnterPw 炮值不足 玩家={} minBv={} maxbv={}", 
                            playerid, entry.getValue().minBv, maxbv);
                    build.setCode(3); // 炮值不足
                    break;
                }

                long desk = entry.getValue().deskid;
                if (!mapDeskById.containsKey(desk)) {
                    logger.info("OnReqEnterPw 密码桌不存在，准备创建 deskid={}", entry.getValue().id);
                    if (desk == 0L) {
                        // 分配Game创建桌子
                        String min = "";
                        int minVal = 9999999;
                        Object[] games = kernel.getServersByType("game");
                        for (int i = 0; i < games.length; ++i) {
                            String name = games[i].toString();
                            if (!mapGameSer.containsKey(name)) {
                                mapGameSer.put(name, 0);
                                min = name;
                                break;
                            } else {
                                if (mapGameSer.get(name) < minVal) {
                                    minVal = mapGameSer.get(name);
                                    min = name;
                                }
                            }
                        }
                        if (!min.isEmpty()) {
                            ServerMsg.CreateCustomDesk.Builder cdesk = ServerMsg.CreateCustomDesk.newBuilder();
                            int deskid = entry.getValue().id;
                            cdesk.setDeskid(deskid);
                            cdesk.setTotalPlay(m_mapCustomGameData.get(deskid).totalPlay);
                            cdesk.setTotalWin(m_mapCustomGameData.get(deskid).totalWin);
                            cdesk.setMinbv(m_mapCustomGameData.get(deskid).minBv);
                            cdesk.setMaxbv(m_mapCustomGameData.get(deskid).maxBv);
                            cdesk.setLevel(m_mapCustomGameData.get(deskid).level);
                            cdesk.setLimit(m_mapCustomGameData.get(deskid).enterLimit);
                            cdesk.setAutoKick(m_mapCustomGameData.get(deskid).autoKick);
                            cdesk.setDeskbg(3);
                            cdesk.setRoomType(m_mapCustomGameData.get(deskid).roomType);
                            cdesk.setType(1);
                            kernel.sendServerMsg(min, ServerMsgDef.M2G_CREATE_CUSTOM_DESK.ordinal(),
                                    cdesk.build().toByteArray());
                            WaitCustomDesk wait = new WaitCustomDesk();
                            wait.playerid = playerid;
                            wait.reqid = reqid;
                            wait.deskid = deskid;
                            wait.seatid = seatid;
                            m_waitCustom.add(wait);
                            logger.info("OnReqEnterPw 密码桌创建请求已发送 玩家={} deskid={} gameServer={} seatid={}", 
                                    playerid, deskid, min, seatid);
                            return;
                        } else {
                            logger.warn("OnReqEnterPw 没有可用的game服务器创建密码桌");
                        }
                    }
                    break;
                } else {
                    DeskData deskData = mapDeskById.get(desk);
                    long now = kernel.getServerTime();
                    int finalSeatid = seatid;
                    
                    if (seatid == -1) {
                        // 完全随机选择座位
                        List<Integer> availableSeats = new ArrayList<>();
                        for (int i = 0; i < deskData.seats.length; i++) {
                            if (deskData.seats[i].state == SeatState.FREE
                                    || (deskData.seats[i].state == SeatState.ORDER
                                    && now - deskData.seats[i].orderTime >= ORDER_MAX_TIME)) {
                                availableSeats.add(i);
                            }
                        }
                        if (!availableSeats.isEmpty()) {
                            finalSeatid = availableSeats.get(m_rand.nextInt(availableSeats.size()));
                            logger.info("OnReqEnterPw 密码桌随机选择座位 玩家={} desk={} 选中座位={} 可用座位数={}", 
                                    playerid, desk, finalSeatid, availableSeats.size());
                        } else {
                            logger.warn("OnReqEnterPw 密码桌没有可用座位 玩家={} desk={}", playerid, desk);
                            build.setCode(1); // 座位被占用
                            break;
                        }
                    } else {
                        // 按照指定的座位id
                        if (deskData.seats.length <= seatid) {
                            logger.warn("OnReqEnterPw 密码桌指定座位超出范围 玩家={} desk={} seatid={} 座位总数={}", 
                                    playerid, desk, seatid, deskData.seats.length);
                            break;
                        }
                        if (deskData.seats[seatid].state != SeatState.FREE
                                && !(deskData.seats[seatid].state == SeatState.ORDER
                                && now - deskData.seats[seatid].orderTime >= ORDER_MAX_TIME)) {
                            logger.warn("OnReqEnterPw 密码桌指定座位被占用 玩家={} desk={} seatid={} state={}", 
                                    playerid, desk, seatid, deskData.seats[seatid].state);
                            build.setCode(1); // 座位被占用
                            break;
                        }
                        logger.debug("OnReqEnterPw 密码桌指定座位可用 玩家={} desk={} seatid={}", playerid, desk, seatid);
                    }
                    
                    // sit down
                    build.setCode(0);
                    build.setDeskid(desk);
                    build.setSeatid(finalSeatid);
                    deskData.seats[finalSeatid].state = SeatState.ORDER;
                    deskData.seats[finalSeatid].orderTime = now;
                    logger.info("OnReqEnterPw 密码桌进入成功 玩家={} desk={} seatid={}", playerid, desk, finalSeatid);
                    break;
                }
            }
        }
        if (!bFound) {
            logger.info("OnReqEnterPw 未找到匹配密码桌，查找空白密码桌 玩家={} roomType={} passwd={}", 
                    playerid, roomType, passwd);
            List<Object[]> availableBlankDesks = new ArrayList<>(); // [deskid, seatid, CustomGameData]
            long now = kernel.getServerTime();
            
            for (Entry<Integer, CustomGameData> entry : m_mapCustomGameData.entrySet()) {
                CustomGameData customGameData = entry.getValue();
                // 检查type=1（密码桌）、roomType相同、密码为空
                if (customGameData.type == 1 && customGameData.roomType == roomType 
                        && (customGameData.passwd == null || customGameData.passwd.isEmpty())) {
                    // 检查炮值限制
                    if (customGameData.minBv > maxbv) {
                        continue;
                    }
                    
                    long desk = customGameData.deskid;
                    // 检查桌子是否存在
                    if (mapDeskById.containsKey(desk)) {
                        DeskData deskData = mapDeskById.get(desk);
                        // 查找空闲座位
                        if (seatid == -1) {
                            // 完全随机选择座位
                            for (int i = 0; i < deskData.seats.length; i++) {
                                if (deskData.seats[i].state == SeatState.FREE
                                        || (deskData.seats[i].state == SeatState.ORDER
                                        && now - deskData.seats[i].orderTime >= ORDER_MAX_TIME)) {
                                    availableBlankDesks.add(new Object[]{desk, i, customGameData});
                                }
                            }
                        } else {
                            // 按照指定的座位id
                            if (seatid < deskData.seats.length) {
                                if (deskData.seats[seatid].state == SeatState.FREE
                                        || (deskData.seats[seatid].state == SeatState.ORDER
                                        && now - deskData.seats[seatid].orderTime >= ORDER_MAX_TIME)) {
                                    availableBlankDesks.add(new Object[]{desk, seatid, customGameData});
                                }
                            }
                        }
                    } else if (desk == 0L) {
                        // 桌子不存在但可以创建，也加入候选列表
                        availableBlankDesks.add(new Object[]{desk, seatid, customGameData});
                    }
                }
            }
            
            // 随机选择一个符合条件的空白密码桌子
            if (!availableBlankDesks.isEmpty()) {
                Object[] selected = availableBlankDesks.get(m_rand.nextInt(availableBlankDesks.size()));
                long selectedDesk = (Long) selected[0];
                int selectedSeatid = (Integer) selected[1];
                CustomGameData selectedCustomGameData = (CustomGameData) selected[2];
                
                logger.info("OnReqEnterPw 空白密码桌随机选择成功 玩家={} deskid={} desk={} seatid={} 候选数={}", 
                        playerid, selectedCustomGameData.id, selectedDesk, selectedSeatid, availableBlankDesks.size());
                
                // 设置密码
                selectedCustomGameData.passwd = passwd;
                // 更新数据库
                kernel.updatePwGame(selectedCustomGameData.id, -1, selectedCustomGameData.type, 
                        selectedCustomGameData.roomType, selectedCustomGameData.level,
                        selectedCustomGameData.minBv, selectedCustomGameData.maxBv, 
                        selectedCustomGameData.online, selectedCustomGameData.totalPlay,
                        selectedCustomGameData.totalWin, selectedCustomGameData.enterLimit, passwd);
                
                logger.info("OnReqEnterPw 空白密码桌设置密码成功 玩家={} deskid={} passwd={}", 
                        playerid, selectedCustomGameData.id, passwd);
                
                bFound = true;
                build.setRoomType(selectedCustomGameData.roomType);
                
                // 再次检查炮值限制（虽然前面已经检查过，但为了安全）
                if (selectedCustomGameData.minBv > maxbv) {
                    logger.warn("OnReqEnterPw 空白密码桌炮值不足 玩家={} deskid={} minBv={} maxbv={}", 
                            playerid, selectedCustomGameData.id, selectedCustomGameData.minBv, maxbv);
                    build.setCode(3); // 炮值不足
                    kernel.responseServer(reqid, build.build().toByteArray());
                    return;
                }
                
                if (!mapDeskById.containsKey(selectedDesk)) {
                    // 桌子不存在，需要创建
                    if (selectedDesk == 0L) {
                        // 分配Game创建桌子
                        String min = "";
                        int minVal = 9999999;
                        Object[] games = kernel.getServersByType("game");
                        for (int i = 0; i < games.length; ++i) {
                            String name = games[i].toString();
                            if (!mapGameSer.containsKey(name)) {
                                mapGameSer.put(name, 0);
                                min = name;
                                break;
                            } else {
                                if (mapGameSer.get(name) < minVal) {
                                    minVal = mapGameSer.get(name);
                                    min = name;
                                }
                            }
                        }
                        if (!min.isEmpty()) {
                            ServerMsg.CreateCustomDesk.Builder cdesk = ServerMsg.CreateCustomDesk.newBuilder();
                            int deskid = selectedCustomGameData.id;
                            cdesk.setDeskid(deskid);
                            cdesk.setTotalPlay(selectedCustomGameData.totalPlay);
                            cdesk.setTotalWin(selectedCustomGameData.totalWin);
                            cdesk.setMinbv(selectedCustomGameData.minBv);
                            cdesk.setMaxbv(selectedCustomGameData.maxBv);
                            cdesk.setLevel(selectedCustomGameData.level);
                            cdesk.setLimit(selectedCustomGameData.enterLimit);
                            cdesk.setAutoKick(selectedCustomGameData.autoKick);
                            cdesk.setDeskbg(3);
                            cdesk.setRoomType(selectedCustomGameData.roomType);
                            cdesk.setType(1);
                            kernel.sendServerMsg(min, ServerMsgDef.M2G_CREATE_CUSTOM_DESK.ordinal(),
                                    cdesk.build().toByteArray());
                            WaitCustomDesk wait = new WaitCustomDesk();
                            wait.playerid = playerid;
                            wait.reqid = reqid;
                            wait.deskid = deskid;
                            wait.seatid = selectedSeatid;
                            m_waitCustom.add(wait);
                            logger.info("OnReqEnterPw 空白密码桌创建请求已发送 玩家={} deskid={} gameServer={} seatid={}", 
                                    playerid, deskid, min, selectedSeatid);
                            return;
                        } else {
                            logger.warn("OnReqEnterPw 没有可用的game服务器创建空白密码桌");
                        }
                    }
                } else {
                    DeskData deskData = mapDeskById.get(selectedDesk);
                    int finalSeatid = selectedSeatid;
                    
                    if (seatid == -1) {
                        // 完全随机选择座位（如果之前选中的座位不可用，重新选择）
                        List<Integer> availableSeats = new ArrayList<>();
                        for (int i = 0; i < deskData.seats.length; i++) {
                            if (deskData.seats[i].state == SeatState.FREE
                                    || (deskData.seats[i].state == SeatState.ORDER
                                    && now - deskData.seats[i].orderTime >= ORDER_MAX_TIME)) {
                                availableSeats.add(i);
                            }
                        }
                        if (!availableSeats.isEmpty()) {
                            finalSeatid = availableSeats.get(m_rand.nextInt(availableSeats.size()));
                            logger.info("OnReqEnterPw 空白密码桌重新随机选择座位 玩家={} desk={} 选中座位={} 可用座位数={}", 
                                    playerid, selectedDesk, finalSeatid, availableSeats.size());
                        } else {
                            logger.warn("OnReqEnterPw 空白密码桌没有可用座位 玩家={} desk={}", playerid, selectedDesk);
                            build.setCode(1); // 座位被占用
                            kernel.responseServer(reqid, build.build().toByteArray());
                            return;
                        }
                    } else {
                        // 按照指定的座位id
                        if (deskData.seats.length <= selectedSeatid) {
                            logger.warn("OnReqEnterPw 空白密码桌指定座位超出范围 玩家={} desk={} seatid={} 座位总数={}", 
                                    playerid, selectedDesk, selectedSeatid, deskData.seats.length);
                            build.setCode(1); // 座位不存在
                            kernel.responseServer(reqid, build.build().toByteArray());
                            return;
                        }
                        if (deskData.seats[selectedSeatid].state != SeatState.FREE
                                && !(deskData.seats[selectedSeatid].state == SeatState.ORDER
                                && now - deskData.seats[selectedSeatid].orderTime >= ORDER_MAX_TIME)) {
                            logger.warn("OnReqEnterPw 空白密码桌指定座位被占用 玩家={} desk={} seatid={} state={}", 
                                    playerid, selectedDesk, selectedSeatid, deskData.seats[selectedSeatid].state);
                            build.setCode(1); // 座位被占用
                            kernel.responseServer(reqid, build.build().toByteArray());
                            return;
                        }
                    }
                    
                    build.setCode(0);
                    build.setDeskid(selectedDesk);
                    build.setSeatid(finalSeatid);
                    deskData.seats[finalSeatid].state = SeatState.ORDER;
                    deskData.seats[finalSeatid].orderTime = now;
                    logger.info("OnReqEnterPw 空白密码桌进入成功 玩家={} desk={} seatid={}", 
                            playerid, selectedDesk, finalSeatid);
                }
            } else {
                logger.info("OnReqEnterPw 空白密码桌没有找到可用桌子 玩家={} roomType={}", playerid, roomType);
            }
        }

        if (!bFound) {
            logger.warn("OnReqEnterPw 未找到任何可用桌子 玩家={} roomType={} passwd={} seatid={}", 
                    playerid, roomType, passwd, seatid);
            build.setCode(2); // 桌子不存在
        }

        kernel.responseServer(reqid, build.build().toByteArray());
        logger.debug("OnReqEnterPw 返回结果 玩家={} code={} deskid={} seatid={}", 
                playerid, build.getCode(), build.getDeskid(), build.getSeatid());
        //logger.info("OnReqEnterPw build {}", build.build().toString());
    }

    public void OnReqSitDown(MatchKernel kernel, int reqid, byte[] data) throws InvalidProtocolBufferException {
        ServerMsg.ReqSitDown enter = ServerMsg.ReqSitDown.parseFrom(data);
        int deskid = enter.getDeskid();
        int seatid = enter.getSeatid();
        long playerid = enter.getPlayerid();

        // 0成功，1桌子不存在，2等待，3座位被占用
        ServerMsg.SitDownRes.Builder build = ServerMsg.SitDownRes.newBuilder();
        build.setPlayerid(playerid);
        do {
            if (!m_mapCustomGameData.containsKey(deskid)) {
                build.setCode(1);
                break;
            }
            if (m_mapCustomGameData.get(deskid).type == 1) {
                // 密码桌
                build.setCode(1);
                break;
            }

            long desk = m_mapCustomGameData.get(deskid).deskid;
            if (desk == 0L) {
                // 分配Game创建桌子
                String min = "";
                int minVal = 9999999;
                Object[] games = kernel.getServersByType("game");
                for (int i = 0; i < games.length; ++i) {
                    String name = games[i].toString();
                    if (!mapGameSer.containsKey(name)) {
                        mapGameSer.put(name, 0);
                        min = name;
                        break;
                    } else {
                        if (mapGameSer.get(name) < minVal) {
                            minVal = mapGameSer.get(name);
                            min = name;
                        }
                    }
                }

                if (!min.isEmpty()) {
                    ServerMsg.CreateCustomDesk.Builder cdesk = ServerMsg.CreateCustomDesk.newBuilder();
                    cdesk.setDeskid(deskid);
                    cdesk.setTotalPlay(m_mapCustomGameData.get(deskid).totalPlay);
                    cdesk.setTotalWin(m_mapCustomGameData.get(deskid).totalWin);
                    cdesk.setMinbv(m_mapCustomGameData.get(deskid).minBv);
                    cdesk.setMaxbv(m_mapCustomGameData.get(deskid).maxBv);
                    cdesk.setLevel(m_mapCustomGameData.get(deskid).level);
                    cdesk.setLimit(m_mapCustomGameData.get(deskid).enterLimit);
                    cdesk.setAutoKick(m_mapCustomGameData.get(deskid).autoKick);
                    cdesk.setDeskbg(m_deskbg);
                    cdesk.setRoomType(m_mapCustomGameData.get(deskid).roomType);
                    cdesk.setType(0);
                    kernel.sendServerMsg(min, ServerMsgDef.M2G_CREATE_CUSTOM_DESK.ordinal(), cdesk.build().toByteArray());
                    WaitCustomDesk wait = new WaitCustomDesk();
                    wait.playerid = playerid;
                    wait.reqid = reqid;
                    wait.deskid = deskid;
                    if (seatid == -1) {
                        seatid = 0; // 海神殿快速开始会卡住的问题修复: 新建桌子就默认坐0号位 不然坐不下去
                    }
                    wait.seatid = seatid;
                    m_waitCustom.add(wait);
                } else {
                    build.setCode(2);
                    break;
                }
                return;
            } else {
                if (!mapDeskById.containsKey(desk)) {
                    build.setCode(1);
                    break;
                }
                DeskData deskData = mapDeskById.get(desk);
                if (deskData.seats.length <= seatid) {
                    build.setCode(3);
                    break;
                }
                long now = kernel.getServerTime();
                // 快速入座
                if (seatid == -1) {
                    for (int k = 0; k < deskData.seats.length; k++) {
                        seatid = k;
                        if (deskData.seats[seatid].state == SeatState.FREE
                                || (deskData.seats[seatid].state == SeatState.ORDER
                                && now - deskData.seats[seatid].orderTime >= ORDER_MAX_TIME)) {
                            break;
                        }
                    }
                }
                if (deskData.seats[seatid].state == SeatState.FREE || (deskData.seats[seatid].state == SeatState.ORDER
                        && now - deskData.seats[seatid].orderTime >= ORDER_MAX_TIME)) {
                    // sit down
                    build.setCode(0);
                    build.setDeskid(desk);
                    build.setSeatid(seatid);
                    build.setPlayerid(playerid);
                    deskData.seats[seatid].state = SeatState.ORDER;
                    deskData.seats[seatid].orderTime = now;
                } else {
                    build.setCode(3);
                }
            }
        } while (false);

        kernel.responseServer(reqid, build.build().toByteArray());
    }

    boolean chooseSeat(MatchKernel kernel, int reqid, long playerid, long desk) {
        DeskData deskData = mapDeskById.get(desk);
        long now = kernel.getServerTime();

        ServerMsg.MatchRes.Builder build = ServerMsg.MatchRes.newBuilder();
        build.setRoomType(4);
        build.setDeskid(desk);
        build.setSeatid(-1);
        build.setPlayerid(playerid);

        for (int i = 0; i < deskData.seatCount; ++i) {
            if (deskData.seats[i].state == SeatState.FREE || (deskData.seats[i].state == SeatState.ORDER
                    && now - deskData.seats[i].orderTime >= ORDER_MAX_TIME)) {
                // sit down
                build.setSeatid(i);
                deskData.seats[i].state = SeatState.ORDER;
                deskData.seats[i].orderTime = now;
                kernel.responseServer(reqid, build.build().toByteArray());
                return true;
            }
        }
        return false;

    }

    boolean CheckMatch(MatchKernel kernel, int reqid, long playerid, int count, long desk) {
        logger.info("CheckMatch {}", count);
        DeskData deskData = mapDeskById.get(desk);
        long now = kernel.getServerTime();
        int haveCount = 0;
        for (int i = 0; i < deskData.seatCount; ++i) {
            if (deskData.seats[i].state == SeatState.HAVE || (deskData.seats[i].state == SeatState.ORDER
                    && now - deskData.seats[i].orderTime < ORDER_MAX_TIME)) {
                ++haveCount;
            }
        }

        if (haveCount != count) {
            return false;
        }

        ServerMsg.MatchRes.Builder build = ServerMsg.MatchRes.newBuilder();
        build.setRoomType(4);
        build.setDeskid(desk);
        build.setSeatid(-1);
        build.setPlayerid(playerid);

        for (int i = 0; i < deskData.seatCount; ++i) {
            if (deskData.seats[i].state == SeatState.FREE || (deskData.seats[i].state == SeatState.ORDER
                    && now - deskData.seats[i].orderTime >= ORDER_MAX_TIME)) {
                // sit down
                build.setSeatid(i);
                deskData.seats[i].state = SeatState.ORDER;
                deskData.seats[i].orderTime = now;
                kernel.responseServer(reqid, build.build().toByteArray());
                return true;
            }
        }
        return false;
    }

    void MatchCustomGame(MatchKernel kernel, int reqid, long playerid, int bv, int type, int uid) {
        //logger.info("MatchCustomGame {} {}", playerid, bv);
        /** 如果有该玩家建的私人桌,优先进入私人桌 **/
        for (Entry<Integer, CustomGameData> entry : m_mapCustomGameData.entrySet()) {
            long desk = entry.getValue().deskid;
            if (entry.getValue().creator == uid) {
                if (desk == 0 && entry.getValue().type == 0) {
                    type = entry.getValue().roomType;
                }
                if (desk != 0 && chooseSeat(kernel, reqid, playerid, desk)) {
                    return;
                }
            }
        }
        /** 没有私人桌的,随机选择一个房间最大炮值>=玩家炮值的房间 **/
        for (Entry<Integer, CustomGameData> entry : m_mapCustomGameData.entrySet()) {
            long desk = entry.getValue().deskid;
            int myRoomType = entry.getValue().roomType;
            if (RoomModule.isNuclear(myRoomType)) {
                continue;
            }
            if (entry.getValue().type == 0 && entry.getValue().maxBv >= bv) {
                if (desk == 0) {
                    type = myRoomType;
                }

                if (desk != 0 && chooseSeat(kernel, reqid, playerid, desk)) {
                    return;
                }
            }
        }

        Collection<CustomGameData> cusDatas = m_mapCustomGameData.values();
        List<CustomGameData> cusList = new LinkedList<>();
        for (CustomGameData data : cusDatas) {
            if (data == null) {
                continue;
            }
            if (data.type == 0 && !RoomModule.isNuclear(data.roomType)) {
                cusList.add(data);
            }
        }

        cusList.sort((o1, o2) -> o2.maxBv - o1.maxBv);
        int endIndex = 0;
        for (int i = 1; i < cusList.size(); i++) {
            if (cusList.get(i).maxBv == cusList.get(0).maxBv) {
                endIndex = i;
            } else {
                break;
            }
        }
        if (endIndex == 0 && cusList.size() > 0) {
            /** 如果玩家解锁的炮值>所有房间最大炮值，则进入房间炮值最大的房间 **/
            long deskId = cusList.get(endIndex).deskid;
            int cusType = cusList.get(endIndex).type;
            if (deskId == 0 && cusType == 0) {
                type = cusList.get(endIndex).roomType;
            }
            if (deskId != 0 && cusType == 0 && chooseSeat(kernel, reqid, playerid, deskId)) {
                return;
            }
        } else if (endIndex > 0) {
            /** 如果有多个房间炮值最大的房间，则优先在地心深渊中随机选择一个桌。若无地心深渊类型，则在其他房间随机选择一个 **/
            for (int i = 0; i <= endIndex; i++) {
                if (cusList.get(i).roomType == RoomType.ROOM_SUPREME_3.ordinal()
                        && cusList.get(i).deskid == 0 && cusList.get(i).type == 0) {
                    type = cusList.get(i).roomType;
                }
                if (cusList.get(i).deskid != 0 && cusList.get(i).type == 0
                        && cusList.get(i).roomType == RoomType.ROOM_SUPREME_3.ordinal()
                        && chooseSeat(kernel, reqid, playerid, cusList.get(i).deskid)) {
                    return;
                }
            }
            Random random = new Random();
            while (endIndex > 0 && cusList.size() > 0) {
                int index = random.nextInt(endIndex + 1);
                if (cusList.get(index).deskid == 0 && cusList.get(index).type == 0) {
                    type = cusList.get(index).roomType;
                }
                if (cusList.get(index).type == 0 && cusList.get(index).deskid != 0
                        && chooseSeat(kernel, reqid, playerid, cusList.get(index).deskid)) {
                    return;
                }
                cusList.remove(index);
                endIndex--;
            }

        }

        // 创建
        boolean created = false;
        for (Entry<Integer, CustomGameData> entry : m_mapCustomGameData.entrySet()) {
            if (entry.getValue().type == 1) {
                continue;
            }
            // m_setSupreme.contains(entry.getValue().roomType)
            long desk = entry.getValue().deskid;
            int _roomType = entry.getValue().roomType;
            if (desk == 0L && entry.getValue().minBv <= bv && m_setSupreme.contains(_roomType)
                    && !RoomModule.isNuclear(_roomType)) {
                // 分配Game创建桌子
                String min = "";
                int minVal = 9999999;
                Object[] games = kernel.getServersByType("game");
                for (int i = 0; i < games.length; ++i) {
                    String name = games[i].toString();
                    if (!mapGameSer.containsKey(name)) {
                        mapGameSer.put(name, 0);
                        min = name;
                        break;
                    } else {
                        if (mapGameSer.get(name) < minVal) {
                            minVal = mapGameSer.get(name);
                            min = name;
                        }
                    }
                }

                if (!min.isEmpty()) {
                    int deskid = entry.getKey();
                    ServerMsg.CreateCustomDesk.Builder cdesk = ServerMsg.CreateCustomDesk.newBuilder();
                    cdesk.setDeskid(deskid);
                    cdesk.setTotalPlay(m_mapCustomGameData.get(deskid).totalPlay);
                    cdesk.setTotalWin(m_mapCustomGameData.get(deskid).totalWin);
                    cdesk.setMinbv(m_mapCustomGameData.get(deskid).minBv);
                    cdesk.setMaxbv(m_mapCustomGameData.get(deskid).maxBv);
                    cdesk.setLevel(m_mapCustomGameData.get(deskid).level);
                    cdesk.setLimit(m_mapCustomGameData.get(deskid).enterLimit);
                    cdesk.setAutoKick(m_mapCustomGameData.get(deskid).autoKick);
                    cdesk.setDeskbg(m_deskbg);
                    cdesk.setRoomType(m_mapCustomGameData.get(deskid).roomType);
                    cdesk.setType(0);
                    kernel.sendServerMsg(min, ServerMsgDef.M2G_CREATE_CUSTOM_DESK.ordinal(), cdesk.build().toByteArray());
                    WaitCustomDesk wait = new WaitCustomDesk();
                    wait.playerid = playerid;
                    wait.reqid = reqid;
                    wait.deskid = deskid;
                    wait.seatid = -1;
                    m_waitCustom.add(wait);
                    created = true;
                    break;
                }
            }
        }

        if (!created) {
            ServerMsg.MatchRes.Builder build = ServerMsg.MatchRes.newBuilder();
            build.setRoomType(type);
            build.setDeskid(0L);
            build.setSeatid(-1);
            build.setPlayerid(playerid);
            kernel.responseServer(reqid, build.build().toByteArray());
        }
    }


    public void OnMatch(MatchKernel kernel, int reqid, byte[] data) throws InvalidProtocolBufferException {
        ServerMsg.Match match = ServerMsg.Match.parseFrom(data);
        int type = match.getRoomType();
        int deskType = 0;
        if (match.hasDeskType()) {
            deskType = match.getDeskType();
        }
        long playerid = match.getPlayerid();
        int uid = match.getUid();
        int gameid = -1;
        if (match.hasGameid()) {
            gameid = match.getGameid();
        }
        long curDeskId = 0l;
        if (match.hasCurDeskId()) {
            curDeskId = match.getCurDeskId();
        }
        //logger.info("OnMatch roomType: {}; uid: {}; playerId: {}; bulletVal: {}", type,uid,playerid,match.getBulletval());
        if (m_setSupreme.contains(type)) {
            //至尊选座
            MatchCustomGame(kernel, reqid, playerid, match.getBulletval(), type, uid);
            return;
        }
        if (mysteryLegendRoomSet.contains(type)) {
            // 秘境传说
            MatchMysteryLegend(kernel, reqid, playerid, type, match.getMysteryLegendRoomConfig());
            return;
        }
        int bulletVal = (match.hasBulletval() ? match.getBulletval() : -1);
        if (!CheckMatch1(kernel, type, playerid, reqid, gameid, bulletVal, deskType, curDeskId)) {
            // 通知其他服务器创建房间
            String min = "";
            int minVal = 9999999;
            Object[] games = kernel.getServersByType("game");
            for (int i = 0; i < games.length; ++i) {
                String name = games[i].toString();
                if (!mapGameSer.containsKey(name)) {
                    mapGameSer.put(name, 0);
                    min = name;
                    break;
                } else {
                    if (mapGameSer.get(name) < minVal) {
                        minVal = mapGameSer.get(name);
                        min = name;
                    }
                }
            }
            if (!min.isEmpty()) {
                ServerMsg.CreateDesk.Builder desk = ServerMsg.CreateDesk.newBuilder();
                desk.setType(type);
                if (gameid != -1) {
                    desk.setGameid(gameid);
                }
                desk.setBulletVal(bulletVal);
                desk.setDeskType(deskType);
                kernel.sendServerMsg(min, ServerMsgDef.MMSG_CREATE_DESK.ordinal(), desk.build().toByteArray());
                WaitEnterData enter = new WaitEnterData();
                enter.playerid = playerid;
                enter.reqid = reqid;
                enter.type = type;
                enter.gameid = gameid;
                enter.bulletVal = bulletVal;
                enter.deskType = deskType;
                m_waitEnter.add(enter);
            } else {
                ServerMsg.MatchRes.Builder build = ServerMsg.MatchRes.newBuilder();
                build.setRoomType(type);
                build.setDeskid(0l);
                build.setSeatid(-1);
                build.setPlayerid(playerid);
                kernel.responseServer(reqid, build.build().toByteArray());
            }
        }
    }

    void MatchMysteryLegend(MatchKernel kernel, int reqId, long playerId, int type,
                            ServerMsg.MysteryLegendRoomConfig mysteryLegendRoomConfig) {
        RoomData roomData = mapRooms.get(type);
        if (roomData != null) {
            // 复用空桌子
            for (Entry<Long, DeskData> desk : roomData.mapDesks.entrySet()) {
                DeskData deskData = desk.getValue();
                if (deskData.maxBv != mysteryLegendRoomConfig.getMaxBulletValue()
                        && deskData.minBv != mysteryLegendRoomConfig.getMinBulletValue()) {
                    continue;
                }
                for (int i = 0; i < deskData.seatCount; i++) {
                    SeatData seat = deskData.seats[i];
                    // 桌子有座位
                    if (seat.isEmpty(kernel.getServerTime())) {
                        ServerMsg.MysteryLegendMatchRes.Builder build = ServerMsg.MysteryLegendMatchRes.newBuilder();
                        build.setDeskId(deskData.objid);
                        build.setSeatId(i);
                        build.setRoomId(mysteryLegendRoomConfig.getId());
                        seat.state = SeatState.ORDER;
                        seat.orderTime = kernel.getServerTime();
                        kernel.responseServer(reqId, build.build().toByteArray());
                        return;
                    }
                }
            }
        }
        String min = null;
        int minVal = 9999999;
        Object[] games = kernel.getServersByType("game");
        for (Object game : games) {
            String name = game.toString();
            if (!mapGameSer.containsKey(name)) {
                mapGameSer.put(name, 0);
                min = name;
                break;
            } else {
                Integer val = mapGameSer.get(name);
                if (val < minVal) {
                    minVal = val;
                    min = name;
                }
            }
        }
        if (StringUtils.isEmpty(min)) {
            ServerMsg.MysteryLegendMatchRes.Builder build = ServerMsg.MysteryLegendMatchRes.newBuilder();
            build.setDeskId(0L);
            build.setSeatId(-1);
            kernel.responseServer(reqId, build.build().toByteArray());
            return;
        }
        ServerMsg.AddMysteryLegendDesk.Builder desk = ServerMsg.AddMysteryLegendDesk.newBuilder();
        desk.setRoomType(type);
        desk.setPlayerId(playerId);
        desk.setMaxBV(mysteryLegendRoomConfig.getMaxBulletValue());
        desk.setMinBv(mysteryLegendRoomConfig.getMinBulletValue());
        kernel.sendServerMsg(min, ServerMsgDef.M2G_CREATE_MYSTERY_LEGEND_DESK.ordinal(), desk.build().toByteArray());

        WaitMysteryLegendDesk wait = new WaitMysteryLegendDesk();
        wait.playerId = playerId;
        wait.reqId = reqId;
        wait.roomId = mysteryLegendRoomConfig.getId();
        waitMysteryLegendDesks.add(wait);
    }

    boolean CheckMatch1(MatchKernel kernel, int type, long playerid, int reqid, int gameid, int bulletVal,
                        int deskType, long curDeskId) {
        ServerMsg.MatchRes.Builder build = ServerMsg.MatchRes.newBuilder();
        build.setRoomType(type);
        build.setDeskid(0l);
        build.setSeatid(-1);
        build.setPlayerid(playerid);
        long now = kernel.getServerTime();
        boolean alloced = false;
        do {
            RoomData room = mapRooms.get(type);
            if (room == null) {
                break;
            }
            for (Entry<Long, DeskData> entry : room.mapDesks.entrySet()) {
                DeskData desk = entry.getValue();
                if (desk.gameid != gameid || desk.objid == curDeskId) {
                    continue;
                }
                for (int i = 0; i < desk.seatCount; i++) {
                    SeatData seatData = desk.seats[i];
                    if (seatData.isEmpty(now)) {
                        //火龙房间桌子设置最大、最小炮值 alter by 赵俊@20190516
                        boolean exist = m_mapDragonBvCfg.containsKey(deskType);
                        if (!RoomModule.isDragon(type) || (exist && desk.deskType == deskType) || (deskType == 0 && bulletVal >= desk.minBv && bulletVal <= desk.maxBv)) {
                            seatData.state = SeatState.ORDER;
                            seatData.orderTime = now;
                            build.setDeskid(desk.objid);
                            build.setSeatid(i);
                            alloced = true;
                            break;
                        }
                    }
                }
                if (alloced) {
                    break;
                }
            }
        } while (false);
        if (alloced) {
            kernel.responseServer(reqid, build.build().toByteArray());
            return true;
        }
        return false;
    }

    public void CheckWaitList(MatchKernel kernel) {
        for (int i = 0; i < m_waitEnter.size(); ) {
            WaitEnterData data = m_waitEnter.get(i);
            if (CheckMatch1(kernel, data.type, data.playerid, data.reqid, data.gameid, data.bulletVal, data.deskType, 0l)) {
                m_waitEnter.remove(i);
            } else {
                i++;
            }
        }
    }

    void CheckCustomList(MatchKernel kernel, int deskid) {
        Iterator<WaitCustomDesk> itr = m_waitCustom.iterator();
        while (itr.hasNext()) {
            WaitCustomDesk data = itr.next();
            if (data.deskid != -1 && data.deskid != deskid) {
                continue;
            }
            boolean matched = false;
            // 0成功，1桌子不存在，2等待，3座位被占用
            ServerMsg.SitDownRes.Builder build = ServerMsg.SitDownRes.newBuilder();
            do {
                CustomGameData gameData = m_mapCustomGameData.get(deskid);
                if (!mapDeskById.containsKey(gameData.deskid)) {
                    build.setCode(1);
                    break;
                }
                DeskData deskData = mapDeskById.get(gameData.deskid);
                if (deskData.seats.length <= data.seatid) {
                    build.setCode(3);
                    break;
                }
                long now = kernel.getServerTime();
                if (data.seatid != -1) {
                    matched = true;
                    if (deskData.seats[data.seatid].state == SeatState.FREE
                            || (deskData.seats[data.seatid].state == SeatState.ORDER
                            && now - deskData.seats[data.seatid].orderTime >= ORDER_MAX_TIME)) {
                        // sit down
                        build.setCode(0);
                        build.setDeskid(gameData.deskid);
                        build.setSeatid(data.seatid);
                        build.setPlayerid(data.playerid);
                        deskData.seats[data.seatid].state = SeatState.ORDER;
                        deskData.seats[data.seatid].orderTime = now;
                    } else {
                        build.setCode(3);
                    }
                } else {
                    for (int i = 0; i < deskData.seats.length; ++i) {
                        if (deskData.seats[i].state == SeatState.FREE || (deskData.seats[i].state == SeatState.ORDER
                                && now - deskData.seats[i].orderTime >= ORDER_MAX_TIME)) {
                            // sit down
                            build.setCode(0);
                            build.setDeskid(gameData.deskid);
                            build.setSeatid(i);
                            deskData.seats[i].state = SeatState.ORDER;
                            deskData.seats[i].orderTime = now;
                            matched = true;
                            break;
                        }
                    }
                }
            } while (false);
            if (matched) {
                kernel.responseServer(data.reqid, build.build().toByteArray());
                itr.remove();
            }
        }
    }

    void OnAllocRobot(MatchKernel kernel, int reqid, byte[] data) throws InvalidProtocolBufferException {
        ServerMsg.MatchAllocRobotRes.Builder build = ServerMsg.MatchAllocRobotRes.newBuilder();
        ServerMsg.MatchAllocRobot alloc = ServerMsg.MatchAllocRobot.parseFrom(data);
        long deskid = alloc.getDeskId();
        int type = alloc.getDeskType();
        build.setDeskId(deskid);
        build.setDeskType(type);
        build.setSeatId(-1);
        //logger.info("OnAllocRobot {} {}", deskid, type);
        do {
            if (!mapDeskById.containsKey(deskid)) {
                //logger.info("not contain desk");
                break;
            }
            if (!m_mapRobotByRoom.containsKey(type)) {
                //logger.info("not contain type");
                break;
            }
            List<RobotData> robots = m_mapRobotByRoom.get(type);
            long now = kernel.getServerTime();
            DeskData desk = mapDeskById.get(deskid);
            for (int i = 0; i < desk.seatCount; ++i) {
                if (desk.seats[i].state == SeatState.FREE || (desk.seats[i].state == SeatState.ORDER
                        && now - desk.seats[i].orderTime >= ORDER_MAX_TIME)) {
                    List<Integer> list = new ArrayList<>();
                    for (int j = 0; j < robots.size(); ++j) {
                        RobotData robot = robots.get(j);
                        if (!robot.open || robot.state != RobotState.FREE.ordinal()) {
                            continue;
                        }
                        list.add(j);
                    }
                    if (list.size() > 0) {
                        Random rand = new Random();
                        int index = rand.nextInt(list.size());
                        RobotData robot = robots.get(list.get(index));
                        robot.state = RobotState.INUSE.ordinal();
                        kernel.changeRobotState(robot.uid, robot.state);
                        robot.deskid = deskid;
                        desk.seats[i].state = SeatState.ORDER;
                        desk.seats[i].orderTime = now;

                        build.setUid(robot.uid);
                        build.setBulletLevel(robot.bulletLevel);
                        build.setLevel(robot.level);
                        build.setVipLevel(robot.vipLevel);
                        build.setDiamond(robot.diamond);
                        build.setGold(robot.gold);

                        String[] preName = {"vivo", "oppo", "jinli", "qihoo", "baidu", "xiaomi"};
                        int virsualid = (int) (Math.random() * 11000) + 170000;
                        String robotName = preName[(int) (Math.random() * preName.length)] + virsualid;
                        build.setName(robotName); // 随机昵称 robot.name
                        build.setSeatId(i);

                        logger.info("alloc a robot {}.{}.{}", robot.uid, robot.name, robotName);

                        for (Entry<String, Integer> entry : robot.mapItems.entrySet()) {
                            build.addItem(entry.getKey());
                            build.addCount(entry.getValue());
                        }
                    }

                    break;
                }
            }
        } while (false);

        kernel.responseServer(reqid, build.build().toByteArray());
    }

    void OnFreeRobot(MatchKernel kernel, int reqid, byte[] data) {

    }

    void OnReqDeskList(MatchKernel kernel, int reqid, byte[] data) {
        ServerMsg.DeskList.Builder deskList = ServerMsg.DeskList.newBuilder();
        deskList.setPwcfg(m_pwCfg);
        for (Entry<Integer, CustomGameData> entry : m_mapCustomGameData.entrySet()) {
            if (entry.getValue().bSwitch && entry.getValue().type == 0) {
                ServerMsg.DeskData.Builder deskData = ServerMsg.DeskData.newBuilder();
                deskData.setDeskid(entry.getValue().id);
                deskData.setMinbv(entry.getValue().minBv);
                deskData.setMaxbv(entry.getValue().maxBv);
                deskData.setLimit(entry.getValue().enterLimit);
                deskData.setRoomType(entry.getValue().roomType);
                deskData.setType(entry.getValue().type);
                long deskobj = entry.getValue().deskid;
                if (deskobj != 0L && mapDeskById.containsKey(deskobj)) {
                    DeskData desk = mapDeskById.get(deskobj);
                    for (int i = 0; i < desk.seatCount; ++i) {
                        if (desk.seats[i].state == SeatState.HAVE) {
                            deskData.addSeatid(i);
                            deskData.addUid(desk.seats[i].uid);
                            deskData.addVip(desk.seats[i].vip);
                            deskData.addName(desk.seats[i].name);
                        }
                    }
                }

                deskList.addDesks(deskData.build());
            }
        }

        kernel.responseServer(reqid, deskList.build().toByteArray());
    }

    void RobotStandup(MatchKernel kernel, RobotData robot) {
        long deskid = robot.deskid;
        int serid = ClassSet.getObjectSerID(deskid);
        ServerMsg.RobotStandup.Builder build = ServerMsg.RobotStandup.newBuilder();
        build.setDeskid(deskid);
        build.setUid(robot.uid);

        kernel.sendServerMsg(serid, ServerMsgDef.M2G_ROBOT_OFFLINE.ordinal(), build.build().toByteArray());
    }

    void UpdateRobot2Game(MatchKernel kernel, RobotData robot) {
        long deskid = robot.deskid;
        int serid = ClassSet.getObjectSerID(deskid);
        ServerMsg.UpdateRobot2Game.Builder build = ServerMsg.UpdateRobot2Game.newBuilder();
        build.setDeskid(deskid);
        build.setUid(robot.uid);
        build.setBulletLevel(robot.bulletLevel);
        build.setLevel(robot.level);
        build.setVipLevel(robot.vipLevel);
        build.setDiamond(robot.diamond);
        build.setGold(robot.gold);
        build.setName(robot.name);

        kernel.sendServerMsg(serid, ServerMsgDef.M2G_ROBOT_UPDATE.ordinal(), build.build().toByteArray());
    }

    void OnReqGetDeskData(MatchKernel kernel, int reqid, byte[] data) throws InvalidProtocolBufferException {
        ServerMsg.CustomDesk request = ServerMsg.CustomDesk.parseFrom(data);
        ServerMsg.CustomDeskData.Builder deskData = ServerMsg.CustomDeskData.newBuilder();
        CustomGameData customGameData = m_mapCustomGameData.get(request.getDeskId());
        if (customGameData == null) {
            kernel.responseServer(reqid, deskData.build().toByteArray());
            return;
        }
        deskData.setRoomType(customGameData.roomType);
        DeskData desk = mapDeskById.get(customGameData.deskid);
        if (desk == null) {
            kernel.responseServer(reqid, deskData.build().toByteArray());
            return;
        }
        for (int i = 0; i < 4; i++) {
            ServerMsg.CustomSeatData.Builder seatData = ServerMsg.CustomSeatData.newBuilder();
            seatData.setDeskId(desk.deskid);
            seatData.setSeatId(i + 1);
            SeatData seat = desk.seats[i];
            seatData.setUid(0);
            seatData.setTotalPlay(0l);
            seatData.setTotalWin(0l);
            if (seat != null && seat.state != SeatState.FREE) {
                seatData.setUid(seat.uid);
            }
            deskData.addSeatData(seatData);
        }
        kernel.responseServer(reqid, deskData.build().toByteArray());
    }

    // 定时更新总玩总赢数据
    void OnUpdatePlayWin(MatchKernel kernel, int serid, int msgid, byte[] data) throws InvalidProtocolBufferException {
        ServerMsg.StandUp standUp = ServerMsg.StandUp.parseFrom(data);
        long deskid = standUp.getDeskid();

        for (Entry<Integer, RoomData> entry : mapRooms.entrySet()) {
            if (entry.getValue().mapDesks.containsKey(deskid)) {
                DeskData desk = entry.getValue().mapDesks.get(deskid);
                // 至尊选座
                if (m_setSupreme.contains(entry.getKey()) && m_mapCustomGameData.containsKey(desk.deskid)) {
                    CustomGameData gameData = m_mapCustomGameData.get(desk.deskid);
                    // 存储总玩总赢
                    long totalp = standUp.getTotalplay();
                    long totalw = standUp.getTotalwin();
                    // 没有变化就不更新
                    if (totalp == gameData.totalPlay && totalw == gameData.totalWin) {
                        break;
                    }
                    gameData.totalPlay = totalp;
                    gameData.totalWin = totalw;
                    logger.info("update desk  objid {} {} totalPlay {} totalWin {}",desk.objid, desk.deskid, totalp, totalw);
                    //更新人数、总玩总赢
                    int online = gameData.online;
                    if (gameData.roomType == RoomType.ROOM_PERSONAL.ordinal()) {
                        SeatData[] seatData = desk.seats;
                        if (seatData.length > 0) {
//							online = seatData[0].uid;
                            online = 1;
                        }
                    }
                    kernel.updateOneOnline(desk.deskid, online, gameData.totalPlay, gameData.totalWin);
                }
                break;
            }
        }
    }

    void OnReqMysteryLegendDeskObjId(MatchKernel kernel, int reqId, byte[] data) {
        Map<String, Integer> map = JsonUtil.decodeToMap(new String(data), String.class, Integer.class);
        Objects.requireNonNull(map);
        int roomType = map.get("type");
        int minBV = map.get("minBV");
        int maxBV = map.get("maxBV");
        RoomData roomData = mapRooms.get(roomType);
        if (roomData == null) {
            return;
        }
        Map<Long, DeskData> mapDesks = roomData.mapDesks;
        List<Long> deskObjIds = mapDesks.entrySet().
                stream().
                filter(e -> e.getValue().maxBv == maxBV && e.getValue().minBv == minBV).
                map(Entry::getKey).
                collect(Collectors.toList());
        kernel.responseServer(reqId, JsonUtil.encodeToStr(deskObjIds).getBytes());

    }

    void OnDeleteMysteryLegendRoomConfig(MatchKernel kernel, int serId, int msgId, byte[] data) {
        Map<String, Integer> map = JsonUtil.decodeToMap(new String(data), String.class, Integer.class);
        Objects.requireNonNull(map);
        int roomType = map.get("type");
        int minBV = map.get("minBV");
        int maxBV = map.get("maxBV");
        RoomData roomData = mapRooms.get(roomType);
        if (roomData == null) {
            return;
        }
        Map<Long, DeskData> mapDesks = roomData.mapDesks;
        List<Long> deleteIds = mapDesks.
                keySet().
                stream().
                filter(type -> {
                    DeskData deskData = mapDesks.get(type);
                    return deskData.maxBv == maxBV && deskData.minBv == minBV;
                }).
                collect(Collectors.toList());
        deleteIds.forEach(mapDesks::remove);
        deleteIds.forEach(mapDeskById::remove);

    }
}
