package framework.logic;

import com.google.protobuf.InvalidProtocolBufferException;
import framework.BaseServer;
import framework.ILogic;
import framework.ServerConfig;
import framework.game.ClassSet;
import framework.net.DebugMsgDef;
import framework.net.InnerMsgDef;
import framework.net.SendMessage;
import framework.net.message.DebugMsg;
import framework.net.message.InnerMsg;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DebugLogic implements ILogic {
	private Logger logger = LoggerFactory.getLogger(DebugLogic.class);
	private BaseServer m_baseServer;

	@Override
	public boolean onInit(BaseServer ser) {
		m_baseServer = ser;
		m_baseServer.addClientMsgListener(this, DebugMsgDef.DEBUG_REQUEST_SER_LIST.ordinal(), "onRequestServerList");
		m_baseServer.addClientMsgListener(this, DebugMsgDef.DEBUG_REFRESH_SERVER.ordinal(), "onRequestServerData");
		m_baseServer.addClientMsgListener(this, DebugMsgDef.DEBUG_LOAD_OBJECT.ordinal(), "onRequestObjData");
		m_baseServer.addClientMsgListener(this, DebugMsgDef.DEBUG_PUB_SER.ordinal(), "onRequestPubSer");
		m_baseServer.addClientMsgListener(this, DebugMsgDef.DEBUG_PUB_SPACE.ordinal(), "onRequestPubSpace");
		m_baseServer.addClientMsgListener(this, DebugMsgDef.DEBUG_PUB_DATA.ordinal(), "onRequestPubData");
		return true;
	}

	@Override
	public void onReady() {
		m_baseServer.onLogicReady();
	}


	@Override
	public BaseServer getServer() {
		return m_baseServer;
	}

	void onRequestServerList(IoSession session, byte[] bytes) {
		logger.info("onRequestServerList");
		List<String> sers = m_baseServer.getServerSet().getServers();
		DebugMsg.SerList.Builder list = DebugMsg.SerList.newBuilder();
		for (int i = 0; i < sers.size(); ++i) {
			String ser = sers.get(i);
			ServerConfig cfg = m_baseServer.getServerSet().getServerConfig(ser);
			if (cfg.type.equals("game") || cfg.type.equals("public")) {
				list.addName(ser);
				list.addSid(cfg.id);
				list.addType(cfg.type);
			}
		}
		SendMessage msg = new SendMessage(DebugMsgDef.DEBUG_RES_SER_LIST.ordinal(), list.build().toByteArray());
		session.write(msg);
	}

	void onRequestServerData(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		logger.info("onRequestServerData");
		DebugMsg.ReqSer reqSer = DebugMsg.ReqSer.parseFrom(bytes);
		int serid = reqSer.getSerid();
		m_baseServer.request(serid, InnerMsgDef.INNER_MSG_REQ_OBJ_LIST.ordinal(), null, (data) -> {
			InnerMsg.ReqObjectListRes res = null;
			try {
				res = InnerMsg.ReqObjectListRes.parseFrom(data);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			DebugMsg.SerObj.Builder objs = DebugMsg.SerObj.newBuilder();
			objs.setSid(serid);
			objs.setCount(res.getCount());
			for (int i = 0; i < res.getCount(); ++i) {
				objs.addName(res.getName(i));
				objs.addObjid(res.getObjectid(i));
				objs.addScript(res.getScript(i));
			}
			logger.info("onRequestServerData {}", res.getCount());
			SendMessage msg = new SendMessage(DebugMsgDef.DEBUG_RES_SERVER.ordinal(), objs.build().toByteArray());
			session.write(msg);
		});
	}

	void onRequestObjData(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		logger.info("onRequestObjData");
		DebugMsg.ReqObj reqObj = DebugMsg.ReqObj.parseFrom(bytes);
		long objid = reqObj.getObjid();
		int serid = ClassSet.getObjectSerID(objid);
		InnerMsg.ReqObjectData.Builder reqObjBuild = InnerMsg.ReqObjectData.newBuilder();
		reqObjBuild.setObjectid(objid);
		m_baseServer.request(serid, InnerMsgDef.INNER_MSG_REQ_OBJ_DATA.ordinal(), reqObjBuild.build().toByteArray(), (data) -> {
			if (data == null) {
				return;
			}
			InnerMsg.ReqObjectDataRes res = null;
			try {
				res = InnerMsg.ReqObjectDataRes.parseFrom(data);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			DebugMsg.LoadObj.Builder loadObjBuild = DebugMsg.LoadObj.newBuilder();
			loadObjBuild.setObjectId(objid);
			loadObjBuild.setData(res.getData());
			loadObjBuild.setCount(res.getCount());
			for (int i = 0; i < res.getCount(); ++i) {
				loadObjBuild.addChildname(res.getChildname(i));
				loadObjBuild.addChildid(res.getChildid(i));
				loadObjBuild.addChildscript(res.getChildscript(i));
			}
			logger.info("onRequestObjData {}", res.getCount());
			SendMessage msg = new SendMessage(DebugMsgDef.DEBUG_RES_OBJ.ordinal(),loadObjBuild.build().toByteArray());
			session.write(msg);
		});
	}

	void onRequestPubSer(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		DebugMsg.LoadPubSer msg = DebugMsg.LoadPubSer.parseFrom(bytes);
		int serid = msg.getSerid();
		m_baseServer.request(serid, InnerMsgDef.INNER_MSG_PS_LIST.ordinal(), null, (byte[] data) -> {
			InnerMsg.PubSpaceListRes res = null;
			try {
				res = InnerMsg.PubSpaceListRes.parseFrom(data);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			DebugMsg.LoadPubSerRes.Builder build = DebugMsg.LoadPubSerRes.newBuilder();
			build.setSerid(serid);
			for (int i = 0; i < res.getNameCount(); ++i) {
				build.addName(res.getName(i));
			}
			SendMessage sendMsg = new SendMessage(DebugMsgDef.DEBUG_PUB_SER_RES.ordinal(), build.build().toByteArray());
			session.write(sendMsg);
		});
	}

	void onRequestPubSpace(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		DebugMsg.LoadPubSpace msg = DebugMsg.LoadPubSpace.parseFrom(bytes);
		int serid = msg.getSerid();
		String name = msg.getName();
		InnerMsg.PubDataList.Builder reqDataList = InnerMsg.PubDataList.newBuilder();
		reqDataList.setName(name);
		m_baseServer.request(serid, InnerMsgDef.INNER_MSG_PD_LIST.ordinal(), reqDataList.build().toByteArray(),(data) -> {
			InnerMsg.PubDataListRes res = null;
			try {
				res = InnerMsg.PubDataListRes.parseFrom(data);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			DebugMsg.LoadPubSpaceRes.Builder build = DebugMsg.LoadPubSpaceRes.newBuilder();
			build.setSerid(serid);
			build.setName(name);
			for (int i = 0; i < res.getNameCount(); ++i) {
				String dataName = res.getName(i);
				if (!dataName.startsWith("Player_")) {
					build.addDataname(dataName);
				}
			}
			SendMessage sendMsg = new SendMessage(DebugMsgDef.DEBUG_PUB_SPACE_RES.ordinal(), build.build().toByteArray());
			session.write(sendMsg);
		});
	}

	void onRequestPubData(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		DebugMsg.LoadPubData msg = DebugMsg.LoadPubData.parseFrom(bytes);
		int serid = msg.getSerid();
		String name = msg.getName();
		String dataname = msg.getDataname();
		InnerMsg.PubData.Builder reqData = InnerMsg.PubData.newBuilder();
		reqData.setSpacename(name);
		reqData.setName(dataname);
		m_baseServer.request(serid, InnerMsgDef.INNER_MSG_PD.ordinal(), reqData.build().toByteArray(), (data) -> {
			InnerMsg.PubDataRes res = null;
			try {
				res = InnerMsg.PubDataRes.parseFrom(data);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			DebugMsg.LoadPubDataRes.Builder build = DebugMsg.LoadPubDataRes.newBuilder();
			build.setSerid(serid);
			build.setName(name);
			build.setDataname(dataname);
			build.setData(res.getData());
			SendMessage sendMsg = new SendMessage(DebugMsgDef.DEBUG_PUB_DATA_RES.ordinal(),build.build().toByteArray());
			session.write(sendMsg);
		});
	}
}
