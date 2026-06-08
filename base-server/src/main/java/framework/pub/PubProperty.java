package framework.pub;

import org.apache.mina.core.buffer.IoBuffer;

import framework.game.UtilFunc;
import framework.game.ValueType;

/**
 * 
 * 描述： 对象属性 创建人：胡中伟 创建时间：2018年3月12日 下午6:16:03
 * 
 */
public class PubProperty {
	String m_name;
	private ValueType m_type;
	private boolean m_save;
	private Object m_value;

	public PubProperty(String name) {
		m_name = name;
		m_save = true;
	}

	public PubProperty(String name, ValueType type, Object val, boolean save) {
		m_name = name;
		m_type = type;
		m_save = save;

		switch (type) {
		case SHORT:
			m_value = (short) 0;
			break;
		case BOOL:
			m_value = false;
			break;
		case INT:
			m_value = 0;
			break;
		case LONG:
			m_value = 0L;
			break;
		case FLOAT:
			m_value = (float) 0.0f;
			break;
		case DOUBLE:
			m_value = (double) 0.0f;
			break;
		case STRING:
			m_value = "";
			break;
		case OBJECT:
			m_value = null;
			break;
		default:
			break;
		}

		if (UtilFunc.getValueType(val) == m_type) {
			m_value = val;
		}
	}

	public String getName() {
		return m_name;
	}

	public boolean setValue(Object obj) {
		if (UtilFunc.getValueType(obj) != m_type) {
			return false;
		}
		if (obj.equals(m_value)) {
			return false;
		}
		m_value = obj;
		return true;
	}

	public Object getValue() {
		return m_value;
	}

	public ValueType getType() {
		return m_type;
	}

	public boolean needSave() {
		return m_save;
	}

	public void load(IoBuffer buffer) {
		m_type = ValueType.values()[(int) buffer.getShort()];
		loadValue(buffer);
	}

	public void loadValue(IoBuffer buffer) {
		switch (m_type) {
		case SHORT:
			m_value = buffer.getShort();
			break;
		case INT:
			m_value = buffer.getInt();
			break;
		case LONG:
			m_value = buffer.getLong();
			break;
		case BOOL:
			m_value = buffer.get() == '1';
			break;
		case FLOAT:
			m_value = buffer.getFloat();
			break;
		case DOUBLE:
			m_value = buffer.getDouble();
			break;
		case STRING:
			m_value = UtilFunc.getStringFromIoBuffer(buffer);
			break;
		default:
			break;
		}
	}

	public void store(IoBuffer buffer) {
		buffer.putShort((short) m_type.ordinal());
		storeValue(buffer);
	}

	public void storeValue(IoBuffer buffer) {
		switch (m_type) {
		case SHORT:
			buffer.putShort((short) m_value);
			break;
		case INT:
			buffer.putInt((int) m_value);
			break;
		case LONG:
			buffer.putLong((long) m_value);
			break;
		case BOOL:
			buffer.put(((boolean) m_value ? "1" : "0").getBytes());
			break;
		case FLOAT:
			buffer.putFloat((float) m_value);
			break;
		case DOUBLE:
			buffer.putDouble((double) m_value);
			break;
		case STRING:
			UtilFunc.putStringToIoBuffer(buffer, m_value.toString());
			break;
		default:
			break;
		}
	}
}
