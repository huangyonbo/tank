package framework.logic;

import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import framework.*;
import framework.BaseServer.State;
import framework.game.*;
import framework.game.Kernel.KickType;
import framework.mybatis.data.SimpleRole;
import framework.mybatis.domain.Heads;
import framework.mybatis.service.impl.HeadsService;
import framework.mybatis.service.impl.InviteService;
import framework.mybatis.service.impl.MailsService;
import framework.mybatis.service.impl.RolesService;
import framework.net.ClientMsgDef;
import framework.net.InnerMsgDef;
import framework.net.SendMessage;
import framework.net.http.HttpClientApi;
import framework.net.message.InnerMsg;
import framework.pub.PubUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/**
 * 
 * 描述： 游戏服逻辑
 * 
 */
@Component
@Scope("prototype")
public class GameLogic implements ILogic,PropertyKey {
	private Logger logger;
	private Kernel kernel;
	private BaseServer baseServer;
	@Autowired
	private HttpClientApi httpClientApi;

	@Override
	public boolean onInit(BaseServer ser) {
		logger = LoggerFactory.getLogger(GameLogic.class);
		logger.debug("GameLogic OnInit");
		baseServer = ser;
		kernel = new Kernel();
		if (!kernel.onInit(this)) {
			return false;
		}
		baseServer.addNetMsgListener(this,InnerMsgDef.INNER_MSG_FORWARD.ordinal(), "onRecForwardMsg");
		baseServer.addNetMsgListener(this,InnerMsgDef.INNER_MSG_CLIENT_DISCONNECT.ordinal(), "onRecCliDisconnect");
		baseServer.addNetMsgListener(this,InnerMsgDef.INNER_MSG_ADD_PLAYER.ordinal(), "onRecAddPlayer");
		baseServer.addNetMsgListener(this,InnerMsgDef.INNER_MSG_DEL_PLAYER.ordinal(), "onRecDelPlayer");
		baseServer.addNetMsgListener(this,InnerMsgDef.INNER_MSG_PLAYER_CHANGE_SER.ordinal(),"onRecPlayerChangeSer");
		baseServer.addNetMsgListener(this,InnerMsgDef.INNER_MSG_COMMAND.ordinal(), "onRecCommand");
		baseServer.addNetMsgListener(this,InnerMsgDef.INNER_MSG_JUST_SEND_DATA.ordinal(), "onJustSendDataToClient");

		baseServer.addNetMsgListener(this,InnerMsgDef.INNER_MSG_CUSTOM_MSG.ordinal(), "onCustomMsg");
		baseServer.addNetMsgListener(this,InnerMsgDef.INNER_MSG_CUSTOM_REQUEST.ordinal(), "onCustomRequest");
		baseServer.addNetMsgListener(this,InnerMsgDef.INNER_MSG_CUSTOM_RESPONSE.ordinal(), "onCustomResponse");

		baseServer.addRequestListener(this,InnerMsgDef.INNER_MSG_REQ_OBJ_LIST.ordinal(), "onReqObjList");
		baseServer.addRequestListener(this,InnerMsgDef.INNER_MSG_REQ_OBJ_DATA.ordinal(), "onReqObjData");
		baseServer.addRequestListener(this,InnerMsgDef.INNER_MSG_LOAD_PLAYER.ordinal(), "onRecLoadPlayer");

		//m_baseServer.AddNetMsgListener(this,InnerMsgDef.INNER_MSG_SYNC_PUB_COMP.ordinal(), "onSyncPubDataComp");
		baseServer.addNetMsgListener(this,InnerMsgDef.INNER_MSG_UPDATE_MAIL_PROP.ordinal(), "onUpdateMailProp");
        kernel.regServerMsg(InnerMsgDef.INNER_MSG_B2G_UPDATE_RATE_LIMIT.ordinal(),this, "onUpdateRateLimit");
        return true;
	}

	ActorTimer actorTimer;

