package framework.http;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import framework.*;
import framework.logic.HttpLogic;
import framework.net.InnerMsgDef;
import framework.net.message.InnerMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class HttpKernel {
	private static Logger logger = LoggerFactory.getLogger(HttpKernel.class);
	private Map<String, IHttpModule> m_mapModules = new HashMap<>();
	private HttpLogic m_HttpLogic = null;

	private Map<String, MethodCallBackData> m_httpRes = new HashMap<>();

	private Map<Integer, IRequestCallback> m_mapReqs = new HashMap<>();
	//private Map<Integer, RecvReqData> m_serverResponses = new HashMap<>();
	//private Map<Integer, MethodCallBackData> m_serverRequest = new HashMap<>();
	private int m_reqid = 0;

	public boolean onInit(HttpLogic logic) {
		m_HttpLogic = logic;
		try {
			loadModules("http.modules");
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
		for (Entry<String, IHttpModule> entry : m_mapModules.entrySet()) {
			if (!entry.getValue().onInit(this)) {
				logger.error("Init module [{}] failed.", entry.getKey());
				return false;
			}
		}
		return true;
	}

	public void onDestroy() {
		// destroy modules
		for (Entry<String, IHttpModule> entry : m_mapModules.entrySet()) {
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

								if (IHttpModule.class.isAssignableFrom(c)) {
									String[] n = c.getName().split("\\.");

									IHttpModule module = null;
									try {
										Constructor<?> cons = c.getConstructor(HttpKernel.class);
										module = (IHttpModule) cons.newInstance(this);
									} catch (NoSuchMethodException e) {
										module = (IHttpModule) c.newInstance();
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

				if (IHttpModule.class.isAssignableFrom(c)) {
					String[] n = c.getName().split("\\.");

					IHttpModule module = null;
					try {
						Constructor<?> cons = c.getConstructor(HttpKernel.class);
						module = (IHttpModule) cons.newInstance(this);
					} catch (NoSuchMethodException e) {
						module = (IHttpModule) c.newInstance();
					}
					addModule(n[n.length - 1], module);
				}
			}
		}
	}

	public void addModule(String name, IHttpModule module) {
		if (m_mapModules.containsKey(name)) {
			logger.error("Module {} is exist, please check and fix", name);
			return;
		}
		m_mapModules.put(name, module);
	}

	public IHttpModule getModule(String name) {
		if (m_mapModules.containsKey(name)) {
			return m_mapModules.get(name);
		}
		return null;
	}

	public void regHttpMessage(String url, Object listener, String methodName) {
		MethodCallBackData data = new MethodCallBackData();
		data.listener = listener;
		data.access = MethodAccessCache.tryToGet(listener.getClass());
		data.methodIndex = data.access.getIndex(methodName, HttpKernel.class, String.class);
		m_httpRes.put(url, data);

	}

	public String onRecHttpMsg(String url, String json) {
		MethodCallBackData cb = m_httpRes.get(url);
		if (cb == null) {
			return "";
		}
		return (String) cb.access.invoke(cb.listener, cb.methodIndex, this, json);
	}

	public String getUserSession(int uid) {
		return m_HttpLogic.getSessionFromKey(uid);
	}

	public void sendServerMsg(String sername, int msgid, byte[] data) {
		InnerMsg.CustomMsg.Builder build = InnerMsg.CustomMsg.newBuilder();
		build.setMsgid(msgid);
		build.setData(ByteString.copyFrom(data));
		m_HttpLogic.getServer().sendMsgToServer(sername, InnerMsgDef.INNER_MSG_CUSTOM_MSG.ordinal(),build.build().toByteArray());
	}

	public void requestServerByType(String type , int msgid, byte[] data) {
		Object[] servers = m_HttpLogic.getServer().getServerSet().getServersByType(type);
		for (int i = 0; i < servers.length ; i++) {
			String serId = servers[i].toString();
			requestServer(serId,msgid,data,(byte[] msg)->{
				logger.info("from {} {}",serId,new String(msg));
			});
		}
	}

	public void requestServer(String sername, int msgid, byte[] data, IRequestCallback cb) {
		if (m_reqid < 0){
			m_reqid = 0;
		}
		int reqid = m_reqid ++;
		m_mapReqs.put(reqid, cb);
		InnerMsg.Request.Builder build = InnerMsg.Request.newBuilder();
		build.setReqid(reqid);
		build.setMsgid(msgid);
		if (data != null) {
			build.setData(ByteString.copyFrom(data));
		}
		m_HttpLogic.getServer().sendMsgToServer(sername, InnerMsgDef.INNER_MSG_CUSTOM_REQUEST.ordinal(),build.build().toByteArray());
	}

	public void requestServer(int serid, int msgid, byte[] data, IRequestCallback cb) {
		if (m_reqid < 0){
			m_reqid = 0;
		}
		int reqid = m_reqid ++;
		m_mapReqs.put(reqid, cb);
		InnerMsg.Request.Builder build = InnerMsg.Request.newBuilder();
		build.setReqid(reqid);
		build.setMsgid(msgid);
		if (data != null) {
			build.setData(ByteString.copyFrom(data));
		}
		m_HttpLogic.getServer().sendMsgToServer(serid, InnerMsgDef.INNER_MSG_CUSTOM_REQUEST.ordinal(), build.build().toByteArray());
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

	public void addPayErrorLog(int uid, String goodsId, int orderId, byte code) {
		InnerMsg.String.Builder builder = InnerMsg.String.newBuilder();
		Map<String,Object> msg = new HashMap<>();
		msg.put("uid",uid);
		msg.put("goodsId",goodsId);
		msg.put("orderId",orderId);
		msg.put("code",code);
		builder.setValue(JsonUtil.encodeToStr(msg));
		byte[] data = builder.build().toByteArray();
		m_HttpLogic.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE,InnerMsgDef.INNER_MSG_PAY_CALL_BACK_ERROR.ordinal(),data);
	}
}
