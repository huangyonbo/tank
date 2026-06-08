package framework.pub;

import framework.game.UtilFunc;
import framework.game.ValueType;
import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * 描述： 创建人：胡中伟 创建时间：2018年3月13日 下午1:05:16
 * 
 */
public class PubRecord implements IPubRecord {
	private String m_name;
	private boolean m_save;
	private int m_maxRow;
	private int m_cols;
	private List<ValueType> m_colTypes = new ArrayList<>();
	private List<List<Object>> m_values = new LinkedList<>();
	private static Logger logger = LoggerFactory.getLogger(PubRecord.class);

	public PubRecord(String name) {
		m_name = name;
		m_save = true;
	}

	public PubRecord(String name, int cols, int maxRow, boolean save) {
		m_name = name;
		m_cols = cols;
		m_maxRow = maxRow;
		m_save = save;
		for (int i = 0; i < cols; ++i) {
			m_colTypes.add(ValueType.INT);
		}
	}

	public String getName() {
		return m_name;
	}

	public void setMaxRow(int maxRow) {
		m_maxRow = maxRow;
	}

	public void setColType(int col, ValueType type) {
		if (col < 0 || col >= m_cols) {
			return;
		}
		m_colTypes.set(col, type);
	}

	public boolean addRow(Object... objects) {
		if (m_values.size() >= m_maxRow) {
			return false;
		}
		if (objects.length != m_cols) {
			return false;
		}
		List<Object> row = new ArrayList<>();
		for (int i = 0; i < m_cols; ++i) {
			row.add(objects[i]);
		}
		m_values.add(row);
		return true;
	}

	public boolean insertRow(int rowIndex, Object... objects) {
		if (m_values.size() >= m_maxRow) {
			return false;
		}
		if (objects.length != m_cols) {
			return false;
		}
		if (rowIndex < 0 || rowIndex > m_values.size()) {
			return false;
		}
		List<Object> row = new ArrayList<>();
		for (int i = 0; i < m_cols; ++i) {
			row.add(objects[i]);
		}
		m_values.add(rowIndex, row);
		return true;
	}

	public void removeRow(int rowIndex) {
		if (rowIndex < 0 || rowIndex >= m_values.size()) {
			return;
		}
		m_values.remove(rowIndex);
	}

	public void setValue(int rowIndex, int colIndex, Object obj) {
		if (rowIndex < 0 || rowIndex >= m_values.size()) {
			return;
		}
		if (colIndex < 0 || colIndex >= m_cols) {
			return;
		}
		if (getValue(rowIndex, colIndex).equals(obj)) {
			return;
		}
		ValueType _type = m_colTypes.get(colIndex);
		if (_type != UtilFunc.getValueType(obj)){
			return;
		}
		m_values.get(rowIndex).set(colIndex,obj);
	}

	public Object getValue(int rowIndex, int colIndex) {
		if (rowIndex < 0 || rowIndex >= m_values.size()) {
			return null;
		}
		if (colIndex < 0 || colIndex >= m_cols) {
			return null;
		}
		return m_values.get(rowIndex).get(colIndex);
	}

	public short getShort(int rowIndex, int colIndex) {
		if (rowIndex < 0 || rowIndex >= m_values.size()) {
			return 0;
		}
		if (colIndex < 0 || colIndex >= m_cols) {
			return 0;
		}
		if (m_colTypes.get(colIndex) != ValueType.SHORT) {
			return 0;
		}
		return (short) m_values.get(rowIndex).get(colIndex);
	}

	public boolean getBool(int rowIndex, int colIndex) {
		if (rowIndex < 0 || rowIndex >= m_values.size()) {
			return false;
		}
		if (colIndex < 0 || colIndex >= m_cols) {
			return false;
		}
		if (m_colTypes.get(colIndex) != ValueType.BOOL) {
			return false;
		}
		return (boolean) m_values.get(rowIndex).get(colIndex);
	}

	public int getInt(int rowIndex, int colIndex) {
		if (rowIndex < 0 || rowIndex >= m_values.size()) {
			return 0;
		}
		if (colIndex < 0 || colIndex >= m_cols) {
			return 0;
		}
		if (m_colTypes.get(colIndex) != ValueType.INT) {
			return 0;
		}
		return (int) m_values.get(rowIndex).get(colIndex);
	}

	public long getLong(int rowIndex, int colIndex) {
		if (rowIndex < 0 || rowIndex >= m_values.size()) {
			return 0l;
		}
		if (colIndex < 0 || colIndex >= m_cols) {
			return 0l;
		}
		if (m_colTypes.get(colIndex) != ValueType.LONG) {
			return 0l;
		}
		return (long) m_values.get(rowIndex).get(colIndex);
	}

