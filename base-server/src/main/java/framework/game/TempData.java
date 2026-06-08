package framework.game;

public class TempData {
	GameObject m_owner;
	String m_name;
	private ValueType m_type;
	private Object m_value;

	public TempData(String name, ValueType type, Object obj)
	{
		m_name = name;
		m_type = type;
		
		switch(type)
		{
		case SHORT: m_value = (short)0; break;
		case BOOL: m_value = false; break;
		case INT: m_value = 0; break;
		case LONG: m_value = 0L; break;
		case FLOAT: m_value = (float)0.0f; break;
		case DOUBLE: m_value = (double)0.0f; break;
		case STRING: m_value = ""; break;
		case OBJECT: m_value = obj; break;
		default:break;
		}

		if(UtilFunc.getValueType(obj) == m_type)
		{
			m_value = obj;
		}		
	}
	
	public void setOwner(GameObject owner)
	{
		m_owner = owner;
	}
	
	public void setValue(Object obj)
	{
		if(m_type != ValueType.OBJECT && UtilFunc.getValueType(obj) != m_type)
		{
			return;
		}
		
		if(obj == m_value)
		{
			return;
		}
		m_value = obj;
	}
	
	public Object getValue()
	{
		return m_value;
	}
	
	public short getShort()
	{
		if(m_type != ValueType.STRING)
		{
			return 0;
		}
		return (short) m_value;
	}
	
	public boolean getBool()
	{
		if(m_type != ValueType.BOOL)
		{
			return false;
		}
		return (boolean) m_value;
	}
	public int getInt()
	{
		if(m_type != ValueType.INT)
		{
			return 0;
		}
		return (int) m_value;
	}
	public long getLong()
	{
		if(m_type != ValueType.LONG)
		{
			return 0L;
		}
		return (long) m_value;
	}
	public float getFloat()
	{
		if(m_type != ValueType.FLOAT)
		{
			return 0.0f;
		}
		return (float) m_value;
	}
	public double getDouble()
	{
		if(m_type != ValueType.DOUBLE)
		{
			return 0.0;
		}
		return (double) m_value;
	}
	public String getString()
	{
		if(m_type != ValueType.STRING)
		{
			return "";
		}
		return (String) m_value;
	}
	
	public ValueType getType()
	{
		return m_type;
	}

	public void onDestroy() {
		m_owner = null;
		m_value = null;
		m_type  = null;
		m_value = null;
	}
}
