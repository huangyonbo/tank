package framework.game;

import com.google.protobuf.ByteString;
import framework.ActorTimer;
import framework.game.IKernel.PlayerState;
import framework.net.ClientMsgDef;
import framework.net.message.ClientMsg;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;

/**
 * 
 * 描述： 游戏对象
 * 
 */
public class GameObject implements IGameObject {

	private static Logger logger = LoggerFactory.getLogger(GameObject.class);
	protected String m_Script;
	protected long m_ObjectID;
	protected GameObjectType m_Type = GameObjectType.GOTYPE_UNKNOW;
	protected boolean m_visible = false;
	protected boolean m_save = true;
	protected boolean m_isVaild = true;
	protected boolean m_root = false;//是不是根对象
	protected int m_pos = -1;
	protected Map<String, Property> m_Properties = new HashMap<>();
	protected Map<String, TempData> m_TempDatas = new HashMap<>();
	protected Map<String, Record> m_Records = new HashMap<>();
	protected Map<String, ActorTimer> m_HeartBeats = new HashMap<>();
	protected Kernel m_kernel;
	protected long m_parent = 0;
	protected ArrayList<Long> m_childs = new ArrayList<>();
	protected Set<String> m_changedPro = new HashSet<>();
	protected Map<String, Integer> m_Containers = new HashMap<>();

	public GameObject() {

	}

	public GameObject(Kernel kernel) {
		m_kernel = kernel;
	}

	public void setState(PlayerState state) {
	}

	public PlayerState getState() {
		return PlayerState.STATE_NORMAL;
	}

	public void setObjectID(long id) {
		m_ObjectID = id;
	}

	public long getObjectID() {
		return m_ObjectID;
	}

	public Kernel getKernel() {
		return m_kernel;
	}

	public void initInnerData() {
		declareProperty(PLAYER_PROPERTY_ID, ValueType.STRING, false, false, false);
		declareProperty(PLAYER_PROPERTY_NAME, ValueType.STRING, false, false, false);
		declareProperty(PLAYER_PROPERTY_SCRIPT, ValueType.STRING, false, false, false);
		declareProperty(PLAYER_PROPERTY_CAPACITY, ValueType.INT, false, false, false);
	}

	public void onCreate() {
		m_kernel.getClassSet().runEvent(KernelEvent.KEVENT_ON_CREATE, m_Script, this);
	}

	public void onDestroy() {
		m_isVaild = false;
		m_kernel.getClassSet().runEvent(KernelEvent.KEVENT_ON_DESTROY, m_Script, this);
		for (ActorTimer actor : m_HeartBeats.values()) {
			actor.stop();
		}
		m_HeartBeats.clear();
		for (int i = 0; i < m_childs.size(); ++i) {
			long objId = m_childs.get(i);
			if (objId  > 0l) {
				GameObject child = m_kernel.getGameObject(objId);
				if (child != null) {
					m_kernel.destroyGameObject(child);
				}
			}
		}
		for (Entry<String, Property> entry : m_Properties.entrySet()){
			entry.getValue().onDestroy();
		}
		m_Properties.clear();
		for (Entry<String, TempData> entry : m_TempDatas.entrySet()){
			entry.getValue().onDestroy();
		}
		m_TempDatas.clear();
		for (Entry<String, Record> entry : m_Records.entrySet()){
			entry.getValue().onDestroy();
		}
		m_Records.clear();
		m_changedPro.clear();
		m_Containers.clear();
		m_childs.clear();
		IGameObject parent  = getParent();
		if (parent != null) {
			parent.removeChild(this);
		}
		m_kernel = null;
	}

	public void onLoad() {
		m_kernel.getClassSet().runEvent(KernelEvent.KEVENT_ON_LOAD, m_Script, this);
	}

	public void copyFrom(GameObject template) {
		for (Entry<String, Property> entry : template.m_Properties.entrySet()) {
			String key = entry.getKey();
			if (m_Properties.containsKey(key)) {
				continue;
			}
			Property pro = entry.getValue();
			m_Properties.put(key, new Property(key, pro.getType(), pro.isPubVisible(),pro.isPriVisible(), pro.needSave()));
		}
		for (Entry<String, Record> entry : template.m_Records.entrySet()) {
			String key = entry.getKey();
			if (m_Records.containsKey(key)) {
				continue;
			}
			Record rec = entry.getValue();
			Record newRec = new Record(key, rec.getCols(), rec.getMaxRow(), rec.isPubVisible(),rec.isPriVisible(), rec.needSave());
			m_Records.put(key, newRec);
			for (int i = 0; i < rec.getCols(); ++i) {
				newRec.setColType(i, rec.getColType(i));
			}
		}
	}

