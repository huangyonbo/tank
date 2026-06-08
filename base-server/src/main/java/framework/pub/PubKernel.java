package framework.pub;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import framework.*;
import framework.game.ICfgReader;
import framework.game.MailSystemDef;
import framework.game.MailTypeDef;
import framework.game.XmlReader;
import framework.logic.PublicLogic;
import framework.mybatis.service.AbstractService;
import framework.net.InnerMsgDef;
import framework.net.SendMessage;
import framework.net.message.InnerMsg;
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

public class PubKernel implements IPubKernel {

	private Map<String, Set<MethodCallBackData>> m_onLoadCB = new HashMap<>();

	private static Logger logger = LoggerFactory.getLogger(PubKernel.class);
	private Map<String, IPubModule> modules = new HashMap<>();
	private PublicLogic publicLogic;

	private Map<Integer, Set<MethodCallBackData>> serverMsg = new HashMap<>();

	private Map<Integer, IRequestCallback> mapReqs = new HashMap<>();
	private Map<Integer, RecReqData> serverResponses = new HashMap<>();
	private Map<Integer, MethodCallBackData> serverRequest = new HashMap<>();
	private int reqId = 0;
	private Map<Integer, MethodCallBackData> stopEvents = new HashMap<>();
	private int stopOrder = 1;
	
