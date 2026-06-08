package framework.master;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import framework.*;
import framework.game.MailSystemDef;
import framework.logic.MasterLogic;
import framework.net.InnerMsgDef;
import framework.net.SendMessage;
import framework.net.message.InnerMsg;
import org.apache.mina.core.session.IoSession;
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
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class MasterKernel {

	class CmdData {
		IoSession session;
		long start;
	}

	private static Logger logger = LoggerFactory.getLogger(MasterKernel.class);
	private Map<String, IMasterModule> m_mapModules = new HashMap<>();
	private MasterLogic m_MasterLogic = null;

	private Map<Integer, Set<MethodCallBackData>> m_serverMsg = new HashMap<>();

	private Map<Integer, IRequestCallback> m_mapReqs = new HashMap<>();
	private Map<Integer, RecReqData> m_serverResponses = new HashMap<>();
	private Map<Integer, MethodCallBackData> m_serverRequest = new HashMap<>();
	private Map<Integer, CmdData> m_cmdResponses = new HashMap<>();

	private Map<String, MethodCallBackData> m_cmdMsg = new HashMap<>();
	private int m_reqid = 0;
	private int m_cmdid = 0;

	public boolean onInit(MasterLogic logic) {
		m_MasterLogic = logic;
		try {
			loadModules("master.modules");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

		// init modules
		for (Entry<String, IMasterModule> entry : m_mapModules.entrySet()) {
			if (!entry.getValue().onInit(this)) {
				logger.error("Init module [{}] failed.", entry.getKey());
				return false;
			}
		}
		//RegCmd("shutdown", this, "OnCmdShutDown");
		regCmd("useWhiteList", this, "onCmdUseWhiteList");
		regCmd("perfSwitch", this, "onCmdPerfSwitch");
		regCmd("perfDump", this, "onCmdPerfDump");
		return true;
	}

	public void onDestroy() {
		// destroy modules
		for (Entry<String, IMasterModule> entry : m_mapModules.entrySet()) {
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

								if (IMasterModule.class.isAssignableFrom(c)) {
									String[] n = c.getName().split("\\.");

									IMasterModule module = null;
									try {
										Constructor<?> cons = c.getConstructor(MasterKernel.class);
										module = (IMasterModule) cons.newInstance(this);
									} catch (NoSuchMethodException e) {
										module = (IMasterModule) c.newInstance();
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

				if (IMasterModule.class.isAssignableFrom(c)) {
					String[] n = c.getName().split("\\.");

					IMasterModule module = null;
					try {
						Constructor<?> cons = c.getConstructor(MasterKernel.class);
						module = (IMasterModule) cons.newInstance(this);
					} catch (NoSuchMethodException e) {
						module = (IMasterModule) c.newInstance();
					}
					addModule(n[n.length - 1], module);
				}
			}
		}
	}

	public void addModule(String name, IMasterModule module) {
		if (m_mapModules.containsKey(name)) {
			logger.error("Module {} is exist, please check and fix", name);
			return;
		}
		m_mapModules.put(name, module);
	}

	public IMasterModule getModule(String name) {
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
		data.methodIndex = data.access.getIndex(methodName, MasterKernel.class, int.class, int.class, byte[].class);

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
		cb.methodIndex = cb.access.getIndex(methodName, MasterKernel.class, int.class, byte[].class);

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
		build.setData(ByteString.copyFrom(data));
		m_MasterLogic.getServer().sendMsgToServer(serid, InnerMsgDef.INNER_MSG_CUSTOM_MSG.ordinal(),build.build().toByteArray());
	}

	public void sendServerMsg(String sername, int msgid, byte[] data) {
		InnerMsg.CustomMsg.Builder build = InnerMsg.CustomMsg.newBuilder();
		build.setMsgid(msgid);
		build.setData(ByteString.copyFrom(data));
		m_MasterLogic.getServer().sendMsgToServer(sername, InnerMsgDef.INNER_MSG_CUSTOM_MSG.ordinal(),build.build().toByteArray());
	}

	public void broadToServer(String type, int msgid, byte[] data) {
		InnerMsg.CustomMsg.Builder build = InnerMsg.CustomMsg.newBuilder();
		build.setMsgid(msgid);
		build.setData(ByteString.copyFrom(data));
		SendMessage msg = new SendMessage(InnerMsgDef.INNER_MSG_CUSTOM_MSG.ordinal(), build.build().toByteArray());
		m_MasterLogic.getServer().broadToServer(type, msg);
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
		m_MasterLogic.getServer().sendMsgToServer(serid, InnerMsgDef.INNER_MSG_CUSTOM_REQUEST.ordinal(),build.build().toByteArray());
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
		m_MasterLogic.getServer().sendMsgToServer(sername, InnerMsgDef.INNER_MSG_CUSTOM_REQUEST.ordinal(),build.build().toByteArray());
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

		m_MasterLogic.getServer().sendMsgToServer(req.serId, InnerMsgDef.INNER_MSG_CUSTOM_RESPONSE.ordinal(),
				build.build().toByteArray());
		m_serverResponses.remove(reqid);
	}

	public void onCmd(IoSession session, String[] cmd) {
		if (!m_cmdMsg.containsKey(cmd[0])) {
			session.write("no handle for cmd " + cmd[0]);
			return;
		}

		int cmdid = m_cmdid++;
		CmdData data = new CmdData();
		data.session = session;
		data.start = getServerTime();
		m_cmdResponses.put(cmdid, data);

		MethodCallBackData cb = m_cmdMsg.get(cmd[0]);
		cb.access.invoke(cb.listener, cb.methodIndex, this, cmdid, cmd);
	}

	public void regCmd(String cmd, Object listener, String methodName) {
		if (m_cmdMsg.containsKey(cmd)) {
			logger.error("cmd handle for [{}] is exist, please check and retry.", cmd);
			return;
		}
		MethodCallBackData cb = new MethodCallBackData();
		cb.listener = listener;
		cb.access = MethodAccessCache.tryToGet(listener.getClass());
		cb.methodIndex = cb.access.getIndex(methodName, MasterKernel.class, int.class, String[].class);
		m_cmdMsg.put(cmd, cb);
	}

	public void responseCmd(int cmdid, String response) {
		if (!m_cmdResponses.containsKey(cmdid)) {
			return;
		}

		m_cmdResponses.get(cmdid).session.write(response);
		m_cmdResponses.remove(cmdid);
	}

	int m_shutdowntick = 0;

	public void logicShutdownWait() {
		++m_shutdowntick;
	}

	public void logicShutdownReady() {
		--m_shutdowntick;
		if (m_shutdowntick == 0) {
			//m_MasterLogic.GetCurServer().BroadToAllServer(InnerMsgDef.INNER_MSG_CLOSE.ordinal(), null);
			//m_MasterLogic.GetCurServer().Stop();
		}
	}

	void onCmdShutDown(MasterKernel kernel, int cmdid, String[] cmd) {
		logicShutdownWait();
		for (Entry<String, IMasterModule> entry : m_mapModules.entrySet()) {
			IMasterModule module = entry.getValue();
			MethodAccess access = MethodAccessCache.tryToGet(module.getClass());
			try {
				int methodIndex = access.getIndex("OnBeforeShutdown", MasterKernel.class);
				access.invoke(module, methodIndex, this);
			} catch (Exception ex) {

			}
		}
		logicShutdownReady();
	}

	void onCmdUseWhiteList(MasterKernel kernel, int cmdid, String[] cmd) {
		boolean use = false;
		if (cmd.length == 2) {
			use = cmd[1].equals("true");
		}

		InnerMsg.CommonVal.Builder build = InnerMsg.CommonVal.newBuilder();
		build.setValBool(use);
		SendMessage msg = new SendMessage(InnerMsgDef.INNER_MSG_USE_WHITELIST.ordinal(), build.build().toByteArray());

		m_MasterLogic.getServer().broadToServer("gate", msg);

		responseCmd(cmdid, use ? "set use." : "set unuse.");
	}

	void onCmdPerfSwitch(MasterKernel kernel, int cmdid, String[] cmd) {
		boolean on = false;
		if (cmd.length > 1) {
			on = cmd[1].toLowerCase().equals("on");
		}

		Perf.GetInstane().staticSwitch(on);
		responseCmd(cmdid, "PerfSwitch " + (on ? "on" : "off"));
	}

	void onCmdPerfDump(MasterKernel kernel, int cmdid, String[] cmd) {
		responseCmd(cmdid, Perf.GetInstane().dumpPerf());
	}

	public long getServerTime() {
		return m_MasterLogic.getServer().getServerTime();
	}

	public Object[] getServers() {
		return m_MasterLogic.getServer().getServerSet().getServers().toArray();
	}

	public Object[] getServersByType(String type) {
		return m_MasterLogic.getServer().getServerSet().getServersByType(type);
	}

	public String getServerNameByID(int serid) {
		return m_MasterLogic.getServer().getServerSet().getServerConfig(serid).name;
	}

	public ServerConfig getServerCfg(String name) {
		return m_MasterLogic.getServer().getServerSet().getServerConfig(name);
	}

	public long getServerSecid(String name) {
		IoSession session = m_MasterLogic.getServer().getServerSet().getServer(name);
		if (session == null) {
			return -1l;
		}
		return session.getId();
	}

	public void sendMail(int type, int channel, String title, String context, int senduid, String sendName, int recvuid,
						 String recvName, long lifeTime, String appendix) {
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
		build.setSystem(MailSystemDef.MAIL_MASTER.ordinal());
		m_MasterLogic.getServer().sendMsgToServer(framework.ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_SEND_MAIL.ordinal(),build.build().toByteArray());
	}
	
	public BaseServer getServer(){
		return m_MasterLogic.getServer();
	}

	public void execute() {

	}
}