	public void innerInit() {
		for (Entry<String, Property> entry : m_Properties.entrySet()) {
			entry.getValue().setOwner(this);
		}
		for (Entry<String, Record> entry : m_Records.entrySet()) {
			entry.getValue().setOwner(this);
		}
	}

	public void innerSetCapacity(int capacity) {
	}

	public void setScript(String script) {
		m_Script = script;
		setProperty("Script", script);
	}

	public void setVisible(boolean visible) {
		m_visible = visible;
	}

	public void setSave(boolean save) {
		m_save = save;
	}

	public void setParent(long objid) {
		m_parent = objid;
	}

	public void setPos(int pos) {
		m_pos = pos;
	}

	public int getCapacity() {
		return m_childs.size();
	}

	public String getScript() {
		return m_Script;
	}

	public GameObjectType getType() {
		return m_Type;
	}

	public boolean isVisible() {
		return m_visible;
	}

	public boolean needSave() {
		return m_save;
	}

	public boolean isVaild() {
		return m_isVaild;
	}

	public IGameObject getParent() {
		return m_kernel.getGameObject(m_parent);
	}

	public int getPos() {
		return m_pos;
	}

	public void declareProperty(String name, ValueType type, boolean pubVisible, boolean priVisible, boolean save) {
		if (m_Properties.containsKey(name)) {
			logger.error("DeclareProperty failed. property {} is defined", name);
			return;
		}
		m_Properties.put(name, new Property(name, type, pubVisible, priVisible, save));
	}

	public void setVisible(String name, boolean pubVisible, boolean priVisible, boolean save) {
		Property pro = m_Properties.get(name);
		if (pro == null){
			logger.error("SetVisible failed. property {} not found", name);
			return;
		}
		pro.setVisible(pubVisible, priVisible, save);
	}

	public Record declareRecord(String name, int cols, int maxRow, boolean pubVisible, boolean priVisible,
								boolean save) {
		if (m_Records.containsKey(name)) {
			logger.error("DeclareRecord failed. Record {} is defined", name);
			return null;
		}
		Record rec = new Record(name, cols, maxRow, pubVisible, priVisible, save);
		m_Records.put(name, rec);
		return rec;
	}

	public IRecord getRecord(String name) {
		return m_Records.get(name);
	}

	public boolean addChild(int pos, IGameObject child) {
		if (pos < 0 || pos >= m_childs.size()) {
			return false;
		}
		if (m_childs.get(pos) != 0l) {
			return false;
		}
		IGameObject oldParent = child.getParent();
		if (oldParent != null) {
			oldParent.removeChild(child);
		}
		m_childs.set(pos, child.getObjectID());
		GameObject _child = (GameObject) child;
		_child.setParent(getObjectID());
		_child.setPos(pos);
		m_kernel.getClassSet().runEvent(KernelEvent.KEVENT_ON_ENTER, child.getScript(), child, this);
		return true;
	}

	public int addChild(IGameObject child) {
		int _size = m_childs.size();
		for (int i = 0 ; i < _size ; ++i) {
			if (m_childs.get(i) == 0l) {
				if (addChild(i,child)) {
					return i;
				}
			}
		}
		int pos = m_childs.size();
		m_childs.add(0l);
		if (addChild(pos,child)) {
			return pos;
		}
		return -1;
	}

	public boolean addContainer(String name, GameContainer container) {
		int pos = addChild(container);
		if (pos == -1) {
			return false;
		}
		m_Containers.put(name, pos);
		return true;
	}

	public boolean addContainer(String name, int pos, GameContainer container) {
		if (!addChild(pos, container)) {
			return false;
		}
		m_Containers.put(name, pos);
		return true;
	}

	public GameObject getContainer(String name) {
		Integer pos = m_Containers.get(name);
		if (pos == null){
			return null;
		}
		return getChild(pos.intValue());
	}

	public GameObject getChild(int index) {
		return m_kernel.getGameObject(m_childs.get(index));
	}

	public int getChildsByScript(String script, List<Long> childs) {
		int count = 0;
		for (int i = 0; i < m_childs.size(); ++i) {
			long id = m_childs.get(i);
			if (id == 0l) {
				continue;
			}

			IGameObject child = m_kernel.getGameObject(id);
			if (child != null && StringUtils.equals(child.getScript(), script)) {
				childs.add(id);
				++count;
			}
		}
		return count;
	}

