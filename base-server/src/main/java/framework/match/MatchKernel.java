package framework.match;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import framework.*;
import framework.Store.ParamData;
import framework.game.ICfgReader;
import framework.game.XmlReader;
import framework.logic.MatchLogic;
import framework.mybatis.domain.CustomGame;
import framework.mybatis.service.AbstractService;
import framework.mybatis.service.impl.CustomGameService;
import framework.mybatis.service.impl.PlayRobotService;
import framework.net.InnerMsgDef;
import framework.net.SendMessage;
import framework.net.message.InnerMsg;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MatchKernel {
	private static Logger logger = LoggerFactory.getLogger(MatchKernel.class);
	private Map<String, IMatchModule> modules = new HashMap<>();
	private MatchLogic matchLogic = null;

	private Map<Integer, Set<MethodCallBackData>> serverMsg = new HashMap<>();

	private Map<Integer, IRequestCallback> mapReqs = new HashMap<>();
	private Map<Integer, RecReqData> serverResponses = new HashMap<>();
	private Map<Integer, MethodCallBackData> serverRequest = new HashMap<>();
	private Set<IMatchModule> frameRunModules = new HashSet<>();
	private Set<MethodCallBackData> robotLoadCb = new HashSet<>();
	private Set<MethodCallBackData> customGameLoadCb = new HashSet<>();
	private int reqId = 0;

	public boolean onInit(MatchLogic logic) {
		matchLogic = logic;
		try {
			loadModules("match.modules");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		// init modules
		for (Entry<String, IMatchModule> entry : modules.entrySet()) {
			if (!entry.getValue().onInit(this)) {
				logger.error("Init module [{}] failed.", entry.getKey());
				return false;
			}
		}
		return true;
	}

	public void loadModules(String packageName)
			throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException,
			NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		String path = packageName.replace('.', '/');

		Enumeration<URL> resources = classLoader.getResources(path);

		while (resources.hasMoreElements()) {
			URL resource = (URL) resources.nextElement();
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

								if (IMatchModule.class.isAssignableFrom(c)) {
									String[] n = c.getName().split("\\.");

									IMatchModule module = null;
									try {
										Constructor<?> cons = c.getConstructor(MatchKernel.class);
										module = (IMatchModule) cons.newInstance(this);
									} catch (NoSuchMethodException e) {
										module = (IMatchModule) c.newInstance();
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

				if (IMatchModule.class.isAssignableFrom(c)) {
					String[] n = c.getName().split("\\.");

					IMatchModule module = null;
					try {
						Constructor<?> cons = c.getConstructor(MatchKernel.class);
						module = (IMatchModule) cons.newInstance(this);
					} catch (NoSuchMethodException e) {
						module = (IMatchModule) c.newInstance();
					}
					addModule(n[n.length - 1], module);
				}
			}
		}
	}

	public void addModule(String name, IMatchModule module) {
		if (modules.containsKey(name)) {
			logger.error("Module {} is exist, please check and fix", name);
			return;
		}
		modules.put(name, module);
	}

	public IMatchModule getModule(String name) {
		if (modules.containsKey(name)) {
			return modules.get(name);
		}
		return null;
	}

	// 注册服务器消息
	public void regServerMsg(int msgid, Object listener, String methodName) {
		MethodCallBackData data = new MethodCallBackData();
		data.listener = listener;
		data.access = MethodAccessCache.tryToGet(listener.getClass());
		data.methodIndex = data.access.getIndex(methodName, MatchKernel.class, int.class, int.class, byte[].class);

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
		cb.methodIndex = cb.access.getIndex(methodName, MatchKernel.class, int.class, byte[].class);

		serverRequest.put(msgid, cb);
	}

	public void onRecServerMsg(int serid, int msgid, byte[] data) {
		Set<MethodCallBackData> cbs = serverMsg.get(msgid);
		if (cbs == null) {
			return;
		}

		for (MethodCallBackData cb : cbs) {
			cb.access.invoke(cb.listener, cb.methodIndex, this, serid, msgid, data);
		}
	}

	public void onServerRequest(int serid, byte[] data) throws InvalidProtocolBufferException {
		InnerMsg.Request req = InnerMsg.Request.parseFrom(data);
		int reqid = req.getReqid();
		int reqmsgid = req.getMsgid();
		byte[] msg = null;
		if (req.getData() != null) {
			msg = req.getData().toByteArray();
		}

		MethodCallBackData cb = serverRequest.get(reqmsgid);
		if (cb == null) {
			return;
		}

		RecReqData reqData = new RecReqData();
		reqData.reqId = reqid;
		reqData.serId = serid;
		serverResponses.put(reqid, reqData);
		cb.access.invoke(cb.listener, cb.methodIndex, this, reqid, msg);
	}

	public void onServerResponse(int serid, byte[] data) throws InvalidProtocolBufferException {
		InnerMsg.Response req = InnerMsg.Response.parseFrom(data);
		int reqid = req.getReqid();
		byte[] msg = null;
		if (req.getData() != null) {
			msg = req.getData().toByteArray();
		}

		if (!mapReqs.containsKey(reqid)) {
			return;
		}

		IRequestCallback cb = mapReqs.get(reqid);
		cb.execute(msg);

		mapReqs.remove(reqid);
	}

	public void sendServerMsg(int serid, int msgid, byte[] data) {
		InnerMsg.CustomMsg.Builder build = InnerMsg.CustomMsg.newBuilder();
		build.setMsgid(msgid);
		if (data != null) {
			build.setData(ByteString.copyFrom(data));
		}

		matchLogic.getServer().sendMsgToServer(serid, InnerMsgDef.INNER_MSG_CUSTOM_MSG.ordinal(),
				build.build().toByteArray());
	}

	public void sendServerMsg(String sername, int msgid, byte[] data) {
		InnerMsg.CustomMsg.Builder build = InnerMsg.CustomMsg.newBuilder();
		build.setMsgid(msgid);
		if (data != null) {
			build.setData(ByteString.copyFrom(data));
		}

		matchLogic.getServer().sendMsgToServer(sername, InnerMsgDef.INNER_MSG_CUSTOM_MSG.ordinal(),
				build.build().toByteArray());
	}

	public void broadToServer(String type, int msgid, byte[] data) {
		InnerMsg.CustomMsg.Builder build = InnerMsg.CustomMsg.newBuilder();
		build.setMsgid(msgid);
		if (data != null) {
			build.setData(ByteString.copyFrom(data));
		}

		SendMessage msg = new SendMessage(InnerMsgDef.INNER_MSG_CUSTOM_MSG.ordinal(), build.build().toByteArray());

		matchLogic.getServer().broadToServer(type, msg);
	}

	public void requestServer(int serid, int msgid, byte[] data, IRequestCallback cb) {
		if (reqId < 0){
			reqId = 0;
		}
		int reqid = this.reqId++;
		mapReqs.put(reqid, cb);
		InnerMsg.Request.Builder build = InnerMsg.Request.newBuilder();
		build.setReqid(reqid);
		build.setMsgid(msgid);
		if (data != null) {
			build.setData(ByteString.copyFrom(data));
		}
		matchLogic.getServer().sendMsgToServer(serid, InnerMsgDef.INNER_MSG_CUSTOM_REQUEST.ordinal(), build.build().toByteArray());
	}

	public void requestServer(String sername, int msgid, byte[] data, IRequestCallback cb) {
		if (reqId < 0){
			reqId = 0;
		}
		int reqid = this.reqId++;
		mapReqs.put(reqid, cb);
		InnerMsg.Request.Builder build = InnerMsg.Request.newBuilder();
		build.setReqid(reqid);
		build.setMsgid(msgid);
		if (data != null) {
			build.setData(ByteString.copyFrom(data));
		}
		matchLogic.getServer().sendMsgToServer(sername, InnerMsgDef.INNER_MSG_CUSTOM_REQUEST.ordinal(), build.build().toByteArray());
	}

	public void responseServer(int reqid, byte[] data) {
		if (!serverResponses.containsKey(reqid)) {
			return;
		}

		RecReqData req = serverResponses.get(reqid);
		InnerMsg.Response.Builder build = InnerMsg.Response.newBuilder();
		build.setReqid(req.reqId);
		if (data != null) {
			build.setData(ByteString.copyFrom(data));
		}

		matchLogic.getServer().sendMsgToServer(req.serId, InnerMsgDef.INNER_MSG_CUSTOM_RESPONSE.ordinal(),
				build.build().toByteArray());
		serverResponses.remove(reqid);
	}

	public void addFrameRun(IMatchModule module) {
		frameRunModules.add(module);
	}

	// public void Run()
	// {
	// for(IMatchModule module : m_frameRunModules)
	// {
	// module.Run();
	// }
	// }

	public long getServerTime() {
		return matchLogic.getServer().getServerTime();
	}

	public Object[] getServersByType(String type) {
		return matchLogic.getServer().getServerSet().getServersByType(type);
	}

	public String getServerNameByID(int serid) {
		return matchLogic.getServer().getServerSet().getServerConfig(serid).name;
	}

	public void regRobotLoadEvent(Object listener, String methodName) {
		MethodCallBackData data = new MethodCallBackData();
		data.listener = listener;
		data.access = MethodAccessCache.tryToGet(listener.getClass());
		data.methodIndex = data.access.getIndex(methodName, MatchKernel.class, ResultSet.class);

		robotLoadCb.add(data);
	}

	public void regCustomGameLoadEvent(Object listener, String methodName) {
		MethodCallBackData data = new MethodCallBackData();
		data.listener = listener;
		data.access = MethodAccessCache.tryToGet(listener.getClass());
		data.methodIndex = data.access.getIndex(methodName, MatchKernel.class, ResultSet.class);

		customGameLoadCb.add(data);
	}

	public void loadRobot(ResultSet robot) {
		for (MethodCallBackData cb : robotLoadCb) {
			cb.access.invoke(cb.listener, cb.methodIndex, this, robot);
		}
	}

	public void loadCustomGame(ResultSet game) {
		for (MethodCallBackData cb : customGameLoadCb) {
			cb.access.invoke(cb.listener, cb.methodIndex, this, game);
		}
	}

	public void changeRobotState(int uid, int state) {
		List<Object> params = new ArrayList<>();
		params.add(uid);
		params.add(state);
		executeSomeToStore(PlayRobotService.class, "updateRobot", params, null);
	}

	public void updateRobot(int uid, int level, long diamond, long gold, String bag) throws SQLException {
		List<Object> params = new ArrayList<>();
		params.add(uid);
		params.add(level);
		params.add(diamond);
		params.add(gold);
		params.add(StringUtils.isEmpty(bag) ? "" : bag);
		executeSomeToStore(PlayRobotService.class, "updateRobot", params, null);
	}

	// 更新密码房
	public void updatePwGame(int deskid, int uid, int type, int roomType, int level, int minbv, int maxbv, int online,
							 long totalP, long totalW, String enterLimit, String passwd) {
//		logger.info("UpdatePwGame {} {} {} {} {} {} {} {} {} {} {} {}", deskid, uid, type, roomType, level, minbv, maxbv, online, totalP, totalW, enterLimit, passwd);
		if (type != 1) {
			// 只有密码房能新增
//			logger.info("只有密码房能新增");
			return;
		}
		CustomGame customGame = new CustomGame();

		if (uid != -1) {
			customGame.setCreateBy(uid);
		}
		
		if (enterLimit == null){
			logger.error("UpdatePwGame {} {} {} {} {} {} {} {} {} {} {} {}", deskid, uid, type, roomType, level, minbv,
				maxbv, online, totalP, totalW, enterLimit, passwd);
		}
		
		// 海神殿
		if (roomType >= 12 && enterLimit != null) {
			customGame.setEnterLimit(enterLimit);
		} else {
			customGame.setEnterLimit("-");
		}
		customGame.setId(deskid);
		customGame.setType(type);
		customGame.setRoomType(roomType);
		customGame.setOnline(online);
		customGame.setTotalPlay(totalP);
		customGame.setTotalWin(totalW);
		customGame.setStatus(1);
		customGame.setLevel(level);
		customGame.setMinBv(minbv);
		customGame.setMaxBv(maxbv);
		customGame.setPasswd(passwd);
		customGame.setCreateTime(new Date());
		List<Object> params = new ArrayList<>();
		params.add(customGame);
		executeSomeToStore(CustomGameService.class, "UpdateOrAdd", params, null);
	}

	// 更新选桌房间的在线人数 总玩总赢 相对于UpdatePwGame更新了变动的数据
	public void updateOneOnline(int deskid, int online, long totalPlay, long totalWin) {
		//logger.info("UpdateOnline {} {} {} {}", deskid, online, totalWin, totalPlay);
		List<Object> params = new ArrayList<>();
		params.add(deskid);
		params.add(online);
		params.add(totalPlay);
		params.add(totalWin);
		executeSomeToStore(CustomGameService.class, "updateOneOnline", params, null);
	}

	public ICfgReader loadXmlConfig(String path) {
		XmlReader reader = new XmlReader();
		if (!reader.loadConfig(path)) {
			return null;
		}
		return reader;
	}

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
		matchLogic.getServer().request(framework.ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_EXECUTE_SQL_METHOD.ordinal(), msg, (bytes) -> {
			try {
				InnerMsg.ComeFromDbData datas = InnerMsg.ComeFromDbData.parseFrom(bytes);
				int code = datas.getCode();
				if (code == 0) {
					if (cb != null) {
						String _data = datas.getDatas();
						cb.accept(_data.length() == 0 ? null : _data);
					}
				} else {
					if (cb != null) {
						cb.accept(null);
					}
					//logger.error("LoadDataFromDB error code = " + code);
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
		for (IMatchModule module : modules.values()) {
			module.onNetReady(this);
		}
	}

	public void execute() {

	}
}