	@Override
	public void onReady() {
		kernel.executeSomeToStore(RolesService.class,"loadSimpleRole", null, (str)->{
			if (str == null){
				return;
			}
			List<SimpleRole> roles = framework.JsonUtil.decodeToList(str, SimpleRole.class);
			for (int i = 0 ; i < roles.size() ; i++){
				SimpleRole role = roles.get(i);
				kernel.addRoleList(role.getId(),role.getUserName(),role.getHeadId(),role.getProxyId());
			}
		});
		kernel.executeSomeToStore(HeadsService.class,"loadAll", null, (str)->{
			if (str == null){
				return;
			}
			List<Heads> heads = framework.JsonUtil.decodeToList(str,Heads.class);
			for (int i = 0 ; i < heads.size() ; i++){
				Heads head = heads.get(i);
				kernel.addHead(head.getId(),head.getUrl());
			}
		});
		kernel.onNetReady();
		baseServer.onLogicReady();
		//sync load to gate
		actorTimer = baseServer.setTimer(this, 10000, -1, "onSyncLoad", null);
	}

	@Override
	public void onStop() {
		actorTimer.stop();
	}

	@Override
	public void onDestroy() {
		kernel.onDestroy();
	}

	long lastExecTime = 0L;

	@Override
	public void execute() {
		long now = baseServer.getServerTime();
		if (now - lastExecTime >= 600) {
			lastExecTime = now;
			kernel.execute();
		}
	}

	@Override
	public BaseServer getServer() {
		return baseServer;
	}

	public void sendCustomMsgToClient(int uid, String front, byte[] bytes) {
		sendMsgToClient(uid, ClientMsgDef.CLIENT_CUSTOM.ordinal(), front, bytes);
	}

	public void sendMsgToClient(int uid, int msgid, String front, byte[] bytes) {
		InnerMsg.ForwardMsg.Builder builder = InnerMsg.ForwardMsg.newBuilder();
		builder.setUid(uid);
		builder.setMsgid(msgid);
		builder.setData(ByteString.copyFrom(bytes));
		byte[] data = builder.build().toByteArray();
		baseServer.sendMsgToServer(front, InnerMsgDef.INNER_MSG_FORWARD.ordinal(), data);
	}

	public void onSyncLoad(Object obj, int leftCount) {
		int load = kernel.getPlayerCount();
		InnerMsg.SyncLoad.Builder builder = InnerMsg.SyncLoad.newBuilder();
		builder.setLoad(load);
		byte[] data = builder.build().toByteArray();
		Object[] gates = baseServer.getServerSet().getServersByType("gate");
		for (int i = 0; i < gates.length; i++) {
			baseServer.sendMsgToServer(gates[i].toString(), InnerMsgDef.INNER_MSG_SYNC_LOAD.ordinal(), data);
		}
	}
	
	void onJustSendDataToClient(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		String str = new String(bytes);
		int uid = Integer.parseInt(str);
		GamePlayer player = kernel.getPlayer(uid,true);
		if (player == null) {
			return;
		}
		kernel.onClientReconnect(player);
	}

	// 更新邮件记录中寄件和收件的人的道具数量(传至碎)
	void onUpdateMailProp(IoSession session, byte[] bytes) throws InvalidProtocolBufferException{
		InnerMsg.String tmp = InnerMsg.String.parseFrom(bytes);
		String dataStr = tmp.getValue();
		JsonObject json = JsonUtil.decodeToObj(dataStr, JsonObject.class);
		int senderUid = json.get("SenderUid").getAsInt();
		int recvUid = json.get("RecvUid").getAsInt();
		String appendix = json.get("Appendix").getAsString();
		String mailId = json.get("MailId").getAsString();
		
		String senderHas = "-";
		String recvHas = "-";
		int senderVip = -1;
		int recvVip = -1;
		
		GamePlayer player = kernel.getPlayer(senderUid);
		if (player != null) {
			senderVip = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
			IGameObject itemBag = player.getContainer("ItemBag");
			if (itemBag != null) {
				senderHas = getItemHas(itemBag, appendix, false);
			}
		}
		player = kernel.getPlayer(recvUid);
		if (player != null) {
			recvVip = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
			IGameObject itemBag = player.getContainer("ItemBag");
			if (itemBag != null) {
				recvHas = getItemHas(itemBag, appendix, true);
			}
        }
		List<Object> params = new ArrayList<>();
		params.add(mailId);
		params.add(senderVip);
		params.add(recvVip);
		params.add(senderHas);
		params.add(recvHas);
		kernel.executeSomeToStore(MailsService.class,"updateVipAndProp", params,null);
	}
	