	public int getChildsByType(GameObjectType type, List<Long> childs) {
		int count = 0;
		for (int i = 0; i < m_childs.size(); ++i) {
			long id = m_childs.get(i);
			if (id == 0l) {
				continue;
			}
			IGameObject child = m_kernel.getGameObject(id);
			if (child != null && child.getType() == type) {
				childs.add(id);
				++count;
			}
		}
		return count;
	}

	public int getChildsById(String id, List<Long> childs) {
		int count = 0;
		for (int i = 0; i < m_childs.size(); ++i) {
			long objid = m_childs.get(i);
			if (objid == 0l) {
				continue;
			}
			IGameObject child = m_kernel.getGameObject(objid);
			if (child != null && StringUtils.equals(child.getProperty("Id").toString(), id)) {
				childs.add(objid);
				++count;
			}
		}
		return count;
	}

	public int findChildById(int pos, String id) {
		for (int i = pos; i < m_childs.size(); ++i) {
			long objid = m_childs.get(i);
			if (objid == 0l) {
				continue;
			}
			IGameObject child = m_kernel.getGameObject(objid);
			if (child != null && StringUtils.equals(child.getProperty("Id").toString(), id)) {
				return i;
			}
		}
		return -1;
	}
	
	public List<IGameObject> findChildObjById(String id) {
		List<IGameObject> list = new ArrayList<>();
		for (int i = 0; i < m_childs.size(); ++i) {
			long objid = m_childs.get(i);
			if (objid == 0l) {
				continue;
			}
			IGameObject child = m_kernel.getGameObject(objid);
			if (child != null && StringUtils.equals(child.getProperty("Id").toString(), id)) {
				list.add(child);
			}
		}
		return list;
	}

	public void removeChild(IGameObject child) {
		if (m_childs.get(child.getPos()) != child.getObjectID()) {
			return;
		}
		m_kernel.getClassSet().runEvent(KernelEvent.KEVENT_ON_LEAVE, child.getScript(), child, this);
		m_childs.set(child.getPos(), 0l);
		GameObject obj = ((GameObject) child);
		obj.setPos(-1);
		obj.setParent(0l);
	}

	public void removeChild(int index) {
		IGameObject child = getChild(index);
		if (child != null) {
			removeChild(child);
		}
	}

	public boolean loadFromConfig(Element cfg) {
		int proCount = cfg.attributeCount();
		for (int i = 0; i < proCount; ++i) {
			String name = cfg.attribute(i).getName();
			String val = cfg.attribute(i).getValue();
			if (!m_Properties.containsKey(name)) {
				continue;
			}
			Property pro = m_Properties.get(name);
			ValueType type = pro.getType();
			switch (type) {
			case SHORT:
				pro.setValue(Short.parseShort(val),true);
				break;
			case INT:
				pro.setValue(Integer.parseInt(val),true);
				break;
			case LONG:
				pro.setValue(Long.parseLong(val),true);
				break;
			case BOOL:
				pro.setValue(StringUtils.equals(val.toLowerCase(), "true") || StringUtils.equals(val.toLowerCase(), "1"),true);
				break;
			case FLOAT:
				pro.setValue(Float.parseFloat(val),true);
				break;
			case DOUBLE:
				pro.setValue(Double.parseDouble(val),true);
				break;
			case STRING:
				pro.setValue(val,true);
				break;
			case OBJECT:
				pro.setValue(val,true);
				break;
			default:
				break;
			}
		}
		// record
		for (Iterator<?> i = cfg.elementIterator("Record"); i.hasNext();) {
			Element rec = (Element) i.next();
			String name = rec.attributeValue(PLAYER_PROPERTY_NAME);
			if (!m_Records.containsKey(name)) {
				continue;
			}
			Record record = m_Records.get(name);
			int col = record.getCols();
			Object[] rowData = new Object[col];
			for (Iterator<?> j = rec.elementIterator("Row"); j.hasNext();) {
				Element row = (Element) j.next();
				for (int k = 0; k < col; ++k) {
					String val = row.attributeValue("Col" + Integer.toString(k));
					if (val == null) {
						rowData[0] = null;
						break;
					}
					ValueType type = record.getColType(k);
					switch (type) {
					case SHORT:
						rowData[k] = (Short.parseShort(val));
						break;
					case INT:
						rowData[k] = (Integer.parseInt(val));
						break;
					case LONG:
						rowData[k] = (Long.parseLong(val));
						break;
					case BOOL:
						rowData[k] = (StringUtils.equals(val.toLowerCase(), "true"));
						break;
					case FLOAT:
						rowData[k] = (Float.parseFloat(val));
						break;
					case DOUBLE:
						rowData[k] = (Double.parseDouble(val));
						break;
					case STRING:
						rowData[k] = (val);
						break;
					case OBJECT:
						rowData[k] = (val);
						break;
					default:
						break;
					}
				}
				if (rowData[0] != null) {
					record.addRow(rowData);
				}
			}
		}

		return true;
	}

