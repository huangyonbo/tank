package framework;

import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;
import org.apache.mina.core.session.IoSession;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 
 * 描述： 服务集合
 * 
 */
public class ServerSet implements PropertyKey{

	public static final String SERVER_LOGIC_NAME_BACK   = "Back";
	public static final String SERVER_LOGIC_NAME_MATCH  = "Match";
	public static final String SERVER_LOGIC_NAME_PUBLIC = "Public";
	public static final String SERVER_LOGIC_NAME_GAME   = "Game1";
	public static final String SERVER_LOGIC_NAME_STORE  = "Store";

	private static Logger logger = LoggerFactory.getLogger(ServerSet.class);
	private static Map<String, ServerConfig> m_serConfigs = new HashMap<>();
	private static Map<Integer, ServerConfig> m_serConfigsbyID = new HashMap<>();
	private static List<String> m_serNames = new ArrayList<>();
	private static Map<String, Set<String>> m_serNamesByType = new HashMap<>();
	private static Map<String, BaseServer> m_baseServer = new HashMap<>();

	private Map<String, IoSession> m_mapSessions = new HashMap<String, IoSession>();
	private Map<Integer, IoSession> m_mapSessionsByID = new HashMap<Integer, IoSession>();

	String m_name;

	public ServerSet(String name) {
		m_name = name;
	}

	public void addServer(String name, IoSession session) {
		ServerConfig config = getServerConfig(name);
		int id = config.id;
//		logger.info("[{}] add server: [{}] {}",m_name,name,session.toString());
		m_mapSessions.put(name,session);
		m_mapSessionsByID.put(id,session);
		session.setAttribute("SerID",id);
		session.setAttribute(PLAYER_PROPERTY_NAME,name);
		session.setAttribute("Type",config.type);
		session.setAttribute("Load",0);
	}

	public void removeServer(String name) {
		ServerConfig config = getServerConfig(name);
		int id = config == null ? -1 : config.id;
		if (m_mapSessions.containsKey(name)) {
			m_mapSessions.remove(name);
		}
		if (m_mapSessionsByID.containsKey(id)) {
			m_mapSessionsByID.remove(id);
		}
	}

	public void addServer(String name, BaseServer ser) {
		m_baseServer.put(name, ser);
	}

	public BaseServer getBaseServer(String name) {
		if (m_baseServer.containsKey(name)) {
			return m_baseServer.get(name);
		}
		return null;
	}

	public IoSession getServer(String name) {
		if (m_mapSessions.containsKey(name)) {
			return m_mapSessions.get(name);
		}
		return null;
	}
	
	public int getServerSize() {
		return m_mapSessions.size();
	}
	
	public IoSession getServer(int index) {
		if (m_mapSessionsByID.containsKey(index)) {
			return m_mapSessionsByID.get(index);
		}
		return null;
	}
	
	public ServerConfig getServerConfig(int index) {
		if (m_serConfigsbyID.containsKey(index)) {
			return m_serConfigsbyID.get(index);
		}
		return null;
	}

	public ServerConfig getServerConfig(String name) {
		if (m_serConfigs.containsKey(name)) {
			return m_serConfigs.get(name);
		}
		return null;
	}

	public List<String> getServers() {
		return m_serNames;
	}
	
	public Object[] getServersByType(String type) {
		if (!m_serNamesByType.containsKey(type)) {
			return new Object[0];
		}
		return m_serNamesByType.get(type).toArray();
	}
	
	public int getServersCountByType(String type) {
		if (!m_serNamesByType.containsKey(type)) {
			return 0;
		}
		return m_serNamesByType.get(type).size();
	}
	
	public static ServerConfig decodeFromJson(JsonObject element){
		ServerConfig serCfg = new ServerConfig();
		serCfg.id = element.get("id").getAsInt();
		serCfg.logicName = element.get("logicName").getAsString();
		serCfg.name = element.get("name").getAsString();
		serCfg.type = element.get("type").getAsString();
		serCfg.addr = element.get("addr").getAsString();
		serCfg.ser  = element.get("ser").getAsString();
		serCfg.port = element.get("port").getAsInt();
		serCfg.front = element.get("front").getAsBoolean();
		if (serCfg.front) {
			serCfg.frontAddr = element.get("frontAddr").getAsString();
			serCfg.frontPort = element.get("frontPort").getAsInt();
		}
		serCfg.next = element.get("next").getAsString();
		return serCfg;
	}
	
	public boolean addServerConfig(ServerConfig serCfg) {
		if (m_serNames.contains(serCfg.name)) {
			logger.error("contains key {}", serCfg.name);
			return false;
		}
		Set<String> temp = m_serNamesByType.get(serCfg.type);
		if (!StringUtils.equals(serCfg.type,"gate")){
			//只有gate可以开多个实例,其余都是单例
			if (temp != null && temp.size() > 0){
				return false;
			}
		}
		m_serNames.add(serCfg.name);
		if (temp == null) {
			temp = new HashSet<String>();
			m_serNamesByType.put(serCfg.type, temp);
		}
		temp.add(serCfg.name);
		m_serConfigs.put(serCfg.name,serCfg);
		m_serConfigsbyID.put(serCfg.id,serCfg);
		return true;
	}
	
	public boolean addServerConfig(JsonObject element) {
		ServerConfig serCfg = decodeFromJson(element);
		return addServerConfig(serCfg);
	}

	private ServerConfig DecodeFromXml(Element element,String localIp){
		ServerConfig serCfg = new ServerConfig();
		serCfg.id = Integer.parseInt(element.attributeValue("id"));
		serCfg.logicName = element.attributeValue("logicName");
		serCfg.name = element.attributeValue("name");
		serCfg.type = element.getName();
		serCfg.addr = localIp;
		serCfg.ser  = element.attributeValue("ser");
		serCfg.port = Integer.parseInt(element.attributeValue("port"));
		String frontIp = element.attributeValue("frontIp");
		if (frontIp != null) {
			serCfg.front = true;
			serCfg.frontAddr = "".equals(frontIp) ? localIp : frontIp;
			serCfg.frontPort = Integer.parseInt(element.attributeValue("frontPort"));
		}
		serCfg.next = element.attributeValue("next");
		return serCfg;
	}

	public boolean addServerConfig(Map<String, String> map, String localIp) {
		ServerConfig serCfg = DecodeFromMap(map,localIp);
		return addServerConfig(serCfg);
	}

	public void RemoveName(String name){
		ServerConfig config = m_serConfigs.remove(name);
		if (config == null){
			return;
		}
		m_serConfigsbyID.remove(config.id);
		m_serNames.remove(name);
		Set<String> names  = m_serNamesByType.get(config.type);
		names.remove(name);
	}

	private ServerConfig DecodeFromMap(Map<String, String> map,String localIp){
		ServerConfig serCfg = new ServerConfig();
		serCfg.id = Integer.parseInt(map.get("id"));
		serCfg.logicName = map.get("logicName");
		serCfg.name = map.get("name");
		serCfg.type = map.get("type");
		serCfg.addr = localIp;
		serCfg.ser  = map.get("ser");
		serCfg.port = Integer.parseInt(map.get("port"));
		String frontIp = map.get("frontIp");
		if (frontIp != null) {
			serCfg.front = true;
			serCfg.frontAddr = "".equals(frontIp) ? localIp : frontIp;
			serCfg.frontPort = Integer.parseInt(map.get("frontPort"));
		}
		serCfg.next = map.get("next");
		return serCfg;
	}
}