	// flag 记录时将附件的道具(传至碎)加到收件人上(相当于寄出即领取)
	String getItemHas(IGameObject itemBag, String appendix, boolean isRecv) {
		String itemHas = "";
		int hbombCount = 0;
		int nbombCount = 0;
		int hbombDebrisCount = 0;
		int nbombDebrisCount = 0;
		int itemCount = 0;
		String itemId = null;
		// item_gold_1*20000;item_diamond_1*50;
		if (appendix != null && !"".equals(appendix) && isRecv) {
			String[] tmp = appendix.split(";");
			for (String str : tmp) {
				String [] tmps = str.split("\\*");
				if (tmps.length < 2) {
					continue;
				}
				itemId = tmps[0];
				itemCount = Integer.parseInt(tmps[1]);
				if (itemId.equals("item_skill_hbomb")) {
					hbombCount += itemCount;
				}
				if (itemId.equals("item_skill_nbomb")) {
					nbombCount += itemCount;
				}
				if (itemId.equals("item_debris_hbomb")) {
					hbombDebrisCount += itemCount;
				}
				if (itemId.equals("item_debris_nbomb")) {
					nbombDebrisCount += itemCount;
				}
			}
		}
		hbombCount += kernel.getItemCountByName(itemBag, ITEM_PROPERTY_SKILL_HBOMB);
		nbombCount += kernel.getItemCountByName(itemBag, ITEM_PROPERTY_SKILL_NBOMB);
		hbombDebrisCount += kernel.getItemCountByName(itemBag, ITEM_PROPERTY_DEBRIS_HBOMB);
		nbombDebrisCount += kernel.getItemCountByName(itemBag, ITEM_PROPERTY_DEBRIS_NBOMB);
		
		if (hbombCount > 0){
			itemHas += "item_skill_hbomb*" + hbombCount+";";
		}
		if (nbombCount > 0){
			itemHas += "item_skill_nbomb*" + nbombCount+";";
		}
		if (hbombDebrisCount > 0){
			itemHas += "item_debris_hbomb*" + hbombDebrisCount+";";
		}
		if (nbombDebrisCount > 0){
			itemHas += "item_debris_nbomb*" + nbombDebrisCount+";";
		}
		return "".equals(itemHas) ? "-" : itemHas;
	}
	