	public boolean onInit(PublicLogic logic) {
		publicLogic = logic;
		try {
			loadModules("pub.modules");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		// init modules
		for (Entry<String, IPubModule> entry : modules.entrySet()) {
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

								if (IPubModule.class.isAssignableFrom(c)) {
									String[] n = c.getName().split("\\.");

									IPubModule module = null;
									try {
										Constructor<?> cons = c.getConstructor(IPubKernel.class);
										module = (IPubModule) cons.newInstance(this);
									} catch (NoSuchMethodException e) {
										module = (IPubModule) c.newInstance();
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

				if (IPubModule.class.isAssignableFrom(c)) {
					String[] n = c.getName().split("\\.");

					IPubModule module = null;
					try {
						Constructor<?> cons = c.getConstructor(IPubKernel.class);
						module = (IPubModule) cons.newInstance(this);
					} catch (NoSuchMethodException e) {
						module = (IPubModule) c.newInstance();
					}
					addModule(n[n.length - 1], module);
				}
			}
		}
	}

	public void addModule(String name, IPubModule module) {
		if (modules.containsKey(name)) {
			logger.error("Module {} is exist, please check and fix", name);
			return;
		}
		modules.put(name, module);
	}

	public IPubModule getModule(String name) {
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
		data.methodIndex = data.access.getIndex(methodName, IPubKernel.class, int.class, int.class, byte[].class);

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
		cb.methodIndex = cb.access.getIndex(methodName, IPubKernel.class, int.class, byte[].class);

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

		publicLogic.getServer().sendMsgToServer(serid, InnerMsgDef.INNER_MSG_CUSTOM_MSG.ordinal(),
				build.build().toByteArray());
	}

	public void sendServerMsg(String sername, int msgid, byte[] data) {
		InnerMsg.CustomMsg.Builder build = InnerMsg.CustomMsg.newBuilder();
		build.setMsgid(msgid);
		if (data != null) {
			build.setData(ByteString.copyFrom(data));
		}

		publicLogic.getServer().sendMsgToServer(sername, InnerMsgDef.INNER_MSG_CUSTOM_MSG.ordinal(),
				build.build().toByteArray());
	}

	public void broadToServer(String type, int msgid, byte[] data) {
		InnerMsg.CustomMsg.Builder build = InnerMsg.CustomMsg.newBuilder();
		build.setMsgid(msgid);
		if (data != null) {
			build.setData(ByteString.copyFrom(data));
		}

		SendMessage msg = new SendMessage(InnerMsgDef.INNER_MSG_CUSTOM_MSG.ordinal(), build.build().toByteArray());

		publicLogic.getServer().broadToServer(type, msg);
	}

	public void requestServer(int serid, int msgid, byte[] data, IRequestCallback cb) {
		if (reqId < 0){
			reqId = 0;
		}
		int reqid = reqId++;
		mapReqs.put(reqid, cb);
		InnerMsg.Request.Builder build = InnerMsg.Request.newBuilder();
		build.setReqid(reqid);
		build.setMsgid(msgid);
		if (data != null) {
			build.setData(ByteString.copyFrom(data));
		}
		publicLogic.getServer().sendMsgToServer(serid, InnerMsgDef.INNER_MSG_CUSTOM_REQUEST.ordinal(), build.build().toByteArray());
	}

	public void requestServer(String sername, int msgid, byte[] data, IRequestCallback cb) {
		if (reqId < 0){
			reqId = 0;
		}
		int reqid = reqId++;
		mapReqs.put(reqid, cb);
		InnerMsg.Request.Builder build = InnerMsg.Request.newBuilder();
		build.setReqid(reqid);
		build.setMsgid(msgid);
		if (data != null) {
			build.setData(ByteString.copyFrom(data));
		}
		publicLogic.getServer().sendMsgToServer(sername, InnerMsgDef.INNER_MSG_CUSTOM_REQUEST.ordinal(), build.build().toByteArray());
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

		publicLogic.getServer().sendMsgToServer(req.serId, InnerMsgDef.INNER_MSG_CUSTOM_RESPONSE.ordinal(),
				build.build().toByteArray());
		serverResponses.remove(reqid);
	}

	public void onDestroy() {
		//destroy modules
		for (Entry<String, IPubModule> entry : modules.entrySet()) {
			entry.getValue().onDestroy();
		}
	}

	public PubData getPubData(String name, boolean create) {
		PubData data = PubUtils.loadData(publicLogic.getJedis(),name);
		if (data == null && create){
			data = new PubData(name);
		}
		return data;
	}

	public boolean storePubData(IPubData pubData){
		return PubUtils.storeData(publicLogic.getJedis(),(PubData)pubData);
	}

	public boolean deletePubData(String pubName){
		return PubUtils.deleteData(publicLogic.getJedis(),pubName);
	}

	public void regOnLoadEvent(String spacename, Object listener, String methodName) {
		MethodCallBackData data = new MethodCallBackData();
		data.listener = listener;
		data.access = MethodAccessCache.tryToGet(listener.getClass());
		data.methodIndex = data.access.getIndex(methodName, IPubKernel.class, String.class);

		if (!m_onLoadCB.containsKey(spacename)) {
			Set<MethodCallBackData> temp = new HashSet<MethodCallBackData>();
			temp.add(data);
			m_onLoadCB.put(spacename, temp);
		} else {
			m_onLoadCB.get(spacename).add(data);
		}
	}

	public PublicLogic getPublicLogic() {
		return publicLogic;
	}

	public void onLoadPubSpaceComp(String name) {
		if (!m_onLoadCB.containsKey(name)) {
			return;
		}
		for (MethodCallBackData cb : m_onLoadCB.get(name)) {
			cb.access.invoke(cb.listener, cb.methodIndex, this, name);
		}
	}

	public ICfgReader loadXmlConfig(String path) {
		XmlReader reader = new XmlReader();
		if (!reader.loadConfig(path)) {
			return null;
		}

		return reader;
	}

	public InnerMsg.PubSpaceListRes getPsList() {
		InnerMsg.PubSpaceListRes.Builder build = InnerMsg.PubSpaceListRes.newBuilder();
		return build.build();
	}

	@Override
	public void addOrder(int uid, int channel, String name, String item, String material, boolean isComp,
						 String fullName, String qq, String wechat, String cellphone, String address,int vip) {
		publicLogic.addOrder(uid, channel, name, item, material, isComp, fullName, qq, wechat, cellphone, address,vip);
	}

	public long getServerTime() {
		return publicLogic.getServer().getServerTime();
	}

	public void addDayPlayWin(String ser, int roomType, long date, long tp, long tw) {
		publicLogic.addRunRecord(ser, roomType, date, tp, tw);
	}

	public void updateOnlineCount(int channel, int count) {
		InnerMsg.OnlinePeak.Builder build = InnerMsg.OnlinePeak.newBuilder();
		build.setChannel(channel);
		build.setCount(count);
		build.setDate(publicLogic.getServer().getDayFormat().format(getServerTime()));
		byte[] data = build.build().toByteArray();
		publicLogic.getServer().sendMsgToServer(framework.ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_UPDATE_ONLINEPEAK.ordinal(), data);
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
		publicLogic.getServer().sendMsgToServer(framework.ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_SEND_MAIL.ordinal(),build.build().toByteArray());
	}

	public boolean sendSystemMail(int recvuid, int channel, String title, String context, long lifetime,
								  String appendix) {
		String recvName = "";
		sendMail(MailTypeDef.MAIL_SYSTEM.ordinal(), channel, title, context, -1, "System", recvuid, recvName, lifetime,
				appendix, MailSystemDef.MAIL_NORMAL);
		return true;
	}

	@Override
	public boolean sendSystemMail(int recvuid, int channel, String title, String context, long lifetime,
								  String appendix, MailSystemDef system) {
		String recvName = "";
		sendMail(MailTypeDef.MAIL_SYSTEM.ordinal(), channel, title, context, -1, "System", recvuid, recvName, lifetime,
				appendix, system);

		return true;
	}

	public void reqSystemMail(int recvuid, int channel, String title, String context, long lifetime, String appendix,
							  Consumer<byte[]> cb) {
		InnerMsg.SendMail.Builder build = InnerMsg.SendMail.newBuilder();
		build.setType(MailTypeDef.MAIL_SYSTEM.ordinal());
		build.setChannel(channel);
		build.setTitle(title);
		build.setContext(context);
		build.setSenderuid(-1);
		build.setSendername("System");
		build.setRecvuid(recvuid);
		build.setRecvname("");
		build.setLifetime(lifetime);
		build.setAppendix(appendix);
		publicLogic.getServer().request(framework.ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_REQ_SEND_MAIL.ordinal(),build.build().toByteArray(),cb);
	}

	public void addArenaRecord(int gameid, int turnid, int signPop, int signCount, int joinPop, int joinCount,
							   long start, long end, String signs, String rewards, IoBuffer ranks) {
		publicLogic.addArenaRecord(gameid, turnid, signPop, signCount, joinPop, joinCount, start, end, signs, rewards,ranks);
	}

	@Override
	public void cardStats(String statsTime, int uid, int friendAmount, int friendRecharge, int fansRecharge,
						  int cardAmount, Consumer<Boolean> cb) {
		InnerMsg.CardStats.Builder builder = InnerMsg.CardStats.newBuilder();
		builder.setStatsTime(statsTime);
		builder.setUid(uid);
		builder.setFriendAmount(friendAmount);
		builder.setFriendRecharge(friendRecharge);
		builder.setFansRecharge(fansRecharge);
		builder.setCardAmount(cardAmount);
		publicLogic.getServer().request(framework.ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_CARD_STATS.ordinal(),
		builder.build().toByteArray(), (byte[] data) -> {
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
	public void createListHistory(String type, String name, byte[] data, String createTime) {
		publicLogic.addListHistory(type, name, data, createTime);
	}

	@Override
	public void addAreaRoomLog(int id, long startTime, long endTime, String rank) {
		String now = publicLogic.getServer().getTimeFormat().format(getServerTime());
		String log = new StringBuilder().append(now).append("|").append(id).append("|")
				.append(publicLogic.getServer().getTimeFormat().format(startTime)).append("|").append(publicLogic.getServer().getTimeFormat().format(endTime)).append("|")
				.append(rank).append("|").toString();
		InnerMsg.BaseValue.Builder build = InnerMsg.BaseValue.newBuilder();
		build.setStrValue(log);
		publicLogic.getServer().sendMsgToServer(framework.ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_AREA_ROOM_LOG.ordinal(), build.build().toByteArray());
	}
	
	@Override
	public void executeSomeToStore(Class<? extends AbstractService<?>> requireType, String method, List<Object> objects, Consumer<String> cb) {
		publicLogic.executeSomeToStore(requireType,method,objects,cb);
	}
	
	public void regStopListener(Object listener, int order, String methodName) {
		if (stopEvents.containsKey(order)){
			logger.error("StopHandler for msg[{}] is exist, please check and retry.", order);
			return;
		}
		MethodCallBackData data = new MethodCallBackData();
		data.listener = listener;
		data.access = MethodAccessCache.tryToGet(listener.getClass());
		data.methodIndex = data.access.getIndex(methodName,IPubKernel.class,int.class,String.class);
		stopEvents.put(order,data);
	}
	
	public boolean runStopByOrder(){
		MethodCallBackData cb = stopEvents.remove(stopOrder);
		if (cb != null) {
			boolean result = (boolean) cb.access.invoke(cb.listener, cb.methodIndex, this, stopOrder, null);
			if (result) {
				String[] methodNames = cb.access.getMethodNames();
				logger.info("order: [{}], [{}.{}] execute success", stopOrder, cb.listener.getClass().getName(), methodNames[cb.methodIndex]);
				stopOrder++;
			}
			return false;
		}else if (stopEvents.size() > 0){
			stopOrder++;
			return false;
		}
		BaseServer server = publicLogic.getServer();
		InnerMsg.NotifyNextReady.Builder builder = InnerMsg.NotifyNextReady.newBuilder();
		builder.setName(server.getName());
		byte[] datas = builder.build().toByteArray();
		SendMessage msg = new SendMessage(InnerMsgDef.INNER_MSG_NOTIFY_ME_CLOSE.ordinal(),datas);
		server.broadToServer("game",msg);
		server.broadToServer("store",msg);
		return true;
	}

	public void execute() {

	}

	@Override
	public BaseServer getServer() {
		return publicLogic.getServer();
	}
}