	// 从存档加载对象
	public boolean loadFromArchive(IoBuffer buffer) {
		int proCount = buffer.getShort();
		ValueType[] _types = ValueType.values();
		for (int i = 0; i < proCount; ++i) {
			String name = UtilFunc.getStringFromIoBuffer(buffer);
			ValueType type = _types[buffer.getShort()];
			Object val = UtilFunc.loadObjFromBuffer(type, buffer);
			Property pro = m_Properties.get(name);
			if (pro != null) {
				pro.innerSetValue(val);
				pro.checkSync();
			}
		}
		int recCount = buffer.getShort();
		for (int i = 0; i < recCount; ++i) {
			String name = UtilFunc.getStringFromIoBuffer(buffer);
			Record rec = m_Records.get(name);
			int cols = buffer.getShort();
			ValueType[] types = new ValueType[cols];
			for (int j = 0; j < cols; ++j) {
				types[j] = _types[buffer.getShort()];
			}
			int rows = buffer.getShort();
			Object[] objs = new Object[cols];
			for (int j = 0; j < rows; ++j) {
				for (int k = 0; k < cols; ++k) {
					objs[k] = UtilFunc.loadObjFromBuffer(types[k], buffer);
				}
				if (rec != null) {
					rec.innerAddRow(objs);
				}
			}
		}

		boolean isContainer = getType() == GameObjectType.GOTYPE_CONTAINER;
		if (isContainer) {
			innerSetCapacity(getInt("Capacity"));
		}

		int childCount = buffer.getShort();
		for (int i = 0; i < childCount; ++i) {
			int pos = buffer.getShort();
			String cfgid = UtilFunc.getStringFromIoBuffer(buffer);
			String script = UtilFunc.getStringFromIoBuffer(buffer);
			GameObject child = m_kernel.innerCreateObjectByConfig(cfgid, null);
			if (child == null) {
				child = m_kernel.createObjectByScript(script);
			}
			if (child != null) {
				child.loadFromArchive(buffer);

				if (child.getType() == GameObjectType.GOTYPE_CONTAINER) {
					String name = child.getString(PLAYER_PROPERTY_NAME);
					if (name.isEmpty()) {
						name = script;
						child.setProperty(PLAYER_PROPERTY_NAME, name);
					}
					// logger.info("AddContainer {}", name);
					if (isContainer) {
						addContainer(name, pos, (GameContainer) child);
					} else {
						addContainer(name, (GameContainer) child);
					}
				} else {
					// logger.info("AddChild {}", child.GetString(PLAYER_PROPERTY_NAME));
					if (isContainer) {
						if (child.getInt("Count") > 0) {
							addChild(pos, child);
						}
					} else {
						addChild(child);
					}
				}
			}
		}

		onLoad();
		return true;
	}

	// 存储存档数据
	public void storeToArchive(IoBuffer buffer) {
		// property
		int pos = buffer.position();
		buffer.putShort((short) 0);
		int proCount = 0;
		for (Entry<String, Property> entry : m_Properties.entrySet()) {
			Property pro = entry.getValue();
			if (pro.needSave()) {
				pro.storeToBuff(buffer);
				++proCount;
			}
		}
		int newpos = buffer.position();
		buffer.position(pos).putShort((short) proCount);
		buffer = buffer.position(newpos);
		// record
		pos = buffer.position();
		buffer.putShort((short) 0);
		int recCount = 0;
		for (Entry<String, Record> entry : m_Records.entrySet()) {
			Record rec = entry.getValue();
			if (rec.needSave()) {
				rec.storeToBuff(buffer);
				++recCount;
			}
		}
		newpos = buffer.position();
		buffer.position(pos).putShort((short) recCount);
		buffer = buffer.position(newpos);

		// child
		pos = buffer.position();
		buffer.putShort((short) 0);
		int childCount = 0;
		for (int i = 0; i < m_childs.size(); ++i) {
			GameObject child = (GameObject) getChild(i);
			if (child == null) {
				continue;
			}
			if (!child.needSave()) {
				continue;
			}
			buffer.putShort((short) i);
			UtilFunc.putStringToIoBuffer(buffer, child.getString("Id"));
			UtilFunc.putStringToIoBuffer(buffer, child.getScript());
			child.storeToArchive(buffer);
			++childCount;
		}
		newpos = buffer.position();
		buffer.position(pos).putShort((short) childCount);
		buffer = buffer.position(newpos);
	}

