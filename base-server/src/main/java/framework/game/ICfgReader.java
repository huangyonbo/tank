package framework.game;

public interface ICfgReader {
	/**
	 * 获取配置数
	 * 
	 * @return 数量
	 */
	int getItemCount();

	/**
	 * 获取根节点属性
	 * 
	 * @param key
	 *            属性名
	 * @return 属性值
	 */
	String getString(String key);

	boolean getBool(String key);

	short getShort(String key);

	int getInt(String key);

	long getLong(String key);

	float getFloat(String key);

	double getDouble(String key);

	String[] getStringArray(String key, String regex);

	int[] getIntArray(String key, String regex);

	float[] getFloatArray(String key, String regex);

	/**
	 * 根据配置id获取属性
	 * 
	 * @param id 置id
	 * @return 属性值
	 */
	boolean containsKey(String id);

	String getString(String id, String key);

	boolean getBool(String id, String key);

	short getShort(String id, String key);

	int getInt(String id, String key);

	long getLong(String id, String key);

	float getFloat(String id, String key);

	double getDouble(String id, String key);

	String[] getStringArray(String id, String key, String regex);

	int[] getIntArray(String id, String key, String regex);

	float[] getFloatArray(String id, String key, String regex);

	/**
	 * 根据索引获取属性
	 * 
	 * @param index
	 *            索引id
	 * @param key
	 *            属性名
	 * @return 属性值
	 */
	String getString(int index, String key);

	boolean getBool(int index, String key);

	short getShort(int index, String key);

	int getInt(int index, String key);

	long getLong(int index, String key);

	float getFloat(int index, String key);

	double getDouble(int index, String key);

	String[] getStringArray(int index, String key, String regex);

	int[] getIntArray(int index, String key, String regex);

	float[] getFloatArray(int index, String key, String regex);
}
