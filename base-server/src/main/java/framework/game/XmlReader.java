package framework.game;

import framework.SystemConfigData;
import org.apache.commons.codec.binary.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.*;

public class XmlReader implements ICfgReader {
	private static Logger logger = LoggerFactory.getLogger(XmlReader.class);

	private static String cfgUrl = null;
	String m_CfgName;
	Map<String, Integer> m_mapItems = new HashMap<>();
	List<Element> m_listItems = new ArrayList<>();
	Element root = null;

	public XmlReader() {
	}

	public boolean loadConfig(String path) {
		if (cfgUrl == null) {
			String config = SystemConfigData.getConfig("user.dir","");
			cfgUrl = "file:" + config + File.separator + "config/";
		}
		try {
			m_CfgName = cfgUrl + path;
			URL url = new URL(m_CfgName);
			SAXReader reader = new SAXReader();
			Document doc = reader.read(url);
			root = doc.getRootElement();
			Element foo = null;
			for (Iterator<?> i = root.elementIterator("item"); i.hasNext();) {
				foo = (Element) i.next();
				String configid = foo.attributeValue("Id");
				if (m_mapItems.containsKey(configid)) {
					logger.error("id [{}] is repeat in config [{}]", configid, m_CfgName);
					continue;
				}
				m_mapItems.put(configid, m_listItems.size());
				m_listItems.add(foo);
			}

		} catch (Exception e) {
			logger.error("load config [{}] failed.", path);
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public String getString(String key) {
		if (root == null) {
			return null;
		}
		return root.attributeValue(key);
	}

	public boolean getBool(String key) {
		String val = getString(key);
		if (val == null) {
			return false;
		}

		val = val.toLowerCase();
		if (StringUtils.equals(val, "false") || StringUtils.equals(val, "0")) {
			return false;
		}

		return true;
	}

	public short getShort(String key) {
		String val = getString(key);
		if (val == null) {
			return 0;
		}

		short res = 0;
		try {
			res = Short.parseShort(val);
		} catch (NumberFormatException exp) {
			logger.error("can't read short value from cfg:{} key:{}.", m_CfgName, key);
			return 0;
		}

		return res;
	}

	public int getInt(String key) {
		String val = getString(key);
		if (val == null) {
			return 0;
		}

		int res = 0;
		try {
			res = Integer.parseInt(val);
		} catch (NumberFormatException exp) {
			logger.error("can't read int value from cfg:{} key:{}.", m_CfgName, key);
			return 0;
		}

		return res;
	}

	public long getLong(String key) {
		String val = getString(key);
		if (val == null) {
			return 0;
		}

		long res = 0;
		try {
			res = Long.parseLong(val);
		} catch (NumberFormatException exp) {
			logger.error("can't read long value from cfg:{} key:{}.", m_CfgName, key);
			return 0;
		}

		return res;
	}

	public float getFloat(String key) {
		String val = getString(key);
		if (val == null) {
			return 0f;
		}

		float res = 0f;
		try {
			res = Float.parseFloat(val);
		} catch (NumberFormatException exp) {
			logger.error("can't read float value from cfg:{} key:{}.", m_CfgName, key);
			return 0f;
		}

		return res;
	}

	public double getDouble(String key) {
		String val = getString(key);
		if (val == null) {
			return 0f;
		}

		double res = 0f;
		try {
			res = Double.parseDouble(val);
		} catch (NumberFormatException exp) {
			logger.error("can't read double value from cfg:{} key:{}.", m_CfgName, key);
			return 0f;
		}

		return res;
	}

	public String[] getStringArray(String key, String regex) {
		String val = getString(key);
		if (val == null) {
			return null;
		}

		String[] res = val.split(regex);
		if (res.length <= 0) {
			return null;
		}

		return res;
	}

	public int[] getIntArray(String key, String regex) {
		String[] res = getStringArray(key, regex);
		if (res == null || res.length <= 0) {
			return null;
		}

		int[] array = new int[res.length];

		try {
			for (int i = 0; i < res.length; ++i) {
				array[i] = Integer.parseInt(res[i]);
			}
		} catch (NumberFormatException exp) {
			logger.error("can't read IntArray from cfg:{} key:{}.", m_CfgName, key);
			return null;
		}

		return array;
	}

	public float[] getFloatArray(String key, String regex) {
		String[] res = getStringArray(key, regex);
		if (res == null || res.length <= 0) {
			return null;
		}

		float[] array = new float[res.length];

		try {
			for (int i = 0; i < res.length; ++i) {
				array[i] = Float.parseFloat(res[i]);
			}
		} catch (NumberFormatException exp) {
			logger.error("can't read FloatArray from cfg:{} key:{}.", m_CfgName, key);
			return null;
		}

		return array;
	}

	public boolean containsKey(String id) {
		return m_mapItems.containsKey(id);
	}

	public String getString(String id, String key) {
		if (!m_mapItems.containsKey(id)) {
			return null;
		}
		Element node = m_listItems.get(m_mapItems.get(id));
		return node.attributeValue(key);
	}

	public boolean getBool(String id, String key) {
		String val = getString(id, key);
		if (val == null) {
			return false;
		}

		val = val.toLowerCase();
		if (StringUtils.equals(val, "false") || StringUtils.equals(val, "0")) {
			return false;
		}

		return true;
	}

	public short getShort(String id, String key) {
		String val = getString(id, key);
		if (val == null) {
			return 0;
		}

		short res = 0;
		try {
			res = Short.parseShort(val);
		} catch (NumberFormatException exp) {
			logger.error("can't read short value from cfg:{} id:{} key:{}.", m_CfgName, id, key);
			return 0;
		}

		return res;
	}

	public int getInt(String id, String key) {
		String val = getString(id, key);
		if (val == null) {
			return 0;
		}

		int res = 0;
		try {
			res = Integer.parseInt(val);
		} catch (NumberFormatException exp) {
			logger.error("can't read int value from cfg:{} id:{} key:{}.", m_CfgName, id, key);
			return 0;
		}

		return res;
	}

	public long getLong(String id, String key) {
		String val = getString(id, key);
		if (val == null) {
			return 0;
		}

		long res = 0;
		try {
			res = Long.parseLong(val);
		} catch (NumberFormatException exp) {
			logger.error("can't read long value from cfg:{} id:{} key:{}.", m_CfgName, id, key);
			return 0;
		}

		return res;
	}

	public float getFloat(String id, String key) {
		String val = getString(id, key);
		if (val == null) {
			return 0f;
		}

		float res = 0f;
		try {
			res = Float.parseFloat(val);
		} catch (NumberFormatException exp) {
			logger.error("can't read float value from cfg:{} id:{} key:{}.", m_CfgName, id, key);
			return 0f;
		}

		return res;
	}

	public double getDouble(String id, String key) {
		String val = getString(id, key);
		if (val == null) {
			return 0f;
		}

		double res = 0f;
		try {
			res = Double.parseDouble(val);
		} catch (NumberFormatException exp) {
			logger.error("can't read double value from cfg:{} id:{} key:{}.", m_CfgName, id, key);
			return 0f;
		}

		return res;
	}

	public String[] getStringArray(String id, String key, String regex) {
		String val = getString(id, key);
		if (val == null) {
			return null;
		}

		String[] res = val.split(regex);
		if (res.length <= 0) {
			return null;
		}

		return res;
	}

	public int[] getIntArray(String id, String key, String regex) {
		String[] res = getStringArray(id, key, regex);
		if (res == null || res.length <= 0) {
			return null;
		}

		int[] array = new int[res.length];

		try {
			for (int i = 0; i < res.length; ++i) {
				array[i] = Integer.parseInt(res[i]);
			}
		} catch (NumberFormatException exp) {
			logger.error("can't read IntArray from cfg:{} id:{} key:{}.", m_CfgName, id, key);
			return null;
		}

		return array;
	}

	public float[] getFloatArray(String id, String key, String regex) {
		String[] res = getStringArray(id, key, regex);
		if (res == null || res.length <= 0) {
			return null;
		}

		float[] array = new float[res.length];

		try {
			for (int i = 0; i < res.length; ++i) {
				array[i] = Float.parseFloat(res[i]);
			}
		} catch (NumberFormatException exp) {
			logger.error("can't read FloatArray from cfg:{} id:{} key:{}.", m_CfgName, id, key);
			return null;
		}

		return array;
	}

	public int getItemCount() {
		return m_listItems.size();
	}

	public String getString(int index, String key) {
		if (index < 0 || index >= m_listItems.size()) {
			return null;
		}
		Element node = m_listItems.get(index);
		return node.attributeValue(key);
	}

	public boolean getBool(int index, String key) {
		String val = getString(index, key);
		if (val == null || val.isEmpty()) {
			return false;
		}
		val = val.toLowerCase();
		if (StringUtils.equals(val, "false") || StringUtils.equals(val, "0")) {
			return false;
		}
		return true;
	}

	public short getShort(int index, String key) {
		String val = getString(index, key);
		if (val == null || val.isEmpty()) {
			return 0;
		}

		short res = 0;
		try {
			res = Short.parseShort(val);
		} catch (NumberFormatException exp) {
			logger.error("can't read short value from cfg:{} index:{} key:{}.", m_CfgName, index, key);
			return 0;
		}

		return res;
	}

	public int getInt(int index, String key) {
		String val = getString(index, key);
		if (val == null || val.isEmpty()) {
			return 0;
		}

		int res = 0;
		try {
			res = Integer.parseInt(val);
		} catch (NumberFormatException exp) {
			logger.error("can't read int value from cfg:{} index:{} key:{}.", m_CfgName, index, key);
			return 0;
		}

		return res;
	}

	public long getLong(int index, String key) {
		String val = getString(index, key);
		if (val == null || val.isEmpty()) {
			return 0;
		}

		long res = 0;
		try {
			res = Long.parseLong(val);
		} catch (NumberFormatException exp) {
			logger.error("can't read long value from cfg:{} index:{} key:{}.", m_CfgName, index, key);
			return 0;
		}

		return res;
	}

	public float getFloat(int index, String key) {
		String val = getString(index, key);
		if (val == null || val.isEmpty()) {
			return 0f;
		}

		float res = 0f;
		try {
			res = Float.parseFloat(val);
		} catch (NumberFormatException exp) {
			logger.error("can't read float value from cfg:{} index:{} key:{}.", m_CfgName, index, key);
			return 0f;
		}

		return res;
	}

	public double getDouble(int index, String key) {
		String val = getString(index, key);
		if (val == null || val.isEmpty()) {
			return 0f;
		}

		double res = 0f;
		try {
			res = Double.parseDouble(val);
		} catch (NumberFormatException exp) {
			logger.error("can't read double value from cfg:{} index:{} key:{}.", m_CfgName, index, key);
			return 0f;
		}

		return res;
	}

	public String[] getStringArray(int index, String key, String regex) {
		String val = getString(index, key);
		if (val == null || val.length() == 0) {
			return null;
		}

		String[] res = val.split(regex);
		if (res.length <= 0) {
			return null;
		}

		return res;
	}

	public int[] getIntArray(int index, String key, String regex) {
		String[] res = getStringArray(index, key, regex);
		if (res == null || res.length <= 0) {
			return null;
		}

		int[] array = new int[res.length];

		try {
			for (int i = 0; i < res.length; ++i) {
				array[i] = Integer.parseInt(res[i]);
			}
		} catch (NumberFormatException exp) {
			logger.error("can't read IntArray from cfg:{} index:{} key:{}.", m_CfgName, index, key);
			return null;
		}

		return array;
	}

	public float[] getFloatArray(int index, String key, String regex) {
		String[] res = getStringArray(index, key, regex);
		if (res == null || res.length <= 0) {
			return null;
		}

		float[] array = new float[res.length];

		try {
			for (int i = 0; i < res.length; ++i) {
				array[i] = Float.parseFloat(res[i]);
			}
		} catch (NumberFormatException exp) {
			logger.error("can't read FloatArray from cfg:{} index:{} key:{}.", m_CfgName, index, key);
			return null;
		}

		return array;
	}
}