	// 添加临时数据
	public void addTempData(String key, ValueType type, Object value) {
		if (m_TempDatas.containsKey(key)) {
			return;
		}
		TempData data = new TempData(key, type, value);
		m_TempDatas.put(key, data);
	}

	public boolean haveTempData(String key) {
		return m_TempDatas.containsKey(key);
	}

	public void setTempData(String key, Object value) {
		if (m_TempDatas.containsKey(key)) {
			TempData data = m_TempDatas.get(key);
			data.setValue(value);
		} else {
			ValueType type = UtilFunc.getValueType(value);
			if (type == ValueType.NONE) {
				return;
			}
			addTempData(key, type, value);
		}
	}

	public void removeTempData(String key) {
		m_TempDatas.remove(key);
	}

	public Object getTempData(String key) {
		TempData data = m_TempDatas.get(key);
		if (data == null) {
			return null;
		}
		return data.getValue();
	}

	public short getTempShort(String key) {
		TempData data = m_TempDatas.get(key);
		if (data == null) {
			return 0;
		}
		return data.getShort();
	}

	public boolean getTempBool(String key) {
		TempData data = m_TempDatas.get(key);
		if (data == null) {
			return false;
		}
		return data.getBool();
	}

	public int getTempInt(String key) {
		TempData data = m_TempDatas.get(key);
		if (data == null) {
			return 0;
		}
		return data.getInt();
	}

	public long getTempLong(String key) {
		TempData data = m_TempDatas.get(key);
		if (data == null) {
			return 0l;
		}
		return data.getLong();
	}

	public float getTempFloat(String key) {
		TempData data = m_TempDatas.get(key);
		if (data == null) {
			return 0.0f;
		}
		return data.getFloat();
	}

	public double getTempDouble(String key) {
		TempData data = m_TempDatas.get(key);
		if (data == null) {
			return 0.0f;
		}
		return data.getDouble();
	}

	public String getTempString(String key) {
		TempData data = m_TempDatas.get(key);
		if (data == null) {
			return "";
		}
		return data.getString();
	}

	public ValueType getTempType(String key) {
		TempData data = m_TempDatas.get(key);
		if (data == null) {
			return ValueType.NONE;
		}
		return data.getType();
	}

	@Override
	public void setProperty(String key, Object value) {
		setProperty(key,value,true);
	}

	@Override
	public void setProperty(String key, Object value,boolean runChange) {
		Property property = m_Properties.get(key);
		if (property == null){
			return;
		}
		property.setValue(value,runChange);
		m_kernel.getClassSet().onSetProperty(this, key, value);
	}

	public void setPropertyImmediately(String key, Object value) {
		if (!m_Properties.containsKey(key)) {
			return;
		}
		Property pro = m_Properties.get(key);
		pro.setValue(value,true);
		if (pro.isPriVisible() && pro.checkSync()) {
			// sync
			ClientMsg.SyncOneProperty.Builder builder = ClientMsg.SyncOneProperty.newBuilder();
			builder.setObjectId(getObjectID());
			builder.setName(key);
			builder.setType(pro.getType().ordinal());
			builder.setValue(value.toString());
			byte[] data = builder.build().toByteArray();
			m_kernel.sendMessage(this, ClientMsgDef.CLIENT_SYNC_ONE_PRO.ordinal(), data);
			if (pro.isPubVisible()) {
				m_kernel.broadCastByKenWithOutSelf(this, ClientMsgDef.CLIENT_SYNC_ONE_PRO.ordinal(), data);
			}
		}
	}

	public Object getProperty(String name) {
		Property property = m_Properties.get(name);
		if (property == null) {
			return null;
		}
		return property.getValue();
	}

	public ValueType getProType(String name) {
		Property property = m_Properties.get(name);
		if (property == null) {
			return ValueType.NONE;
		}
		return property.getType();
	}

	public short getShort(String name) {
		Property property = m_Properties.get(name);
		if (property == null) {
			return 0;
		}
		return property.getShort();
	}

	public boolean getBool(String name) {
		Property property = m_Properties.get(name);
		if (property == null) {
			return false;
		}
		return property.getBool();
	}

