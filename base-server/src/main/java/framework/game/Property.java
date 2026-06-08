package framework.game;

import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * 描述： 对象属性
 * 
 */
public class Property {
	private GameObject m_owner;
	private String m_name;
	private ValueType m_type;
	private boolean m_pubVis;
	private boolean m_priVis;
	private boolean m_save;
	private Object m_value;
	private Object m_clip;
	static Logger logger = LoggerFactory.getLogger(Property.class);
	
	public void onDestroy() {
		m_owner = null;
		m_name  = null;
		m_value = null;
		m_clip  = null;
		m_type  = null;
	}
	
	public String getName() {
		return m_name;
	}

	public Property(String name, ValueType type, boolean pub, boolean pri, boolean save) {
		m_name = name;
		m_type = type;
		m_pubVis = pub;
		m_priVis = pri || pub;
		m_save = save;
		switch (type) {
			case SHORT:
				m_value = Short.parseShort("0");
				break;
			case BOOL:
				m_value = false;
				break;
			case INT:
				m_value = new Integer(0);
				break;
			case LONG:
				m_value = new Long(0L);
				break;
			case FLOAT:
				m_value = Float.parseFloat("0");
				break;
			case DOUBLE:
				m_value = Double.parseDouble("0");
				break;
			case STRING:
				m_value = "";
				break;
			case OBJECT:
				m_value = null;
				m_pubVis = false;
				m_priVis = false;
				m_save = false;
				break;
			default:
				break;
		}
		m_clip = m_value;
	}

	public void setVisible(boolean pubVisible, boolean priVisible, boolean save) {
		if (m_type != ValueType.OBJECT) {
			m_pubVis = pubVisible;
			m_priVis = priVisible || pubVisible;
			m_save = save;
		}
	}

	public void setOwner(GameObject owner) {
		m_owner = owner;
	}
	
	public void setCmdValue(String value) {
		switch (m_type) {
			case SHORT:
				m_value = new Short(value);
				break;
			case BOOL:
				m_value = new Boolean(value);
				break;
			case INT:
				m_value = new Integer(value);
				break;
			case LONG:
				m_value = new Long(value);
				break;
			case FLOAT:
				m_value = new Float(value);
				break;
			case DOUBLE:
				m_value = new Double(value);
				break;
			case STRING:
				m_value = value;
				break;
			default:
				break;
		}
	}

	public void setValue(Object obj,boolean runChange) {
		ValueType setType = UtilFunc.getValueType(obj);
		try {
			if (m_type != ValueType.OBJECT && setType != m_type) {
				if (m_type == ValueType.INT && setType == ValueType.LONG) {
					obj = Integer.parseInt(obj.toString());
				} else if (m_type == ValueType.LONG && setType == ValueType.INT) {
					obj = Long.parseLong(obj.toString());
				} else {
					return;
				}
			}
		} catch (NumberFormatException e) {
			logger.error("SetValue error",e);
		}
		if (m_value != null && m_value.equals(obj)) {
			return;
		}
		Object oldValue = m_value;
		m_value = obj;
		if (m_owner == null){
			return;
		}
		if (oldValue != null && runChange){
			m_owner.getKernel().getClassSet().onPropertyChange(m_owner,m_name,oldValue);
		}
		if (m_priVis) {
			if (!m_value.equals(m_clip)) {
				m_owner.addChangedPro(m_name);
			} else {
				m_owner.removeChangedPro(m_name);
			}
		}
	}

	public void innerSetValue(Object obj) {
		ValueType objtype = UtilFunc.getValueType(obj);
		try {
			if (m_type != ValueType.OBJECT && objtype != m_type) {
				if (m_type == ValueType.INT && objtype == ValueType.LONG) {
					obj = Integer.parseInt(obj.toString());
				} else if (m_type == ValueType.LONG && objtype == ValueType.INT) {
					obj = Long.parseLong(obj.toString());
				} else {
					return;
				}
			}
		} catch (NumberFormatException e) {
			logger.error("InnerSetValue error",e);
		}
		if (obj.equals(m_value)) {
			return;
		}
		m_value = obj;
		m_clip = obj;
	}

	public Object getValue() {
		return m_value;
	}

	public short getShort() {
		if (m_type != ValueType.SHORT) {
			return 0;
		}
		return (short) m_value;
	}

	public boolean getBool() {
		if (m_type != ValueType.BOOL) {
			return false;
		}
		return (boolean) m_value;
	}

	public int getInt() {
		if (m_type != ValueType.INT) {
			return 0;
		}
		return (int) m_value;
	}

	public long getLong() {
		if (m_type != ValueType.LONG) {
			return 0L;
		}
		return (long) m_value;
	}

	public float getFloat() {
		if (m_type != ValueType.FLOAT) {
			return 0.0f;
		}
		return (float) m_value;
	}

	public double getDouble() {
		if (m_type != ValueType.DOUBLE) {
			return 0.0;
		}
		return (double) m_value;
	}

	public String getString() {
		if (m_type != ValueType.STRING) {
			return "";
		}
		return m_value.toString();
	}

	public ValueType getType() {
		return m_type;
	}

	public boolean needSave() {
		return m_save;
	}

	public boolean checkSync() {
		boolean res = !m_value.equals(m_clip);
		if (m_type == ValueType.OBJECT) {
			res = false;
		}

		m_clip = m_value;
		return res;
	}

	public boolean isPubVisible() {
		return m_pubVis;
	}

	public boolean isPriVisible() {
		return m_priVis;
	}

	public void loadFromBuff(IoBuffer buffer) {
		Object val = UtilFunc.loadObjFromBuffer(m_type, buffer);
		innerSetValue(val);
	}

	public void storeToBuff(IoBuffer buff) {
		UtilFunc.putStringToIoBuffer(buff,m_name);
		buff.putShort((short)m_type.ordinal());
		UtilFunc.storeObjToBuffer(m_type,m_value,buff);
	}
}
