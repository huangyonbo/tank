package framework;

import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import framework.net.*;
import framework.net.message.InnerMsg;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.util.Pool;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 
 * 描述： 服务基类 创建人：胡中伟 创建时间：2018年3月12日 下午6:13:22
 *
 */
public class BaseServer extends Actor {
	class RecvReqData {
		int reqId;
		int serId;
		long time;
		String methodInfo;
	}
	private int recvReqId = 0;
	public enum State {
		UNKNOW,START,RUN,STOPING,STOPED,SHUTDOWN,END
	}
	private State state;
	private ILogic logic;
	private static Logger logger = LoggerFactory.getLogger(BaseServer.class);
	private Map<String,Boolean> nextReady = new HashMap<>();
	private int serverId;
	private String m_name;
	private String serverType;
	private String logicName;
	String address;
	private int listenPort;
	private ServerSet serverSet;
	private ServerConfig serverConfig;
	protected NetAdapter netAdapter;
	private IoAcceptor ioAcceptor;
	ActorTimer checkActor;
	private int reqId = 0;
	private Map<Integer, Consumer<byte[]>> mapReqs = new HashMap<>();
	private Map<Integer, RecvReqData> waitResponses = new HashMap<>();
	private Map<Integer, MethodCallBackData> requestMsg = new HashMap<>();
	private Map<Integer, MethodCallBackData> colonyReqMsg = new HashMap<>();
	private Map<Integer, MethodCallBackData> colonyRespMsg = new HashMap<>();
	private boolean couldConnectOther = false;
	private int reconnectCount = 0;//重新和掉线模块建立连接的次数
	private long startTime;
	private Map<String,Boolean> mustCloseBeforeMe = new HashMap<>();
	private boolean forceStop = false;//节点强制离线
	private boolean mustCloseRightNow = false;//立即关闭
	private long maxShutDownTime;//最迟5秒后需要关闭节点
	private Map<String,ColonyModuleHeart> colonyHearts = new HashMap<>();

	private final DateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private final DateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
	private final DateFormat timeFormat2 = new SimpleDateFormat("yyyyMMddHHmmssSSS");
	private Pool<Jedis> jedisPool;

	public BaseServer() {
		state = State.UNKNOW;
	}

	public BaseServer(String name,ServerSet set, IoAcceptor io, NetAdapter net) {
		ioAcceptor = io;
		netAdapter = net;
		state = State.UNKNOW;
		m_name = name;
		serverSet = set;
		serverConfig = serverSet.getServerConfig(name);
		serverId = serverConfig.id;
		this.setName(m_name);
		serverType = serverConfig.type;
		logicName = serverConfig.logicName;
		address = serverConfig.addr;
		listenPort = serverConfig.port;
		netAdapter.addServer(listenPort,this);
		if (serverConfig.front) {
			netAdapter.addServer(serverConfig.frontPort,this);
		}
		addNetMsgListener(this, InnerMsgDef.INNER_MSG_CLOSE.ordinal(), "onRecCloseServer");
		addNetMsgListener(this, InnerMsgDef.INNER_MSG_INFO.ordinal(), "onRecServerInfo");
		addNetMsgListener(this, InnerMsgDef.INNER_MSG_NOTIFY_NEXT_READY.ordinal(), "onRecCouldReady");
		addNetMsgListener(this, InnerMsgDef.INNER_MSG_NOTIFY_PRE_ME_READY.ordinal(), "onRecNextReady");
		addNetMsgListener(this, InnerMsgDef.INNER_MSG_NOTIFY_ME_CLOSE.ordinal(), "onRecOtherClosed");
		addNetMsgListener(this, InnerMsgDef.INNER_MSG_REQ_SERVER_MODULE.ordinal(), "onReqServerModule");
		addNetMsgListener(this, InnerMsgDef.INNER_MSG_RESP_SERVER_MODULE.ordinal(), "onRespServerModule");
		addNetMsgListener(this, InnerMsgDef.INNER_MSG_REQUEST.ordinal(), "onRequest");
		addNetMsgListener(this, InnerMsgDef.INNER_MSG_RESPONSE.ordinal(), "onResponse");
		addNetMsgListener(this, InnerMsgDef.INNER_MSG_NOTIFY_SHUTDOWN.ordinal(), "onShutdown");
		addNetEventListener(this, NetEvent.SESSION_OPENED, "onSessionOpened");
		addNetEventListener(this, NetEvent.SESSION_CLOSED, "onSessionClosed");
		addNetEventListener(this, NetEvent.SESSION_IDLE, "onSessionIdle");
		addColonyReqListener(this, ColonyType.COLONY_TYPE_LOAD_RUN_INFO.ordinal(),"onLoadStartInfo");
		addColonyReqListener(this, ColonyType.COLONY_TYPE_ONLINE.ordinal(),"onColonyOnline");
		addColonyReqListener(this, ColonyType.COLONY_TYPE_OFFLINE.ordinal(),"onColonyOffline");
		addColonyReqListener(this, ColonyType.COLONY_TYPE_HEART.ordinal(),"onColonyHeartReq");
		addColonyRespListener(this,ColonyType.COLONY_TYPE_HEART.ordinal(),"onColonyHeartResp");
		addNetMsgListener(this, InnerMsgDef.INNER_MSG_RELOAD_CONFIG.ordinal(), "onReloadConf");
		start();
	}
	