	public int getInt(String name) {
		Property property = m_Properties.get(name);
		if (property == null) {
			return 0;
		}
		return property.getInt();
	}

	public long getLong(String name) {
		Property property = m_Properties.get(name);
		if (property == null) {
			return 0l;
		}
		return property.getLong();
	}

	public float getFloat(String name) {
		Property property = m_Properties.get(name);
		if (property == null) {
			return 0f;
		}
		return property.getFloat();
	}

	public double getDouble(String name) {
		Property property = m_Properties.get(name);
		if (property == null) {
			return 0f;
		}
		return property.getDouble();
	}

	public String getString(String name) {
		Property property = m_Properties.get(name);
		if (property == null) {
			return "";
		}
		return property.getString();
	}

	public boolean addHeartBeat(String name, ActorTimer actor) {
		if (m_HeartBeats.containsKey(name)) {
			return false;
		}
		m_HeartBeats.put(name,actor);
		return true;
	}

	public boolean haveHeartBeat(String name) {
		return m_HeartBeats.containsKey(name);
	}

	public void removeHeartBeat(String name) {
		ActorTimer actor = m_HeartBeats.remove(name);
		if (actor != null){
			actor.stop();
		}
	}

	private void copyProDataToBuffer(String name, Property pro, IoBuffer buffer) {
		UtilFunc.putStringToIoBuffer(buffer,name);
		ValueType type = pro.getType();
		buffer.putShort((short)type.ordinal());
		UtilFunc.storeObjToBuffer(type,pro.getValue(),buffer);
	}

	public void addChangedPro(String name) {
		if (!m_changedPro.contains(name)) {
			m_changedPro.add(name);
		}
	}

	public void removeChangedPro(String name) {
		if (m_changedPro.contains(name)) {
			m_changedPro.remove(name);
		}
	}

	public int getSyncProperty(IoBuffer pubBuffer, IoBuffer priBuffer) {
		if (m_changedPro.size() <= 0) {
			return 0;
		}
//		 logger.info("m_changedPro: {}  {}",this.m_Script, m_changedPro);
		int priCount = 0;
		int pubCount = 0;
		for (String name : m_changedPro) {
			Property pro = m_Properties.get(name);
			if (pro.checkSync()) {
				if (priBuffer != null) {
					copyProDataToBuffer(name, pro, priBuffer);
					priCount++;
				}
				if (pro.isPubVisible() && pubBuffer != null) {
					copyProDataToBuffer(name, pro, pubBuffer);
					pubCount++;
				}
			}
		}
		m_changedPro.clear();
		return ((priCount) << 16) | (pubCount & 0xFFFF);
	}

	public ClientMsg.LoadObject getLoadObjectData(boolean forSelf) {
		ClientMsg.LoadObject.Builder builder = ClientMsg.LoadObject.newBuilder();
		builder.setObjectId(getObjectID());
		builder.setType(m_Type.ordinal());
		builder.setData(ByteString.copyFrom(getSyncData(forSelf)));

		return builder.build();
	}

	public byte[] getSyncData(boolean forSelf) {
		IoBuffer buffer = IoBuffer.allocate(10);
		buffer.setAutoExpand(true);

		// property
		int pos = buffer.position();
		buffer.putShort((short) 0);
		int proCount = 0;
		for (Entry<String, Property> entry : m_Properties.entrySet()) {
			Property pro = entry.getValue();
			if ((forSelf && pro.isPriVisible()) || (!forSelf && pro.isPubVisible())) {
				UtilFunc.putStringToIoBuffer(buffer, entry.getKey());

				ValueType type = pro.getType();
				buffer.putShort((short) type.ordinal());

				UtilFunc.storeObjToBuffer(type, pro.getValue(), buffer);
				++proCount;
			}
		}

		int newpos = buffer.position();
		buffer.position(pos).putShort((short) proCount);
		buffer = buffer.position(newpos);

		// record
		pos = buffer.position();
		buffer.putShort((short) 0);
		int recCount = 0;
		for (Entry<String, Record> entry : m_Records.entrySet()) {
			Record rec = entry.getValue();
			if ((forSelf && rec.isPriVisible()) || (!forSelf && rec.isPubVisible())) {
				UtilFunc.putStringToIoBuffer(buffer, entry.getKey());
				buffer.putShort((short) rec.getCols());

				for (int i = 0; i < rec.getCols(); ++i) {
					buffer.putShort((short) rec.getColType(i).ordinal());
				}
				buffer.putShort((short) rec.getRows());
				for (int i = 0; i < rec.getRows(); ++i) {
					for (int j = 0; j < rec.getCols(); ++j) {
						ValueType type = rec.getColType(j);
						UtilFunc.storeObjToBuffer(type, rec.getValue(i, j), buffer);
					}
				}
				++recCount;
			}
		}
		newpos = buffer.position();
		buffer.position(pos).putShort((short) recCount);
		buffer = buffer.position(newpos);

		return buffer.array();
	}

