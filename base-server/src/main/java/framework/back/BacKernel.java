package framework.back;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import framework.*;
import framework.game.MailSystemDef;
import framework.game.MailTypeDef;
import framework.logic.BackLogic;
import framework.net.InnerMsgDef;
import framework.net.SendMessage;
import framework.net.message.InnerMsg;
import framework.pub.PubUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class BacKernel {
	private static Logger logger = LoggerFactory.getLogger(BacKernel.class);
	private Map<String, IBackModule> m_mapModules = new HashMap<>();
	private BackLogic m_BackLogic = null;

	private Map<Integer, Set<MethodCallBackData>> m_serverMsg = new HashMap<>();

	private Map<Integer, IRequestCallback> m_mapReqs = new HashMap<>();
	private Map<Integer, RecReqData> m_serverResponses = new HashMap<>();
	private Map<Integer, MethodCallBackData> m_serverRequest = new HashMap<>();
	//Map<Integer, GameObjectData> m_loadedDatas = new HashMap<>();
	private int m_reqid = 0;

	public boolean onInit(BackLogic logic) {
		m_BackLogic = logic;
		try {
			loadModules("back.modules");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		//init modules
		for (Entry<String, IBackModule> entry : m_mapModules.entrySet()) {
			if (!entry.getValue().onInit(this)) {
				logger.error("Init module [{}] failed.", entry.getKey());
				return false;
			}
		}
		return true;
	}

	public void onDestroy() {
		for (Entry<String, IBackModule> entry : m_mapModules.entrySet()) {
			entry.getValue().onDestroy();
		}
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

								if (IBackModule.class.isAssignableFrom(c)) {
									String[] n = c.getName().split("\\.");

									IBackModule module = null;
									try {
										Constructor<?> cons = c.getConstructor(BacKernel.class);
										module = (IBackModule) cons.newInstance(this);
									} catch (NoSuchMethodException e) {
										module = (IBackModule) c.newInstance();
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

				if (IBackModule.class.isAssignableFrom(c)) {
					String[] n = c.getName().split("\\.");

					IBackModule module = null;
					try {
						Constructor<?> cons = c.getConstructor(BacKernel.class);
						module = (IBackModule) cons.newInstance(this);
					} catch (NoSuchMethodException e) {
						module = (IBackModule) c.newInstance();
					}
					addModule(n[n.length - 1], module);
				}
			}
		}
	}

	public void addModule(String name, IBackModule module) {
		if (m_mapModules.containsKey(name)) {
			logger.error("Module {} is exist, please check and fix", name);
			return;
		}
		m_mapModules.put(name, module);
	}

	public IBackModule getModule(String name) {
		if (m_mapModules.containsKey(name)) {
			return m_mapModules.get(name);
		}
		return null;
	}

	// 注册服务器消息
	public void regServerMsg(int msgid, Object listener, String methodName) {
		MethodCallBackData data = new MethodCallBackData();
		data.listener = listener;
		data.access = MethodAccessCache.tryToGet(listener.getClass());
		data.methodIndex = data.access.getIndex(methodName, BacKernel.class, int.class, int.class, byte[].class);
		if (!m_serverMsg.containsKey(msgid)) {
			Set<MethodCallBackData> temp = new HashSet<MethodCallBackData>();
			temp.add(data);
			m_serverMsg.put(msgid, temp);
		} else {
			m_serverMsg.get(msgid).add(data);
		}
	}

	public void regServerRequest(int msgid, Object listener, String methodName) {
		if (m_serverRequest.containsKey(msgid)) {
			logger.error("Request handle for msg[{}] is exist, please check and retry.", msgid);
			return;
		}
		MethodCallBackData cb = new MethodCallBackData();
		cb.listener = listener;
		cb.access = MethodAccessCache.tryToGet(listener.getClass());
		cb.methodIndex = cb.access.getIndex(methodName, BacKernel.class, int.class, byte[].class);
		m_serverRequest.put(msgid, cb);
	}

	public void onRecServerMsg(int serid, int msgid, byte[] data) {
		Set<MethodCallBackData> cbs = m_serverMsg.get(msgid);
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
		MethodCallBackData cb = m_serverRequest.get(reqmsgid);
		if (cb == null) {
			return;
		}
		RecReqData reqData = new RecReqData();
		reqData.reqId = reqid;
		reqData.serId = serid;
		m_serverResponses.put(reqid, reqData);
		cb.access.invoke(cb.listener, cb.methodIndex, this, reqid, msg);
	}

	public void onServerResponse(int serid, byte[] data) throws InvalidProtocolBufferException {
		InnerMsg.Response req = InnerMsg.Response.parseFrom(data);
		int reqid = req.getReqid();
		byte[] msg = null;
		if (req.getData() != null) {
			msg = req.getData().toByteArray();
		}
		if (!m_mapReqs.containsKey(reqid)) {
			return;
		}
		IRequestCallback cb = m_mapReqs.get(reqid);
		cb.execute(msg);

		m_mapReqs.remove(reqid);
	}

	public void sendServerMsg(int serid, int msgid, byte[] data) {
		InnerMsg.CustomMsg.Builder build = InnerMsg.CustomMsg.newBuilder();
		build.setMsgid(msgid);
		if (data != null) {
			build.setData(ByteString.copyFrom(data));
		}
		m_BackLogic.getServer().sendMsgToServer(serid, InnerMsgDef.INNER_MSG_CUSTOM_MSG.ordinal(),build.build().toByteArray());
	}
	
	public void sendServerMsg(String sername, int msgid, byte[] data) {
		InnerMsg.CustomMsg.Builder build = InnerMsg.CustomMsg.newBuilder();
		build.setMsgid(msgid);
		if (data != null) {
			build.setData(ByteString.copyFrom(data));
		}
		m_BackLogic.getServer().sendMsgToServer(sername, InnerMsgDef.INNER_MSG_CUSTOM_MSG.ordinal(),build.build().toByteArray());
	}

	public void broadToServer(String type, int msgid, byte[] data) {
		InnerMsg.CustomMsg.Builder build = InnerMsg.CustomMsg.newBuilder();
		build.setMsgid(msgid);
		if (data != null) {
			build.setData(ByteString.copyFrom(data));
		}
		SendMessage msg = new SendMessage(InnerMsgDef.INNER_MSG_CUSTOM_MSG.ordinal(), build.build().toByteArray());
		m_BackLogic.getServer().broadToServer(type, msg);
	}

	public void requestServer(int serid, int msgid, byte[] data, IRequestCallback cb) {
		if (m_reqid < 0){
			m_reqid = 0;
		}
		int reqid = m_reqid++;
		m_mapReqs.put(reqid, cb);
		InnerMsg.Request.Builder build = InnerMsg.Request.newBuilder();
		build.setReqid(reqid);
		build.setMsgid(msgid);
		if (data != null) {
			build.setData(ByteString.copyFrom(data));
		}
		m_BackLogic.getServer().sendMsgToServer(serid, InnerMsgDef.INNER_MSG_CUSTOM_REQUEST.ordinal(), build.build().toByteArray());
	}

	public void requestServer(String sername, int msgid, byte[] data, IRequestCallback cb) {
		if (m_reqid < 0){
			m_reqid = 0;
		}
		int reqid = m_reqid++;
		m_mapReqs.put(reqid, cb);
		InnerMsg.Request.Builder build = InnerMsg.Request.newBuilder();
		build.setReqid(reqid);
		build.setMsgid(msgid);
		if (data != null) {
			build.setData(ByteString.copyFrom(data));
		}
		m_BackLogic.getServer().sendMsgToServer(sername, InnerMsgDef.INNER_MSG_CUSTOM_REQUEST.ordinal(), build.build().toByteArray());
	}

	public void responseServer(int reqid, byte[] data) {
		if (!m_serverResponses.containsKey(reqid)) {
			return;
		}
		RecReqData req = m_serverResponses.get(reqid);
		InnerMsg.Response.Builder build = InnerMsg.Response.newBuilder();
		build.setReqid(req.reqId);
		if (data != null) {
			build.setData(ByteString.copyFrom(data));
		}
		m_BackLogic.getServer().sendMsgToServer(req.serId, InnerMsgDef.INNER_MSG_CUSTOM_RESPONSE.ordinal(),
				build.build().toByteArray());
		m_serverResponses.remove(reqid);
	}

	/**
	 * 获取玩家所在服务器
	 * 
	 * @param uid 玩家uid
	 * @return 所在服务器，空表示不在线
	 */
	public String getPlayerServer(int uid) {
		String playerKey = PubUtils.getKey("player_" + uid);
		String lastGame = "";
		try {
			lastGame = m_BackLogic.getJedis().hget(playerKey, "LastGame");
		} catch (Exception e) {
			//e.printStackTrace();
		}finally {
			if (lastGame==null){
				lastGame="";
			}
		}
		return lastGame;
	}

	public void loadPlayerFromDB(int uid, boolean needWrite, Object arg, ILoadDataCallBack cb) {
		InnerMsg.RequestRoleData.Builder builder = InnerMsg.RequestRoleData.newBuilder();
		builder.setUid(uid);
		byte[] data = builder.build().toByteArray();
		m_BackLogic.getServer().request(ServerSet.SERVER_LOGIC_NAME_STORE, needWrite ? InnerMsgDef.INNER_MSG_REQ_OFFLINE_ROLE.ordinal() : InnerMsgDef.INNER_MSG_REQ_READ_ROLE.ordinal(), data, (resmsg) -> {
			InnerMsg.LoadRoleData roleData = null;
			try {
				roleData = InnerMsg.LoadRoleData.parseFrom(resmsg);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			GameObjectData newPlayer = null;
			if (roleData.getCode() == 0) {
				newPlayer = new GameObjectData(needWrite);
				newPlayer.setUid(uid);
				byte[] data1 = roleData.getData().toByteArray();
				IoBuffer buff = IoBuffer.wrap(data1);
				newPlayer.loadFromArchive(buff);
			}
			cb.execute(newPlayer, arg);
			if (newPlayer != null){
				newPlayer.clear();
			}
		});
	}

	public void storePlayerData(GameObjectData player) {
		if (!player.canWrite()) {
			logger.info("!CanWrite");
			return;
		}

		int uid = player.getUid();
		IoBuffer buff = IoBuffer.allocate(10);
		buff.setAutoExpand(true);
		player.storeToArchive(buff);
		int size = buff.position();
		buff.flip();

		byte[] store = Arrays.copyOfRange(buff.array(), 0, size);

		InnerMsg.StoreRoleData.Builder builder = InnerMsg.StoreRoleData.newBuilder();
		builder.setUid(uid);
		builder.setData(ByteString.copyFrom(store));

		byte[] data = builder.build().toByteArray();
		SendMessage msg = new SendMessage(InnerMsgDef.INNER_MSG_STORE_OFFROLE.ordinal(), data);
		m_BackLogic.getServer().getServerSet().getServer(ServerSet.SERVER_LOGIC_NAME_STORE).write(msg);

		// m_loadedDatas.remove(uid);
	}

	public void deletePlayerData(int uid) {
		// m_loadedDatas.remove(uid);
	}

	public Object[] getServersByType(String type) {
		return m_BackLogic.getServer().getServerSet().getServersByType(type);
	}

	public ServerConfig getServerCfg(String name) {
		return m_BackLogic.getServer().getServerSet().getServerConfig(name);
	}

	public long getServerTime() {
		return m_BackLogic.getServer().getServerTime();
	}

	public void addOfflineData(int uid, int type, String context, String reason) {
		InnerMsg.OfflineData.Builder data = InnerMsg.OfflineData.newBuilder();
		data.setUid(uid);
		data.setType(type);
		data.setContext(context);
		data.setReason(reason);
		m_BackLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_OFFLINEDATA.ordinal(), data.build().toByteArray());
	}

	public void frozen(int uid) {
		InnerMsg.KickPlayer.Builder build = InnerMsg.KickPlayer.newBuilder();
		build.setUid(uid);
		build.setCode(1);
		SendMessage msg = new SendMessage(InnerMsgDef.INNER_MSG_KICK_PLAYER.ordinal(), build.build().toByteArray());
		m_BackLogic.getServer().broadToServer("gate", msg);
		InnerMsg.Frozen.Builder build1 = InnerMsg.Frozen.newBuilder();
		build1.setUid(uid);
		m_BackLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_FROZEN.ordinal(),build1.build().toByteArray());
	}

	public void unFrozen(int uid) {
		InnerMsg.Frozen.Builder build = InnerMsg.Frozen.newBuilder();
		build.setUid(uid);
		m_BackLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_UNFROZEN.ordinal(),
				build.build().toByteArray());
	}

	private void sendMail(int type, int channel, String title, String context, int senduid, String sendName,
						  int recvuid, String recvName, long lifeTime, String appendix) {
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
		build.setSystem(MailSystemDef.MAIL_BACK.ordinal());
		m_BackLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_SEND_MAIL.ordinal(),build.build().toByteArray());
	}

	public boolean sendSystemMail(int recvuid, int channel, String title, String context, long lifetime,
								  String appendix) {
		String recvName = "";
		sendMail(MailTypeDef.MAIL_SYSTEM.ordinal(), channel, title, context, -1, "System", recvuid, recvName, lifetime,
				appendix);

		return true;
	}

	public void addBlackList(int type, String context, Consumer<Boolean> cb) {
		InnerMsg.BlackList.Builder build = InnerMsg.BlackList.newBuilder();
		build.setType(type);
		build.setContext(context);

		m_BackLogic.getServer().request(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_ADD_BLACKLIST.ordinal(),
		build.build().toByteArray(), (byte[] res) -> {
			cb.accept(true);

			SendMessage msg = new SendMessage(InnerMsgDef.INNER_MSG_ADD_BLACKLIST.ordinal(),
					build.build().toByteArray());
			m_BackLogic.getServer().broadToServer("game", msg);
		});
	}

	public void delBlackList(int type, String context, Consumer<Boolean> cb) {
		InnerMsg.BlackList.Builder build = InnerMsg.BlackList.newBuilder();
		build.setType(type);
		build.setContext(context);
		m_BackLogic.getServer().request(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_DEL_BLACKLIST.ordinal(), build.build().toByteArray(), (res) -> {
			cb.accept(true);
		});
	}

	/**
	 * 玩家日志（道具+部分游玩）
	 * 日志类型：0-道具获得，1-道具消耗，2-进入房间，3-退出房间，4-清除总玩总赢，5-击杀大鱼，6-定时游玩，7-属性变化 日志内容：
	 * 0-道具获得，1-道具消耗： 【原数量，变化量，变化后量】 2-进入房间，3-退出房间： 【】 4-清除总玩总赢： 【房间id：总玩，总赢；】
	 * 5-击杀大鱼： 【掉落信息】 6-定时游玩： 【】 7-属性变化： 【原数量 -> 变化后量】
	 * |时间|玩家uid|日志类型|房间id|系统id|道具id/属性名|渠道id|登录时间|注册时间|VIP等级|玩家等级|金币|钻石|彩券|总玩|
	 * 总赢|日志内容|日志原因
	 */
	public void addPlayerLog(String name, int uid, int vipLevel, int level, long gold, long diamond, long bombCoin, long colorTicket,
							 int type, int system, String context, String reason) {
		int roomType = -1;
		String now = m_BackLogic.getServer().getTimeFormat().format(getServerTime());
		String log = new StringBuilder().append(now).append("|").append(uid).append("|").append(type).append("|")
				.append(roomType).append("|").append(system).append("|").append(name).append("|").append(-1).append("|")
				.append("").append("|").append("").append("|").append(vipLevel).append("|").append(level).append("|")
				.append(gold).append("|").append(diamond).append("|").append(bombCoin).append("|").append(colorTicket).append("|").append(0L)
				.append("|").append(0L).append("|").append(context).append("|").append(reason).append("|").toString();

		InnerMsg.BaseValue.Builder build = InnerMsg.BaseValue.newBuilder();
		build.setStrValue(log);
		m_BackLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_PLAYER_LOG.ordinal(),
				build.build().toByteArray());
	}
	
	public void regColonyRespServerMsg(Object listener, int type, String methodName){
		m_BackLogic.getServer().addColonyRespListener(listener,type,methodName);
	}
	
	public BaseServer getServer(){
		return m_BackLogic.getServer();
	}


	public void execute() {

	}
}
