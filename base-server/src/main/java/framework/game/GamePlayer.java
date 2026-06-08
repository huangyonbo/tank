package framework.game;

import com.google.protobuf.ByteString;
import framework.game.IKernel.PlayerState;
import framework.net.ClientMsgDef;
import framework.net.message.ClientMsg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 * 描述： 玩家对象
 * 
 */
public class GamePlayer extends GameObject {
	PlayerState m_state;
	protected Map<Integer, String> m_Viewports = new HashMap<>();
	protected boolean loadDataError = false;//从数据库读取未发生异常

	public GamePlayer(Kernel kernel) {
		super(kernel);
		m_Type = GameObjectType.GOTYPE_PLAYER;
		m_state = PlayerState.STATE_NORMAL;
	}
	
	public void initInnerData() {
		super.initInnerData();
		declareProperty(PLAYER_PROPERTY_UID, ValueType.INT, false, false, false);
		declareProperty(PLAYER_PROPERTY_SEX, ValueType.INT, false, false, false);
		declareProperty(PLAYER_PROPERTY_HEAD, ValueType.STRING, false, false, false);
		declareProperty(PLAYER_PROPERTY_HEADID, ValueType.INT, true, true, true);
		declareProperty(PLAYER_PROPERTY_FRONTSER, ValueType.STRING, false, false, false);
		declareProperty(PLAYER_PROPERTY_DESKID, ValueType.LONG, false, false, false);
		declareProperty(PLAYER_PROPERTY_SEATID, ValueType.SHORT, false, false, false);
		declareProperty(PLAYER_PROPERTY_CHANNEL, ValueType.INT, false, false, false);
		declareProperty(PLAYER_PROPERTY_REGTIME, ValueType.STRING, false, false, false);
		declareProperty(PLAYER_PROPERTY_VERSION, ValueType.STRING, false, false, false);
		declareProperty(PLAYER_PROPERTY_DEVICEID, ValueType.STRING, false, false, false);
		declareProperty(PLAYER_PROPERTY_MACADDR, ValueType.STRING, false, false, false);
		declareProperty(PLAYER_PROPERTY_IPADDR, ValueType.STRING, false, false, false);
		declareProperty(PLAYER_PROPERTY_PAYINFO, ValueType.STRING, false, false, false);
		declareProperty(PLAYER_PROPERTY_LASTOPT, ValueType.STRING, false, false, false);
		declareProperty(PLAYER_PROPERTY_PHONE, ValueType.STRING, false, true, true);
		declareProperty(PLAYER_PROPERTY_RECRUITED, ValueType.STRING, false, false, false); // 招募者分享码，用于标记是否是招募的玩家
		declareProperty(PLAYER_PROPERTY_CERTIFICATION, ValueType.BOOL, false, true, true); // 实名认证状态
		declareProperty(PLAYER_PROPERTY_AGE, ValueType.INT, false, true, true); // 玩家真实年龄
		declareProperty(PLAYER_PROPERTY_TESTPAY, ValueType.BOOL, false, false, false); // 模拟充值
		declareProperty(PLAYER_ACCOUNT_STATUS, ValueType.INT, false, false, true); // 账号状态
		declareProperty(PLAYER_LAST_PW_DESKID, ValueType.INT, false, false, false); // 账号状态
	}

	public void setState(PlayerState state) {
		m_state = state;
	}

	public PlayerState getState() {
		return m_state;
	}

	public void dataError(){
		loadDataError = true;
	}

	public void onDestroy() {
		if (!loadDataError){
			m_kernel.storePlayer(this, m_state == PlayerState.STATE_DISCONNECT);
		}
		long deskid = getLong(PLAYER_PROPERTY_DESKID);
		if (deskid != 0) {
			m_kernel.standUp(this);
		}
		m_Viewports.clear();
		super.onDestroy();
	}

	public void onLine() {
		m_kernel.getClassSet().runEvent(KernelEvent.KEVENT_ON_LINE, m_Script, this);
	}

	public void offLine() {
		m_kernel.getClassSet().runEvent(KernelEvent.KEVENT_OFF_LINE, m_Script, this);
	}

	public void setProperty(String key, Object value, int system, String reason) {
		String oldValue = getProperty(key).toString();
		m_kernel.addProLog(this, key, oldValue + " -> " + value.toString(), system, reason);
		super.setProperty(key,value);
	}

	@Override
	public boolean addViewport(int viewid, String name) {
		GameContainer obj = (GameContainer) getContainer(name);
		if (obj == null) {
			return false;
		}

		if (m_Viewports.containsKey(viewid)) {
			return false;
		}
		m_Viewports.put(viewid, name);
		obj.setViewid(viewid);

		ClientMsg.AddViewport.Builder addvp = ClientMsg.AddViewport.newBuilder();
		addvp.setViewid(viewid);
		addvp.setObjectId(obj.getObjectID());
		addvp.setData(ByteString.copyFrom(obj.getSyncData(true)));

		m_kernel.innerSendMessage(this, ClientMsgDef.CLIENT_ADD_VIEWPORT.ordinal(), addvp.build().toByteArray());

		obj.syncChilds();

		return true;
	}

	@Override
	public void removeViewport(int viewid) {
		if (!m_Viewports.containsKey(viewid)) {
			return;
		}

		GameContainer obj = (GameContainer) getContainer(m_Viewports.get(viewid));
		if (obj != null) {
			obj.setViewid(-1);
		}

		ClientMsg.RemoveViewport.Builder removevp = ClientMsg.RemoveViewport.newBuilder();
		removevp.setViewid(viewid);
		m_kernel.innerSendMessage(this, ClientMsgDef.CLIENT_REMOVE_VIEWPORT.ordinal(), removevp.build().toByteArray());

		m_Viewports.remove(viewid);
	}

	public void syncViewportPro() {
		for (Entry<Integer, String> entry : m_Viewports.entrySet()) {
			GameContainer obj = (GameContainer) getContainer(entry.getValue());
			if (obj == null) {
				continue;
			}
			obj.syncViewportPro(this);
		}
	}

	public void syncViewPort() {
		for (Entry<Integer, String> entry : m_Viewports.entrySet()) {
			int viewid = entry.getKey();
			String name = entry.getValue();
			GameContainer obj = (GameContainer) getContainer(name);
			if (obj == null) {
				continue;
			}
			ClientMsg.AddViewport.Builder addvp = ClientMsg.AddViewport.newBuilder();
			addvp.setViewid(viewid);
			addvp.setObjectId(obj.getObjectID());
			addvp.setData(ByteString.copyFrom(obj.getSyncData(true)));
			m_kernel.innerSendMessage(this, ClientMsgDef.CLIENT_ADD_VIEWPORT.ordinal(), addvp.build().toByteArray());
			obj.syncChilds();
		}
	}

	public List<Property> findPropByKey(String key) {
		List<Property> props = new ArrayList<>();
		for (Map.Entry<String,Property> entry : m_Properties.entrySet()){
			if (key.equals("-1") || entry.getKey().startsWith(key)){
				props.add(entry.getValue());
			}
		}
		return props;
	}

	public List<Record> findRecordByKey(String key) {
		List<Record> records = new ArrayList<>();
		for (Map.Entry<String,Record> entry : m_Records.entrySet()){
			if (key.equals("-1") || entry.getKey().startsWith(key)){
				records.add(entry.getValue());
			}
		}
		return records;
	}
}