	void onRecLoadPlayer(int reqid, byte[] bytes) throws InvalidProtocolBufferException, SQLException {
		if (baseServer.GetState() != State.RUN){
			baseServer.response(reqid,new byte[]{3});
			return;
		}
		InnerMsg.LoadPlayer loadInfo = InnerMsg.LoadPlayer.parseFrom(bytes);
		int uid = loadInfo.getUid();
		int channel = loadInfo.getChannel();
		String addr = loadInfo.getAddr();
		String cliVer = loadInfo.getCliver();
		int sex = loadInfo.getSex();
		String name    = loadInfo.getName();
		String headurl = loadInfo.getHeadurl();
		String devId = loadInfo.getDeviceId();
		String devName = loadInfo.getDeviceName();
		String cliName = loadInfo.getCliname();
		String macAddr = loadInfo.getMacAddr();
		String front = loadInfo.getFront();
		String payinfo = loadInfo.getPayinfo();
		String phoneBrand = loadInfo.getPhoneBrand();
		String phoneModel = loadInfo.getPhoneModel();
		String phone = loadInfo.getPhone();
		String recruiter = loadInfo.getRecruiter();
		//boolean certification = loadInfo.getCertification();
		//int age = loadInfo.getAge();
//		logger.info(" 玩家登录流程--逻辑服务处理  {} {}",uid);
		boolean testPay = loadInfo.getTestPay();
		//logger.info("try load player {} {} {}",uid,name,channel);
		GamePlayer player = kernel.getPlayer(uid,true);
		if (player != null) {
			player.setProperty(PLAYER_PROPERTY_RECRUITED, recruiter == null ? "" : recruiter);
			String lastFront = player.getString(PLAYER_PROPERTY_FRONTSER);
			if (!lastFront.equals(front)) {//顶号
				kickPlayer(lastFront, uid, Kernel.KickType.RELOGIN.ordinal());
			}
			player.setProperty(PLAYER_PROPERTY_HEAD, headurl);
			player.setProperty(PLAYER_PROPERTY_FRONTSER, front);
			player.setProperty(PLAYER_PROPERTY_VERSION, cliVer);
			player.setProperty(PLAYER_PROPERTY_DEVICEID, devId);
			player.setProperty(PLAYER_PROPERTY_MACADDR, macAddr);
			player.setProperty(PLAYER_PROPERTY_IPADDR, addr);
			player.setProperty(PLAYER_PROPERTY_CHANNEL, channel);
			player.setProperty(PLAYER_PROPERTY_PAYINFO, payinfo);
			player.setProperty(PLAYER_PROPERTY_PHONE, phone);
			player.setProperty(PLAYER_PROPERTY_TESTPAY, testPay);

            player.setProperty(PLAYER_INVITER_VIP_STATUS, loadInfo.getInviteVip());
            InviteService inviteService = SpringContextUtil.getBean(InviteService.class);
            int bindId = inviteService.searchBingId(uid);//绑定的id
            player.setProperty(PLAYER_INVITER_BIND_ID, bindId);
			kernel.onClientReconnect(player);
			baseServer.response(reqid, new byte[]{0});
			logger.info("{} {} {} Reconnect",uid,name,channel);
			return;
		}
		InnerMsg.RequestRoleData.Builder builder = InnerMsg.RequestRoleData.newBuilder();
		builder.setUid(uid);
		builder.setHeadurl(headurl);
		builder.setAddr(addr);
		builder.setDeviceId(devId);
		builder.setDeviceName(devName);
		builder.setCliname(cliName);
		builder.setPhoneBrand(phoneBrand);
		builder.setPhoneModel(phoneModel);
		builder.setPhone(phone);
		byte[] data = builder.build().toByteArray();
//		logger.info(" 玩家登录流程--请求Store逻辑服务  {} {}",uid);
		baseServer.request(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_REQUEST_ROLE_DATA.ordinal(), data, (resmsg) -> {
			InnerMsg.LoadRoleData roleData = null;
//			logger.info(" 玩家登录流程--Store逻辑服务返回结果  {} {}",uid);
			try {
				roleData = InnerMsg.LoadRoleData.parseFrom(resmsg);
			} catch (Exception e) {
				baseServer.response(reqid, new byte[]{2});
				return;
			}
			if (roleData.getCode() == StoreLogic.LoadRoleCode.FROZEN.ordinal()) {//冻结
				logger.info("{} {} is FROZEN",uid,roleData.getName());
				baseServer.response(reqid,new byte[]{1});
				return;
			}
			if (roleData.getCode() >= StoreLogic.LoadRoleCode.EXCEPTION.ordinal()) {//异常
				logger.info("{} {} happen other error",uid,roleData.getName());
				baseServer.response(reqid,new byte[]{2});
				return;
			}
			GamePlayer playerTemp = kernel.getPlayer(uid, true);
			if (playerTemp != null) {
				kernel.onClientReconnect(playerTemp);
				baseServer.response(reqid, new byte[]{0});
				return;
			}
			GamePlayer newPlayer = (GamePlayer) kernel.createObjectByScript("Player");
			newPlayer.setProperty(PLAYER_PROPERTY_UID, uid);
			newPlayer.setProperty(PLAYER_PROPERTY_HEAD, headurl);
			newPlayer.setProperty(PLAYER_PROPERTY_FRONTSER, front);
			//newPlayer.setProperty(PLAYER_PROPERTY_HEADID, roleData.getHeadid());
			newPlayer.setProperty(PLAYER_PROPERTY_VERSION, cliVer);
			newPlayer.setProperty(PLAYER_PROPERTY_DEVICEID, devId);
			newPlayer.setProperty(PLAYER_PROPERTY_MACADDR, macAddr);
			newPlayer.setProperty(PLAYER_PROPERTY_IPADDR, addr);
			newPlayer.setProperty(PLAYER_PROPERTY_CHANNEL, channel);
			newPlayer.setProperty(PLAYER_PROPERTY_PAYINFO, payinfo);
			newPlayer.setProperty(PLAYER_PROPERTY_PHONE, phone);
			newPlayer.setProperty(PLAYER_PROPERTY_RECRUITED, recruiter == null ? "" : recruiter);
			newPlayer.setProperty(PLAYER_PROPERTY_TESTPAY, testPay);
			newPlayer.setProperty(PLAYER_INVITER_VIP_STATUS, roleData.getInviteVip());
            InviteService inviteService = SpringContextUtil.getBean(InviteService.class);
            int bindId = inviteService.searchBingId(uid);//绑定的id
            newPlayer.setProperty(PLAYER_INVITER_BIND_ID, bindId);
			if (roleData.getCode() == StoreLogic.LoadRoleCode.SUCCESS.ordinal()) {
				try {
					newPlayer.setProperty(PLAYER_PROPERTY_SEX, roleData.getSex());
					newPlayer.setProperty(PLAYER_PROPERTY_NAME, roleData.getName());
					newPlayer.setProperty(PLAYER_PROPERTY_REGTIME, roleData.getRegtime());
					byte[] data1  = roleData.getData().toByteArray();
					IoBuffer buff = IoBuffer.wrap(data1);
					newPlayer.loadFromArchive(buff);
					newPlayer.setProperty(PLAYER_PROPERTY_PHONE, phone);
					if (newPlayer.getInt(PLAYER_PROPERTY_HEADID) == 0){
						//纠正以前错误头像便哈
						newPlayer.setProperty(PLAYER_PROPERTY_HEADID,1);
					}
					kernel.loadOfflineData(newPlayer);
					kernel.innerSendMessage(newPlayer, ClientMsgDef.CLIENT_LOAD_OBJECT.ordinal(),newPlayer.getLoadObjectData(true).toByteArray());
					kernel.addPlayer(uid, newPlayer);
					newPlayer.onLine();
					/*
					 * 不要删，这个后面做多借点集群需要
					InnerMsg.AddPlayer.Builder addPlayerBuilder = InnerMsg.AddPlayer.newBuilder();
					addPlayerBuilder.setUid(uid);
					addPlayerBuilder.setObjectid(newPlayer.GetObjectID());
					addPlayerBuilder.setHeadid(roleData.getHeadid());
					addPlayerBuilder.setHead(headurl);
					addPlayerBuilder.setFront(front);
					addPlayerBuilder.setBack(m_baseServer.getName());
					addPlayerBuilder.setName(roleData.getName());
					SendMessage msg = new SendMessage(InnerMsgDef.INNER_MSG_ADD_PLAYER.ordinal(),addPlayerBuilder.build().toByteArray());
					m_baseServer.BroadToServer("game",msg);
					*/
					baseServer.response(reqid, new byte[]{0});
					baseServer.sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_LOGIN_SUCC.ordinal(),data);
					logger.info("load {} {} {}",uid,roleData.getName(),channel);
				}catch (Exception e){
					newPlayer.dataError();
					kernel.destroyGameObject(newPlayer);
					logger.error(uid + " happen error when load player",e);
					baseServer.response(reqid, new byte[2]);
				}
			} else if (roleData.getCode() == StoreLogic.LoadRoleCode.NOROLE.ordinal()) {
				IoBuffer buff = IoBuffer.allocate(10);
				buff.setAutoExpand(true);
				newPlayer.storeToArchive(buff);
				int size = buff.position();
				buff.flip();
				byte[] store = Arrays.copyOfRange(buff.array(), 0, size);
				InnerMsg.StoreRoleData.Builder builder2 = InnerMsg.StoreRoleData.newBuilder();
				builder2.setUid(uid);
				builder2.setName(name);
				builder2.setSex(sex);
				builder2.setHeadurl(headurl);
				builder2.setData(ByteString.copyFrom(store));
				builder2.setOffline(0);
				builder2.setAddr(addr);
				builder2.setDeviceId(devId);
				builder2.setDeviceName(devName);
				builder2.setCliname(cliName);
				builder2.setChannel(channel);
				builder2.setPhoneBrand(phoneBrand);
				builder2.setPhoneModel(phoneModel);
				byte[] msg = builder2.build().toByteArray();
				baseServer.request(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_REG_ROLE_DATA.ordinal(),msg,(resmsg1) -> {
					InnerMsg.LoadRoleData roleData1 = null;
					try {
						roleData1 = InnerMsg.LoadRoleData.parseFrom(resmsg1);
						newPlayer.setProperty(PLAYER_PROPERTY_SEX, sex);
						newPlayer.setProperty(PLAYER_PROPERTY_NAME, roleData1.getName());
						newPlayer.setProperty(PLAYER_PROPERTY_REGTIME, roleData1.getRegtime());
						newPlayer.setProperty(PLAYER_PROPERTY_HEADID,1);
						newPlayer.onLoad();
						logger.info("reg {} {} {}",uid,roleData1.getName(),channel);
						kernel.addPlayer(uid, newPlayer);
						kernel.loadOfflineData(newPlayer);
						kernel.innerSendMessage(newPlayer, ClientMsgDef.CLIENT_LOAD_OBJECT.ordinal(),newPlayer.getLoadObjectData(true).toByteArray());
						newPlayer.onLine();
						/*
						 * 不要删，这个后面做多借点集群需要
						InnerMsg.AddPlayer.Builder addPlayerBuilder = InnerMsg.AddPlayer.newBuilder();
						addPlayerBuilder.setUid(uid);
						addPlayerBuilder.setObjectid(newPlayer.GetObjectID());
						addPlayerBuilder.setHeadid(roleData1.getHeadid());
						addPlayerBuilder.setHead(headurl);
						addPlayerBuilder.setFront(front);
						addPlayerBuilder.setBack(m_baseServer.getName());
						addPlayerBuilder.setName(roleData1.getName());
						SendMessage msg = new SendMessage(InnerMsgDef.INNER_MSG_ADD_PLAYER.ordinal(),addPlayerBuilder.build().toByteArray());
						m_baseServer.BroadToServer("game", msg);
						*/
						baseServer.response(reqid,new byte[]{0});
						baseServer.sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_LOGIN_SUCC.ordinal(),data);
					} catch (Exception e) {
						newPlayer.dataError();
						logger.error(uid + " happen error when register init player",e);
						kernel.destroyGameObject(newPlayer);
						baseServer.response(reqid, new byte[2]);
					}
				});
			}
		});
	}

	void onRecForwardMsg(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		InnerMsg.ForwardMsg forwardMsg = InnerMsg.ForwardMsg.parseFrom(bytes);
		int msgid = forwardMsg.getMsgid();
		int uid = forwardMsg.getUid();
		ForwardMsg msg = new ForwardMsg((short) msgid, uid, forwardMsg.getData().toByteArray());
		baseServer.send(msg);
	}

	void onRecCliDisconnect(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		InnerMsg.CliDisconnect forwardMsg = InnerMsg.CliDisconnect.parseFrom(bytes);
		int uid = forwardMsg.getUid();
		kernel.onClientDisconnect(uid, forwardMsg.getCode());
	}

	void onRecAddPlayer(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		InnerMsg.AddPlayer addPlayer = InnerMsg.AddPlayer.parseFrom(bytes);

		kernel.addOtherSerPlayer(addPlayer.getUid(), addPlayer.getObjectid(), addPlayer.getHeadid(),
				addPlayer.getHead(), addPlayer.getName(), addPlayer.getFront(), addPlayer.getBack());
	}

	void onRecDelPlayer(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		InnerMsg.DelPlayer delPlayer = InnerMsg.DelPlayer.parseFrom(bytes);
		kernel.delOtherSerPlayer(delPlayer.getUid());
	}

	void onRecPlayerChangeSer(IoSession session, byte[] bytes) {

	}

	void onRecCommand(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		InnerMsg.CommandMsg commandMsg = InnerMsg.CommandMsg.parseFrom(bytes);
		kernel.onRecCommand(commandMsg.getCmdid(), commandMsg.getObjectid(), commandMsg.getData().toByteArray());
	}

	public void delPlayer(int uid) {
		String playerKey = PubUtils.getKey("player_" + uid);
		try {
			getJedis().hset(playerKey, "LastGame", "");
		} catch (Exception e) {
			//e.printStackTrace();
		}
		if (baseServer.getServerSet().getServersCountByType("game") > 1){
			InnerMsg.DelPlayer.Builder delPlayerBuilder = InnerMsg.DelPlayer.newBuilder();
			delPlayerBuilder.setUid(uid);
			SendMessage msg = new SendMessage(InnerMsgDef.INNER_MSG_DEL_PLAYER.ordinal(),delPlayerBuilder.build().toByteArray());
			baseServer.broadToServer("game",msg);
		}
	}

	public void setLastGame(int uid) {
		String playerKey = PubUtils.getKey("player_" + uid);
		try {
			getJedis().hset(playerKey, "LastGame", baseServer.getName());
		} catch (Exception e) {
			//e.printStackTrace();
		}
	}

	public void onCustomMsg(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		InnerMsg.CustomMsg custom = InnerMsg.CustomMsg.parseFrom(bytes);
		byte[] msg = null;
		if (custom.getData() != null) {
			msg = custom.getData().toByteArray();
		}
		kernel.onRecServerMsg((int) session.getAttribute("SerID"), custom.getMsgid(), msg);
	}

	public void onCustomRequest(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		kernel.onServerRequest((int) session.getAttribute("SerID"), bytes);
	}

	public void onCustomResponse(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		kernel.onServerResponse((int) session.getAttribute("SerID"), bytes);
	}

	public void onReqObjList(int reqid, byte[] bytes) {
		InnerMsg.ReqObjectListRes.Builder build = InnerMsg.ReqObjectListRes.newBuilder();
		kernel.getObjList(build);
		baseServer.response(reqid, build.build().toByteArray());
	}

	public void onReqObjData(int reqid, byte[] bytes) throws InvalidProtocolBufferException {
		InnerMsg.ReqObjectData req = InnerMsg.ReqObjectData.parseFrom(bytes);
		long objid = req.getObjectid();
		GameObject obj = kernel.getGameObject(objid);
		if (obj == null) {
			baseServer.response(reqid, null);
			return;
		}
		InnerMsg.ReqObjectDataRes.Builder build = InnerMsg.ReqObjectDataRes.newBuilder();
		build.setObjectid(objid);
		build.setData(ByteString.copyFrom(obj.getDebugData()));
		int count = 0;
		for (int i = 0; i < obj.getCapacity(); ++i) {
			GameObject child = (GameObject) obj.getChild(i);
			if (child != null) {
				++count;
				build.addChildid(child.getObjectID());
				build.addChildname(child.getString(PLAYER_PROPERTY_NAME));
				build.addChildscript(child.getScript());
			}
		}
		build.setCount(count);

		baseServer.response(reqid, build.build().toByteArray());
	}

	public void onSyncPubDataComp(IoSession session, byte[] bytes) {
		//m_kernel.OnReady();
		//m_baseServer.OnLogicReady();
	}

    void onUpdateRateLimit(IKernel kernel, int serid, int msgid, byte[] msg) {
        InnerMsg.String.Builder builder = InnerMsg.String.newBuilder();
        byte[] data = builder.build().toByteArray();
        Object[] entrys = baseServer.getServerSet().getServersByType("gate");
        for (int i = 0; i < entrys.length; ++i) {
            baseServer.sendMsgToServer(entrys[i].toString(), InnerMsgDef.INNER_MSG_UPDATE_RATE_LIMIT.ordinal(), data);
        }
        logger.info("更新配置 RateLimiter ");
    }

	public void kickPlayer(String front, int uid, int code) {
		InnerMsg.KickPlayer.Builder build = InnerMsg.KickPlayer.newBuilder();
		build.setUid(uid);
		build.setCode(code);
		baseServer.sendMsgToServer(front, InnerMsgDef.INNER_MSG_KICK_PLAYER.ordinal(),build.build().toByteArray());
		if (code != KickType.RELOGIN.ordinal()){
			kernel.onClientDisconnect(uid,code);
		}
	}

	@Override
	public boolean tryToStop() {
		//需要保持数据
		return kernel.runStopByOrder();
	}

	@Override
	public void serverOffLine(String data) {
		kernel.serverOffLine(data);
	}

	@Override
	public Jedis getJedis() {
		return baseServer.getJedis();
	}

    public HttpClientApi getHttpClient() {
		return httpClientApi;
    }
}
