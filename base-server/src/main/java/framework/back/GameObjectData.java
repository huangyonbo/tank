package framework.back;

import framework.PropertyKey;
import framework.game.Property;
import framework.game.Record;
import framework.game.UtilFunc;
import framework.game.ValueType;
import org.apache.mina.core.buffer.IoBuffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
public class GameObjectData implements PropertyKey {
	protected String m_config;
	protected String m_script;
	protected Map<String, Property> m_Properties = new HashMap<>();
	protected Map<String, Record> m_Records = new HashMap<>();
	protected List<GameObjectData> m_Childs = new ArrayList<>();
	private boolean m_canWrite;

	int m_uid = 0;
	
	public void clear(){
		for (int i = 0 ; i < m_Childs.size() ; i++){
			m_Childs.get(i).clear();
		}
		m_Childs.clear();
		for (Property property : m_Properties.values()){
			property.onDestroy();
		}
		m_Properties.clear();
		for (Record record : m_Records.values()){
			record.clear();
		}
		m_Records.clear();
	}
	
	public void setUid(int uid) {
		m_uid = uid;
	}

	public int getUid() {
		return m_uid;
	}

	public GameObjectData(boolean canWrite) {
		m_canWrite = canWrite;
	}

	public GameObjectData(boolean canWrite, String cfg) {
		m_canWrite = canWrite;
		m_config = cfg;
	}

	public boolean canWrite() {
		return m_canWrite;
	}

	public String getConfig() {
		return m_config;
	}

	public void setScript(String script) {
		m_script = script;
	}

	public String getScript() {
		return m_script;
	}

	public void setProperty(String key, Object value) {
		if (!m_Properties.containsKey(key)) {
			return;
		}
		m_Properties.get(key).setValue(value,false);
	}

	public Object getProperty(String name) {
		if (!m_Properties.containsKey(name)) {
			return null;
		}
		return m_Properties.get(name).getValue();
	}

	public ValueType GetProType(String name) {
		Property property = m_Properties.get(name);
		if (property == null) {
			return ValueType.NONE;
		}
		return property.getType();
	}

	public short getShort(String name) {
		if (!m_Properties.containsKey(name)) {
			return 0;
		}
		Property pro = m_Properties.get(name);
		if (pro.getType() != ValueType.SHORT) {
			return 0;
		}
		return (short) pro.getValue();
	}

	public boolean getBool(String name) {
		if (!m_Properties.containsKey(name)) {
			return false;
		}
		Property pro = m_Properties.get(name);
		if (pro.getType() != ValueType.BOOL) {
			return false;
		}
		return (boolean) pro.getValue();
	}

	public int getInt(String name) {
		if (!m_Properties.containsKey(name)) {
			return 0;
		}
		Property pro = m_Properties.get(name);
		if (pro.getType() != ValueType.INT) {
			return 0;
		}
		return (int) pro.getValue();
	}

	public long getLong(String name) {
		if (!m_Properties.containsKey(name)) {
			return 0l;
		}
		Property pro = m_Properties.get(name);
		if (pro.getType() != ValueType.LONG) {
			return 0l;
		}
		return (long) pro.getValue();
	}

	public float getFloat(String name) {
		if (!m_Properties.containsKey(name)) {
			return 0f;
		}
		Property pro = m_Properties.get(name);
		if (pro.getType() != ValueType.FLOAT) {
			return 0f;
		}
		return (float) pro.getValue();
	}

	public double getDouble(String name) {
		if (!m_Properties.containsKey(name)) {
			return 0f;
		}
		Property pro = m_Properties.get(name);
		if (pro.getType() != ValueType.DOUBLE) {
			return 0f;
		}
		return (double) pro.getValue();
	}

	public String getString(String name) {
		if (!m_Properties.containsKey(name)) {
			return "";
		}
		Property pro = m_Properties.get(name);
		if (pro.getType() != ValueType.STRING) {
			return "";
		}
		return (String) pro.getValue();
	}

	public Record getRecord(String name) {
		if (m_Records.containsKey(name)) {
			return m_Records.get(name);
		}
		return null;
	}

	//从存档加载对象
	public boolean loadFromArchive(IoBuffer buffer) {
		int proCount = buffer.getShort();
		for (int i = 0 ; i < proCount; ++i) {
			String name = UtilFunc.getStringFromIoBuffer(buffer);
			ValueType type = ValueType.values()[buffer.getShort()];
			Property pro = new Property(name,type,false,false,true);
			pro.loadFromBuff(buffer);
			m_Properties.put(name, pro);
		}
		int recCount = buffer.getShort();
		for (int i = 0; i < recCount; i++) {
			String name = UtilFunc.getStringFromIoBuffer(buffer);
			int cols = buffer.getShort();
			Record rec = new Record(name,cols,1000,false,false,true);
			rec.loadFromBuff(buffer);
			m_Records.put(name,rec);
		}
		int childCount = buffer.getShort();
		for (int i = 0; i < childCount; ++i) {
			int pos = buffer.getShort();
			String cfgid = UtilFunc.getStringFromIoBuffer(buffer);
			String script = UtilFunc.getStringFromIoBuffer(buffer);
			GameObjectData child = new GameObjectData(m_canWrite, cfgid);
			child.loadFromArchive(buffer);
			child.setScript(script);
			addChild(pos, child);
		}
		return true;
	}

	public void addChild(int pos, GameObjectData child) {
		m_Childs.add(child);
	}

	public int getChildCount() {
		return m_Childs.size();
	}

	public GameObjectData getChild(int pos) {
		if (pos < 0 || pos >= m_Childs.size()) {
			return null;
		}
		return m_Childs.get(pos);
	}

	public GameObjectData getChildByName(String name) {
		for (GameObjectData child : m_Childs) {
			String childName = child.getString(PLAYER_PROPERTY_NAME);
			if (childName.isEmpty()) {
				childName = child.getScript();
			}
			if (name.equals(childName)) {
				return child;
			}
		}
		return null;
	}

	// 存储存档数据
	public void storeToArchive(IoBuffer buffer) {
		// property
		buffer.putShort((short) m_Properties.size());
		for (Entry<String, Property> entry : m_Properties.entrySet()) {
			entry.getValue().storeToBuff(buffer);
		}
		// record
		buffer.putShort((short) m_Records.size());
		for (Entry<String, Record> entry : m_Records.entrySet()) {
			entry.getValue().storeToBuff(buffer);
		}
		// child
		buffer.putShort((short) m_Childs.size());
		for (int i = 0; i < m_Childs.size(); ++i) {
			buffer.putShort((short) i);
			UtilFunc.putStringToIoBuffer(buffer, m_Childs.get(i).getConfig());
			UtilFunc.putStringToIoBuffer(buffer, m_Childs.get(i).getScript());

			m_Childs.get(i).storeToArchive(buffer);
		}
	}

	public List<Property> findPropByKey(String key) {
		List<Property> props = new ArrayList<>();
		for (Map.Entry<String,Property> entry : m_Properties.entrySet()){
			if (key.equals("-1") || entry.getKey().contains(key)){
				props.add(entry.getValue());
			}
		}
		return props;
	}

	public List<Record> findRecordByKey(String key) {
		List<Record> records = new ArrayList<>();
		for (Map.Entry<String,Record> entry : m_Records.entrySet()){
			if (key.equals("-1") || entry.getKey().contains(key)){
				records.add(entry.getValue());
			}
		}
		return records;
	}
}