	public byte[] getCorssData(IoBuffer buffer) {
		if (buffer == null) {
			buffer = IoBuffer.allocate(10);
			buffer.setAutoExpand(true);
		}

		// property
		int pos = buffer.position();
		buffer.putShort((short) 0);
		int proCount = 0;
		for (Entry<String, Property> entry : m_Properties.entrySet()) {
			Property pro = entry.getValue();
			ValueType type = pro.getType();
			if (type == ValueType.OBJECT) {
				continue;
			}

			UtilFunc.putStringToIoBuffer(buffer, entry.getKey());
			buffer.putShort((short) type.ordinal());

			UtilFunc.storeObjToBuffer(type, pro.getValue(), buffer);
			++proCount;
		}

		int newpos = buffer.position();
		buffer.position(pos).putShort((short) proCount);
		buffer = buffer.position(newpos);

		// tempdata
		pos = buffer.position();
		buffer.putShort((short) 0);
		proCount = 0;
		for (Entry<String, TempData> entry : m_TempDatas.entrySet()) {
			TempData pro = entry.getValue();
			ValueType type = pro.getType();
			if (type == ValueType.OBJECT) {
				continue;
			}

			UtilFunc.putStringToIoBuffer(buffer, entry.getKey());
			buffer.putShort((short) type.ordinal());

			UtilFunc.storeObjToBuffer(type, pro.getValue(), buffer);
			++proCount;
		}

		newpos = buffer.position();
		buffer.position(pos).putShort((short) proCount);
		buffer = buffer.position(newpos);

		// record
		pos = buffer.position();
		buffer.putShort((short) 0);
		int recCount = 0;
		for (Entry<String, Record> entry : m_Records.entrySet()) {
			Record rec = entry.getValue();
			UtilFunc.putStringToIoBuffer(buffer, entry.getKey());
			buffer.putShort((short) rec.getCols());

			for (int i = 0; i < rec.getCols(); ++i) {
				buffer.putShort((short) rec.getColType(i).ordinal());
			}
			buffer.putShort((short) rec.getRows());
			for (int i = 0; i < rec.getRows(); ++i) {
				for (int j = 0; j < rec.getCols(); ++j) {
					ValueType type = rec.getColType(j);
					UtilFunc.storeObjToBuffer(type, rec.getValue(i, j), buffer);
				}
			}
			++recCount;
		}
		newpos = buffer.position();
		buffer.position(pos).putShort((short) recCount);
		buffer = buffer.position(newpos);

		// child
		pos = buffer.position();
		buffer.putShort((short) 0);
		int childCount = 0;
		for (int i = 0; i < m_childs.size(); ++i) {
			GameObject child = (GameObject) getChild(i);
			if (child == null) {
				continue;
			}
			buffer.putShort((short) i);
			UtilFunc.putStringToIoBuffer(buffer, child.getString("Id"));
			UtilFunc.putStringToIoBuffer(buffer, child.getScript());
			child.getCorssData(buffer);
			++childCount;
		}
		newpos = buffer.position();
		buffer.position(pos).putShort((short) childCount);
		buffer = buffer.position(newpos);

		return buffer.array();
	}