	public float getFloat(int rowIndex, int colIndex) {
		if (rowIndex < 0 || rowIndex >= m_values.size()) {
			return 0f;
		}
		if (colIndex < 0 || colIndex >= m_cols) {
			return 0f;
		}
		if (m_colTypes.get(colIndex) != ValueType.FLOAT) {
			return 0f;
		}
		return (float) m_values.get(rowIndex).get(colIndex);
	}

	public double getDouble(int rowIndex, int colIndex) {
		if (rowIndex < 0 || rowIndex >= m_values.size()) {
			return 0f;
		}
		if (colIndex < 0 || colIndex >= m_cols) {
			return 0f;
		}
		if (m_colTypes.get(colIndex) != ValueType.DOUBLE) {
			return 0f;
		}
		return (double) m_values.get(rowIndex).get(colIndex);
	}

	public String getString(int rowIndex, int colIndex) {
		if (rowIndex < 0 || rowIndex >= m_values.size()) {
			return "";
		}
		if (colIndex < 0 || colIndex >= m_cols) {
			return "";
		}
		if (m_colTypes.get(colIndex) != ValueType.STRING) {
			return "";
		}
		return (String) m_values.get(rowIndex).get(colIndex);
	}

	public int getMaxRow() {
		return m_maxRow;
	}

	public int getRows() {
		return m_values.size();
	}

	public int getCols() {
		return m_cols;
	}

	public ValueType getColType(int colIndex) {
		if (colIndex < 0 || colIndex >= m_cols) {
			return ValueType.NONE;
		}
		return m_colTypes.get(colIndex);
	}

	public int findRow(int startRow, int colIndex, Object value) {
		if (colIndex < 0 || colIndex >= m_cols) {
			return -1;
		}
		if (UtilFunc.getValueType(value) != m_colTypes.get(colIndex)) {
			return -1;
		}

		for (int i = startRow; i < m_values.size(); ++i) {
			if (m_values.get(i).get(colIndex).equals(value)) {
				return i;
			}
		}

		return -1;
	}

	public void clear() {
		m_values.clear();
	}

	public boolean needSave() {
		return m_save;
	}

	public void load(IoBuffer buffer) {
		m_cols = buffer.getShort();
		for (int j = 0; j < m_cols; ++j) {
			m_colTypes.add(j, ValueType.values()[buffer.getShort()]);
		}
		m_maxRow = buffer.getShort();
		int rows = buffer.getShort();
		for (int j = 0; j < rows; ++j) {
			loadRow(j, buffer);
		}
	}

	public void loadRow(int pos, IoBuffer buffer) {
		Object[] objs = new Object[m_cols];
		for (int k = 0; k < m_cols; ++k) {
			objs[k] = loadVal(pos, k, buffer);
		}
		insertRow(pos, objs);
	}

	public Object loadVal(int pos, int col, IoBuffer buffer) {
		Object obj = null;
		switch (m_colTypes.get(col)) {
		case SHORT:
			obj = buffer.getShort();
			break;
		case INT:
			obj = buffer.getInt();
			break;
		case LONG:
			obj = buffer.getLong();
			break;
		case BOOL:
			obj = buffer.get() == '1';
			break;
		case FLOAT:
			obj = buffer.getFloat();
			break;
		case DOUBLE:
			obj = buffer.getDouble();
			break;
		case STRING:
			obj = UtilFunc.getStringFromIoBuffer(buffer);
			break;
		default:
			break;
		}
		return obj;
	}

	public void store(IoBuffer buffer) {
		buffer.putShort((short) m_cols);

		for (int i = 0; i < m_cols; ++i) {
			buffer.putShort((short) m_colTypes.get(i).ordinal());
		}
		buffer.putShort((short) m_maxRow);
		buffer.putShort((short) m_values.size());
		for (int i = 0; i < m_values.size(); ++i) {
			storeRow(i, buffer);
		}
	}

	public void storeRow(int pos, IoBuffer buffer) {
		for (int j = 0; j < m_cols; ++j) {
			storeVal(pos, j, buffer);
		}
	}

	public void storeVal(int pos, int col, IoBuffer buffer) {
		switch (m_colTypes.get(col)) {
		case SHORT:
			buffer.putShort((short) m_values.get(pos).get(col));
			break;
		case INT:
			buffer.putInt((int) m_values.get(pos).get(col));
			break;
		case LONG:
			buffer.putLong((long) m_values.get(pos).get(col));
			break;
		case BOOL:
			buffer.put(((boolean) m_values.get(pos).get(col) ? "1" : "0").getBytes());
			break;
		case FLOAT:
			buffer.putFloat((float) m_values.get(pos).get(col));
			break;
		case DOUBLE:
			buffer.putDouble((double) m_values.get(pos).get(col));
			break;
		case STRING:
			UtilFunc.putStringToIoBuffer(buffer, m_values.get(pos).get(col).toString());
			break;
		default:
			break;
		}
	}
}