	void onReloadConf(IoSession session, byte[] bytes) {
		String config = System.getProperty("user.dir");
		SystemConfigData.load(config,null);
	}
	
	public State GetState() {
		return state;
	}
	
	public Map<String, Boolean> getMustCloseBeforeMe() {
		return mustCloseBeforeMe;
	}

	public void addRequestListener(Object listener, int msgid, String methodName) {
		if (requestMsg.containsKey(msgid)) {
			logger.error("Request handle for msg[{}] is exist, please check and retry.", msgid);
			return;
		}
		MethodCallBackData data = new MethodCallBackData();
		data.listener = listener;
		data.access = MethodAccessCache.tryToGet(listener.getClass());
		data.methodIndex = data.access.getIndex(methodName, int.class, byte[].class);
		requestMsg.put(msgid, data);
	}

	public IoSession connectTo(String addr, int port) {
		NioSocketConnector connector = new NioSocketConnector();
		connector.setHandler(netAdapter);
		connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ProtocolCodecFactory()));
		int localPort = 0;
		try {
			ServerSocket serverSocket = new ServerSocket(0);
			localPort = serverSocket.getLocalPort();
			serverSocket.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}//读取空闲的可用端口
		netAdapter.addServer(localPort, this);
		InetSocketAddress local = new InetSocketAddress(localPort);
		ConnectFuture future = connector.connect(new InetSocketAddress(addr,port), local);
		future.awaitUninterruptibly();
		if (!future.isConnected()) {
			future.cancel();
			netAdapter.removeServer(localPort);
			return null;
		}
		IoSession session = future.getSession();
		session.setAttribute("SerObj",this);
		netAdapter.removeServer(localPort);
		return session;
	}

	public void onRecServerInfo(IoSession session, byte[] bytes) throws InvalidProtocolBufferException{
		InnerMsg.SerInfo data = InnerMsg.SerInfo.parseFrom(bytes);
		serverSet.addServer(data.getName(),session);
	}
	
	public void tryToStop() {
		logic.onStop();
		state = State.STOPED;
	}
	
	private void innerStop(){
		List<String> sers = serverSet.getServers();
		for (int i = 0; i < sers.size(); ++i) {
			String ser = sers.get(i);
			if (StringUtils.equals(ser,m_name)){
				continue;
			}
			IoSession session = serverSet.getServer(ser);
			if (session != null) {
				serverSet.removeServer(ser);
				session.close(true);
			}
		}
	}
	
	void onRecCloseServer(IoSession session, byte[] bytes) {
		mustCloseRightNow = true;
		state = State.STOPING;
	}
	
	void onRecCouldReady(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		InnerMsg.NotifyNextReady.Builder builder = InnerMsg.NotifyNextReady.newBuilder();
		builder.setName(serverConfig.name);
		SendMessage msg = new SendMessage(InnerMsgDef.INNER_MSG_NOTIFY_PRE_ME_READY.ordinal(),builder.build().toByteArray());
		session.write(msg);
		if (state != State.START){
			return;
		}
		couldConnectOther = true;
		logic.onReady();
		state = State.RUN;
	}
	
	void onRecNextReady(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		InnerMsg.NotifyNextReady data = InnerMsg.NotifyNextReady.parseFrom(bytes);
		nextReady.put(data.getName(),true);
	}
	
	void onRecOtherClosed(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		InnerMsg.NotifyNextReady data = InnerMsg.NotifyNextReady.parseFrom(bytes);
		mustCloseBeforeMe.put(data.getName(),true);
	}
	
	public ServerSet getServerSet() {
		return serverSet;
	}

	public int getSerID() {
		return serverId;
	}

	public String getSerName() {
		return m_name;
	}

	public int getFrontPort() {
		return serverConfig.frontPort;
	}

	public String getSerType() {
		return serverType;
	}

	public ILogic getLogic() {
		return logic;
	}

	void onSessionClosed(IoSession session) {
		if (logic.onSessionClosed(session)) {
			return;
		}
		String name = (String) session.getAttribute("Name");
		if (serverSet.getServer(name) == session) {
			if (!mustCloseRightNow && !forceStop){
				logger.error("{} disconnection {} ",m_name,name);
				reconnectCount = 3;
			}
			serverSet.removeServer(name);
			logic.initStop();
		}
	}

	public void onSessionIdle(IoSession session) {
		if (logic.onSessionIdle(session)) {
			return;
		}
	}

	@Override
	protected boolean _onInitialized() {
		/*
		 * 1、根据配置，启动对应的Logic 2、监听端口 3、建立集群网络 4、对Logic对象投递事件
		 */
		startTime = System.currentTimeMillis();
		state = State.START;
		try {
			ioAcceptor.bind(new InetSocketAddress(listenPort));
		} catch (IOException e) {
			e.printStackTrace();
			logger.info("bind port {} failed!", listenPort);
		}
		logic = Launch.getBeanBySimpleName(logicName);
		if (!logic.onInit(this)) {
			return false;
		}
		logic.initStop();
		if (serverConfig.type.equals("back")){
			couldConnectOther = true;
		}
		checkActor = setTimer(this,2000,-1,"checkForNet",null);
		Runtime.getRuntime().addShutdownHook(new Thread(()->{
			logic.finalClose();
		}));
		return true;
	}

	public void checkForNet(Object obj, int leftCount) {
		if (mustCloseRightNow){
			return;
		}
		if (!couldConnectOther && reconnectCount <= 0){
			return;
		}
		boolean ready = connectToNet();
		if (ready){
			if (state == State.START && serverConfig.type.equals("back")){
				logic.onReady();
				state = State.RUN;
			}
			if (state != State.RUN){
				return;
			}
			if (couldConnectOther){
				Object[] servers = serverSet.getServersByType(serverConfig.next);
				InnerMsg.NotifyNextReady.Builder builder = InnerMsg.NotifyNextReady.newBuilder();
				builder.setName("");
				SendMessage msg = new SendMessage(InnerMsgDef.INNER_MSG_NOTIFY_NEXT_READY.ordinal(),builder.build().toByteArray());

				for(int i = 0 ; i < servers.length ; i++){
					String ser = servers[i].toString();
					Boolean flag = nextReady.get(ser);
					if (flag != null && flag.booleanValue()){
						continue;
					}
					IoSession ioSession = serverSet.getServer(ser);
					ioSession.write(msg);
				}
				if (nextReady.size() == servers.length){
					couldConnectOther = false;
					nextReady.clear();
				}
			}
		}
		if (reconnectCount > 0){
			reconnectCount--;
		}
	}

	public void onLogicReady() {
		if (serverConfig.front) {
			try {
				ioAcceptor.bind(new InetSocketAddress(serverConfig.frontPort));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		//初始化心跳逻辑
		boolean colonyHeart = SystemConfigData.getConfig("colonyHeart",false);
		if (colonyHeart) {
			List<String> targets = logic.heartList();
			if (targets != null){
				for (String target : targets){
					colonyHearts.put(target, new ColonyModuleHeart(target));
				}
			}
		}
		logger.info("start [{}] success",m_name);
	}
	
	void onSessionOpened(IoSession session) {
		if (state.ordinal() >= State.STOPING.ordinal()) {
			session.close(true);
			return;
		}
		int port = Integer.parseInt(session.getLocalAddress().toString().split(":")[1]);
		if (port == serverConfig.frontPort) {//Client
			logic.onAddClient(session);
		}
	}
	
	private boolean connectToNet() {
		List<String> names = serverSet.getServers();
		for (int i = 0; i < names.size(); i++) {
			String serName = names.get(i);
			if (StringUtils.equals(serName,m_name) || serverSet.getServer(serName) != null) {
				//自己或者已经连接成功的就不处理
				continue;
			}
			ServerConfig cfg = serverSet.getServerConfig(serName);
//			logger.info("[{}=>{}:{}] connect to [{}=>{}:{}]",m_name,m_addr,m_port,cfg.name,cfg.addr,cfg.port);
			IoSession session = connectTo(cfg.addr,cfg.port);
			if (session != null){
				InnerMsg.SerInfo.Builder builder = InnerMsg.SerInfo.newBuilder();
				builder.setName(m_name);
				builder.setType(serverType);
				if (colonyHearts.containsKey(serName)){
					colonyHearts.get(serName).reset();
				}
				byte[] data = builder.build().toByteArray();
				SendMessage msg = new SendMessage(InnerMsgDef.INNER_MSG_INFO.ordinal(), data);
				session.write(msg).addListener(new IoFutureListener<WriteFuture>() {
					@Override
					public void operationComplete(WriteFuture future) {
						if (future.isDone()){
							serverSet.addServer(serName,session);
							future.removeListener(this);
						}
					}
				});
			}
		}
		return serverSet.getServerSize() == names.size() - 1;
	}

	@Override
	protected void execute() {
//		String serName = ((BaseServer) this).getSerName();
//		if (serName.contains("Game")||serName.contains("Gate")) {
//			logger.info("服务名称 {}  执行",getSerName());
//		}
		logic.execute();
		if (state == State.RUN){
			if (colonyHearts.size() > 0){
				long now = System.currentTimeMillis();
				for (ColonyModuleHeart heat : colonyHearts.values()){
					if (heat.tick(this,now)){
						logger.error("{} HeartOutTime {}",m_name,heat.name);
						IoSession session = serverSet.getServer(heat.name);
						if (session != null){
							session.close(true);//关闭连接
						}
						//重新连接
						reconnectCount = 3;
						heat.stop = true;
					}
				}
			}

		}else if (state == State.STOPING){
			if (!forceStop && mustCloseBeforeMe.size() > 0){
				//强制离线是不需要检测关闭事件
				int count = 0;
				for (Boolean flag : mustCloseBeforeMe.values()){
					count += flag ? 1 : 0;
				}
				if (count < mustCloseBeforeMe.size()){
					return;
				}
			}
			if (logic.tryToStop()){
				checkActor.stop();
				tryToStop();
				if (forceStop){
					state = State.SHUTDOWN;
					maxShutDownTime = System.currentTimeMillis();
				}
			}
		}else if (state == State.SHUTDOWN){
			if (System.currentTimeMillis() <= maxShutDownTime + 5000){
				List<String> servers = serverSet.getServers();
				for (int i = 0 ; i < servers.size() ; i++){
					String ser = servers.get(i);
					if (ser.equals(m_name)){
						continue;
					}
					if (serverSet.getServer(ser) != null){
						return;
					}
				}
			}
			shutdown(false);
		}
	}

	@Override
	public void onDestroy() {
		logic.onDestroy();
		state = State.END;
	}

	public long getServerTime() {
		return System.currentTimeMillis();
	}

	public void broadToAllServer(List<String> excepts, int msgid, byte[] data) {
		List<String> servers = serverSet.getServers();
		for (int i = 0; i < servers.size() ; i++) {
			String ser = servers.get(i);
			if (excepts != null && excepts.contains(ser)){
				continue;
			}
			sendMsgToServer(ser,msgid,data);
		}
	}

	public void broadToServer(String type, SendMessage msg) {
		Object[] servers = serverSet.getServersByType(type);
		for (int i = 0; i < servers.length; i++) {
			String _ser = servers[i].toString();
			if (StringUtils.equals(_ser,m_name)) {
				continue;
			}
			sendMsgToServer(_ser,msg.msgID,msg.data);
		}
	}

	public void sendMsgToServer(int serid, int msgid, byte[] data) {
		ServerConfig config = serverSet.getServerConfig(serid);
		if (config == null){
			return;
		}
		BaseServer ser = serverSet.getBaseServer(config.name);
		if (ser != null) {
			NetMessage netMsg = new NetMessage((short) msgid, ser.serverSet.getServer(m_name), data);
			ser.send(netMsg);
		} else {
			IoSession session = serverSet.getServer(serid);
			if (session == null) {
				reconnectCount = 3;
				return;
			}
			SendMessage msg = new SendMessage(msgid, data);
			session.write(msg);
		}
	}
	
	public void sendMsgToServer(String sername, int msgid, byte[] data) {
		BaseServer ser = serverSet.getBaseServer(sername);
		if (ser != null) {
			NetMessage netMsg = new NetMessage((short) msgid,ser.serverSet.getServer(m_name),data);
			ser.send(netMsg);
		} else {
			IoSession session = serverSet.getServer(sername);
			if (session == null) {
				if (serverSet.getServerConfig(sername) != null){
					reconnectCount = 3;
				}
				return;
			}
			SendMessage msg = new SendMessage(msgid,data);
			session.write(msg);
		}
	}

	void onRequest(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		InnerMsg.Request req = InnerMsg.Request.parseFrom(bytes);
		int reqid = req.getReqid();
		int msgid = req.getMsgid();
		byte[] msg = null;
		if (req.getData() != null) {
			msg = req.getData().toByteArray();
		}
		MethodCallBackData cb = requestMsg.get(msgid);
		if (cb == null) {
			return;
		}
		int recvReqid = recvReqId++;
		RecvReqData data = new RecvReqData();
		data.reqId = reqid;
		data.serId = (int)session.getAttribute("SerID");
		data.time = System.currentTimeMillis();
		data.methodInfo = cb.info();
		waitResponses.put(recvReqid,data);
		cb.access.invoke(cb.listener, cb.methodIndex, recvReqid, msg);
	}
	
	void onShutdown(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		maxShutDownTime = System.currentTimeMillis();
		state = State.SHUTDOWN;
		innerStop();
	}

	public void request(int serid, int msgid, byte[] data, Consumer<byte[]> cb) {
		int reqid = reqId++;
		mapReqs.put(reqid,cb);
		InnerMsg.Request.Builder build = InnerMsg.Request.newBuilder();
		build.setReqid(reqid);
		build.setMsgid(msgid);
		if (data != null) {
			build.setData(ByteString.copyFrom(data));
		}
		sendMsgToServer(serid,InnerMsgDef.INNER_MSG_REQUEST.ordinal(),build.build().toByteArray());
	}

	public void request(String sername, int msgid, byte[] data, Consumer<byte[]> cb) {
		int reqid = reqId++;
		mapReqs.put(reqid, cb);
		InnerMsg.Request.Builder build = InnerMsg.Request.newBuilder();
		build.setReqid(reqid);
		build.setMsgid(msgid);
		build.setData(ByteString.copyFrom(data));
		sendMsgToServer(sername, InnerMsgDef.INNER_MSG_REQUEST.ordinal(), build.build().toByteArray());
	}

	private void _response(RecvReqData req,byte[] data){
		if (req == null){
			return;
		}
		InnerMsg.Response.Builder builder = InnerMsg.Response.newBuilder();
		builder.setReqid(req.reqId);
		if (data != null) {
			builder.setData(ByteString.copyFrom(data));
		}
		byte[] msg = builder.build().toByteArray();
		sendMsgToServer(req.serId,InnerMsgDef.INNER_MSG_RESPONSE.ordinal(),msg);
	}

	public void response(int reqId, byte[] data) {
		RecvReqData req = waitResponses.remove(reqId);
		_response(req,data);
	}

	void onResponse(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		InnerMsg.Response req = InnerMsg.Response.parseFrom(bytes);
		Consumer<byte[]> cb = mapReqs.remove(req.getReqid());
		if (cb != null){
			byte[] msg = req.getData() == null ? null : req.getData().toByteArray();
			cb.accept(msg);
		}
	}

	/****
	 * 集群代码开始
	 */
	public void addColonyReqListener(Object listener, int msgId, String methodName) {
		if (colonyReqMsg.containsKey(msgId)) {
			logger.error("ColonyListener for msg[{}] is exist, please check and retry.", msgId);
			return;
		}
		MethodCallBackData data = new MethodCallBackData();
		data.listener = listener;
		data.access = MethodAccessCache.tryToGet(listener.getClass());
		data.methodIndex = data.access.getIndex(methodName, int.class,String.class);
		colonyReqMsg.put(msgId, data);
	}
	
	public void addColonyRespListener(Object listener, int msgId, String methodName) {
		if (colonyRespMsg.containsKey(msgId)) {
			logger.error("ColonyListener for msg[{}] is exist, please check and retry.", msgId);
			return;
		}
		MethodCallBackData data = new MethodCallBackData();
		data.listener = listener;
		data.access = MethodAccessCache.tryToGet(listener.getClass());
		data.methodIndex = data.access.getIndex(methodName,int.class,String.class);
		colonyRespMsg.put(msgId, data);
	}
	
	String _compute(long time,int des,String tail){
		long a = time / des;
		if (a > 10){
			return String.valueOf(a) + tail;
		}else{
			return "0" + String.valueOf(a) + tail;
		}
	}
	
	String computeRunTime(){
		long time = (System.currentTimeMillis() - startTime) / 1000;
		int des = 24 * 60 * 60;
		String str = "";
		if (time > des){
			str += _compute(time,des,"天");
			time = time % des;
		}
		des = 60 * 60;
		if (time > des){
			str += _compute(time,des,"小时");
			time = time % des;
		}
		des = 60;
		if (time > des){
			str += _compute(time,des,"分钟");
			time = time % des;
		}
		str += _compute(time,1,"秒");
		return str;
	}
	
	String colonyInfo(){
		StringBuffer buffer = new StringBuffer();
		List<String> names = serverSet.getServers();
		for (int i = 0; i < names.size(); i++) {
			String serName = names.get(i);
			if (StringUtils.equals(serName,m_name)) {
				continue;
			}
			IoSession session = serverSet.getServer(serName);
			if (session != null){
				String str = String.format("已连接[%s]@%s<br>",serName,session.toString());
				buffer.append(str);
			}else{
				String str = String.format("未连接 [%s]<br>",serName);
				buffer.append(str);
			}
		}
		return buffer.toString();
	}
	
	String onLoadStartInfo(int type,String data){
		JsonObject json = new JsonObject();
		json.addProperty("start",timeFormat.format(startTime));
		json.addProperty("run",computeRunTime());
		json.addProperty("state", state.name());
		json.add("colony",JsonUtil.encodeToElement(colonyInfo()));
		return json.toString();
	}

	String onColonyOnline(int type,String data){
		JsonObject obj = JsonUtil.decodeToObj(data,JsonObject.class);
		if (serverSet.addServerConfig(obj)){
			//添加完配置后需要重新关联关闭事件
			logic.initStop();
		}
		return null;
	}
	
	String onColonyOffline(int type,String data){
		if (data.equals(m_name)){//自己离线了
			forceStop = true;
			state = State.STOPING;
		}else{
			//别人离线了
			logic.serverOffLine(data);
			serverSet.RemoveName(data);
		}
		return null;
	}
	
	String onColonyHeartReq(int type,String data){
		return m_name;
	}
	
	void onColonyHeartResp(int type,String data){
		if (colonyHearts.containsKey(data)){
			colonyHearts.get(data).resp();
		}
	}
	
	void onReqServerModule(IoSession session, byte[] bytes) throws InvalidProtocolBufferException{
		InnerMsg.ServerModuleData serverData = InnerMsg.ServerModuleData.parseFrom(bytes);
		int type = serverData.getType();
		String backName = serverData.getCome();
		MethodCallBackData cb = colonyReqMsg.get(type);
		if (cb == null) {
			return;
		}
		Object respResult = cb.access.invoke(cb.listener,cb.methodIndex,type,serverData.getDatas());
		if (respResult == null){
			return;
		}
		InnerMsg.ServerModuleData.Builder builder = InnerMsg.ServerModuleData.newBuilder();
		builder.setType(type);
		builder.setDatas(respResult.toString());
		builder.setCome(getName());
		byte[] data = builder.build().toByteArray();
		sendMsgToServer(backName,InnerMsgDef.INNER_MSG_RESP_SERVER_MODULE.ordinal(), data);
	}
	
	void onRespServerModule(IoSession session, byte[] bytes) throws InvalidProtocolBufferException{
		InnerMsg.ServerModuleData serverData = InnerMsg.ServerModuleData.parseFrom(bytes);
		int type = serverData.getType();
		MethodCallBackData cb = colonyRespMsg.get(type);
		if (cb == null) {
			return;
		}
		cb.access.invoke(cb.listener,cb.methodIndex,type,serverData.getDatas());
	}

	public Jedis getJedis(){
		if (jedisPool == null){
			JedisPoolConfig poolConfig = new JedisPoolConfig();
			poolConfig.setTestOnBorrow(true);
			String address = SystemConfigData.getConfig("redisaddr", "127.0.0.1");
			jedisPool = new JedisPool(poolConfig,address,6379);
		}
		try (Jedis jedis = jedisPool.getResource()) {
			return jedis;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public DateFormat getTimeFormat() {
		return timeFormat;
	}

	public DateFormat getDayFormat() {
		return dayFormat;
	}

	public DateFormat getTimeFormat2() {
		return timeFormat2;
	}
}
