package framework.game;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import framework.*;
import framework.Store.ParamData;
import framework.logic.GameLogic;
import framework.mybatis.domain.MojinRoomRecord;
import framework.mybatis.domain.PlayerDailyPlayData;
import framework.mybatis.service.AbstractService;
import framework.net.ClientMsgDef;
import framework.net.InnerMsgDef;
import framework.net.SendMessage;
import framework.net.http.HttpClientApi;
import framework.net.message.ClientMsg;
import framework.net.message.InnerMsg;
import framework.pub.PubData;
import framework.pub.PubUtils;
import io.netty.util.internal.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * 
 * 描述： 内核对象
 * 
 */
public class Kernel implements IKernel,PropertyKey {

	public enum PlayerListCols {
		COL_OBJECTID,
	}

	class heartBeatData {
		long target;
		String name;
	}

	class OtherSerPlayer {
		long objectid;
		String name;
		String front;
		String back;
	}

	class RoleData {
		int uid;
		String name;
		int headId;
		int proxyId;
	}

	public enum KickType {
		KICK, // 服务器主动踢
		FROZEN, // 后台冻结
		RELOGIN, // 顶号
		MAINTION, // 维护
		END
	}

	private static String cfgUrl = null;
	private final static Logger logger = LoggerFactory.getLogger(Kernel.class);
	private final Map<String, ILogicModule> modules = new HashMap<>();
	private final Map<String, Element> configs = new HashMap<>();
	private final Map<Integer, Set<MethodCallBackData>> customMsg = new HashMap<>();
	private final Map<Integer, MethodCallBackData> requestMsg = new HashMap<>();
	private final Map<String, MethodCallBackData> callBacks = new HashMap<>();
	private final Map<Integer, Long> playersByUid = new HashMap<>();
	private final Map<Integer, Set<Long>> playersByChannel = new HashMap<>();
	private final Map<String, Set<Integer>> uidByFront = new HashMap<>();
	private final Map<Integer, OtherSerPlayer> otherServerPlayers = new HashMap<>();
	private final List<String> listPreLoad = new ArrayList<>();
	private final List<Long> listNpcs = new LinkedList<>();
	private final Map<String, Long> listPreLoadObj = new HashMap<>();
	private final ClassSet classSet = new ClassSet(this);
	private GameLogic gameLogic;
	private MethodCallBackData disconnectCb = null;

	private final Map<Integer, Set<MethodCallBackData>> serverMsg = new HashMap<>();

	private final Map<Integer, IRequestCallback> mapReqs = new HashMap<>();
	private final Map<Integer, RecReqData> serverResponses = new HashMap<>();
	private final Map<Integer, MethodCallBackData> serverRequest = new HashMap<>();

	private final Map<Integer, RoleData> uid2Role = new HashMap<>();

	private final Map<Integer, String> headers = new HashMap<>();
	private int reqId = 0;
	private GameObject gameWorld = null;
	private boolean mainFlag = true;
	private final Map<Integer, MethodCallBackData> stopEvent = new HashMap<>();
	private int stopOrder = 1;
	private final List<GameDesk> tempDesks = new ArrayList<>();
	private long preCheckTime = 0L;

