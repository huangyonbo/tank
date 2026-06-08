package framework.pub;

import framework.game.ValueType;

public interface IPubData {
	void addProperty(String name, ValueType type, Object val, boolean save);

	void delProperty(String name);

	void setValue(String name, Object val);

	Object getValue(String name);

	ValueType getType(String name);

	IPubRecord addRecord(String name, int cols, int maxRow, boolean save);

	IPubRecord getRecord(String name);

	void delRecord(String name);

	short getShort(String name);

	boolean getBool(String name);

	int getInt(String name);

	long getLong(String name);

	float getFloat(String name);

	double getDouble(String name);

	String getString(String name);

	void clearRecord();
	int getPropertiesCount();
}
