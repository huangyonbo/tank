package framework.game;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;

import framework.JsonUtil;
import redis.clients.jedis.Jedis;

public class UtilFunc {
	
	public static ValueType getValueType(Object obj) {
		if (obj.getClass() == Short.class) {
			return ValueType.SHORT;
		} else if (obj.getClass() == Boolean.class) {
			return ValueType.BOOL;
		} else if (obj.getClass() == Integer.class) {
			return ValueType.INT;
		} else if (obj.getClass() == Long.class) {
			return ValueType.LONG;
		} else if (obj.getClass() == Float.class) {
			return ValueType.FLOAT;
		} else if (obj.getClass() == Double.class) {
			return ValueType.DOUBLE;
		} else if (obj.getClass() == String.class) {
			return ValueType.STRING;
		}
		return ValueType.NONE;
	}

	public static void putStringToIoBuffer(IoBuffer buffer, String str) {
		buffer.putShort((short) StringUtils.getBytesUtf8(str).length);
		buffer.put(StringUtils.getBytesUtf8(str));
	}

	public static String getStringFromIoBuffer(IoBuffer buffer) {
		short len = buffer.getShort();
		if (len <= 0) {
			return "";
		}
		byte[] str = new byte[len];
		buffer.get(str);
		return StringUtils.newStringUtf8(str);
	}

	public static Object loadObjFromBuffer(ValueType type, IoBuffer buffer) {
		Object val = null;
		switch (type) {
		case SHORT:
			val = buffer.getShort();
			break;
		case INT:
			val = buffer.getInt();
			break;
		case LONG:
			val = buffer.getLong();
			break;
		case BOOL:
			val = buffer.get() == '1';
			break;
		case FLOAT:
			val = buffer.getFloat();
			break;
		case DOUBLE:
			val = buffer.getDouble();
			break;
		case STRING:
			val = UtilFunc.getStringFromIoBuffer(buffer);
			break;
		default:
			break;
		}
		return val;
	}

	public static void storeObjToBuffer(ValueType type, Object obj, IoBuffer buffer) {
		switch (type) {
		case SHORT:
			buffer.putShort((short) obj);
			break;
		case INT:
			buffer.putInt((int) obj);
			break;
		case LONG:
			buffer.putLong((long) obj);
			break;
		case BOOL:
			buffer.put(((boolean) obj ? "1" : "0").getBytes());
			break;
		case FLOAT:
			buffer.putFloat((float) obj);
			break;
		case DOUBLE:
			buffer.putDouble((double) obj);
			break;
		case STRING:
			putStringToIoBuffer(buffer, obj.toString());
			break;
		default:
			break;
		}
	}

	public static boolean tryGetDistributedLock(Jedis jedis, String lockKey, String requestId, int expireTime) {
//		String result = jedis.set(lockKey, requestId, "NX", "PX", expireTime);
//
//		if ("OK".equals(result)) {
//			return true;
//		}
		return false;
	}
	
	public static Object transformByType(ValueType type,String value,Logger logger) {
		Object obj = null;
		switch (type) {
		case SHORT:
			obj = new Short(value);
			break;
		case INT:
			obj = new Integer(value);
			break;
		case LONG:
			obj = new Long(value);
			break;
		case BOOL:
			obj = new Boolean(value);
			break;
		case FLOAT:
			obj = new Float(value);
			break;
		case DOUBLE:
			obj = new Double(value);
			break;
		case STRING:
			obj = value;
			break;
		case OBJECT:
			String[] vs = value.split("#");
			Class<?> clazz;
			try {
				clazz = Class.forName(vs[0]);
			} catch (ClassNotFoundException e) {
				logger.error(vs[0] + " can not find");
				return null;
			}
			try {
				obj = JsonUtil.decodeToObj(vs[1],clazz);
			} catch (Exception e) {
				logger.error("{} tranform to {} json error",vs[1],vs[0]);
				return null;
			}
			break;
		default:
			return null;
		}
		return obj;
	}
}