	public boolean onInit(GameLogic logic) {
		String config = SystemConfigData.getConfig("user.dir","");
		cfgUrl = "file:" + config + File.separator + "config/";
		gameLogic = logic;
		// init classes
		classSet.initRootClass("Player", GameObjectType.GOTYPE_PLAYER);
		classSet.initRootClass("Item", GameObjectType.GOTYPE_ITEM);
		classSet.initRootClass("Room", GameObjectType.GOTYPE_ROOM);
		classSet.initRootClass("Desk", GameObjectType.GOTYPE_DESK);
		classSet.initRootClass("Npc", GameObjectType.GOTYPE_NPC);
		classSet.initRootClass("Help", GameObjectType.GOTYPE_HELP);
		classSet.initRootClass("Container", GameObjectType.GOTYPE_CONTAINER);
		classSet.initRootClass("World", GameObjectType.GOTYPE_HELP);
		// load modules
		try {
			loadModules("game.modules");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		//logger.warn("Modules count: {}", m_mapModules.size());
		// init modules
		for (Entry<String, ILogicModule> entry : modules.entrySet()) {
			if (!entry.getValue().onInit(this)) {
				logger.error("Init module [{}] failed.", entry.getKey());
				return false;
			}
		}
		// create classes
		classSet.createClasses();
		BaseServer server = gameLogic.getServer();
		server.addForwardMsgListener(this, ClientMsgDef.CLIENT_CUSTOM.ordinal(), "onRecCustomMsg");
		server.addForwardMsgListener(this, ClientMsgDef.CLIENT_REQUEST.ordinal(), "onRecRequestMsg");
		server.addRequestListener(this, InnerMsgDef.INNER_MSG_CHANGE_SERVER.ordinal(), "OnReqChangeSer");
		server.addNetMsgListener(this, InnerMsgDef.INNER_MSG_CHANGE_RESULT.ordinal(), "onRecChangeSerResult");
		server.addNetMsgListener(this, InnerMsgDef.INNER_MSG_ADD_PUBSPACE.ordinal(), "onRecAddPubSpace");
		server.addNetMsgListener(this, InnerMsgDef.INNER_MSG_ADD_PUBDATA.ordinal(), "onRecAddPubData");
		server.addNetMsgListener(this, InnerMsgDef.INNER_MSG_ADD_PUBPRO.ordinal(), "onRecAddPubPro");
		server.addNetMsgListener(this, InnerMsgDef.INNER_MSG_ADD_PUBREC.ordinal(), "onRecAddPubRec");
		server.addNetMsgListener(this, InnerMsgDef.INNER_MSG_DEL_PUBSPACE.ordinal(), "onRecDelPubSpace");
		server.addNetMsgListener(this, InnerMsgDef.INNER_MSG_DEL_PUBDATA.ordinal(), "onRecDelPubData");
		server.addNetMsgListener(this, InnerMsgDef.INNER_MSG_DEL_PUBPRO.ordinal(), "onRecDelPubPro");
		server.addNetMsgListener(this, InnerMsgDef.INNER_MSG_DEL_PUBREC.ordinal(), "onRecDelPubRec");
		server.addNetMsgListener(this, InnerMsgDef.INNER_MSG_SET_PUBPRO.ordinal(), "onRecSetPubPro");
		server.addNetMsgListener(this, InnerMsgDef.INNER_MSG_SET_PUBREC.ordinal(), "onRecSetPubRec");
		server.addNetMsgListener(this, InnerMsgDef.INNER_MSG_ADDR_PUBREC.ordinal(), "onRecAddPubRecRow");
		server.addNetMsgListener(this, InnerMsgDef.INNER_MSG_SET_COL_TYPE.ordinal(), "onRecSetPubRecColType");
		server.addNetMsgListener(this, InnerMsgDef.INNER_MSG_DELR_PUBREC.ordinal(), "onRecDelPubRecRow");
		server.addNetMsgListener(this, InnerMsgDef.INNER_MSG_CLEAR_PUBREC.ordinal(), "onRecClearPubRec");
		server.addNetMsgListener(this, InnerMsgDef.INNER_MSG_ADD_BLACKLIST.ordinal(), "onRecAddBackList");
		server.addNetMsgListener(this, InnerMsgDef.INNER_MSG_ACCOUNT_STATUS.ordinal(), "OnAccountStatus");
		//declareHeartBeat("HB_store_player", this, "onStorePlayer");
		return true;
	}

	public IGameObject getWorld() {
		return gameWorld;
	}

	public void addRoleList(int uid, String name, int headId,int proxyId) {
		RoleData role = uid2Role.get(uid);
		if (role == null) {
			role = new RoleData();
			role.uid = uid;
			uid2Role.put(uid, role);
		}
		role.headId = headId;
		role.name = name;
		role.proxyId = proxyId;
	}

	public void addHead(int headid, String url) {
		if (headers.containsKey(headid)) {
			return;
		}
		headers.put(headid, url);
	}

	public String getUserName(int uid) {
		RoleData roleData = uid2Role.get(uid);
		if (roleData != null) {
			return roleData.name;
		}
		return null;
	}
	public int getProxyId(int uid) {
		RoleData roleData = uid2Role.get(uid);
		if (roleData != null) {
			return roleData.proxyId;
		}
		return -1;
	}
	public int getUserHeadid(int uid) {
		RoleData roleData = uid2Role.get(uid);
		if (roleData != null) {
			return roleData.headId;
		}
		return -1;
	}

	public String getHeadUrl(int headId) {
		return headers.get(headId);
	}

	public void loadModules(String packageName) throws Exception {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		String path = packageName.replace('.', '/');
		Enumeration<URL> resources = classLoader.getResources(path);
		while (resources.hasMoreElements()) {
			URL resource = resources.nextElement();
			String protocol = resource.getProtocol();

			if ("file".equals(protocol)) {
				File directory = new File(URLDecoder.decode(resource.getFile(), "UTF-8"));
				checkModule(directory, packageName);
			} else {
				JarFile jar = ((JarURLConnection) resource.openConnection()).getJarFile();
				Enumeration<JarEntry> entries = jar.entries();
				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					String name = entry.getName();
					if (name.charAt(0) == '/') {
						name = name.substring(1);
					}
					if (name.startsWith(path)) {
						int idx = name.lastIndexOf('/');
						if (idx != -1) {
							packageName = name.substring(0, idx).replace('/', '.');
						}

						if (name.endsWith(".class") && !entry.isDirectory()) {
							String className = name.substring(packageName.length() + 1, name.length() - 6);
							try {

								Class<?> c = Class.forName(packageName + '.' + className);
								if (ILogicModule.class.isAssignableFrom(c)) {
									String[] n = c.getName().split("\\.");

									ILogicModule module = null;
									try {
										Constructor<?> cons = c.getConstructor(IKernel.class);
										module = (ILogicModule) cons.newInstance(this);
									} catch (NoSuchMethodException e) {
										module = (ILogicModule) c.newInstance();
									}
									addModule(n[n.length - 1], module);
								}

							} catch (ClassNotFoundException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
	}

	public void checkModule(File directory, String packageName) throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, SecurityException, IllegalArgumentException, InvocationTargetException {
		if (!directory.exists()) {
			logger.info("CheckModule !directory.exists() {}", directory.getName());
			return;
		}

		File[] files = directory.listFiles();

		for (File file : files) {
			if (file.isDirectory()) {
				assert !file.getName().contains(".");
				checkModule(file, packageName + "." + file.getName());
			} else if (file.getName().endsWith(".class")) {
				Class<?> c = Class
						.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6));

				if (ILogicModule.class.isAssignableFrom(c)) {
					String[] n = c.getName().split("\\.");

					ILogicModule module = null;
					try {
						Constructor<?> cons = c.getConstructor(IKernel.class);
						module = (ILogicModule) cons.newInstance(this);
					} catch (NoSuchMethodException e) {
						module = (ILogicModule) c.newInstance();
					}
					addModule(n[n.length - 1], module);
				}
			}
		}
	}

	public void onReady() {
		gameWorld = classSet.createWorld();
		for (String cfg : listPreLoad) {
			IGameObject obj = createObjectByConfig(cfg, null);
			if (obj == null) {
				logger.error("Preload object {} failed.", cfg);
			} else {
				listPreLoadObj.put(cfg, obj.getObjectID());
			}
		}
	}

	public void onDestroy() {
		// destroy modules
		for (Entry<String, ILogicModule> entry : modules.entrySet()) {
			entry.getValue().onDestroy();
		}
	}

	public int getSerID() {
		return gameLogic.getServer().getSerID();
	}

	public String getSerName() {
		return gameLogic.getServer().getSerName();
	}

	public ClassSet getClassSet() {
		return classSet;
	}

	public boolean addClass(String name, String parent) {
		return classSet.addClass(name, parent);
	}

	public void declareProperty(String script, String name, ValueType type, boolean pubVisible, boolean priVisible,
								boolean save) {
		GameObject template = classSet.getTemplate(script);
		if (template == null) {
			logger.error("DeclareProperty failed. can't find script [{}]", script);
			return;
		}
		template.declareProperty(name, type, pubVisible, priVisible, save);
	}

	@Override
	public void setVisible(String script, String name, boolean pubVisible, boolean priVisible, boolean save) {
		GameObject template = classSet.getTemplate(script);
		if (template == null) {
			logger.error("SetVisible failed. can't find script [{}]", script);
			return;
		}
		template.setVisible(name, pubVisible, priVisible, save);
	}

	public IRecord declareRecord(String script, String name, int cols, int maxRow, boolean pubVisible,
								 boolean priVisible, boolean save) {
		GameObject template = classSet.getTemplate(script);
		if (template == null) {
			logger.error("DeclareRecord failed. can't find script [{}]", script);
			return null;
		}
		return template.declareRecord(name, cols, maxRow, pubVisible, priVisible, save);
	}

	public boolean declareHeartBeat(String name, Object listener, String methodName) {
		if (callBacks.containsKey(name)) {
			return false;
		}
		MethodCallBackData data = new MethodCallBackData();
		data.listener = listener;
		data.access = MethodAccessCache.tryToGet(listener.getClass());
		data.methodIndex = data.access.getIndex(methodName, IKernel.class, IGameObject.class);
		callBacks.put(name, data);
		return true;
	}

	private void addConfig(String name, Element foo) {
		if (configs.containsKey(name)) {
			logger.warn("config name [{}] is exist, please check and rename.", name);
			return;
		}
		configs.put(name, foo);
	}

	public void preLoadConfig(String path) {
		try {
			String config = cfgUrl + path;
			URL url = new URL(config);
			SAXReader reader = new SAXReader();
			Document doc = reader.read(url);
			Element root = doc.getRootElement();
			for (Iterator<?> i = root.elementIterator("item"); i.hasNext();) {
				Element foo = (Element) i.next();
				String configId = foo.attributeValue("Id");
				addConfig(configId, foo);
			}
		} catch (Exception e) {
			logger.error("load config [{}] failed.", path);
			e.printStackTrace();
		}
	}

	private void updateConfig(String name, Element foo) {
		configs.put(name, foo);
	}

	public boolean updateConfig(String path) {
		try {
			String config = cfgUrl + path;
			URL url = new URL(config);
			SAXReader reader = new SAXReader();
			Document doc = reader.read(url);
			Element root = doc.getRootElement();
			Element foo  = null;
			for (Iterator<?> i = root.elementIterator("item"); i.hasNext();) {
				foo = (Element) i.next();
				String script = foo.attributeValue("Script");
				if (StringUtil.isNullOrEmpty(script)) {
					continue;
				}
				String configid = foo.attributeValue("Id");
				updateConfig(configid, foo);
			}
			classSet.runEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", path);
			return true;
		} catch (Exception e) {
			logger.error("load config [{}] failed.", path);
			return false;
		}
	}

	public void preLoadObject(String cfg) {
		listPreLoad.add(cfg);
	}

	public GameObject createObjectByScript(String script) {
		return classSet.createObjectByScript(script);
	}

	public GameObject createObjectByConfig(String cfg, IGameObject parent) {
		if (!configs.containsKey(cfg)) {
			logger.error("can't create object which config is [{}]", cfg);
			return null;
		}
		Element foo = configs.get(cfg);
		String script = foo.attributeValue("Script");
		GameObject obj = classSet.createObjectByScript(script);
		if (obj == null) {
			logger.error("can't create object which script is [{}] and config is [{}]", script, cfg);
			return null;
		}
		obj.loadFromConfig(foo);
		obj.onLoad();
		if (parent != null) {
			parent.addChild(obj);
		}
		return obj;
	}

	public GameObject createObjectByConfig(String cfg, IGameObject parent, Object... params) {
		if (!configs.containsKey(cfg)) {
			logger.error("can't create object which config is [{}]", cfg);
			return null;
		}
		Element foo = configs.get(cfg);
		String script = foo.attributeValue("Script");
		GameObject obj = classSet.createObjectByScript(script);
		if (obj == null) {
			logger.error("can't create object which script is [{}] and config is [{}]", script, cfg);
			return null;
		}

		obj.loadFromConfig(foo);
		if (params.length > 0 && params.length % 2 == 0) {
			for (int i = 0; i < params.length / 2; ++i) {
				String key = params[i * 2].toString();
				Object val = params[i * 2 + 1];
				obj.setProperty(key, val);
			}
		}
		obj.onLoad();

		if (parent != null) {
			parent.addChild(obj);
		}
		return obj;
	}

	public boolean checkCfgLegal(String cfgId) {
		return configs.containsKey(cfgId);
	}

	public String getCfgProperty(String cfgId, String name) {
		Element foo = configs.get(cfgId);
		if (foo == null){
			return null;
		}
		return foo.attributeValue(name);
	}

	public <T> T getCfgDetailProperty(String cfgId, String name, Object defaultValue) {
		Element foo = configs.get(cfgId);
		Object result = defaultValue;
		if (foo != null) {
			String value = foo.attributeValue(name);
			if (value != null) {
				Class<?> type = defaultValue.getClass();
				if (type == String.class){
					result = value;
				}else if (type == Boolean.class || type == boolean.class) {
					value = value.toLowerCase();
					result = value.equals("1") || value.equals("true");
				} else {
					result = JsonUtil.decodeToObj(value, type);
				}
			}
		}
		return (T)result;
	}

	public GameObject innerCreateObjectByConfig(String cfg, IGameObject parent) {
		if (!configs.containsKey(cfg)) {
			return null;
		}
		Element foo = configs.get(cfg);
		String script = foo.attributeValue("Script");
		GameObject obj = classSet.createObjectByScript(script);
		if (obj == null) {
			logger.error("can't create object which script is [{}] and config is [{}]", script, cfg);
			return null;
		}

		obj.loadFromConfig(foo);

		if (parent != null) {
			parent.addChild(obj);
		}
		return obj;
	}

	public GameObject getPreloadObject(String id) {
		if (listPreLoadObj.containsKey(id)) {
			return getGameObject(listPreLoadObj.get(id));
		}
		return null;
	}

	public GameObject getGameObject(long objectId) {
		if (objectId == 0){
			return null;
		}
		return classSet.getGameObject(objectId);
	}

	public void destroyGameObject(IGameObject object) {
		classSet.destroyGameObject(object);
	}

	public void addPlayer(int uid, IGameObject player) {
		long objId = player.getObjectID();
		playersByUid.put(uid, objId);
		int channel = player.getInt(PLAYER_PROPERTY_CHANNEL);
		if (playersByChannel.containsKey(channel)) {
			playersByChannel.get(channel).add(objId);
		} else {
			Set<Long> temp = new HashSet<>();
			temp.add(objId);
			playersByChannel.put(channel, temp);
		}
		String front = player.getString(PLAYER_PROPERTY_FRONTSER);
		if (uidByFront.containsKey(front)) {
			uidByFront.get(front).add(uid);
		} else {
			Set<Integer> temp = new HashSet<Integer>();
			temp.add(uid);
			uidByFront.put(front, temp);
		}
		addRoleList(uid, player.getString(PLAYER_PROPERTY_NAME),player.getInt(PLAYER_PROPERTY_HEADID),player.getInt(PLAYER_PROPERTY_PROPERTY_PROXY_ID));
		addHead(player.getInt(PLAYER_PROPERTY_HEADID), player.getString(PLAYER_PROPERTY_HEAD));
		if (isMain()) {// 主服务器才可以保存数据
			player.setProperty(PLAYER_PROPERTY_LAST_SAVE,System.currentTimeMillis());
		}
	}

	public GamePlayer getPlayer(int uid,boolean findList) {
		Long objId = playersByUid.get(uid);
		if (objId == null){
			if (!findList){
				return null;
			}
			GamePlayer target = null;
			for (int i = 0; i < classSet.allObjs.size() ; i++) {
				GameObject gameObject = classSet.allObjs.get(i);
				if (gameObject instanceof GamePlayer){
					GamePlayer gamePlayer = (GamePlayer) gameObject;
					int _uid = gamePlayer.getInt(PLAYER_PROPERTY_UID);
					if (_uid == uid){
						target = gamePlayer;
						break;
					}
				}
			}
			if (target != null && target.loadDataError){
				destroyGameObject(target);
				target = null;
			}
			return target;
		}
		return (GamePlayer) classSet.getGameObject(objId);
	}

	public GamePlayer getPlayer(int uid) {
		return getPlayer(uid,false);
	}

	@Override
	public List<IGameObject> getAllOnlinePlayer() {
		return playersByUid.values().stream().map(objId-> classSet.getGameObject(objId)).collect(Collectors.toList());
	}

	public void deletePlayer(GamePlayer player, boolean flag) {
		int uid = player.getInt(PLAYER_PROPERTY_UID);
		String front = player.getString(PLAYER_PROPERTY_FRONTSER);
		Set<Integer> ids = uidByFront.get(front);
		if (ids != null) {
			ids.remove(uid);
		}
		int channel = player.getInt(PLAYER_PROPERTY_CHANNEL);
		Set<Long> temp = playersByChannel.get(channel);
		if (temp != null) {
			temp.remove(player.getObjectID());
		}
		playersByUid.remove(uid);
		if (flag){
			gameLogic.delPlayer(uid);
		}
	}

	public void regEvent(KernelEvent event, String script, Object listener, String methodName) {
		classSet.regEvent(event, script, listener, methodName);
	}

	public void onRecCustomMsg(int uid, byte[] bytes) {
		GamePlayer player = getPlayer(uid);
		if (player == null) {
//			logger.info("onRecCustomMsg player == null {}", uid);
			return;
		}
		if (player.getState() != PlayerState.STATE_NORMAL && player.getState() != PlayerState.STATE_CHANGESCENE) {
			logger.info("onRecCustomMsg player.GetState() != State.STATE_NORMAL {} {}", uid, player.getState());
			return;
		}
		ClientMsg.CustomMsg customMsg = null;
		try {
			customMsg = ClientMsg.CustomMsg.parseFrom(bytes);
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			return;
		}
		int msgid = customMsg.getMsgid();
		byte[] data = null;
		if (customMsg.getData() != null) {
			data = customMsg.getData().toByteArray();
		}
		Set<MethodCallBackData> cbs = this.customMsg.get(msgid);
		if (cbs == null) {
			logger.info("onRecCustomMsg cbs == null {} {}", uid, msgid);
			return;
		}
		//StartPerf("CustomMsg_" + msgid);
		for (MethodCallBackData cb : cbs) {
			cb.access.invoke(cb.listener, cb.methodIndex, this, player, msgid, data);
		}
		//OverPerf("CustomMsg_" + msgid);
	}

	public void onRecRequestMsg(int uid, byte[] bytes) throws InvalidProtocolBufferException {
		GamePlayer player = getPlayer(uid);
		if (player == null) {
			return;
		}
		if (player.getState() != PlayerState.STATE_NORMAL && player.getState() != PlayerState.STATE_CHANGESCENE) {
			logger.info("onRecRequestMsg player.GetState() != State.STATE_NORMAL {} {}", uid, player.getState());
			return;
		}
		ClientMsg.RequestMsg requestMsg = ClientMsg.RequestMsg.parseFrom(bytes);
		int msgid = requestMsg.getMsgid();
		int reqid = requestMsg.getReqid();
		byte[] data = null;
		if (requestMsg.getData() != null) {
			data = requestMsg.getData().toByteArray();
		}

		MethodCallBackData cb = this.requestMsg.get(msgid);
		if (cb == null) {
			return;
		}
		//StartPerf("RequestMsg_" + msgid);
		cb.access.invoke(cb.listener, cb.methodIndex, this, player, msgid, reqid, data);
		//OverPerf("RequestMsg_" + msgid);
	}

	public void response(IGameObject player, int reqid, byte[] data) {
		ClientMsg.ResponseMsg.Builder response = ClientMsg.ResponseMsg.newBuilder();
		response.setReqid(reqid);
		response.setData(ByteString.copyFrom(data));

		innerSendMessage(player, ClientMsgDef.CLIENT_RESPONSE.ordinal(), response.build().toByteArray());
	}

    public void response(IGameObject player, int reqid, JSONObject jsonObject){
        ClientMsg.ResponseMsg.Builder response = ClientMsg.ResponseMsg.newBuilder();
        response.setReqid(reqid);
        byte[] byteArray = InnerMsg.String.newBuilder().setValue(jsonObject.toJSONString()).build().toByteArray();
        response.setData(ByteString.copyFrom(byteArray));

        innerSendMessage(player, ClientMsgDef.CLIENT_RESPONSE.ordinal(), response.build().toByteArray());
    }

	public void regClientMessage(int msgid, Object listener, String methodName) {
		MethodCallBackData data = new MethodCallBackData();
		data.listener = listener;
		// data.access = MethodAccess.get(listener.getClass());
		data.access = MethodAccessCache.tryToGet(listener.getClass());
		data.methodIndex = data.access.getIndex(methodName, IKernel.class, IGameObject.class, int.class, byte[].class);

		if (!customMsg.containsKey(msgid)) {
			Set<MethodCallBackData> temp = new HashSet<MethodCallBackData>();
			temp.add(data);
			customMsg.put(msgid, temp);
		} else {
			customMsg.get(msgid).add(data);
		}
	}

	public void regRequestMessage(int msgid, Object listener, String methodName) {
		if (requestMsg.containsKey(msgid)) {
			logger.error("Request handle for msg[{}] is exist [{} - {}].", msgid, listener.getClass().getName(), requestMsg.get(msgid).listener.getClass().getName());
			return;
		}

		MethodCallBackData data = new MethodCallBackData();
		data.listener = listener;
		// data.access = MethodAccess.get(listener.getClass());
		data.access = MethodAccessCache.tryToGet(listener.getClass());
		data.methodIndex = data.access.getIndex(methodName, IKernel.class, IGameObject.class, int.class, int.class, byte[].class);
		requestMsg.put(msgid, data);
	}

	public void regCommand(int cmdid, String script, Object listener, String methodName) {
		classSet.regCommand(script, cmdid, listener, methodName);
	}

	public void innerSendMessage(IGameObject player, int msgid, byte[] data) {
		if (player == null || player.getType() != GameObjectType.GOTYPE_PLAYER) {
			return;
		}
		int uid = (int) player.getProperty(PLAYER_PROPERTY_UID);
		String gate = player.getString(PLAYER_PROPERTY_FRONTSER);
		gameLogic.sendMsgToClient(uid, msgid, gate, data);
	}

	public void innerBroadCastByRoom(IGameObject room, int msgid, byte[] data) {
		if (room.getType() != GameObjectType.GOTYPE_ROOM) {
			return;
		}
		Map<String, Set<Integer>> players = new HashMap<>();
		int cap = room.getCapacity();
		for (int i = 0; i < cap; ++i) {
			IGameObject desk = room.getChild(i);
			if (desk == null) {
				continue;
			}

			int count = desk.getSeatCount();
			for (int j = 0; j < count; ++j) {
				IGameObject child = desk.getSeatObject(j);
				if (child != null) {
					String front = child.getString(PLAYER_PROPERTY_FRONTSER);
					int uid = child.getInt(PLAYER_PROPERTY_UID);

					if (players.containsKey(front)) {
						players.get(front).add(uid);
					} else {
						Set<Integer> temp = new HashSet<>();
						temp.add(uid);
						players.put(front, temp);
					}
				}
			}
		}
		innerBroadCast(players, msgid, data);
	}

	public void innerBroadCastByDesk(IGameObject desk, int msgid, byte[] data) {
		if (desk.getType() != GameObjectType.GOTYPE_DESK) {
			return;
		}
		Map<String, Set<Integer>> players = new HashMap<>();
		int cap = desk.getSeatCount();
		for (int i = 0; i < cap; ++i) {
			IGameObject child = desk.getSeatObject(i);
			if (child != null) {
				String front = child.getString(PLAYER_PROPERTY_FRONTSER);
				int uid = child.getInt(PLAYER_PROPERTY_UID);

				if (players.containsKey(front)) {
					players.get(front).add(uid);
				} else {
					Set<Integer> temp = new HashSet<>();
					temp.add(uid);
					players.put(front, temp);
				}
			}
		}
		innerBroadCast(players, msgid, data);
	}

	public void innerBroadCastByKen(IGameObject player, int msgid, byte[] data) {
		IGameObject desk = getGameObject(player.getLong(PLAYER_PROPERTY_DESKID));
		if (desk != null) {
			innerBroadCastByDesk(desk, msgid, data);
		} else {
			innerSendMessage(player, msgid, data);
		}
	}

	public void innerBroadCastByKenWithOutSelf(IGameObject player, int msgid, byte[] data) {
		IGameObject desk = getGameObject(player.getLong(PLAYER_PROPERTY_DESKID));
		if (desk != null && desk.getType() == GameObjectType.GOTYPE_DESK) {
			Map<String, Set<Integer>> players = new HashMap<>();
			int cap = desk.getSeatCount();
			for (int i = 0; i < cap; ++i) {
				IGameObject child = desk.getSeatObject(i);
				if (child != null && child.getType() == GameObjectType.GOTYPE_PLAYER && child != player) {
					String front = child.getString(PLAYER_PROPERTY_FRONTSER);
					int uid = child.getInt(PLAYER_PROPERTY_UID);

					if (players.containsKey(front)) {
						players.get(front).add(uid);
					} else {
						Set<Integer> temp = new HashSet<>();
						temp.add(uid);
						players.put(front, temp);
					}
				}
			}
			if (players.size() > 0) {
				innerBroadCast(players, msgid, data);
			}
		}
	}

	public void innerBroadCastByChannel(int channel, int msgid, byte[] data) {
		if (!playersByChannel.containsKey(channel)) {
			return;
		}

		Map<String, Set<Integer>> players = new HashMap<>();
		for (long objid : playersByChannel.get(channel)) {
			IGameObject player = getGameObject(objid);
			if (player == null) {
				continue;
			}

			String front = player.getString(PLAYER_PROPERTY_FRONTSER);
			int uid = player.getInt(PLAYER_PROPERTY_UID);

			if (players.containsKey(front)) {
				players.get(front).add(uid);
			} else {
				Set<Integer> temp = new HashSet<>();
				temp.add(uid);
				players.put(front, temp);
			}
		}

		innerBroadCast(players, msgid, data);
	}

	public void innerBroadCastCurServer(int msgid, byte[] data) {
		innerBroadCast(uidByFront, msgid, data);
	}

	public void innerBroadCast(Map<String, Set<Integer>> players, int msgid, byte[] data) {
		InnerMsg.BroadCastToClients.Builder build = InnerMsg.BroadCastToClients.newBuilder();
		build.setMsgid(msgid);
		build.setData(ByteString.copyFrom(data));

		for (Entry<String, Set<Integer>> entry : players.entrySet()) {
			if (entry.getValue().size() <= 0) {
				continue;
			}

			for (int uid : entry.getValue()) {
				build.addUids(uid);
			}

			gameLogic.getServer().sendMsgToServer(entry.getKey(), InnerMsgDef.INNER_MSG_BROADCAST.ordinal(),
					build.build().toByteArray());
		}
	}

	public void innerBroadCastAllServer(int msgid, byte[] data) {
		InnerMsg.BroadCastToAllClients.Builder build = InnerMsg.BroadCastToAllClients.newBuilder();
		build.setMsgid(msgid);
		build.setData(ByteString.copyFrom(data));

		gameLogic.getServer().broadToServer("gate",
				new SendMessage(InnerMsgDef.INNER_MSG_BROADCAST_ALL.ordinal(), build.build().toByteArray()));
	}

	public void sendMessage(IGameObject player, int msgid, byte[] data) {
		if (player == null || player.getType() != GameObjectType.GOTYPE_PLAYER) {
			return;
		}
		ClientMsg.CustomMsg.Builder builder = ClientMsg.CustomMsg.newBuilder();
		builder.setMsgid(msgid);
		if (data != null) {
			builder.setData(ByteString.copyFrom(data));
		}
		int uid = player.getInt(PLAYER_PROPERTY_UID);
		String gate = player.getString(PLAYER_PROPERTY_FRONTSER);
		gameLogic.sendCustomMsgToClient(uid, gate, builder.build().toByteArray());
	}

	public void broadCastByRoom(IGameObject room, int msgid, byte[] data) {
		if (room.getType() != GameObjectType.GOTYPE_ROOM) {
			return;
		}
		Map<String, Set<Integer>> players = new HashMap<>();
		int cap = room.getCapacity();
		for (int i = 0; i < cap; ++i) {
			IGameObject desk = room.getChild(i);
			if (desk == null) {
				continue;
			}

			int count = desk.getSeatCount();
			for (int j = 0; j < count; ++j) {
				IGameObject child = desk.getSeatObject(j);
				if (child != null && child.getState() == PlayerState.STATE_NORMAL) {
					String front = child.getString(PLAYER_PROPERTY_FRONTSER);
					int uid = child.getInt(PLAYER_PROPERTY_UID);

					if (players.containsKey(front)) {
						players.get(front).add(uid);
					} else {
						Set<Integer> temp = new HashSet<>();
						temp.add(uid);
						players.put(front, temp);
					}
				}
			}
		}
		broadCast(players, msgid, data);
	}

	public void broadCastByDesk(IGameObject desk, int msgid, byte[] data) {
		if (desk.getType() != GameObjectType.GOTYPE_DESK) {
			return;
		}
		Map<String, Set<Integer>> players = new HashMap<>();
		int cap = desk.getSeatCount();
		for (int i = 0; i < cap; ++i) {
			IGameObject child = desk.getSeatObject(i);
			if (child != null && child.getState() == PlayerState.STATE_NORMAL) {
				String front = child.getString(PLAYER_PROPERTY_FRONTSER);
				int uid = child.getInt(PLAYER_PROPERTY_UID);

				if (players.containsKey(front)) {
					players.get(front).add(uid);
				} else {
					Set<Integer> temp = new HashSet<>();
					temp.add(uid);
					players.put(front, temp);
				}
			}
		}
		broadCast(players, msgid, data);
	}

	public void broadCastByKen(IGameObject player, int msgid, byte[] data) {
		IGameObject desk = getGameObject(player.getLong(PLAYER_PROPERTY_DESKID));
		if (desk != null) {
			broadCastByDesk(desk, msgid, data);
		} else {
			sendMessage(player, msgid, data);
		}
	}

	public void broadCastByKenWithOutSelf(IGameObject player, int msgid, byte[] data) {
		IGameObject desk = getGameObject(player.getLong(PLAYER_PROPERTY_DESKID));
		if (desk != null && desk.getType() == GameObjectType.GOTYPE_DESK) {
			Map<String, Set<Integer>> players = new HashMap<>();
			int cap = desk.getSeatCount();
			for (int i = 0; i < cap; ++i) {
				IGameObject child = desk.getSeatObject(i);
				if (child != null && child != player && child.getState() == PlayerState.STATE_NORMAL) {
					String front = child.getString(PLAYER_PROPERTY_FRONTSER);
					int uid      = child.getInt(PLAYER_PROPERTY_UID);
					if (players.containsKey(front)) {
						players.get(front).add(uid);
					} else {
						Set<Integer> temp = new HashSet<>();
						temp.add(uid);
						players.put(front, temp);
					}
				}
			}
			broadCast(players, msgid, data);
		}
	}

	public void broadCastByChannel(int channel,int msgId,byte[] data) {
		if (!playersByChannel.containsKey(channel)) {
			return;
		}
		Map<String, Set<Integer>> players = new HashMap<>();
		for (long objid : playersByChannel.get(channel)) {
			IGameObject player = getGameObject(objid);
			if (player == null) {
				continue;
			}
			String front = player.getString(PLAYER_PROPERTY_FRONTSER);
			int uid = player.getInt(PLAYER_PROPERTY_UID);
			if (players.containsKey(front)) {
				players.get(front).add(uid);
			} else {
				Set<Integer> temp = new HashSet<>();
				temp.add(uid);
				players.put(front, temp);
			}
		}

		broadCast(players, msgId, data);
	}
	
	public void broadCastByUids(List<Integer> uids, int msgid, byte[] data) {
		Map<String, Set<Integer>> players = new HashMap<>();
		for (Integer uid : uids) {
			IGameObject player = getPlayer(uid);
			if (player == null) {
				continue;
			}
			String front = player.getString(PLAYER_PROPERTY_FRONTSER);
			if (players.containsKey(front)) {
				players.get(front).add(uid);
			} else {
				Set<Integer> temp = new HashSet<>();
				temp.add(uid);
				players.put(front, temp);
			}
		}

		broadCast(players, msgid, data);
	}

	public void broadCastCurServer(int msgid, byte[] data) {
		broadCast(uidByFront, msgid, data);
	}

	public void broadCast(Map<String, Set<Integer>> players, int msgid, byte[] data) {
		ClientMsg.CustomMsg.Builder builder = ClientMsg.CustomMsg.newBuilder();
		builder.setMsgid(msgid);
		if (data != null) {
			builder.setData(ByteString.copyFrom(data));
		}
		InnerMsg.BroadCastToClients.Builder build = InnerMsg.BroadCastToClients.newBuilder();
		build.setMsgid(ClientMsgDef.CLIENT_CUSTOM.ordinal());
		build.setData(ByteString.copyFrom(builder.build().toByteArray()));
		for (Entry<String, Set<Integer>> entry : players.entrySet()) {
			if (entry.getValue().size() <= 0) {
				continue;
			}
			for (int uid : entry.getValue()) {
				build.addUids(uid);
			}
			gameLogic.getServer().sendMsgToServer(entry.getKey(), InnerMsgDef.INNER_MSG_BROADCAST.ordinal(), build.build().toByteArray());
		}
	}

	public void broadCastAllServer(int msgid, byte[] data) {
		ClientMsg.CustomMsg.Builder builder = ClientMsg.CustomMsg.newBuilder();
		builder.setMsgid(msgid);
		if (data != null) {
			builder.setData(ByteString.copyFrom(data));
		}

		InnerMsg.BroadCastToAllClients.Builder build = InnerMsg.BroadCastToAllClients.newBuilder();
		build.setMsgid(ClientMsgDef.CLIENT_CUSTOM.ordinal());
		build.setData(ByteString.copyFrom(builder.build().toByteArray()));

		gameLogic.getServer().broadToServer("gate",
				new SendMessage(InnerMsgDef.INNER_MSG_BROADCAST_ALL.ordinal(), build.build().toByteArray()));
	}

	public void command(IGameObject object, int cmdid, Object... args) {
		classSet.onCommand(object.getScript(), object, cmdid, args);
	}

	public void command(long objectid, int cmdid, Object... args) {
		logger.info("command{}",cmdid);
		GameObject target = classSet.getGameObject(objectid);
		if (target != null) {
			command(target, cmdid, args);
		} else {
			int serId = classSet.getObjectSerID(objectid);
			if (serId == gameLogic.getServer().getSerID()) {
				return;
			}
			IoBuffer buffer = IoBuffer.allocate(10);
			buffer.setAutoExpand(true);
			buffer.putShort((short) args.length);

			for (int i = 0 ; i < args.length; i++) {
				ValueType type = UtilFunc.getValueType(args[i]);
				if (type == ValueType.NONE) {
					return;
				}
				buffer.putShort((short) type.ordinal());
				UtilFunc.storeObjToBuffer(type, args[i], buffer);
			}

			InnerMsg.CommandMsg.Builder commandMsgBuilder = InnerMsg.CommandMsg.newBuilder();
			commandMsgBuilder.setCmdid(cmdid);
			commandMsgBuilder.setObjectid(objectid);
			commandMsgBuilder.setData(ByteString.copyFrom(buffer.array()));
			gameLogic.getServer().sendMsgToServer(serId, InnerMsgDef.INNER_MSG_COMMAND.ordinal(),
					commandMsgBuilder.build().toByteArray());
		}
	}

	public void commandPlayer(int uid, int cmdid, Object... args) {
		// 本地玩家
		Long objId = playersByUid.get(uid);
		if (objId != null) {
			IGameObject player = getGameObject(objId.longValue());
			if (player != null) {
				command(player, cmdid, args);
				return;
			}
		}
		// 其他服务器玩家
		OtherSerPlayer other = otherServerPlayers.get(uid);
		if (other != null) {
			command(other.objectid, cmdid, args);
		}
	}

	public void commandAllPlayer(int cmdid, Object... args) {
		List<Long> ids = playersByUid.values().stream().collect(Collectors.toList());
		for (Long id : ids) {
			IGameObject player = getGameObject(id);
			if (player != null) {
				command(player, cmdid, args);
			}
		}
	}

	public void onRecCommand(int cmdid, long objectid, byte[] data) {
		GameObject target = classSet.getGameObject(objectid);
		if (target == null) {
			return;
		}
		IoBuffer buffer = IoBuffer.wrap(data);
		short count = buffer.getShort();
		if (count <= 0) {
			command(target, cmdid);
			return;
		}
		Object[] args = new Object[count];
		for (int i = 0; i < count; ++i) {
			ValueType type = ValueType.values()[(int) buffer.getShort()];
			args[i] = UtilFunc.loadObjFromBuffer(type, buffer);
		}
		command(target, cmdid, args);
	}

	public void addModule(String name, ILogicModule module) {
		if (modules.containsKey(name)) {
			logger.error("Module {} is exist, please check and fix", name);
			return;
		}
		modules.put(name, module);
	}

	public ILogicModule getModule(String name) {
		if (modules.containsKey(name)) {
			return modules.get(name);
		}
		return null;
	}

	@Override
	public <T> T getModule(Class<T> name) {
		if (modules.containsKey(name.getSimpleName())) {
			return (T)modules.get(name.getSimpleName());
		}
		return null;
	}

	public boolean addHeartBeat(String name, IGameObject target, int ms, int repeat) {
		if (!callBacks.containsKey(name)) {
			return false;
		}
		heartBeatData data = new heartBeatData();
		data.target = target.getObjectID();
		data.name = name;
		ActorTimer actor = gameLogic.getServer().setTimer(this, ms, repeat, "onTimer", data);
		((GameObject) target).addHeartBeat(name, actor);
		return true;
	}

	public boolean haveHeartBeat(IGameObject target, String name) {
		return target.haveHeartBeat(name);
	}

	public void removeHeartBeat(IGameObject target, String name) {
		target.removeHeartBeat(name);
	}

	public void onTimer(Object obj, int leftCount) {
		if (obj == null) {
			return;
		}
		heartBeatData data = (heartBeatData) obj;
		GameObject target = classSet.getGameObject(data.target);
		if (target != null) {
			if (!callBacks.containsKey(data.name)) {
				return;
			}
			MethodCallBackData cb = callBacks.get(data.name);
			if (cb != null) {
				cb.access.invoke(cb.listener, cb.methodIndex, this, target);
			}
			if (leftCount == 0) {
				target.removeHeartBeat(data.name);
			}
		}
	}

	public int getPlayerCount() {
		return playersByUid.size();
	}

	public void checkPlayer(long now) {
		if (preCheckTime == 0){
			preCheckTime = now;
			return;
		}
		if (now < preCheckTime + 60000){
			return;
		}
		preCheckTime = now;
		long count = 0,notUse = 0,desk = 0,room = 0,item = 0,other = 0;
		for (int i = 0; i < classSet.allObjs.size(); i++) {
			GameObject gameObject = classSet.allObjs.get(i);
			if (gameObject == null || gameObject.m_root) {
				continue;
			}
			GameObjectType _type = gameObject.getType();
			if (_type == GameObjectType.GOTYPE_PLAYER) {
				count++;
				GamePlayer player = (GamePlayer) gameObject;
				int uid = player.getInt(PLAYER_PROPERTY_UID);
				Long objId = playersByUid.get(uid);
				if (objId != null) {
					if (objId.longValue() != gameObject.getObjectID()) {
						//已经不再使用的对象
						notUse++;
					}
				} else {
					notUse++;
				}
			} else if (_type == GameObjectType.GOTYPE_DESK) {
				desk ++;
			} else if (_type == GameObjectType.GOTYPE_ROOM) {
				room ++;
			} else if (_type == GameObjectType.GOTYPE_ITEM) {
				item ++;
			}else{
				other ++;
			}
		}
		logger.info("GamePlayer: total {}  not used {}  used {}",count,notUse, playersByUid.size());
		logger.info("room: {} desk {} item {} other {}", room,desk,item,other);
	}

	public void execute() {
		ClientMsg.PropertySync.Builder builder = ClientMsg.PropertySync.newBuilder();
		tempDesks.clear();
		// players
		long now = System.currentTimeMillis();
		for (Long objId : playersByUid.values()) {
			GameObject gameObject = classSet.getGameObject(objId);
			if (gameObject == null) {
				continue;
			}
			GamePlayer player = (GamePlayer) gameObject;
			long deskId = player.getLong(PLAYER_PROPERTY_DESKID);
			if (deskId != 0l) {
				GameDesk desk = (GameDesk) getGameObject(deskId);
				if (desk != null && !tempDesks.contains(desk)) {
					tempDesks.add(desk);
				}
			}
			IoBuffer pubBuff = IoBuffer.allocate(10);
			IoBuffer priBuff = IoBuffer.allocate(10);
			pubBuff.setAutoExpand(true);
			priBuff.setAutoExpand(true);
			int count = player.getSyncProperty(pubBuff, priBuff);
			short priCount = (short) (count >> 16);
			short pubCount = (short) (count & 0xFFFF);
			if (priCount > 0) {
				logger.debug("pro sync Execute pri {}", priCount);
				byte[] proData = Arrays.copyOfRange(priBuff.array(), 0, priBuff.position());
				builder.setObjectId(player.getObjectID());
				builder.setCount(priCount);
				builder.setData(ByteString.copyFrom(proData));
				byte[] data = builder.build().toByteArray();
				innerSendMessage(player, ClientMsgDef.CLIENT_SYNC_PROPERTY.ordinal(), data);
			}
			if (pubCount > 0) {
				logger.debug("pro sync Execute pub {}", pubCount);
				byte[] proData = Arrays.copyOfRange(pubBuff.array(), 0, pubBuff.position());
				builder.setObjectId(player.getObjectID());
				builder.setCount(pubCount);
				builder.setData(ByteString.copyFrom(proData));
				byte[] data = builder.build().toByteArray();
				innerBroadCastByKenWithOutSelf(player, ClientMsgDef.CLIENT_SYNC_PROPERTY.ordinal(), data);
			}
			player.syncViewportPro();
			long lastSave = player.getLong(PLAYER_PROPERTY_LAST_SAVE);
			if (now > lastSave + 180000){
				//每隔3分钟存档一次
				storePlayer(player, player.getState() == PlayerState.STATE_DISCONNECT);
				player.setProperty(PLAYER_PROPERTY_LAST_SAVE,now);
			}
		}
		// desks
		for (int i = 0; i < tempDesks.size(); i++) {
			GameDesk desk = tempDesks.get(i);
			IoBuffer pubBuff = IoBuffer.allocate(10);
			IoBuffer priBuff = IoBuffer.allocate(10);
			pubBuff.setAutoExpand(true);
			priBuff.setAutoExpand(true);
			int count = desk.getSyncProperty(pubBuff, priBuff);
			short pubCount = (short) (count & 0xFFFF);
			if (pubCount > 0) {
				byte[] proData = Arrays.copyOfRange(pubBuff.array(), 0, pubBuff.position());
				builder.setObjectId(desk.getObjectID());
				builder.setCount(pubCount);
				builder.setData(ByteString.copyFrom(proData));
				byte[] data = builder.build().toByteArray();
				innerBroadCastByDesk(desk, ClientMsgDef.CLIENT_SYNC_PROPERTY.ordinal(), data);
			}
		}
		// npcs
		for (int i = 0; i < listNpcs.size(); i++) {
			Long objid = listNpcs.get(i);
			GameNpc npc = (GameNpc) getGameObject(objid);
			if (npc == null) {
				continue;
			}
			IoBuffer pubBuff = IoBuffer.allocate(10);
			IoBuffer priBuff = IoBuffer.allocate(10);
			pubBuff.setAutoExpand(true);
			priBuff.setAutoExpand(true);
			int count = npc.getSyncProperty(pubBuff, priBuff);
			short pubCount = (short) (count & 0xFFFF);
			if (pubCount > 0) {
				byte[] proData = Arrays.copyOfRange(pubBuff.array(), 0, pubBuff.position());
				builder.setObjectId(npc.getObjectID());
				builder.setCount(pubCount);
				builder.setData(ByteString.copyFrom(proData));
				byte[] data = builder.build().toByteArray();
				innerBroadCastByKenWithOutSelf(npc, ClientMsgDef.CLIENT_SYNC_PROPERTY.ordinal(), data);
			}
		}
		checkPlayer(now);
	}

	public void listenPropertyChange(String proName, String script, Object listener, String methodName) {
		classSet.listenPropertyChange(proName, script, listener, methodName);
	}

	@Override
	public void listenSetProperty(String proName, String script, Object listener, String methodName) {
		classSet.listenSetProperty(proName, script, listener, methodName);
	}

	public void listenRecordChange(String recName, String script, Object listener, String methodName) {
		classSet.listenRecordChange(recName, script, listener, methodName);
	}

	public void OnReqChangeSer(int reqid, byte[] data) throws InvalidProtocolBufferException {
		InnerMsg.ChangeServer msg = InnerMsg.ChangeServer.parseFrom(data);
		byte[] roleData = msg.getData().toByteArray();
		int uid = msg.getUid();
		GamePlayer newPlayer = (GamePlayer) createObjectByScript("Player");
		newPlayer.setState(PlayerState.STATE_CHANGESER);
		IoBuffer buff = IoBuffer.wrap(roleData);
		newPlayer.loadFromCrossData(buff);
		addPlayer(uid, newPlayer);
		gameLogic.getServer().response(reqid, null);
	}

	public void onRecChangeSerResult(IoSession session, byte[] data) throws InvalidProtocolBufferException {
		InnerMsg.ChangeServerResult res = InnerMsg.ChangeServerResult.parseFrom(data);
		int uid = res.getUid();
		int code = res.getCode();

		gameLogic.setLastGame(uid);

		GamePlayer player = getPlayer(uid);
		if (player == null) {
			return;
		}
		if (code != 0) {
			deletePlayer(player,false);
			classSet.destroyGameObject(player);
			return;
		}
		player.setState(PlayerState.STATE_NORMAL);
		player.onLoad();
		innerSendMessage(player, ClientMsgDef.CLIENT_LOAD_OBJECT.ordinal(), player.getLoadObjectData(true).toByteArray());
		long deskid = 0l;
		int seatid = -1;
		if (player.haveTempData("waitEnterDesk")) {
			deskid = player.getTempLong("waitEnterDesk");
			player.removeTempData("waitEnterDesk");
		}
		if (player.haveTempData("waitEnterSeat")) {
			seatid = player.getTempInt("waitEnterSeat");
			player.removeTempData("waitEnterSeat");
		}

		if (deskid != 0l) {
			if (seatid == -1) {
				sitDown(player, deskid);
			} else {
				sitDown(player, deskid, seatid);
			}
		}
	}

	public long getPlayerObjID(int uid) {
		//本地玩家
		Long objId = playersByUid.get(uid);
		if (objId != null) {
			return objId.longValue();
		}
		//其他服务器玩家
		OtherSerPlayer otherSerPlayer = otherServerPlayers.get(uid);
		if (otherSerPlayer != null) {
			return otherSerPlayer.objectid;
		}
		return -1;
	}

	public void changeServer(IGameObject self, long tarobj) {
		changeServer(self, classSet.getObjectSerID(tarobj));
	}

	public void changeServer(IGameObject self, int serid) {
		GamePlayer player = (GamePlayer) self;
		if (classSet.getObjectSerID(player.getObjectID()) == serid) {
			return;
		}
		byte[] data = player.getCorssData(null);
		int uid = player.getInt(PLAYER_PROPERTY_UID);
		InnerMsg.ChangeServer.Builder build = InnerMsg.ChangeServer.newBuilder();
		build.setData(ByteString.copyFrom(data));
		build.setUid(uid);
		player.setState(PlayerState.STATE_CHANGESER);
		gameLogic.getServer().request(serid, InnerMsgDef.INNER_MSG_CHANGE_SERVER.ordinal(), build.build().toByteArray(), (recvmsg) -> {
			// 目标服务器创建完成成功
			InnerMsg.ChangeBack.Builder builder = InnerMsg.ChangeBack.newBuilder();
			builder.setUid(uid);
			builder.setBack(serid);
			gameLogic.getServer().request(player.getString(PLAYER_PROPERTY_FRONTSER), InnerMsgDef.INNER_MSG_CHANGE_BACK.ordinal(), builder.build().toByteArray(), (recvmsg2) -> {
				// 前段服务器修改后端服成功
				deletePlayer(player,true);
				classSet.destroyGameObject(player);
				// 通知目标服务器,切换完成
				InnerMsg.ChangeServerResult.Builder buildRes = InnerMsg.ChangeServerResult.newBuilder();
				buildRes.setUid(uid);
				buildRes.setCode(0);
				gameLogic.getServer().sendMsgToServer(serid, InnerMsgDef.INNER_MSG_CHANGE_RESULT.ordinal(), buildRes.build().toByteArray());
			});
		});
	}

	public boolean sitDown(IGameObject player, long deskid) {
		if (isCurSerObject(deskid)) {
			IGameObject desk = getGameObject(deskid);
			if (desk == null) {
				return false;
			}
			return sitDown(player, desk);
		}

		int serid = classSet.getObjectSerID(deskid);
		player.setTempData("waitEnterDesk", deskid);
		changeServer(player, serid);
		return true;
	}

	public boolean sitDown(IGameObject player, long deskid, int seatid) {
		if (isCurSerObject(deskid)) {
			IGameObject desk = getGameObject(deskid);
			if (desk == null) {
				return false;
			}
			return sitDown(player, desk, seatid);
		}
		int serId = classSet.getObjectSerID(deskid);
		player.setTempData("waitEnterDesk", deskid);
		player.setTempData("waitEnterSeat", seatid);
		changeServer(player, serId);
		return true;
	}

	public boolean sitDown(IGameObject player, IGameObject desk) {
		if (desk.getType() != GameObjectType.GOTYPE_DESK) {
			logger.error("obj is not GOTYPE_DESK");
			return false;
		}

		if ((long) player.getProperty(PLAYER_PROPERTY_DESKID) != 0) {
			logger.error("player in desk allready");
			return false;
		}

		IRecord rec = desk.getRecord("PlayerList");

		int maxRow = rec.getMaxRow();
		for (int i = 0; i < maxRow; ++i) {
			if ((long) rec.getValue(i, PlayerListCols.COL_OBJECTID.ordinal()) == 0) {
				return sitDown(player, desk, i);
			}
		}
		return false;
	}

	public boolean sitDown(IGameObject player, IGameObject desk, int seatid) {
		if (desk.getType() != GameObjectType.GOTYPE_DESK) {
			logger.error("obj is not GOTYPE_DESK");
			return false;
		}

		if ((long) player.getProperty(PLAYER_PROPERTY_DESKID) != 0) {
			logger.error("player in desk allready");
			return false;
		}

		IRecord rec = desk.getRecord("PlayerList");

		int maxRow = rec.getMaxRow();
		if (seatid < 0 || seatid >= maxRow) {
			logger.error("seatid err. {}", seatid);
			return false;
		}
		if (rec.getLong(seatid, PlayerListCols.COL_OBJECTID.ordinal()) != 0l) {
			logger.error("seatid in use. {}", rec.getLong(seatid, PlayerListCols.COL_OBJECTID.ordinal()));
			return false;
		}

		player.setProperty(PLAYER_PROPERTY_DESKID, desk.getObjectID());
		player.setProperty(PLAYER_PROPERTY_SEATID, (short) seatid);

		byte[] data = null;
		for (int j = 0; j < maxRow; ++j) {
			long objId = rec.getLong(j, PlayerListCols.COL_OBJECTID.ordinal());
			if (objId != 0) {
				GameObject p = getGameObject(objId);
				if (p == null) {
					continue;
				}
				if (data == null){
					data = ((GameObject) player).getLoadObjectData(false).toByteArray();
				}
				innerSendMessage(p, ClientMsgDef.CLIENT_LOAD_OBJECT.ordinal(), data);
				innerSendMessage(player, ClientMsgDef.CLIENT_LOAD_OBJECT.ordinal(), p.getLoadObjectData(false).toByteArray());
			}
		}
		innerSendMessage(player, ClientMsgDef.CLIENT_LOAD_OBJECT.ordinal(), ((GameObject) desk).getLoadObjectData(false).toByteArray());
		rec.setValue(seatid, PlayerListCols.COL_OBJECTID.ordinal(), player.getObjectID());
		classSet.runEvent(KernelEvent.KEVENT_ON_SITDOWN, player.getScript(), player, desk);
		return true;
	}

	public boolean standUp(IGameObject player) {
		long deskID = player.getLong(PLAYER_PROPERTY_DESKID);
		if (deskID == 0) {
			return false;
		}
		IGameObject desk = getGameObject(deskID);
		if (desk == null) {
			return false;
		}
		IRecord rec = desk.getRecord("PlayerList");
		int maxRow = rec.getMaxRow();
		short seatID = player.getShort(PLAYER_PROPERTY_SEATID);
		if (seatID < 0 || seatID >= maxRow) {
			return false;
		}
		if (rec.getLong(seatID, PlayerListCols.COL_OBJECTID.ordinal()) != player.getObjectID()) {
			return false;
		}
		ClientMsg.DeleteObject.Builder builder = ClientMsg.DeleteObject.newBuilder();
		for (int j = 0; j < maxRow; ++j) {
			if (j == seatID) {
				continue;
			}
			long objid = rec.getLong(j, PlayerListCols.COL_OBJECTID.ordinal());
			if (objid != 0) {
				GameObject p = getGameObject(objid);
				if (p == null) {
					continue;
				}
				builder.setObjectId(player.getObjectID());
				innerSendMessage(p, ClientMsgDef.CLIENT_DELETE_OBJECT.ordinal(), builder.build().toByteArray());
				builder.setObjectId(p.getObjectID());
				innerSendMessage(player, ClientMsgDef.CLIENT_DELETE_OBJECT.ordinal(), builder.build().toByteArray());
			}
		}
		builder.setObjectId(desk.getObjectID());
		innerSendMessage(player, ClientMsgDef.CLIENT_DELETE_OBJECT.ordinal(), builder.build().toByteArray());
		rec.setValue(seatID, PlayerListCols.COL_OBJECTID.ordinal(), 0L);
		classSet.runEvent(KernelEvent.KEVENT_ON_STANDUP, player.getScript(), player, desk);
		player.setProperty(PLAYER_PROPERTY_DESKID, 0L);
		player.setProperty(PLAYER_PROPERTY_SEATID, (short) 0);
        player.setProperty(PropertyKey.PLAYER_CURRENT_PLACE, 0);
		return true;
	}

	public void addDisconnectEvent(Object listener, String methodName) {
		if (disconnectCb != null) {
			logger.warn("Disconnect event callback is exist. [{}]", disconnectCb.listener);
			return;
		}
		try {
			listener.getClass().getMethod(methodName, IKernel.class, IGameObject.class);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		disconnectCb = new MethodCallBackData();
		disconnectCb.listener = listener;
		disconnectCb.access = MethodAccessCache.tryToGet(listener.getClass());
		disconnectCb.methodIndex = disconnectCb.access.getIndex(methodName, IKernel.class, IGameObject.class);
	}

	@Override
	public Jedis getJedis() {
		return gameLogic.getJedis();
	}

	public void onClientDisconnect(int uid, int code) {
		logger.info("{} OnClientDisconnect", uid);
		GamePlayer player = getPlayer(uid,true);
		if (player == null) {
			return;
		}
		try {
			player.setState(PlayerState.STATE_DISCONNECT);
			player.offLine();
			deletePlayer(player,true);
			classSet.destroyGameObject(player);
		} catch (Exception e) {
			logger.error(uid + " OnClientDisconnect error ",e);
		}
	}

	public void onClientReconnect(GamePlayer player) {
		player.setState(PlayerState.STATE_NORMAL);
		// 这里先下发桌子数据在下发玩家数据，方便客户端预加载渔场
		// load others
		GameObject desk = getGameObject(player.getLong(PLAYER_PROPERTY_DESKID));
		if (desk != null) {
			innerSendMessage(player, ClientMsgDef.CLIENT_LOAD_OBJECT.ordinal(), desk.getLoadObjectData(false).toByteArray());
			IRecord rec = desk.getRecord("PlayerList");
			int maxRow = rec.getMaxRow();
			for (int i = 0; i < maxRow; ++i) {
				long objid = (long) rec.getValue(i, PlayerListCols.COL_OBJECTID.ordinal());
				if (objid != 0 && objid != player.getObjectID()) {
					GameObject p = (GameObject) getGameObject(objid);
					if (p == null) {
						continue;
					}
					innerSendMessage(player, ClientMsgDef.CLIENT_LOAD_OBJECT.ordinal(), p.getLoadObjectData(false).toByteArray());
				}
			}
		}
		// load self
		innerSendMessage(player, ClientMsgDef.CLIENT_LOAD_OBJECT.ordinal(), player.getLoadObjectData(true).toByteArray());
		// load viewport
		player.syncViewPort();
		classSet.runEvent(KernelEvent.KEVENT_ON_RECONNECT, player.getScript(), player);
	}

	public void onStorePlayer(IKernel kernel, IGameObject gameObject) {
		GamePlayer player = (GamePlayer) gameObject;
		storePlayer(player, player.getState() == PlayerState.STATE_DISCONNECT);
	}

	public void storePlayer(GamePlayer player, boolean offline) {
		classSet.runEvent(KernelEvent.KEVENT_ON_STORE, player.getScript(), player);
		IoBuffer buff = IoBuffer.allocate(10);
		buff.setAutoExpand(true);
		player.storeToArchive(buff);
		int size = buff.position();
		buff.flip();
		byte[] store = Arrays.copyOfRange(buff.array(), 0, size);
		InnerMsg.StoreRoleData.Builder builder = InnerMsg.StoreRoleData.newBuilder();
		builder.setUid(player.getInt(PLAYER_PROPERTY_UID));
		builder.setName(player.getString(PLAYER_PROPERTY_NAME));
		builder.setSex(player.getInt(PLAYER_PROPERTY_SEX));
		builder.setHeadurl(player.getString(PLAYER_PROPERTY_HEAD));
		try {
			int count=GetItemCount(player,"item_skill_hbomb");
			player.setProperty(PLAYER_PROPERTY_BOMB_ITEM,count);
		} catch (Exception e) {
		}
		builder.setBombCoin(player.getLong(PLAYER_PROPERTY_BOMB_COIN));
		builder.setBombItem(player.getInt(PLAYER_PROPERTY_BOMB_ITEM));
		builder.setData(ByteString.copyFrom(store));
		builder.setOffline(offline ? 1 : 0);
		builder.setLastOpt(player.getString(PLAYER_PROPERTY_LASTOPT));
		builder.setVip(player.getInt(PLAYER_INVITER_VIP_STATUS));
		byte[] data = builder.build().toByteArray();
		gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_STORE_ROLE_DATA.ordinal(), data);
	}

	public void addOtherSerPlayer(int uid, long objectId, int headid, String head, String name, String front,
								  String back) {
		OtherSerPlayer player = new OtherSerPlayer();
		player.objectid = objectId;
		player.name     = name;
		player.front    = front;
		player.back     = back;
		otherServerPlayers.put(uid, player);
		addRoleList(uid, name, headid,-1);
		logger.info("添加其他服务的玩家{}", uid);
		addHead(headid, head);
	}
	public int GetItemCount(IGameObject player, String itemName) {
		IGameObject itemBag = player.getContainer("ItemBag");
		if (itemBag == null) {
			return 0;
		}
		int count = 0;
		int pos = itemBag.findChildById(0, itemName);
		while (pos != -1) {
			IGameObject item = itemBag.getChild(pos);
			count += item.getInt("Count");
			pos = itemBag.findChildById(pos + 1, itemName);
		}
		return count;
	}
	public void delOtherSerPlayer(int uid) {
		if (otherServerPlayers.containsKey(uid)) {
			otherServerPlayers.remove(uid);
		}
	}

	public void playerChangeSer(int uid, long objectid, String front, String back) {
		if (!otherServerPlayers.containsKey(uid)) {
			return;
		}
		OtherSerPlayer player = new OtherSerPlayer();
		player.objectid = objectid;
		player.name     = otherServerPlayers.get(uid).name;
		player.front    = front;
		player.back     = back;
		otherServerPlayers.put(uid, player);
	}

	public int getAllPlayerCount() {
		return playersByUid.size() + otherServerPlayers.size();
	}

	public String getPlayerFront(int uid) {
		GamePlayer player = getPlayer(uid);
		if (player != null) {
			return player.getString(PLAYER_PROPERTY_FRONTSER);
		}
		if (otherServerPlayers.containsKey(uid)) {
			return otherServerPlayers.get(uid).front;
		}
		return "";
	}

	public ICfgReader loadXmlConfig(String path) {
		XmlReader reader = new XmlReader();
		if (!reader.loadConfig(path)) {
			return null;
		}
		return reader;
	}

	public File[] listFiles(String dir) {
		String config = System.getProperty("user.dir");
		try {
			File f = new File(config + File.separator + "config" + File.separator + dir);
			return f.listFiles();
		} catch (Exception e) {
			logger.error("ListFiles [{}] failed.", dir);
			e.printStackTrace();
			return new File[0];
		}
	}

	public long getServerTime() {
		return gameLogic.getServer().getServerTime();
	}

	public boolean isCurSerObject(long objid) {
		return classSet.getObjectSerID(objid) == gameLogic.getServer().getSerID();
	}

	// 注册服务器消息
	public void regServerMsg(int msgid, Object listener, String methodName) {
		MethodCallBackData data = new MethodCallBackData();
		data.listener = listener;
		data.access = MethodAccessCache.tryToGet(listener.getClass());
		data.methodIndex = data.access.getIndex(methodName, IKernel.class, int.class, int.class, byte[].class);
		if (!serverMsg.containsKey(msgid)) {
			Set<MethodCallBackData> temp = new HashSet<MethodCallBackData>();
			temp.add(data);
			serverMsg.put(msgid, temp);
		} else {
			serverMsg.get(msgid).add(data);
		}
	}

	public void regServerRequest(int msgid, Object listener, String methodName) {
		if (serverRequest.containsKey(msgid)) {
			logger.error("Request handle for msg[{}] is exist, please check and retry.", msgid);
			return;
		}
		MethodCallBackData cb = new MethodCallBackData();
		cb.listener = listener;
		cb.access = MethodAccessCache.tryToGet(listener.getClass());
		cb.methodIndex = cb.access.getIndex(methodName, IKernel.class, int.class, byte[].class);
		serverRequest.put(msgid, cb);
	}

	public void onRecServerMsg(int serId, int msgId, byte[] data) {
		Set<MethodCallBackData> cbs = serverMsg.get(msgId);
		if (cbs == null) {
			return;
		}
		for (MethodCallBackData cb : cbs) {
			cb.access.invoke(cb.listener, cb.methodIndex, this, serId, msgId, data);
		}
	}

	public void onServerRequest(int serId, byte[] data) throws InvalidProtocolBufferException {
		InnerMsg.Request req = InnerMsg.Request.parseFrom(data);
		int reqId = req.getReqid();
		int msgId = req.getMsgid();
		byte[] msg = null;
		if (req.getData() != null) {
			msg = req.getData().toByteArray();
		}
		MethodCallBackData cb = serverRequest.get(msgId);
		if (cb == null) {
			return;
		}
		RecReqData reqData = new RecReqData();
		reqData.reqId = reqId;
		reqData.serId = serId;
		serverResponses.put(reqId, reqData);
		cb.access.invoke(cb.listener, cb.methodIndex, this, reqId, msg);
	}

	public void onServerResponse(int serId, byte[] data) throws InvalidProtocolBufferException {
		InnerMsg.Response req = InnerMsg.Response.parseFrom(data);
		int reqId = req.getReqid();
		byte[] msg = null;
		if (req.getData() != null) {
			msg = req.getData().toByteArray();
		}
		if (!mapReqs.containsKey(reqId)) {
			return;
		}
		IRequestCallback cb = mapReqs.get(reqId);
		cb.execute(msg);
		mapReqs.remove(reqId);
	}

	public void sendServerMsg(int serId, int msgId, byte[] data) {
		InnerMsg.CustomMsg.Builder build = InnerMsg.CustomMsg.newBuilder();
		build.setMsgid(msgId);
		if (data != null) {
			build.setData(ByteString.copyFrom(data));
		}
		gameLogic.getServer().sendMsgToServer(serId, InnerMsgDef.INNER_MSG_CUSTOM_MSG.ordinal(), build.build().toByteArray());
	}

	public void sendServerMsg(String serverName, int msgId, byte[] data) {
		InnerMsg.CustomMsg.Builder build = InnerMsg.CustomMsg.newBuilder();
		build.setMsgid(msgId);
		if (data != null) {
			build.setData(ByteString.copyFrom(data));
		}
		gameLogic.getServer().sendMsgToServer(serverName, InnerMsgDef.INNER_MSG_CUSTOM_MSG.ordinal(), build.build().toByteArray());
	}

	public void requestServer(int serId, int msgId, byte[] data, IRequestCallback cb) {
		if (reqId < 0){
			reqId = 0;
		}
		int reqId = this.reqId++;
		mapReqs.put(reqId, cb);
		InnerMsg.Request.Builder build = InnerMsg.Request.newBuilder();
		build.setReqid(reqId);
		build.setMsgid(msgId);
		if (data != null) {
			build.setData(ByteString.copyFrom(data));
		}
		gameLogic.getServer().sendMsgToServer(serId, InnerMsgDef.INNER_MSG_CUSTOM_REQUEST.ordinal(), build.build().toByteArray());
	}

	public void requestServer(String serverName, int msgId, byte[] data, IRequestCallback cb) {
		if (reqId < 0){
			reqId = 0;
		}
		int reqId = this.reqId++;
		mapReqs.put(reqId, cb);
		InnerMsg.Request.Builder build = InnerMsg.Request.newBuilder();
		build.setReqid(reqId);
		build.setMsgid(msgId);
		if (data != null) {
			build.setData(ByteString.copyFrom(data));
		}
		gameLogic.getServer().sendMsgToServer(serverName, InnerMsgDef.INNER_MSG_CUSTOM_REQUEST.ordinal(), build.build().toByteArray());
	}

	public void responseServer(int reqId, byte[] data) {
		if (!serverResponses.containsKey(reqId)) {
			return;
		}
		RecReqData req = serverResponses.get(reqId);
		InnerMsg.Response.Builder build = InnerMsg.Response.newBuilder();
		build.setReqid(req.reqId);
		if (data != null) {
			build.setData(ByteString.copyFrom(data));
		}
		gameLogic.getServer().sendMsgToServer(req.serId, InnerMsgDef.INNER_MSG_CUSTOM_RESPONSE.ordinal(), build.build().toByteArray());
		serverResponses.remove(reqId);
	}

	public void getObjList(InnerMsg.ReqObjectListRes.Builder res) {
		int count = 1;
		res.addObjectid(gameWorld.getObjectID());
		res.addName(gameWorld.getString(PLAYER_PROPERTY_NAME));
		res.addScript(gameWorld.getScript());
		for (Entry<String, Long> entry : listPreLoadObj.entrySet()) {
			IGameObject child = getGameObject(entry.getValue());
			if (child != null) {
				res.addObjectid(child.getObjectID());
				res.addName(child.getString(PLAYER_PROPERTY_NAME));
				res.addScript(child.getScript());
				++count;
			}
		}
		for (Entry<Integer, Long> entry : playersByUid.entrySet()) {
			IGameObject child = getGameObject(entry.getValue());
			if (child != null) {
				res.addObjectid(child.getObjectID());
				res.addName(child.getString(PLAYER_PROPERTY_NAME));
				res.addScript(child.getScript());
				++count;
			}
		}
		res.setCount(count);
	}

	public IGameObject createContainer(String name, String script, int capacity, IGameObject target) {
		if (target.getContainer(name) != null) {
			return null;
		}
		IGameObject obj = classSet.createObjectByScript(script);
		if (obj == null) {
			return null;
		}
		if (obj.getType() != GameObjectType.GOTYPE_CONTAINER) {
			classSet.destroyGameObject(obj);
			return null;
		}
		GameContainer container = (GameContainer) obj;
		container.innerSetCapacity(capacity);
		container.setProperty(PLAYER_PROPERTY_NAME, name);
		if (!((GameObject) target).addContainer(name, container)) {
			return null;
		}
		return container;
	}

	public PubData getPubData(String name) {
		if (isMain()){
			return PubUtils.loadData(gameLogic.getJedis(),name);
		}else{
			return PubUtils.loadSyncData(gameLogic.getJedis(),name);
		}
	}

	public void onRecAddPubSpace(IoSession session, byte[] data) throws InvalidProtocolBufferException {
		//InnerMsg.AddPubSpace msg = InnerMsg.AddPubSpace.parseFrom(data);
		//CreatePubSpace(msg.getName());
	}

	public void onRecAddPubData(IoSession session, byte[] data) throws InvalidProtocolBufferException {
//		InnerMsg.AddPubData msg = InnerMsg.AddPubData.parseFrom(data);
//		PubSpace pubSpace = GetPubSpace(msg.getSpacename());
//		if (pubSpace == null) {
//			return;
//		}
//		pubSpace.LoadPubData(msg.getName(), msg.getData().toByteArray());
	}

	public void onRecAddPubPro(IoSession session, byte[] data) throws InvalidProtocolBufferException {
//		InnerMsg.AddPubPro msg = InnerMsg.AddPubPro.parseFrom(data);
//		PubSpace pubSpace = GetPubSpace(msg.getSpacename());
//		if (pubSpace == null) {
//			return;
//		}
//		PubData pubData = pubSpace.GetPubData(msg.getDataname());
//		if (pubData == null) {
//			return;
//		}
//		pubData.LoadProperty(msg.getName(), IoBuffer.wrap(msg.getData().toByteArray()));
	}

	public void onRecAddPubRec(IoSession session, byte[] data) throws InvalidProtocolBufferException {
//		InnerMsg.AddPubRec msg = InnerMsg.AddPubRec.parseFrom(data);
//		PubSpace pubSpace = GetPubSpace(msg.getSpacename());
//		if (pubSpace == null) {
//			return;
//		}
//		PubData pubData = pubSpace.GetPubData(msg.getDataname());
//		if (pubData == null) {
//			return;
//		}
//		pubData.LoadRecord(msg.getName(), IoBuffer.wrap(msg.getData().toByteArray()));
	}

	public void onRecDelPubSpace(IoSession session, byte[] data) throws InvalidProtocolBufferException {
	}

	public void onRecDelPubData(IoSession session, byte[] data) throws InvalidProtocolBufferException {
//		InnerMsg.DelPubData msg = InnerMsg.DelPubData.parseFrom(data);
//		PubSpace pubSpace = GetPubSpace(msg.getSpacename());
//		if (pubSpace == null) {
//			return;
//		}
//		pubSpace.DeletePubData(msg.getName());
	}

	public void onRecDelPubPro(IoSession session, byte[] data) throws InvalidProtocolBufferException {
//		InnerMsg.DelPubPro msg = InnerMsg.DelPubPro.parseFrom(data);
//		PubSpace pubSpace = GetPubSpace(msg.getSpacename());
//		if (pubSpace == null) {
//			return;
//		}
//		PubData pubData = pubSpace.GetPubData(msg.getDataname());
//		if (pubData == null) {
//			return;
//		}
//		pubData.DelProperty(msg.getName());
	}

	public void onRecDelPubRec(IoSession session, byte[] data) throws InvalidProtocolBufferException {
//		InnerMsg.DelPubRec msg = InnerMsg.DelPubRec.parseFrom(data);
//		PubSpace pubSpace = GetPubSpace(msg.getSpacename());
//		if (pubSpace == null) {
//			return;
//		}
//		PubData pubData = pubSpace.GetPubData(msg.getDataname());
//		if (pubData == null) {
//			return;
//		}
//		pubData.DelRecord(msg.getName());
	}

	public void onRecSetPubPro(IoSession session, byte[] data) throws InvalidProtocolBufferException {
//		InnerMsg.SetPubPro msg = InnerMsg.SetPubPro.parseFrom(data);
//		PubSpace pubSpace = GetPubSpace(msg.getSpacename());
//		if (pubSpace == null) {
//			return;
//		}
//		PubData pubData = pubSpace.GetPubData(msg.getDataname());
//		if (pubData == null) {
//			return;
//		}
//		pubData.LoadValue(msg.getName(), IoBuffer.wrap(msg.getData().toByteArray()));
	}

	public void onRecSetPubRec(IoSession session, byte[] data) throws InvalidProtocolBufferException {
//		InnerMsg.SetPubRec msg = InnerMsg.SetPubRec.parseFrom(data);
//		PubSpace pubSpace = GetPubSpace(msg.getSpacename());
//		if (pubSpace == null) {
//			return;
//		}
//		PubData pubData = pubSpace.GetPubData(msg.getDataname());
//		if (pubData == null) {
//			return;
//		}
//		PubRecord rec = (PubRecord) pubData.GetRecord(msg.getName());
//		if (rec == null) {
//			return;
//		}
//		rec.SetValue(msg.getRow(), msg.getCol(),
//				rec.LoadVal(msg.getRow(), msg.getCol(), IoBuffer.wrap(msg.getData().toByteArray())));
	}

	public void onRecAddPubRecRow(IoSession session, byte[] data) throws InvalidProtocolBufferException {
//		InnerMsg.AddPubRecRow msg = InnerMsg.AddPubRecRow.parseFrom(data);
//		PubSpace pubSpace = GetPubSpace(msg.getSpacename());
//		if (pubSpace == null) {
//			return;
//		}
//		PubData pubData = pubSpace.GetPubData(msg.getDataname());
//		if (pubData == null) {
//			return;
//		}
//		PubRecord rec = (PubRecord) pubData.GetRecord(msg.getName());
//		if (rec == null) {
//			return;
//		}
//		rec.LoadRow(msg.getRow(), IoBuffer.wrap(msg.getData().toByteArray()));
	}

	public void onRecSetPubRecColType(IoSession session, byte[] data) throws InvalidProtocolBufferException {
//		InnerMsg.SetPubRecColType msg = InnerMsg.SetPubRecColType.parseFrom(data);
//		PubSpace pubSpace = GetPubSpace(msg.getSpacename());
//		if (pubSpace == null) {
//			return;
//		}
//		PubData pubData = pubSpace.GetPubData(msg.getDataname());
//		if (pubData == null) {
//			return;
//		}
//		PubRecord rec = (PubRecord) pubData.GetRecord(msg.getName());
//		if (rec == null) {
//			return;
//		}
//		ValueType type = ValueType.values()[msg.getType()];
//		rec.SetColType(msg.getCol(), type);
	}

	public void onRecDelPubRecRow(IoSession session, byte[] data) throws InvalidProtocolBufferException {
//		InnerMsg.DelPubRecRow msg = InnerMsg.DelPubRecRow.parseFrom(data);
//		PubSpace pubSpace = GetPubSpace(msg.getSpacename());
//		if (pubSpace == null) {
//			return;
//		}
//		PubData pubData = pubSpace.GetPubData(msg.getDataname());
//		if (pubData == null) {
//			return;
//		}
//		PubRecord rec = (PubRecord) pubData.GetRecord(msg.getName());
//		if (rec == null) {
//			return;
//		}
//		rec.RemoveRow(msg.getRow());
	}

	void onRecClearPubRec(IoSession session, byte[] data) throws InvalidProtocolBufferException {
//		InnerMsg.ClearPubRec msg = InnerMsg.ClearPubRec.parseFrom(data);
//		PubSpace pubSpace = GetPubSpace(msg.getSpacename());
//		if (pubSpace == null) {
//			return;
//		}
//		PubData pubData = pubSpace.GetPubData(msg.getDataname());
//		if (pubData == null) {
//			return;
//		}
//		PubRecord rec = (PubRecord) pubData.GetRecord(msg.getName());
//		if (rec == null) {
//			return;
//		}
//		rec.Clear();
	}

	void onRecAddBackList(IoSession session, byte[] data) throws InvalidProtocolBufferException {
		InnerMsg.BlackList msg = InnerMsg.BlackList.parseFrom(data);
		int type = msg.getType();
		String val = msg.getContext();

		if (type == 1) {
			kickPlayerByIp(val);
		} else if (type == 2) {
			kickPlayerByDevID(val);
		}
	}

    void OnAccountStatus(IoSession session, byte[] data) throws Exception {
        InnerMsg.String string = InnerMsg.String.parseFrom(data);
        int id = Integer.parseInt(string.getValue());
        Long objectId = playersByUid.get(id);
        IGameObject player = getGameObject(objectId);
        player.setProperty(PLAYER_ACCOUNT_STATUS, 1);
    }

	private void sendMail(int type, int channel, String title, String context, int senduid, String sendName,
						  int recvuid, String recvName, long lifeTime, String appendix, MailSystemDef system) {
		InnerMsg.SendMail.Builder build = InnerMsg.SendMail.newBuilder();
		build.setType(type);
		build.setChannel(channel);
		build.setTitle(title);
		build.setContext(context);
		build.setSenderuid(senduid);
		build.setSendername(sendName);
		build.setRecvuid(recvuid);
		build.setRecvname(recvName);
		build.setLifetime(lifeTime);
		build.setAppendix(appendix);
		build.setSystem(system.ordinal());
		gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_SEND_MAIL.ordinal(), build.build().toByteArray());
	}

	public boolean sendSystemMail(int recvuid, int channel, String title, String context, long lifetime,
								  String appendix) {
		String recvName = "";
		if (recvuid != -1) {
			recvName = getUserName(recvuid);
			if (recvName == null) {
				return false;
			}
		}
		int mailSys = MailTypeDef.MAIL_SYSTEM.ordinal();
		if (context.indexOf("京东购物卡") != -1) {
			mailSys = MailTypeDef.MAIL_JD_CARD.ordinal();
		}
		sendMail(mailSys, channel, title, context, -1, "System", recvuid, recvName, lifetime, appendix, MailSystemDef.MAIL_NORMAL);
		return true;
	}

	@Override
	public boolean sendSystemMail(int recvuid, int channel, String title, String context, long lifetime,
								  String appendix, MailSystemDef system) {
		String recvName = "";
		if (recvuid != -1) {
			recvName = getUserName(recvuid);
			if (recvName == null) {
				return false;
			}
		}
		sendMail(MailTypeDef.MAIL_SYSTEM.ordinal(), channel, title, context, -1, "System", recvuid, recvName, lifetime, appendix, system);
		return true;
	}

	public boolean sendPayMail(int recvuid, int channel, String title, String context, long lifetime, String appendix) {
		String recvName = "";
		if (recvuid != -1) {
			recvName = getUserName(recvuid);
			if (recvName == null) {
				return false;
			}
		}
		sendMail(MailTypeDef.MAIL_PAY.ordinal(), channel, title, context, -1, "System", recvuid, recvName, lifetime,
				appendix, MailSystemDef.MAIL_BACK);
		return true;
	}

	public boolean sendNormalMail(IGameObject player, int recvuid, String title, String context, long lifetime,
								  String appendix) {
		if (player == null || player.getType() != GameObjectType.GOTYPE_PLAYER) {
			logger.error("SendNormalMail player not valid");
			return false;
		}
		String recvName = "";
		if (recvuid != -1) {
			recvName = getUserName(recvuid);
			if (recvName == null) {
				logger.error("recvName is null, uid:{}", recvuid);
				return false;
			}
		}

		sendMail(MailTypeDef.MAIL_NORMAL.ordinal(), -1, title, context, player.getInt(PLAYER_PROPERTY_UID),
				player.getString(PLAYER_PROPERTY_NAME), recvuid, recvName, lifetime, appendix,
				MailSystemDef.MAIL_NORMAL);
		return true;
	}

	// 赠送道具邮件
	public boolean sendItemMail(IGameObject player, int recvuid, String title, String context, long lifetime,
								String appendix) {
		if (player == null || player.getType() != GameObjectType.GOTYPE_PLAYER) {
			logger.error("SendItemMail player not valid");
			return false;
		}

		String recvName = "";
		if (recvuid != -1) {
			recvName = getUserName(recvuid);
			if (recvName == null) {
				logger.error("recvName is null, uid:{}", recvuid);
				return false;
			}
		}
		sendMail(MailTypeDef.MAIL_SENDITEM.ordinal(), -1, title, context,
				player.getInt(PLAYER_PROPERTY_UID), player.getString(PLAYER_PROPERTY_NAME),
				recvuid, recvName, lifetime, appendix, MailSystemDef.MAIL_NORMAL);
		return true;
	}

	public void queryMail(int uid, int channel, String lastMailid, Consumer<List<MailData>> cb) {
		InnerMsg.QueryMail.Builder query = InnerMsg.QueryMail.newBuilder();
		query.setUid(uid);
		query.setChannel(channel);
		query.setMailid(lastMailid);
		gameLogic.getServer().request(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_QUERY_MAIL.ordinal(), query.build().toByteArray(), (data) -> {
			InnerMsg.QueryMailRes res = null;
			try {
				res = InnerMsg.QueryMailRes.parseFrom(data);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			List<MailData> list = new ArrayList<>();
			List<InnerMsg.ReadMailRes> mails = res.getMailsList();
			for (int i = 0; i < mails.size(); i++) {
				InnerMsg.ReadMailRes _mail = mails.get(i);
				MailData mail = new MailData();
				mail.id = _mail.getId();
				mail.type = _mail.getType();
				mail.senduid = _mail.getSenderuid();
				mail.sendName = _mail.getSendername();
				mail.recvuid = _mail.getRecvuid();
				mail.recvName = _mail.getRecvname();
				mail.title = _mail.getTitle();
				mail.context = _mail.getContext();
				mail.sendTime = _mail.getSendtime();
				mail.endTime = _mail.getEndtime();
				mail.appendix = _mail.getAppendix();
				mail.system = _mail.getSystem();
				list.add(mail);
			}
			cb.accept(list);
		});
	}

	public void readMail(String mailId, Consumer<MailData> cb) {
		InnerMsg.ReadMail.Builder read = InnerMsg.ReadMail.newBuilder();
		read.setMailid(mailId);
		gameLogic.getServer().request(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_READ_MAIL.ordinal(), read.build().toByteArray(), (data) -> {
			InnerMsg.ReadMailRes res = null;
			try {
				res = InnerMsg.ReadMailRes.parseFrom(data);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			MailData mail = new MailData();
			mail.id = res.getId();
			mail.type = res.getType();
			mail.senduid = res.getSenderuid();
			mail.sendName = res.getSendername();
			mail.recvuid = res.getRecvuid();
			mail.recvName = res.getRecvname();
			mail.title = res.getTitle();
			mail.context = res.getContext();
			mail.sendTime = res.getSendtime();
			mail.endTime = res.getEndtime();
			mail.appendix = res.getAppendix();
			mail.system = res.getSystem();
			cb.accept(mail);
		});
	}

	public void delMail(String mailId) {
		InnerMsg.DelMail.Builder del = InnerMsg.DelMail.newBuilder();
		del.setMailid(mailId);
		gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_DEL_MAIL.ordinal(), del.build().toByteArray());
	}

	public void KickPlayer(String gate, IGameObject player) {
		if (player == null || player.getType() != GameObjectType.GOTYPE_PLAYER) {
			return;
		}
		gameLogic.kickPlayer(gate, player.getInt(PLAYER_PROPERTY_UID), KickType.KICK.ordinal());
	}

	public void kickPlayer(IGameObject player) {
		if (player == null || player.getType() != GameObjectType.GOTYPE_PLAYER) {
			return;
		}
		gameLogic.kickPlayer(player.getString(PLAYER_PROPERTY_FRONTSER), player.getInt(PLAYER_PROPERTY_UID), KickType.KICK.ordinal());
	}

	public void KickPlayer(IGameObject player, int reason) {
		if (player == null || player.getType() != GameObjectType.GOTYPE_PLAYER) {
			return;
		}
		gameLogic.kickPlayer(player.getString(PLAYER_PROPERTY_FRONTSER), player.getInt(PLAYER_PROPERTY_UID), reason);
	}

	public void kickPlayerNoTip(IGameObject player) {
		if (player == null || player.getType() != GameObjectType.GOTYPE_PLAYER) {
			return;
		}
		gameLogic.kickPlayer(player.getString(PLAYER_PROPERTY_FRONTSER), player.getInt(PLAYER_PROPERTY_UID), -1);
	}

	public void kickPlayerByChannel(int channel) {
		Set<Long> playrIds = playersByChannel.get(channel);
		if (playrIds == null){
			return;
		}
		List<Long> ids = playrIds.stream().collect(Collectors.toList());
		for (long objid : ids) {
			kickPlayer(getGameObject(objid));
		}
	}

	public void kickPlayerByChannelWhenMaintain(int channel) {
		Set<Long> playrIds = playersByChannel.get(channel);
		if (playrIds == null){
			return;
		}
		List<Long> ids = playrIds.stream().collect(Collectors.toList());
		for (long objid : ids) {
			GameObject player = getGameObject(objid);
			KickPlayer(player, KickType.MAINTION.ordinal());
		}
	}

	public void kickPlayerByIp(String addr) {
		List<Long> ids = playersByUid.values().stream().collect(Collectors.toList());
		for (Long id : ids) {
			GameObject player = getGameObject(id);
			if (player != null && player.getString(PLAYER_PROPERTY_IPADDR).equals(addr)) {
				kickPlayer(player);
			}
		}
	}

	public void kickPlayerByDevID(String devid) {
		List<Long> ids = playersByUid.values().stream().collect(Collectors.toList());
		for (Long id : ids) {
			GameObject player = getGameObject(id);
			if (player != null && player.getString(PLAYER_PROPERTY_DEVICEID).equals(devid)) {
				kickPlayer(player);
			}
		}
	}

	public void kickAllPlayer() {
		List<Long> ids = playersByUid.values().stream().collect(Collectors.toList());
		for (Long uid : ids) {
			kickPlayer(getGameObject(uid));
		}
	}

	public void kickAllPlayerWhenMaintain() {
		List<Long> ids = playersByUid.values().stream().collect(Collectors.toList());
		for (Long uid : ids) {
			GameObject player = getGameObject(uid);
			KickPlayer(player, KickType.MAINTION.ordinal());
		}
	}

	public void addGameLog(IGameObject player, LogKind kind, LogType type, String context, String system, String reason, int target) {
		InnerMsg.GameLog.Builder log = InnerMsg.GameLog.newBuilder();
		log.setUid(player.getInt(PLAYER_PROPERTY_UID));
		log.setChannel(player.getInt(PLAYER_PROPERTY_CHANNEL));
		log.setType(type.ordinal());
		log.setContext(context);
		log.setReason(reason);
		log.setTargetuid(target);
		log.setKind(kind.ordinal());
		log.setSystem(system);
		gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_GAME_LOG.ordinal(), log.build().toByteArray());
	}

	@Override
	public void addItemLog(IGameObject player, String itemid, int count, String roomName, String output, String useway) {
		InnerMsg.ItemLog.Builder log = InnerMsg.ItemLog.newBuilder();
		StringBuffer playerHas = new StringBuffer();
		IGameObject itemBag = player.getContainer("ItemBag");
		int nbomb = getItemCountByName(itemBag, ITEM_PROPERTY_SKILL_NBOMB);
		int hbomb = getItemCountByName(itemBag, ITEM_PROPERTY_SKILL_HBOMB);
		int debrisnbomb = getItemCountByName(itemBag, ITEM_PROPERTY_DEBRIS_NBOMB);
		int debrishbomb = getItemCountByName(itemBag, ITEM_PROPERTY_DEBRIS_HBOMB);
		log.setUid(player.getInt(PLAYER_PROPERTY_UID));
		log.setCreateTime(gameLogic.getServer().getTimeFormat().format(getServerTime()));
		log.setNickName(player.getString(PLAYER_PROPERTY_NAME));
		log.setVipLevel(player.getInt(PLAYER_PROPERTY_VIPLEVEL));
		log.setItem(itemid);
		log.setCount(count);
		log.setOutput(output);
		log.setUesWay(useway);
		log.setRoomName(roomName);
		long colorTicket = player.getLong(PLAYER_PROPERTY_COLORTICKET);
		append(playerHas,nbomb,"至尊");
		append(playerHas,hbomb,"传说");
		append(playerHas,debrisnbomb,"至尊碎片");
		append(playerHas,debrishbomb,"传说碎片");
		append(playerHas,colorTicket,"奖劵");
		log.setPlayerHas("".equals(playerHas.toString()) ? "-" : playerHas.toString());
		gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_ITEM_LOG.ordinal(),
				log.build().toByteArray());
	}

	private void append(StringBuffer buffer,long num,String tail){
		if (num > 0){
			if (buffer.length() > 0){
				buffer.append("、");
			}
			buffer.append(num).append("个").append(tail);
		}
	}

	public int getItemCountByName(IGameObject itemBag, String itemName){
		if (itemBag == null) {
            return 0;
        }
        int count = 0;
        List<IGameObject> items = itemBag.findChildObjById(itemName);
        for (IGameObject item : items){
        	count += item.getInt("Count");
        }
        return count;
	}

	void addProLog(IGameObject player, String name, String context, int system, String reason) {
		if (player == null || player.getBool("IsRobot")) {
			return;
		}
		long deskid = player.getLong(PLAYER_PROPERTY_DESKID);
		IGameObject desk = getGameObject(deskid);
		int roomType = -1;
		IRecord totalPw = player.getRecord("TotalPlayWin");
		if (desk != null) {
			roomType = desk.getInt(DESK_TYPE_KEY);
		}
		long totalPlay = roomType == -1 ? 0L : totalPw.getLong(roomType, 0);
		long totalWin = roomType == -1 ? 0L : totalPw.getLong(roomType, 1);
		if (roomType >= 9 && roomType <= 11) {
			// 至尊选座，记录玩家在4号房间的总玩总赢
			totalPlay = totalPw.getLong(4, 0);
			totalWin = totalPw.getLong(4, 1);
		}
		if ((roomType >= 12 && roomType <= 15) || roomType == 17) {
			// 核弹场和单人模式，记录桌子的总玩总赢
			totalPlay = desk.getLong(PLAYER_PROPERTY_TOTALPLAY);
			totalWin = desk.getLong(PLAYER_PROPERTY_TOTALWIN);
		}
		String now = gameLogic.getServer().getTimeFormat().format(getServerTime());
		String log = new StringBuilder().append(now).append("|").append(player.getInt(PLAYER_PROPERTY_UID))
				.append("|").append(PlayerLogType.PROP_CHANGE.ordinal()).append("|").append(roomType).append("|")
				.append(system).append("|").append(name).append("|")
				.append(player.getInt(PLAYER_PROPERTY_CHANNEL)).append("|")
				.append(player.getString("LoginTime")).append("|")
				.append(player.getString(PLAYER_PROPERTY_REGTIME)).append("|")
				.append(player.getInt(PLAYER_PROPERTY_VIPLEVEL)).append("|")
				.append(player.getInt(PLAYER_PROPERTY_LEVEL)).append("|")
				.append(player.getLong(PLAYER_PROPERTY_GOLD)).append("|")
				.append(player.getLong(PLAYER_PROPERTY_DIAMOND)).append("|")
				.append(player.getLong(PLAYER_PROPERTY_BOMB_COIN)).append("|")
				.append(player.getLong(PLAYER_PROPERTY_COUPONS)).append("|")
				.append(player.getLong(PLAYER_PROPERTY_COLORTICKET)).append("|")
				.append(player.getLong(PLAYER_PROPERTY_TOTALCOLORTICKET)).append("|")// 累计获得
				.append(player.getLong(PLAYER_PROPERTY_TOTALUSECOLORTICKET)).append("|")// 累计消耗
				.append(totalPlay).append("|").append(totalWin).append("|").append(context).append("|").append(reason)
				.append("|").toString();

		InnerMsg.BaseValue.Builder build = InnerMsg.BaseValue.newBuilder();
		build.setStrValue(log);
//		gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_PLAYER_LOG.ordinal(),
//				build.build().toByteArray());
	}

	public void addMailLog(IGameObject player, String mailid, MailLogType type, String context, String reason) {
		InnerMsg.MailLog.Builder log = InnerMsg.MailLog.newBuilder();
		log.setUid(player.getInt(PLAYER_PROPERTY_UID));
		log.setType(type.ordinal());
		log.setMailid(mailid);
		log.setContext(context);
		log.setReason(reason);
		
		gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_MAIL_LOG.ordinal(),
				log.build().toByteArray());
	}

	public void addPlayLog(IGameObject player, IGameObject desk, PlayLogType type, String context, String reason) {
		InnerMsg.PlayLog.Builder log = InnerMsg.PlayLog.newBuilder();
		log.setUid(player.getInt(PLAYER_PROPERTY_UID));
		log.setType(type.ordinal());
		log.setRoom(desk != null ? desk.getInt(DESK_TYPE_KEY) : -1);
		log.setDesk(desk != null ? desk.getObjectID() : 0);
		log.setGold(player.getLong(PLAYER_PROPERTY_GOLD));
		log.setContext(context);
		log.setReason(reason);

//		gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_PLAY_LOG.ordinal(),
//				log.build().toByteArray());
	}

	/**
	 * 玩家日志（道具+部分游玩）
	 * 日志类型：0-道具获得，1-道具消耗，2-进入房间，3-退出房间，4-清除总玩总赢，5-击杀大鱼，6-定时游玩，7-属性变化 日志内容：
	 * 0-道具获得，1-道具消耗： 【原数量，变化量，变化后量】 2-进入房间，3-退出房间： 【】 4-清除总玩总赢： 【房间id：总玩，总赢；】
	 * 5-击杀大鱼： 【掉落信息】 6-定时游玩： 【】 7-属性变化： 【原数量 -> 变化后量】
	 * |时间|玩家uid|日志类型|房间id|系统id|道具id/属性名|渠道id|登录时间|注册时间|VIP等级|玩家等级|金币|钻石|魔晶|彩券|总玩|总赢|日志内容|日志原因
	 *
	 */
	public void addPlayerLog(IGameObject player, IGameObject item, int type, int system, String context,
							 String reason) {
		if (player == null || player.getBool("IsRobot")) {
			return;
		}

		long deskid = player.getLong(PLAYER_PROPERTY_DESKID);
		IGameObject desk = getGameObject(deskid);

		int roomType = -1;
		IRecord totalPw = player.getRecord("TotalPlayWin");
		if (desk != null) {
			roomType = desk.getInt(DESK_TYPE_KEY);
		}

		long totalPlay = roomType == -1 ? 0L : totalPw.getLong(roomType, 0);
		long totalWin = roomType == -1 ? 0L : totalPw.getLong(roomType, 1);
		if (roomType >= 9 && roomType <= 11) {
			// 至尊选座，记录玩家在4号房间的总玩总赢
			totalPlay = totalPw.getLong(4, 0);
			totalWin = totalPw.getLong(4, 1);
		}

		String now = gameLogic.getServer().getTimeFormat().format(getServerTime());
		String log = new StringBuilder().append(now).append("|").append(player.getInt(PLAYER_PROPERTY_UID))
				.append("|").append(type).append("|").append(roomType).append("|").append(system).append("|")
				.append(item == null ? "null" : item.getString("Id")).append("|")
				.append(player.getInt(PLAYER_PROPERTY_CHANNEL)).append("|")
				.append(player.getString("LoginTime")).append("|")
				.append(player.getString(PLAYER_PROPERTY_REGTIME)).append("|")
				.append(player.getInt(PLAYER_PROPERTY_VIPLEVEL)).append("|")
				.append(player.getInt(PLAYER_PROPERTY_LEVEL)).append("|")
				.append(player.getLong(PLAYER_PROPERTY_GOLD)).append("|")
				.append(player.getLong(PLAYER_PROPERTY_DIAMOND)).append("|")
				.append(player.getLong(PLAYER_PROPERTY_BOMB_COIN)).append("|")
//				.append(player.getLong(PLAYER_PROPERTY_COUPONS)).append("|")
//				.append(player.getLong(PLAYER_PROPERTY_COLORTICKET)).append("|")
//				.append(player.getLong(PLAYER_PROPERTY_TOTALCOLORTICKET)).append("|")// 累计获得
//				.append(player.getLong(PLAYER_PROPERTY_TOTALUSECOLORTICKET)).append("|")// 累计消耗
//				.append("秘境值:").append(player.getLong(PLAYER_PROPERTY_MYSTERY_LEGEND_ENERGY)).append("|")
				.append(totalPlay).append("|").append(totalWin).append("|").append(context).append("|").append(reason)
				.append("|").toString();

		InnerMsg.BaseValue.Builder build = InnerMsg.BaseValue.newBuilder();
		build.setStrValue(log);
		gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_PLAYER_LOG.ordinal(),
				build.build().toByteArray());
	}

