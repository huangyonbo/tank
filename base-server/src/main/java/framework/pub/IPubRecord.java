package framework.pub;

import framework.game.ValueType;

/**
 * 
 * 描述： 创建人：胡中伟 创建时间：2018年3月13日 下午5:17:24
 * 
 */
public interface IPubRecord {

	/**
	 * 设置列类型
	 * 
	 * @param col
	 *            列序号
	 * @param type
	 *            类型
	 */
	void setColType(int col, ValueType type);

	ValueType getColType(int col);

	int getCols();

	/**
	 * 增加一行数据
	 * 
	 * @param objects
	 *            参数
	 * @return 是否成功
	 */
	boolean addRow(Object... objects);

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
	 * 获取数据
	 * 
	 * @param rowIndex
	 *            行号
	 * @param colIndex
	 *            列
	 * @return 值
	 */
	Object getValue(int rowIndex, int colIndex);

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
	 * 查找数据
	 * 
	 * @param startRow
	 * @param colIndex
	 * @param value
	 * @return
	 */
	int findRow(int startRow, int colIndex, Object value);

	/**
	 * 当前行数
	 * 
	 * @return 数据行数
	 */
	int getRows();

	/**
	 * 清空表格
	 * 
	 */
	void clear();
}
