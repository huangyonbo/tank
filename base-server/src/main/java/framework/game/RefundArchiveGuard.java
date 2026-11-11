package framework.game;

import org.apache.mina.core.buffer.IoBuffer;
import java.util.*;

/** Store-side compatibility boundary: old game binaries must not erase permanent refund receipts. */
public final class RefundArchiveGuard {
    public static final String RECORD = "GooglePlayRefundRec";
    private static final class Archive {
        int countOffset, recordsEnd, count;
        byte[] receipt;
        final Map<String,List<Object>> rows = new HashMap<>();
    }

    private static Archive read(byte[] bytes) {
        Archive result = new Archive();
        if (bytes == null || bytes.length == 0) return result;
        IoBuffer buffer = IoBuffer.wrap(bytes);
        ValueType[] types = ValueType.values();
        int properties = buffer.getShort();
        if (properties < 0) throw new IllegalArgumentException("invalid archive properties");
        for (int i=0;i<properties;i++) {
            UtilFunc.getStringFromIoBuffer(buffer);
            UtilFunc.loadObjFromBuffer(types[buffer.getShort()], buffer);
        }
        result.countOffset = buffer.position();
        result.count = buffer.getShort();
        if (result.count < 0) throw new IllegalArgumentException("invalid archive records");
        for (int i=0;i<result.count;i++) {
            int start = buffer.position();
            String name = UtilFunc.getStringFromIoBuffer(buffer);
            int cols = buffer.getShort();
            if (cols < 0) throw new IllegalArgumentException("invalid archive columns");
            ValueType[] columns = new ValueType[cols];
            for (int col=0;col<cols;col++) columns[col] = types[buffer.getShort()];
            int rows = buffer.getShort();
            if (rows < 0) throw new IllegalArgumentException("invalid archive rows");
            boolean refund = RECORD.equals(name);
            if (refund && (result.receipt != null || cols != 6
                    || !Arrays.equals(columns, new ValueType[]{ValueType.STRING,ValueType.STRING,
                        ValueType.LONG,ValueType.LONG,ValueType.LONG,ValueType.INT})))
                throw new IllegalArgumentException("invalid refund receipt schema");
            for (int row=0;row<rows;row++) {
                List<Object> values = new ArrayList<>();
                for (ValueType type : columns) values.add(UtilFunc.loadObjFromBuffer(type, buffer));
                if (refund) {
                    if (result.rows.put((String)values.get(0), values) != null
                            || !Arrays.asList(0,1).contains(values.get(5)))
                        throw new IllegalArgumentException("invalid refund receipt identity or state");
                }
            }
            if (refund) result.receipt = Arrays.copyOfRange(bytes, start, buffer.position());
        }
        result.recordsEnd = buffer.position();
        return result;
    }

    /** Missing entire records are carried forward; existing receipts cannot be removed or rewritten. */
    public static byte[] preserve(byte[] previous, byte[] incoming) {
        Archive before = read(previous);
        if (before.rows.isEmpty()) return incoming;
        Archive after = read(incoming);
        if (after.receipt != null) {
            for (Map.Entry<String,List<Object>> entry : before.rows.entrySet()) {
                List<Object> next = after.rows.get(entry.getKey()), old = entry.getValue();
                if (next == null || !old.subList(0,5).equals(next.subList(0,5))
                        || (Integer)next.get(5) < (Integer)old.get(5))
                    throw new IllegalStateException("permanent refund receipt regression");
            }
            return incoming;
        }
        if (incoming == null || incoming.length == 0 || after.count == Short.MAX_VALUE)
            throw new IllegalStateException("cannot preserve permanent refund receipts");
        IoBuffer output = IoBuffer.allocate(incoming.length + before.receipt.length);
        output.put(incoming, 0, after.recordsEnd);
        output.put(before.receipt);
        output.put(incoming, after.recordsEnd, incoming.length - after.recordsEnd);
        output.putShort(after.countOffset, (short)(after.count + 1));
        return output.array();
    }

    private RefundArchiveGuard() { }
}