	public boolean loadFromCrossData(IoBuffer buffer) {
		int proCount = buffer.getShort();
		for (int i = 0; i < proCount; ++i) {
			String name = UtilFunc.getStringFromIoBuffer(buffer);
			ValueType type = ValueType.values()[buffer.getShort()];
			Property pro = m_Properties.get(name);
			if (pro != null && pro.getType() == type) {
				pro.loadFromBuff(buffer);
			}
		}
		int tempData = buffer.getShort();
		for (int i = 0; i < tempData; ++i) {
			String name = UtilFunc.getStringFromIoBuffer(buffer);
			ValueType type = ValueType.values()[(int) buffer.getShort()];
			Object val = UtilFunc.loadObjFromBuffer(type, buffer);

			addTempData(name, type, val);
		}
		int recCount = buffer.getShort();
		for (int i = 0; i < recCount; ++i) {
			String name = UtilFunc.getStringFromIoBuffer(buffer);
			Record rec = m_Records.get(name);
			int cols = buffer.getShort();
			ValueType[] types = new ValueType[cols];
			for (int j = 0; j < cols; ++j) {
				types[j] = ValueType.values()[buffer.getShort()];
			}
			int rows = buffer.getShort();
			Object[] objs = new Object[cols];
			for (int j = 0; j < rows; ++j) {
				for (int k = 0; k < cols; ++k) {
					objs[k] = UtilFunc.loadObjFromBuffer(types[k], buffer);
				}
				if (rec != null) {
					rec.addRow(objs);
				}
			}
		}

		boolean isContainer = getType() == GameObjectType.GOTYPE_CONTAINER;
		if (isContainer) {
			innerSetCapacity(getInt("Capacity"));
		}

		int childCount = buffer.getShort();
		for (int i = 0; i < childCount; ++i) {
			int pos = buffer.getShort();
			String cfgid = UtilFunc.getStringFromIoBuffer(buffer);
			String script = UtilFunc.getStringFromIoBuffer(buffer);
			GameObject child = m_kernel.innerCreateObjectByConfig(cfgid, null);
			if (child == null) {
				child = m_kernel.createObjectByScript(script);
			}
			if (child != null) {
				child.loadFromCrossData(buffer);
				if (child.getType() == GameObjectType.GOTYPE_CONTAINER) {
					String name = child.getString(PLAYER_PROPERTY_NAME);
					if (name.isEmpty()) {
						name = script;
						child.setProperty(PLAYER_PROPERTY_NAME, name);
					}
					if (isContainer) {
						addContainer(name, pos, (GameContainer) child);
					} else {
						addContainer(name, (GameContainer) child);
					}
				} else {
					if (isContainer) {
						addChild(pos, child);
					} else {
						addChild(child);
					}
				}
			}
		}

		return true;
	}

	public byte[] getDebugData() {
		IoBuffer buffer = IoBuffer.allocate(10);
		buffer.setAutoExpand(true);

		// property
		int pos = buffer.position();
		buffer.putShort((short) 0);
		int proCount = 0;
		for (Entry<String, Property> entry : m_Properties.entrySet()) {
			Property pro = entry.getValue();
			ValueType type = pro.getType();
			if (type == ValueType.OBJECT) {
				continue;
			}

			UtilFunc.putStringToIoBuffer(buffer, entry.getKey());
			buffer.putShort((short) type.ordinal());
			buffer.put((pro.isPubVisible() ? "1" : "0").getBytes());
			buffer.put((pro.isPriVisible() ? "1" : "0").getBytes());
			buffer.put((pro.needSave() ? "1" : "0").getBytes());
			UtilFunc.storeObjToBuffer(type, pro.getValue(), buffer);
			++proCount;
		}

		int newpos = buffer.position();
		buffer.position(pos).putShort((short) proCount);
		buffer = buffer.position(newpos);

		// record
		pos = buffer.position();
		buffer.putShort((short) 0);
		int recCount = 0;
		for (Entry<String, Record> entry : m_Records.entrySet()) {
			Record rec = entry.getValue();
			UtilFunc.putStringToIoBuffer(buffer, entry.getKey());
			buffer.put((rec.isPubVisible() ? "1" : "0").getBytes());
			buffer.put((rec.isPriVisible() ? "1" : "0").getBytes());
			buffer.put((rec.needSave() ? "1" : "0").getBytes());
			buffer.putShort((short) rec.getCols());

			for (int i = 0; i < rec.getCols(); ++i) {
				buffer.putShort((short) rec.getColType(i).ordinal());
			}
			buffer.putShort((short) rec.getMaxRow());
			buffer.putShort((short) rec.getRows());
			for (int i = 0; i < rec.getRows(); ++i) {
				for (int j = 0; j < rec.getCols(); ++j) {
					ValueType type = rec.getColType(j);
					UtilFunc.storeObjToBuffer(type, rec.getValue(i, j), buffer);
				}
			}
			++recCount;
		}
		newpos = buffer.position();
		buffer.position(pos).putShort((short) recCount);
		buffer = buffer.position(newpos);

		return buffer.array();
	}

	public int getSeatCount() {
		return 0;
	}

	public int getFreeSeatCount() {
		return 0;
	}

	public GameObject getSeatObject(int seatid) {
		return null;
	}

	public boolean addViewport(int viewid, String name) {
		return false;
	}

	public void removeViewport(int viewid) {

	}

	public int getLeftTime() {
		return 0;
	}
}
