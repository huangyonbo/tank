package framework.game;

public interface IRecord {

	/**
	 * 设置列类型
	 * 
	 * @param col
	 *            列序号
	 * @param type
	 *            类型
	 */
	void setColType(int col, ValueType type);

	ValueType getColType(int colIndex);

	/**
	 * 增加一行数据
	 * 
	 * @param objects
	 *            参数
	 * @return 是否成功
	 */
	boolean addRow(Object... objects);
	
	/**
	 * 命令行添加
	 * @param value 命令行输入
	 */
	boolean addCmdRow(String value);
	
	/**
	 * 插入一行数据
	 * 
	 * @param rowIndex
	 *            位置
	 * @param objects
	 *            参数
	 * @return 是否成功
	 */
	boolean insertRow(int rowIndex, Object... objects);

	/**
	 * 删除一行数据
	 * 
	 * @param rowIndex
	 *            行号
	 */
	void removeRow(int rowIndex);

	/**
	 * 设置数据
	 * 
	 * @param rowIndex
	 *            行号
	 * @param colIndex
	 *            列
	 * @param value
	 *            值
	 */
	void setValue(int rowIndex, int colIndex, Object value);

	/**
	 * 一次设置多个属性
	 * @param rowIndex
	 * @param objs
	 */
	void setMoreValue(int rowIndex, Object... objs);

	/**
	 * 设置cmd的值
	 * @param rowIndex
	 * @param colIndex
	 * @param value
	 */
	void setCmdValue(int rowIndex, int colIndex, String value);
	
	/**
	 * 获取数据
	 * 
	 * @param rowIndex
	 *            行号
	 * @param colIndex
	 *            列
	 * @return 值
	 */
	<T> T getValue(int rowIndex, int colIndex);

	short getShort(int rowIndex, int colIndex);

	boolean getBool(int rowIndex, int colIndex);

	int getInt(int rowIndex, int colIndex);

	long getLong(int rowIndex, int colIndex);

	float getFloat(int rowIndex, int colIndex);

	double getDouble(int rowIndex, int colIndex);

	String getString(int rowIndex, int colIndex);

	/**
	 * 最大行数
	 * 
	 * @return 最大行数
	 */
	int getMaxRow();

	/**
	 * 当前行数
	 * 
	 * @return 数据行数
	 */
	int getRows();

	int getCols();

	/**
	 * 查找数据
	 * 
	 * @param startRow
	 *            起始行
	 * @param colIndex
	 *            查找的列
	 * @param value
	 *            需要查找的值
	 * @return 查找到的第一个行号。未找到返回-1
	 */
	int findRow(int startRow, int colIndex, Object value);

	/**
	 * 多个条件查询
	 * @param startRow
	 * @param filter
	 * @return
	 */
	int findRow(int startRow, RowFilter filter);
	/**
	 * 后台命令行查找
	 * @param startRow
	 * @param colIndex
	 * @param value
	 * @return
	 */
	int findCmdRow(int startRow, int colIndex, String value);
	/**
	 * 清空表格
	 * 
	 */
	void clear();
}
