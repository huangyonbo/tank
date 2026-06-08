package framework.game;

import com.google.protobuf.ByteString;
import framework.net.ClientMsgDef;
import framework.net.message.ClientMsg;
import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Record implements IRecord {
	private GameObject m_owner;
	private String m_name;
	private boolean m_pubVis;
	private boolean m_priVis;
	private boolean m_save;
	private int m_maxRow;
	private int m_cols;
	private List<ValueType> m_colTypes = new ArrayList<>();

	private List<List<Object>> m_values = new LinkedList<>();
	static Logger logger = LoggerFactory.getLogger(Record.class);

	public Record(String name, int cols, int maxRow, boolean pub, boolean pri, boolean save) {
		m_name = name;
		m_cols = cols;
		m_maxRow = maxRow;
		m_pubVis = pub;
		m_priVis = pri || pub;
		m_save = save;
		for (int i = 0; i < cols; i++) {
			m_colTypes.add(ValueType.INT);
		}
	}
	
	public void onDestroy() {
		m_owner = null;
		m_name  = null;
		m_colTypes.clear();
		for (List<Object> list : m_values){
			list.clear();
		}
		m_values.clear();
	}
	
	public String getName() {
		return m_name;
	}

	public void setMaxRow(int maxRow) {
		m_maxRow = maxRow;
	}

	public void setOwner(GameObject owner) {
		m_owner = owner;
	}

	public void setColType(int col, ValueType type) {
		if (col < 0 || col >= m_cols) {
			return;
		}
		m_colTypes.set(col, type);
		if (type == ValueType.OBJECT) {
			m_pubVis = false;
			m_priVis = false;
			m_save = false;
		}
	}
	
	public boolean addCmdRow(String value) {
		String[] objects = value.split("\\|");
		if (objects.length != m_cols) {
			logger.error(m_name + "  objects.lenght not match");
			return false;
		}
		List<Object> row = new ArrayList<>();
		for (int i = 0; i < m_cols; i++) {
			String _value = objects[i];
			ValueType type = m_colTypes.get(i);
			Object obj = UtilFunc.transformByType(type, _value, logger);
			if (obj != null){
				row.add(obj);
			}else{
				return false;
			}
		}
		m_values.add(row);
		if (m_owner != null) {
			m_owner.getKernel().getClassSet().onRecordChange(m_owner, m_name);
			if (m_owner.getType() == GameObjectType.GOTYPE_PLAYER && m_priVis) {
				IoBuffer buffer = IoBuffer.allocate(1);
				buffer.setAutoExpand(true);
				for (int j = 0; j < m_cols; ++j) {
					ValueType type = m_colTypes.get(j);
					UtilFunc.storeObjToBuffer(type,row.get(j),buffer);
				}
				ClientMsg.RecAddRow.Builder addRow = ClientMsg.RecAddRow.newBuilder();
				addRow.setObjectId(m_owner.getObjectID());
				addRow.setName(m_name);
				addRow.setRow(m_values.size() - 1);
				addRow.setData(ByteString.copyFrom(buffer.array()));
				m_owner.getKernel().innerSendMessage(m_owner, ClientMsgDef.CLIENT_REC_ADD_ROW.ordinal(),addRow.build().toByteArray());
			}
		}
		return true;
	}
	
	public boolean addRow(Object... objects) {
		if (m_values.size() >= m_maxRow) {
            logger.error(m_name + "  max row exceeded");
			return false;
		}
		if (objects.length != m_cols) {
            logger.error(m_name + "  objects.lenght not match");
			return false;
		}

		for (int i = 0; i < m_cols; ++i) {
			if (m_colTypes.get(i) != ValueType.OBJECT && UtilFunc.getValueType(objects[i]) != m_colTypes.get(i)) {
				logger.error(m_name + "  type mismatch");
                return false;
			}
		}
		List<Object> row = new ArrayList<>();
		for (int i = 0; i < m_cols; ++i) {
			row.add(objects[i]);
		}
		m_values.add(row);
		if (m_owner != null) {
			m_owner.getKernel().getClassSet().onRecordChange(m_owner, m_name);
			if (m_owner.getType() == GameObjectType.GOTYPE_PLAYER && m_priVis) {
				IoBuffer buffer = IoBuffer.allocate(1);
				buffer.setAutoExpand(true);
				for (int j = 0; j < m_cols; ++j) {
					ValueType type = m_colTypes.get(j);
					UtilFunc.storeObjToBuffer(type,objects[j],buffer);
				}
				ClientMsg.RecAddRow.Builder addRow = ClientMsg.RecAddRow.newBuilder();
				addRow.setObjectId(m_owner.getObjectID());
				addRow.setName(m_name);
				addRow.setRow(m_values.size() - 1);
				addRow.setData(ByteString.copyFrom(buffer.array()));
				m_owner.getKernel().innerSendMessage(m_owner, ClientMsgDef.CLIENT_REC_ADD_ROW.ordinal(),addRow.build().toByteArray());
			}
		}
		return true;
	}

	public boolean innerAddRow(Object... objects) {
		if (m_values.size() >= m_maxRow) {
			return false;
		}
		if (objects.length != m_cols) {
			return false;
		}
		for (int i = 0; i < m_cols; i++) {
			ValueType _type = m_colTypes.get(i);
			if (_type != ValueType.OBJECT && UtilFunc.getValueType(objects[i]) != _type) {
				return false;
			}
		}
		ArrayList<Object> row = new ArrayList<Object>();
		for (int i = 0; i < m_cols ; i++) {
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
		for (int i = 0; i < m_cols; ++i) {
			if (m_colTypes.get(i) != ValueType.OBJECT && UtilFunc.getValueType(objects[i]) != m_colTypes.get(i)) {
				return false;
			}
		}
		if (rowIndex < 0 || rowIndex > m_values.size()) {
			return false;
		}
		ArrayList<Object> row = new ArrayList<>();
		for (int i = 0; i < m_cols; ++i) {
			row.add(objects[i]);
		}
		m_values.add(rowIndex, row);
		if (m_owner == null){
			return true;
		}
		m_owner.getKernel().getClassSet().onRecordChange(m_owner, m_name);
		if (m_owner.getType() == GameObjectType.GOTYPE_PLAYER && m_priVis) {
			IoBuffer buffer = IoBuffer.allocate(128);
			buffer.setAutoExpand(true);
			for (int j = 0; j < m_cols; ++j) {
				ValueType type = m_colTypes.get(j);
				UtilFunc.storeObjToBuffer(type,objects[j],buffer);
			}
			ClientMsg.RecAddRow.Builder addRow = ClientMsg.RecAddRow.newBuilder();
			addRow.setObjectId(m_owner.getObjectID());
			addRow.setName(m_name);
			addRow.setRow(rowIndex);
			addRow.setData(ByteString.copyFrom(buffer.array()));
			m_owner.getKernel().innerSendMessage(m_owner, ClientMsgDef.CLIENT_REC_ADD_ROW.ordinal(),addRow.build().toByteArray());
		}

		return true;
	}

	public void removeRow(int rowIndex) {
		if (rowIndex < 0 || rowIndex >= m_values.size()) {
			return;
		}
		m_values.remove(rowIndex);
		if (m_owner == null){
			return;
		}
		m_owner.getKernel().getClassSet().onRecordChange(m_owner, m_name);
		if (m_owner.getType() == GameObjectType.GOTYPE_PLAYER && m_priVis) {
			ClientMsg.RecDelRow.Builder addRow = ClientMsg.RecDelRow.newBuilder();
			addRow.setObjectId(m_owner.getObjectID());
			addRow.setName(m_name);
			addRow.setRow(rowIndex);
			m_owner.getKernel().innerSendMessage(m_owner, ClientMsgDef.CLIENT_REC_DEL_ROW.ordinal(),addRow.build().toByteArray());
		}
	}
	
	public void setCmdValue(int rowIndex, int colIndex, String value) {
		if (rowIndex < 0 || rowIndex >= m_values.size()) {
			return;
		}
		if (colIndex < 0 || colIndex >= m_cols) {
			return;
		}
		ValueType type = m_colTypes.get(colIndex);
		Object obj = UtilFunc.transformByType(type,value,logger);
		if (obj != null){
			m_values.get(rowIndex).set(colIndex,obj);
			if (m_owner == null){
				return;
			}
			m_owner.getKernel().getClassSet().onRecordChange(m_owner, m_name);
			if (m_owner.getType() == GameObjectType.GOTYPE_PLAYER && m_priVis) {
				IoBuffer buffer = IoBuffer.allocate(128);
				buffer.setAutoExpand(true);
				UtilFunc.storeObjToBuffer(type,obj,buffer);
				ClientMsg.RecSetVal.Builder addRow = ClientMsg.RecSetVal.newBuilder();
				addRow.setObjectId(m_owner.getObjectID());
				addRow.setName(m_name);
				addRow.setRow(rowIndex);
				addRow.setCol(colIndex);
				addRow.setData(ByteString.copyFrom(buffer.array()));
				m_owner.getKernel().innerSendMessage(m_owner, ClientMsgDef.CLIENT_REC_SET_VAL.ordinal(),addRow.build().toByteArray());
			}
		}
	}
	
	public void setValue(int rowIndex, int colIndex, Object obj) {
		if (rowIndex < 0 || rowIndex >= m_values.size()) {
			return;
		}
		if (colIndex < 0 || colIndex >= m_cols) {
			return;
		}
		if (getValue(rowIndex, colIndex) == obj) {
			return;
		}
		ValueType type = m_colTypes.get(colIndex);
		if (type != ValueType.OBJECT && UtilFunc.getValueType(obj) != type) {
			return;
		}
		m_values.get(rowIndex).set(colIndex, obj);
		if (m_owner == null){
			return;
		}
		m_owner.getKernel().getClassSet().onRecordChange(m_owner, m_name);
		if (m_owner.getType() == GameObjectType.GOTYPE_PLAYER && m_priVis) {
			IoBuffer buffer = IoBuffer.allocate(128);
			buffer.setAutoExpand(true);
			UtilFunc.storeObjToBuffer(type,obj,buffer);
			ClientMsg.RecSetVal.Builder addRow = ClientMsg.RecSetVal.newBuilder();
			addRow.setObjectId(m_owner.getObjectID());
			addRow.setName(m_name);
			addRow.setRow(rowIndex);
			addRow.setCol(colIndex);
			addRow.setData(ByteString.copyFrom(buffer.array()));
			m_owner.getKernel().innerSendMessage(m_owner, ClientMsgDef.CLIENT_REC_SET_VAL.ordinal(),addRow.build().toByteArray());
		}
	}

	@Override
	public void setMoreValue(int rowIndex, Object... objs){
		if (objs.length % 2 != 0){
			return;
		}
		if (rowIndex < 0 || rowIndex >= m_values.size()) {
			return;
		}
		for (int i = 0; i < objs.length ;) {
			int index = (int)objs[i++];
			Object obj = objs[i++];
			if (index < 0 || index >= m_cols) {
				return;
			}
			if (m_colTypes.get(index) != UtilFunc.getValueType(obj)) {
				return;
			}
		}
		List<Object> target = m_values.get(rowIndex);
		IoBuffer buffer = null;
		if (m_owner != null && m_owner.getType() == GameObjectType.GOTYPE_PLAYER && m_priVis){
			buffer = IoBuffer.allocate(128);
			buffer.setAutoExpand(true);
			buffer.putInt(objs.length / 2);
		}
		for (int i = 0; i < objs.length ;) {
			int index = (int)objs[i++];
			Object obj = objs[i++];
			target.set(index,obj);
			if (buffer != null){
				buffer.putInt(index);
				UtilFunc.storeObjToBuffer(m_colTypes.get(index),obj,buffer);
			}
		}
		if (m_owner != null){
			m_owner.getKernel().getClassSet().onRecordChange(m_owner,m_name);
		}
		if (buffer == null){
			return;
		}
		ClientMsg.RecSetVal.Builder builder = ClientMsg.RecSetVal.newBuilder();
		builder.setObjectId(m_owner.getObjectID());
		builder.setName(m_name);
		builder.setRow(rowIndex);
		builder.setCol(0);
		builder.setData(ByteString.copyFrom(buffer.array()));
		m_owner.getKernel().innerSendMessage(m_owner, ClientMsgDef.CLIENT_REC_SET_MORE_VAL.ordinal(),builder.build().toByteArray());
	}

	@SuppressWarnings("unchecked")
	public <T> T getValue(int rowIndex, int colIndex) {
		if (rowIndex < 0 || rowIndex >= m_values.size()) {
			return null;
		}
		if (colIndex < 0 || colIndex >= m_cols) {
			return null;
		}
		return (T) m_values.get(rowIndex).get(colIndex);
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
	
	public int findCmdRow(int startRow, int colIndex, String value) {
		if (colIndex < 0 || colIndex >= m_cols) {
			return -1;
		}
		ValueType type = m_colTypes.get(colIndex);
		Object obj = UtilFunc.transformByType(type,value,logger);
		if (obj == null){
			return -1;
		}
		for (int i = startRow ; i < m_values.size(); i++) {
			List<Object> objs = m_values.get(i);
			if (objs.get(colIndex).equals(obj)) {
				return i;
			}
		}
		return -1;
	}
	
	public int findRow(int startRow, int colIndex, Object value) {
		if (colIndex < 0 || colIndex >= m_cols) {
			return -1;
		}
		if (UtilFunc.getValueType(value) != m_colTypes.get(colIndex)) {
			return -1;
		}
		for (int i = startRow; i < m_values.size(); ++i) {
			List<Object> objs = m_values.get(i);
			if (objs.get(colIndex).equals(value)) {
				return i;
			}
		}
		return -1;
	}

	public int findRow(int startRow, RowFilter filter){
		for (int i = startRow; i < m_values.size(); ++i) {
			List<Object> objs = m_values.get(i);
			if (filter.check(i,objs)){
				return i;
			}
		}
		return -1;
	}

	public void clear() {
		if (m_values.size() == 0){
			return;
		}
		m_values.clear();
		if (m_owner == null){
			return;
		}
		m_owner.getKernel().getClassSet().onRecordChange(m_owner, m_name);
		if (m_owner.getType() == GameObjectType.GOTYPE_PLAYER && m_priVis) {
			ClientMsg.RecClear.Builder addRow = ClientMsg.RecClear.newBuilder();
			addRow.setObjectId(m_owner.getObjectID());
			addRow.setName(m_name);
			m_owner.getKernel().innerSendMessage(m_owner, ClientMsgDef.CLIENT_REC_CLEAR.ordinal(),addRow.build().toByteArray());
		}
	}

	public boolean needSave() {
		return m_save;
	}

	public boolean isPubVisible() {
		return m_pubVis;
	}

	public boolean isPriVisible() {
		return m_priVis;
	}

	public void loadFromBuff(IoBuffer buffer) {
		ValueType[] _types = ValueType.values();
		m_colTypes.clear();
		for (int i = 0; i < m_cols ; i++) {
			m_colTypes.add(_types[buffer.getShort()]);
		}
		int rows = buffer.getShort();
		Object[] objs = new Object[m_cols];
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < m_cols ; j++) {
				objs[j] = UtilFunc.loadObjFromBuffer(m_colTypes.get(j),buffer);
			}
			innerAddRow(objs);
		}
	}

	public void storeToBuff(IoBuffer buffer) {
		UtilFunc.putStringToIoBuffer(buffer,m_name);
		buffer.putShort((short)m_cols);
		for (int i = 0; i < m_cols; i++) {
			buffer.putShort((short) getColType(i).ordinal());
		}
		short rows = (short) getRows();
		buffer.putShort(rows);
		for (int i = 0 ; i < rows ; i++) {
			for (int j = 0 ; j < m_cols ; j++){
				ValueType type = getColType(j);
				UtilFunc.storeObjToBuffer(type, getValue(i,j),buffer);
			}
		}
	}
}
