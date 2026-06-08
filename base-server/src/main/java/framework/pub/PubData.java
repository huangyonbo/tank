package framework.pub;

import framework.game.UtilFunc;
import framework.game.ValueType;
import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class PubData implements IPubData {
    private String m_name;
    private Map<String, PubProperty> m_Properties = new HashMap<>();
    private Map<String, PubRecord> m_Records = new HashMap<>();
    private static Logger logger = LoggerFactory.getLogger(PubData.class);
    private long lastUse = 0;

    public PubData(String name) {
        m_name = name;
        lastUse = System.currentTimeMillis();
    }

    public void onDestroy() {
        m_Properties.clear();
        m_Records.clear();
    }

    public void onUse() {
        lastUse = System.currentTimeMillis();
    }

    public String getName() {
        return m_name;
    }

    public void addProperty(String name, ValueType type, Object val, boolean save) {
        if (m_Properties.containsKey(name)) {
            return;
        }
        PubProperty pro = new PubProperty(name, type, val, save);
        m_Properties.put(name, pro);

    }

    public void delProperty(String name) {
        if (!m_Properties.containsKey(name)) {
            return;
        }
        m_Properties.remove(name);
    }

    public void setValue(String name, Object val) {
        PubProperty pro = m_Properties.get(name);
        if (pro == null) {
            logger.info("pro == null {}", name);
            return;
        }
        pro.setValue(val);
    }

    public Object getValue(String name) {
        if (m_Properties.containsKey(name)) {
            return m_Properties.get(name).getValue();
        }
        return null;
    }

    public ValueType getType(String name) {
        if (m_Properties.containsKey(name)) {
            return m_Properties.get(name).getType();
        }
        return ValueType.NONE;
    }

    public IPubRecord addRecord(String name, int cols, int maxRow, boolean save) {
        if (m_Records.containsKey(name)) {
            return null;
        }
        PubRecord rec = new PubRecord(name, cols, maxRow, save);
        m_Records.put(name, rec);
        return rec;
    }

    public IPubRecord getRecord(String name) {
        if (m_Records.containsKey(name)) {
            return m_Records.get(name);
        }
        return null;
    }

    public void delRecord(String name) {
        if (!m_Records.containsKey(name)) {
            return;
        }
        m_Records.remove(name);
    }

    public PubData load(byte[] data) {
        IoBuffer buffer = IoBuffer.wrap(data);
        int proCount = buffer.getShort();
        for (int i = 0; i < proCount; ++i) {
            String name = UtilFunc.getStringFromIoBuffer(buffer);
            PubProperty pro = new PubProperty(name);
            pro.load(buffer);
            m_Properties.put(name, pro);
        }
        int recCount = buffer.getShort();
        for (int i = 0; i < recCount; ++i) {
            String name = UtilFunc.getStringFromIoBuffer(buffer);
            PubRecord rec = new PubRecord(name);
            rec.load(buffer);
            m_Records.put(name, rec);
        }
        return this;
    }

    public byte[] getStoreData() {
        IoBuffer buffer = IoBuffer.allocate(1);
        buffer.setAutoExpand(true);
        //property
        int pos = buffer.position();
        buffer.putShort((short) 0);
        int proCount = 0;
        for (Entry<String, PubProperty> entry : m_Properties.entrySet()) {
            PubProperty pro = entry.getValue();
            if (pro.needSave()) {
                UtilFunc.putStringToIoBuffer(buffer, entry.getKey());
                pro.store(buffer);
                ++proCount;
            }
        }
        int newpos = buffer.position();
        buffer.position(pos).putShort((short) proCount);
        buffer = buffer.position(newpos);
        // record
        pos = buffer.position();
        buffer.putShort((short) 0);
        int recCount = 0;
        for (Entry<String, PubRecord> entry : m_Records.entrySet()) {
            PubRecord rec = entry.getValue();
            if (rec.needSave()) {
                UtilFunc.putStringToIoBuffer(buffer, entry.getKey());
                rec.store(buffer);
                ++recCount;
            }
        }
        newpos = buffer.position();
        buffer.position(pos).putShort((short) recCount);
        buffer = buffer.position(newpos);
        int size = buffer.position();
        return Arrays.copyOfRange(buffer.array(), 0, size);
    }

    public byte[] getDebugData() {
        IoBuffer buffer = IoBuffer.allocate(1);
        buffer.setAutoExpand(true);
        // property
        buffer.putShort((short) m_Properties.size());
        for (Entry<String, PubProperty> entry : m_Properties.entrySet()) {
            PubProperty pro = entry.getValue();
            UtilFunc.putStringToIoBuffer(buffer, entry.getKey());
            buffer.put((pro.needSave() ? "1" : "0").getBytes());
            pro.store(buffer);
        }
        // record
        buffer.putShort((short) m_Records.size());
        for (Entry<String, PubRecord> entry : m_Records.entrySet()) {
            PubRecord rec = entry.getValue();
            UtilFunc.putStringToIoBuffer(buffer, entry.getKey());
            buffer.put((rec.needSave() ? "1" : "0").getBytes());
            rec.store(buffer);
        }

        int size = buffer.position();
        return Arrays.copyOfRange(buffer.array(), 0, size);
    }

    public short getShort(String name) {
        if (!m_Properties.containsKey(name)) {
            return 0;
        }
        return (short) m_Properties.get(name).getValue();
    }

    public boolean getBool(String name) {
        if (!m_Properties.containsKey(name)) {
            return false;
        }
        return (boolean) m_Properties.get(name).getValue();
    }

    public int getInt(String name) {
        if (!m_Properties.containsKey(name)) {
            return 0;
        }
        return (int) m_Properties.get(name).getValue();
    }

    public long getLong(String name) {
        if (!m_Properties.containsKey(name)) {
            return 0l;
        }
        return (long) m_Properties.get(name).getValue();
    }

    public float getFloat(String name) {
        if (!m_Properties.containsKey(name)) {
            return 0f;
        }
        return (float) m_Properties.get(name).getValue();
    }

    public double getDouble(String name) {
        if (!m_Properties.containsKey(name)) {
            return 0f;
        }
        return (double) m_Properties.get(name).getValue();
    }

    public String getString(String name) {
        if (!m_Properties.containsKey(name)) {
            return "";
        }
        return m_Properties.get(name).getValue().toString();
    }

    public void clearRecord() {
        m_Records.clear();
    }

    @Override
    public int getPropertiesCount() {
        return m_Properties.size();
    }

    public List<PubProperty> getPropertys() {
        return m_Properties.values().stream().collect(Collectors.toList());
    }

    public List<PubRecord> getRecords() {
        return m_Records.values().stream().collect(Collectors.toList());
    }

    public boolean tick(long time) {
        if (time - lastUse > 600000) {
            //10分钟未用到就可以清理
            return true;
        }
        return false;
    }
}