	@Override
	public void addActivityLog(IGameObject player, int activity,String methodName, String data) {
		if (player == null || player.getBool("IsRobot")) {
			return;
		}

		String now = gameLogic.getServer().getTimeFormat().format(getServerTime());
		String log = new StringBuilder().append(now).append("|").append(player.getInt(PLAYER_PROPERTY_UID)).append("|").append(player.getInt(PLAYER_PROPERTY_VIPLEVEL))
				.append("|").append(methodName).append("|").append(activity).append("|").append(data).append("|").toString();

		InnerMsg.BaseValue.Builder build = InnerMsg.BaseValue.newBuilder();
		build.setStrValue(log);
//		gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_ACTIVITY_LOG.ordinal(),
//				build.build().toByteArray());
	}

	@Override
	public void addKillFishLog(IGameObject player, IGameObject desk) {
		if (player == null ) {
			return;
		}
		String now = gameLogic.getServer().getTimeFormat().format(getServerTime());
		int type = desk.getInt("Type");
		StringBuilder stringBuilder = new StringBuilder();
		if(type<=4){
			String log=stringBuilder.append(now).append("|").append(player.getInt(PLAYER_PROPERTY_UID))
					.append("|").append(type)
					.append("|").append(player.getProperty("TotalRechargeAmount"))
					.append("|").append(player.getProperty("ItemScore"))
					.append("|").append(player.getProperty("RechargeScore"))
					.append("|").append(player.getProperty("Gold"))
					.append("|").append(player.getProperty("UsedProtectPlay"))
					.append("|").append(player.getProperty("MaxProtectPlay"))
					.append("|").append(player.getProperty("TotalPlay"))
					.append("|").append(player.getProperty("TotalWin"))
					.append("|").toString();
			InnerMsg.BaseValue.Builder build = InnerMsg.BaseValue.newBuilder();
			build.setStrValue(log);
			gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_KILL_FISH_LOG.ordinal(),
					build.build().toByteArray());
		}else{
			String log=stringBuilder.append(now).append("|").append(player.getInt(PLAYER_PROPERTY_UID))
					.append("|").append(desk.getParent().getProperty("Type"))
					.append("|").append(desk.getProperty("DeskID"))
					.append("|").append(desk.getParent().getProperty("TotalPlay"))
					.append("|").append(desk.getParent().getProperty("TotalWin"))
					.append("|").append(desk.getProperty("TotalPlay"))
					.append("|").append(desk.getProperty("TotalWin"))
					.append("|").toString();
			InnerMsg.BaseValue.Builder build = InnerMsg.BaseValue.newBuilder();
			build.setStrValue(log);
//			gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_KILL_FISH_LOG_MJ.ordinal(),
//					build.build().toByteArray());
		}

	}

	@Override
	public void addActivityLuckyPuzzleLog(IGameObject player, int option, String items) {
		if (player == null) {
			return;
		}
		InnerMsg.ActivityLuckyPuzzleLog.Builder build = InnerMsg.ActivityLuckyPuzzleLog.newBuilder();
		build.setCreateTime(gameLogic.getServer().getTimeFormat().format(getServerTime()));
		build.setUid(player.getInt(PLAYER_PROPERTY_UID));
		build.setNickName(player.getString(PLAYER_PROPERTY_NAME));
		build.setOption(option);
		build.setItems(items);
//		gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_ACTIVITY_LUCKY_PUZZLE_LOG.ordinal(),
//				build.build().toByteArray());

	}

	/**
	 * GM操作日志
	 * 
	 * @param player
	 *            GM
	 * @param target
	 *            操作目标
	 * @param cmds
	 *            命令
	 * @param result
	 *            结果
	 */
	public void addGmLog(IGameObject player, IGameObject target, String cmds, String result) {
		if (player == null || target == null) {
			return;
		}
		String now = gameLogic.getServer().getTimeFormat().format(getServerTime());
		String log = new StringBuilder().append(now).append("|").append(player.getInt(PLAYER_PROPERTY_UID))
				.append("|").append(target.getInt(PLAYER_PROPERTY_UID)).append("|").append(cmds).append("|")
				.append(result).append("|").toString();

		InnerMsg.BaseValue.Builder build = InnerMsg.BaseValue.newBuilder();
		build.setStrValue(log);
		gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_GM_LOG.ordinal(),
				build.build().toByteArray());
	}

	@Override
	public void addFunFishRecord(IGameObject player, String fish, int room, int gold, int bombCoin, int rewardSendState) {
		if (player == null) {
			return;
		}
		JsonObject json = new JsonObject();
		json.addProperty("createTime", gameLogic.getServer().getTimeFormat().format(getServerTime()));
		json.addProperty("uid", player.getInt(PLAYER_PROPERTY_UID));
		json.addProperty("nickname", player.getString(PLAYER_PROPERTY_NAME));
		json.addProperty("vipLevel", player.getInt(PLAYER_PROPERTY_VIPLEVEL));
		json.addProperty("bulletValue", player.getInt(PLAYER_PROPERTY_BULLETVALUE));
		json.addProperty("fishType", fish);
		json.addProperty("room", room);
		json.addProperty("gold", gold);
		json.addProperty("bombCoin", bombCoin);
		json.addProperty("rewardSendState", rewardSendState);
		gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_FUN_FISH_RECORD.ordinal(), json.toString().getBytes());

	}

	@Override
	public void addActivityFishPondRecord(IGameObject player, int option, String cost, String income, int caughtUid, String caughtNickname) {
		if (player == null) {
			return;
		}
		JsonObject json = new JsonObject();
		json.addProperty("createTime", gameLogic.getServer().getTimeFormat().format(getServerTime()));
		json.addProperty("uid", player.getInt(PLAYER_PROPERTY_UID));
		json.addProperty("nickName", player.getString(PLAYER_PROPERTY_NAME));
		json.addProperty("vipLevel", player.getInt(PLAYER_PROPERTY_VIPLEVEL));
		json.addProperty("op", option);
		json.addProperty("cost", cost == null ? "-" : cost);
		json.addProperty("income", income == null ? "-" : income);
		json.addProperty("caughtUid", caughtUid);
		json.addProperty("caughtNickname", caughtNickname == null ? "-" : caughtNickname);
		gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE,InnerMsgDef.INNER_MSG_ACTIVITY_FISH_POND_RECORD.ordinal(),json.toString().getBytes());
	}

	@Override
	public void addActivitySystemFishRecord(String fishId, String fishName, int fishState, String fishValue, int caughtUid, String caughtNickname) {
		JsonObject json = new JsonObject();
		json.addProperty("createTime", gameLogic.getServer().getTimeFormat().format(getServerTime()));
		json.addProperty("systemFishId", fishId);
		json.addProperty("systemFishName", fishName);
		json.addProperty("systemFishState", fishState);
		json.addProperty("systemFishValue", fishValue);
		json.addProperty("caughtUid", caughtUid);
		json.addProperty("caughtNickname", caughtNickname == null ? "-" : caughtNickname);
		gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_ACTIVITY_SYSTEM_FISH_RECORD.ordinal(), json.toString().getBytes());
	}

	@Override
	public void addFishPondMsgRecord(int type, int uid, String nickname, String cost) {
		JsonObject json = new JsonObject();
		json.addProperty("createTime", gameLogic.getServer().getTimeFormat().format(getServerTime()));
		json.addProperty("uid", uid);
		json.addProperty("type", type);
		json.addProperty("nickname", nickname);
		json.addProperty("cost", cost);
		gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_ACTIVITY_FISH_POND_MSG_RECORD.ordinal(), json.toString().getBytes());
	}

	public void setState(IGameObject player, PlayerState state) {
		if (player.getType() != GameObjectType.GOTYPE_PLAYER) {
			return;
		}

		((GamePlayer) player).setState(state);
	}

	public void addOfflineData(int uid, int type, String context, String reason) {
		InnerMsg.OfflineData.Builder data = InnerMsg.OfflineData.newBuilder();
		data.setUid(uid);
		data.setType(type);
		data.setContext(context);
		data.setReason(reason);

		gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_OFFLINEDATA.ordinal(),
				data.build().toByteArray());
	}

	public void loadOfflineData(IGameObject player) {
		InnerMsg.ReqOfflineData.Builder data = InnerMsg.ReqOfflineData.newBuilder();
		data.setUid(player.getInt(PLAYER_PROPERTY_UID));
		gameLogic.getServer().request(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_REQ_OFFLINE.ordinal(), data.build().toByteArray(), (bytes) -> {
			InnerMsg.ReqOfflineDataRes res = null;
			try {
				res = InnerMsg.ReqOfflineDataRes.parseFrom(bytes);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			InnerMsg.DelOfflineData.Builder del = InnerMsg.DelOfflineData.newBuilder();
			int count = res.getIdCount();
			for (int i = 0; i < count; ++i) {
				int id = res.getId(i);
				int type = res.getType(i);
				String context = res.getContext(i);
				String reason = res.getReason(i);
				classSet.runEvent(KernelEvent.KEVENT_ON_OFFLINEDATA, "Player", player, type, context, reason);
				del.addId(id);
			}
			if (count > 0) {
				gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_DEL_OFFLINE.ordinal(),
						del.build().toByteArray());
			}
		});
	}

	public void changeName(IGameObject player, String newName, Consumer<Boolean> cb) {
		long objid = player.getObjectID();
		InnerMsg.ChangeName.Builder build = InnerMsg.ChangeName.newBuilder();
		build.setUid(player.getInt(PLAYER_PROPERTY_UID));
		build.setNewName(newName);
		byte[] msg = build.build().toByteArray();
		gameLogic.getServer().request(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_CHANGE_NAME.ordinal(), msg, (data) -> {
			if (getGameObject(objid) != player) {
				cb.accept(false);
				return;
			}
			try {
				InnerMsg.ComResponse res = InnerMsg.ComResponse.parseFrom(data);
				if (res.getCode() == 0) {
					player.setProperty(PLAYER_PROPERTY_NAME, newName);
				}
				cb.accept(res.getCode() == 0);
			} catch (Exception e) {
				e.printStackTrace();
				cb.accept(false);
				return;
			}
		});
	}

	public void advice(IGameObject player, String context, int type) {
		InnerMsg.Advice.Builder build = InnerMsg.Advice.newBuilder();
		build.setUid(player.getInt(PLAYER_PROPERTY_UID));
		build.setChannel(player.getInt(PLAYER_PROPERTY_CHANNEL));
		build.setType(type);
		build.setCliVer(player.getString(PLAYER_PROPERTY_VERSION));
		build.setContext(context);
		gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_ADVICE.ordinal(), build.build().toByteArray());
	}

	public void addWarningItemScore(IGameObject player, int chargeScore, int killFishItemScore, int drawAwardItemScore, int maxItemScore) {
		InnerMsg.WarningItemScoreLog.Builder build = InnerMsg.WarningItemScoreLog.newBuilder();
		build.setUid(player.getInt(PLAYER_PROPERTY_UID));
		build.setVipLevel(player.getInt(PLAYER_PROPERTY_VIPLEVEL));
		build.setNickName(player.getString(PLAYER_PROPERTY_NAME));
		build.setChargeScore(chargeScore);
		build.setKillFishItemScore(killFishItemScore);
		build.setDrawAwardItemScore(drawAwardItemScore);
		build.setCurItemScore(player.getInt(PLAYER_PROPERTY_ITEMSCORE));
		build.setMaxItemScore(maxItemScore);
		build.setBombCoin(player.getLong(PLAYER_PROPERTY_BOMB_COIN));
		build.setGold(player.getLong(PLAYER_PROPERTY_GOLD));
		gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_WARNING_ITEMSCORE.ordinal(), build.build().toByteArray());
	}
	
	public void addWarningMoJin(IGameObject player, long play, long win, int hbomb, int hbomb_debris, int nbomb,
								int nbomb_debris, String other_detail, long dmojin){
		InnerMsg.WarningMoJinLog.Builder build = InnerMsg.WarningMoJinLog.newBuilder();
		build.setUid(player.getInt(PLAYER_PROPERTY_UID));
		build.setVipLevel(player.getInt(PLAYER_PROPERTY_VIPLEVEL));
		build.setNickName(player.getString(PLAYER_PROPERTY_NAME));
		build.setPlay(play);
		build.setWin(win);
		build.setCur(player.getLong(PLAYER_PROPERTY_BOMB_COIN));
		build.setHbomb(hbomb);
		build.setHbombDebris(hbomb_debris);
		build.setNbomb(nbomb);
		build.setNbombDebris(nbomb_debris);
		build.setOtherDetail(other_detail);
		build.setDmojin(dmojin);
		gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_WARNING_MOJIN.ordinal(), build.build().toByteArray());
	}
	
	public void addMoJinRoomActiveData(MojinRoomRecord record){
		InnerMsg.String.Builder build = InnerMsg.String.newBuilder();
		build.setValue(JsonUtil.encodeToStr(record));
		gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_MOJIN_ROOM_RECORD.ordinal(), build.build().toByteArray());
	}

	public void startPerf(String func) {
		Perf.GetInstane().startPerf(func);
	}

	public void overPerf(String func) {
		Perf.GetInstane().overPerf(func);
	}

	public void addOnlineTime(IGameObject player, int itme) {
		InnerMsg.Dau.Builder build = InnerMsg.Dau.newBuilder();

		long now = getServerTime();
		String nowDate = new SimpleDateFormat("yyyy-MM-dd").format(now);
		String regDate = player.getString(PLAYER_PROPERTY_REGTIME).substring(0, 10); // YYYY-MM-DD

		build.setUid(player.getInt(PLAYER_PROPERTY_UID));
		build.setChannel(player.getInt(PLAYER_PROPERTY_CHANNEL));
		build.setDate(nowDate);
		build.setTime(itme);
		build.setCount(-1);
		build.setIsnew(regDate.equals(nowDate));

		byte[] data = build.build().toByteArray();
		gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_UPDATE_DAU.ordinal(), data);
	}

	public void addLoginCount(IGameObject player) {
		InnerMsg.Dau.Builder build = InnerMsg.Dau.newBuilder();

		long now = getServerTime();
		String nowDate = new SimpleDateFormat("yyyy-MM-dd").format(now);
		String regDate = player.getString(PLAYER_PROPERTY_REGTIME).substring(0, 10); // YYYY-MM-DD

		build.setUid(player.getInt(PLAYER_PROPERTY_UID));
		build.setChannel(player.getInt(PLAYER_PROPERTY_CHANNEL));
		build.setDate(nowDate);
		build.setTime(-1);
		build.setCount(1);
		build.setIsnew(regDate.equals(nowDate));

		byte[] data = build.build().toByteArray();
		gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_UPDATE_DAU.ordinal(), data);
	}

	public void updateOnlineCount(int channel, int count) {
		InnerMsg.OnlinePeak.Builder build = InnerMsg.OnlinePeak.newBuilder();
		build.setChannel(channel);
		build.setCount(count);
		build.setDate(new SimpleDateFormat("yyyy-MM-dd").format(getServerTime()));

		byte[] data = build.build().toByteArray();
		gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_UPDATE_ONLINEPEAK.ordinal(), data);
	}

	public void addNpc(GameNpc npc) {
		listNpcs.add(npc.getObjectID());
	}

	public void removeNpc(GameNpc npc) {
		listNpcs.remove(npc.getObjectID());
	}

	boolean checkRedeemCode(String code) {
		if (code.length() != 8) {
			return false;
		}
		int val = 0;
		for (int i = 0; i < 7; ++i) {
			char ch = code.charAt(i);
			if ((ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'Z')) {
				val += code.charAt(i);
			} else {
				return false;
			}
		}
		char check = 0;
		int num = val % 36;
		check = (char) ((num < 10) ? ('0' + num) : ('A' + num - 10));
		if (check != code.charAt(7)) {
			return false;
		}
		return true;
	}

	public void useRedeemCode(String code, int uid, int channel, String devid, Consumer<String> cb) {
		if (!checkRedeemCode(code)) {
			cb.accept("");
			return;
		}
		InnerMsg.ReqRedeemCode.Builder build = InnerMsg.ReqRedeemCode.newBuilder();
		build.setUid(uid);
		build.setChannel(channel);
		build.setDevid(devid);
		build.setCode(code);
		gameLogic.getServer().request(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_REQ_REDEEM_CODE.ordinal(), build.build().toByteArray(), (byte[] res) -> {
			InnerMsg.RedeemCodeRes msg = null;
			try {
				msg = InnerMsg.RedeemCodeRes.parseFrom(res);
			} catch (Exception e) {
				e.printStackTrace();
				cb.accept("");
				return;
			}
			String context = msg.getContext();
			cb.accept(context);
		});
	}

	public void checkCardItem(IGameObject player, String itemid, int type, Consumer<List<CardData>> cb) {
		InnerMsg.UseCardItem.Builder build = InnerMsg.UseCardItem.newBuilder();
		build.setUid(player.getInt(PLAYER_PROPERTY_UID));
		build.setItemid(itemid);
		build.setType(type);
		gameLogic.getServer().request(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_REQ_CARD_ITEM.ordinal(), build.build().toByteArray(), (byte[] res) -> {
			InnerMsg.UseCardItemRes msg = null;
			try {
				msg = InnerMsg.UseCardItemRes.parseFrom(res);
			} catch (Exception e) {
				e.printStackTrace();
				cb.accept(null);
				return;
			}

			List<CardData> list = null;
			int count = msg.getIdCount();
			if (count > 0) {
				list = new ArrayList<>();
				for (int i = 0; i < count; ++i) {
					CardData data = new CardData();
					data.id = msg.getId(i);
					data.passwd = msg.getPasswd(i);
					data.endDate = msg.getEnd(i);
					data.itemid = msg.getItemid(i);
					list.add(data);
				}
			}
			cb.accept(list);
		});
	}

	/**
	 * 实名认证
	 */
	public void realName(IGameObject player, String name, String idnum, Consumer<Boolean> cb) {
		InnerMsg.RealName.Builder build = InnerMsg.RealName.newBuilder();
		build.setUid(player.getInt(PLAYER_PROPERTY_UID));
		build.setName(name);
		build.setIdcard(idnum);
		gameLogic.getServer().request(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_REALNAME.ordinal(),
		build.build().toByteArray(), (byte[] data) -> {
			cb.accept(data[0] == 1);
		});
	}

	/**
	 * 绑定代理
	 */
	public void bindProxy(IGameObject player, int proxyId, Consumer<Boolean> cb) {
		InnerMsg.BindProxy.Builder build = InnerMsg.BindProxy.newBuilder();
		build.setUid(player.getInt(PLAYER_PROPERTY_UID));
		build.setProxyId(proxyId);
		gameLogic.getServer().request(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_BIND_PROXYID.ordinal(), build.build().toByteArray(), (data) -> {
			cb.accept(data[0] == 1);
		});
	}

	@Override
	public void exchangeCard(int uid, String items, String goods, Consumer<Boolean> cb) {
		InnerMsg.ExchangeCard.Builder build = InnerMsg.ExchangeCard.newBuilder();
		build.setUid(uid);
		build.setItems(items);
		build.setGoods(goods);
		gameLogic.getServer().request(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_EXCHANGE_CARD.ordinal(), build.build().toByteArray(), (data) -> {
			try {
				InnerMsg.ComResponse res = InnerMsg.ComResponse.parseFrom(data);
				cb.accept(res.getCode() == 0);
			} catch (Exception e) {
				e.printStackTrace();
				cb.accept(false);
				return;
			}
		});
	}

	@Override
	public void addRecruit(int uid, String openDate) {
		InnerMsg.Recruit.Builder build = InnerMsg.Recruit.newBuilder();
		build.setUid(uid);
		build.setOpenDate(openDate);
		gameLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_RECRUIT.ordinal(), build.build().toByteArray());
	}

	Map<Integer, List<Consumer<IGameObject>>> mapLoadPlayerDataCb = new HashMap<>();
	Map<Integer, IGameObject> mapLoadedPlayerData = new HashMap<>();

	/**
	 * 加载离线玩家数据（不会触发OnLoad、Online等等事件）
	 * 
	 * @param uid
	 * @param cb
	 */
	public void loadPlayerData(int uid, Consumer<IGameObject> cb) {
		long objid = getPlayerObjID(uid);
		if (objid != -1) {
			// 在线玩家，拒绝加载 add by 胡中伟, 2019年5月30日 下午4:43:46
			cb.accept(null);
			return;
		}

		if (mapLoadedPlayerData.containsKey(uid)) {
			loadPlayerDataComp(uid);
			return;
		}

		InnerMsg.RequestRoleData.Builder builder = InnerMsg.RequestRoleData.newBuilder();
		builder.setUid(uid);
		byte[] data = builder.build().toByteArray();

		gameLogic.getServer().request(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_REQ_OFFLINE_ROLE.ordinal(), data, (resmsg) -> {
			if (mapLoadedPlayerData.containsKey(uid)) {
				loadPlayerDataComp(uid);
				return;
			}
			InnerMsg.LoadRoleData roleData = null;
			try {
				roleData = InnerMsg.LoadRoleData.parseFrom(resmsg);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			GameObject newPlayer = null;
			if (roleData.getCode() == 0) {
				newPlayer = new GameObject();
				byte[] data1 = roleData.getData().toByteArray();
				IoBuffer buff = IoBuffer.wrap(data1);
				newPlayer.loadFromArchive(buff);
				mapLoadedPlayerData.put(uid, newPlayer);
			}
			loadPlayerDataComp(uid);
		});
	}

	void loadPlayerDataComp(int uid) {
		if (!mapLoadedPlayerData.containsKey(uid) || !mapLoadPlayerDataCb.containsKey(uid)) {
			return;
		}
		IGameObject obj = mapLoadedPlayerData.get(uid);
		List<Consumer<IGameObject>> list = mapLoadPlayerDataCb.get(uid);
		for (Consumer<IGameObject> cb : list) {
			cb.accept(obj);
		}
		list.clear();
	}

	@Override
	public void executeSomeToStore(Class<? extends AbstractService<?>> requireType, String method, List<Object> objects, Consumer<String> cb) {
		if (ObjectUtils.isEmpty(requireType)) {
			logger.error("error params <requireType> when call LoadDataFromDB");
			return;
		}
		if (StringUtils.isEmpty(method)) {
			logger.error("error params <method> when call LoadDataFromDB");
			return;
		}
		InnerMsg.LoadDataFromDb.Builder builder = InnerMsg.LoadDataFromDb.newBuilder();
		String serviceName = StringUtils.uncapitalize(requireType.getSimpleName());
		builder.setDao(serviceName);
		builder.setMethod(method);
		if (objects != null) {
			for (int i = 0; i < objects.size(); i++) {
				Object obj = objects.get(i);
				Class<?> clazz = obj.getClass();
				if (List.class.isAssignableFrom(clazz)){
					clazz = List.class;
				}
				if (Map.class.isAssignableFrom(clazz)){
					clazz = Map.class;
				}
				builder.addTypes(clazz.getTypeName());
				if (obj instanceof String) {
					builder.addValues(obj.toString());
				} else {
					String valueStr = new ParamData().encode(obj);
					if (valueStr == null){
						cb.accept(null);
						return;
					}
					builder.addValues(valueStr);
				}
			}
		}
		byte[] msg = builder.build().toByteArray();
		gameLogic.getServer().request(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_EXECUTE_SQL_METHOD.ordinal(), msg, (bytes) -> {
			try {
				InnerMsg.ComeFromDbData datas = InnerMsg.ComeFromDbData.parseFrom(bytes);
				int code = datas.getCode();
				if (code == 0) {
					if (cb != null) {
						String _data = datas.getDatas();
						cb.accept(_data.length() == 0 ? null : _data);
					}
				} else {
					//logger.error("LoadDataFromDB error code = " + code);
					if (cb != null) {
						cb.accept(null);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				if (cb != null) {
					cb.accept(null);
				}
			}
		});
	}

	public void onNetReady() {
		onReady();
		for (ILogicModule module : modules.values()) {
			module.onNetReady(this);
		}
	}

	@Override
	public boolean isMain() {
		return mainFlag;
	}

	/**
	 * 注册功能模块关闭事件
	 * 
	 * @param listener
	 * @param order
	 * @param methodName
	 */
	@Override
	public void regStopListener(Object listener, int order, String methodName) {
		if (stopEvent.containsKey(order)) {
			logger.error("StopHandler for msg[{}] is exist, please check and retry.", order);
			return;
		}
		MethodCallBackData data = new MethodCallBackData();
		data.listener = listener;
		data.access = MethodAccessCache.tryToGet(listener.getClass());
		data.methodIndex = data.access.getIndex(methodName, IKernel.class, int.class, String.class);
		stopEvent.put(order, data);
	}

	public boolean runStopByOrder() {
		MethodCallBackData cb = stopEvent.remove(stopOrder);
		if (cb != null) {
			boolean result = (boolean) cb.access.invoke(cb.listener, cb.methodIndex, this, stopOrder, null);
			if (result) {
				String[] methodNames = cb.access.getMethodNames();
				logger.info("order: [{}], [{}.{}] execute success", stopOrder, cb.listener.getClass().getName(), methodNames[cb.methodIndex]);
				stopOrder++;
			}
			return false;
		}else if (stopEvent.size() > 0){
			stopOrder++;
			return false;
		}
		BaseServer server = gameLogic.getServer();
		InnerMsg.NotifyNextReady.Builder builder = InnerMsg.NotifyNextReady.newBuilder();
		builder.setName(server.getName());
		byte[] datas = builder.build().toByteArray();
		SendMessage msg = new SendMessage(InnerMsgDef.INNER_MSG_NOTIFY_ME_CLOSE.ordinal(), datas);
		server.broadToServer("store",msg);
		server.broadToServer("gate",msg);
		server.broadToServer("public",msg);
		return true;
	}

	public void serverOffLine(String data) {
		if (!data.startsWith("Gate")) {
			return;
		}
		//有网关离线了
		List<Long> ids = playersByUid.values().stream().collect(Collectors.toList());
		for (Long objId : ids) {
			GamePlayer player = (GamePlayer) getGameObject(objId);
			if (player != null) {
				String gate = player.getString(PLAYER_PROPERTY_FRONTSER);
				if (StringUtils.equals(gate, data)) {
					// 踢出和即将的离线网关连接的玩家
					player.offLine();
					deletePlayer(player,true);
					classSet.destroyGameObject(player);
				}
			}
		}
	}

	public void sendMsgToServer(String sername, int msgid, byte[] data) {
		gameLogic.getServer().sendMsgToServer(sername, msgid, data);
	}

	public void requestToServer(String sername, int msgid, byte[] data, Consumer<byte[]> cb) {
		gameLogic.getServer().request(sername, msgid, data, cb);
	}

	public void addOrUpdatePlayerDailyPlayData(List<PlayerDailyPlayData> list){
		InnerMsg.String.Builder build = InnerMsg.String.newBuilder();
		build.setValue(JsonUtil.encodeToStr(list));
		gameLogic.getServer().sendMsgToServer(framework.ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_STORE_MOJIN_DATA.ordinal(),
				build.build().toByteArray());
	}

    @Override
    public List<GamePlayer> listPlayer(List<Integer> uidList) {
		return uidList.stream()
				.filter(playersByUid::containsKey)
				.map(uid -> (GamePlayer) classSet.getGameObject(playersByUid.get(uid)))
				.collect(Collectors.toList());
    }
	public HttpClientApi getHttpClient(){
		return gameLogic.getHttpClient();
	}

	@Override
	public BaseServer getServer() {
		return gameLogic.getServer();
	}

	public JSONObject getPlayerLog(IGameObject player){
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("palyer",player.getProperty("Uid"));
		return jsonObject;
	}
	public JSONObject getActivityLog(IGameObject player){
		JSONObject jsonObject = new JSONObject();
		return jsonObject;
	}

}
