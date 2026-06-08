package framework;

import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SystemConfigData {

	private static Map<String,Object> values = new HashMap<>();

	public static Properties getProps(String path) {
		Properties properties = new Properties();
		try {
			InputStream in = new FileInputStream(path);
			properties.load(in);
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return properties;
	}

	public static boolean load(String path,YamlMapFactoryBean yamlMapFactoryBean) {
		if (yamlMapFactoryBean == null){
			String filePath = path + File.separator + "config" + File.separator + "application.yml";
			yamlMapFactoryBean = new YamlMapFactoryBean();
			yamlMapFactoryBean.setResources(new FileSystemResource(filePath));
		}
		values.clear();
		Map<String, Object> alls = yamlMapFactoryBean.getObject();
		String active = null;
		Map<String, Object> temp = (Map<String, Object>)alls.get("spring");
		if (temp != null){
			Object obj = temp.get("profiles");
			if (obj != null){
				Map<String, Object> profiles = (Map<String, Object>)obj;
				Object _active = profiles.get("active");
				if (_active != null){
					active = _active.toString();
				}
			}
		}
		if (active != null) {
			String filePath = path + File.separator + "config" + File.separator + "application-" + active + ".yml";
			yamlMapFactoryBean = new YamlMapFactoryBean();
			yamlMapFactoryBean.setResources(new FileSystemResource(filePath));
			alls = yamlMapFactoryBean.getObject();
		}
		Map<String, Object> configs = (Map<String, Object>)alls.get("system-config");
		values.putAll(configs);
		return true;
	}

	@SuppressWarnings("unchecked")
	public static <T> T getConfig(String key, T defaultValue) {
		Object value = values.get(key);
		if (value == null){
			//尝试获取系统属性
			value = System.getProperty(key);
		}
		if (value != null) {
			return (T) value;
		}
		return defaultValue;
	}

	public static String getLoginServerUrl(String path){
		String loginHost = getConfig("loginHost","127.0.0.1");
		int loginPort    = getConfig("loginPort", 6186);
		String head;
		if (loginPort == 443){
			head = "https://";
		}else{
			head = "http://";
		}
		String mid = (loginPort == 80 || loginPort == 443) ? "" : (":" + loginPort);
		String url = head + loginHost + mid + "/app";
		if (path.startsWith("/")){
			url += path;
		}else{
			url += "/" + path;
		}
		return url;
	}

	public static String getPayServerUrl(String path){
		String loginHost = getConfig("payHost","127.0.0.1");
		int loginPort    = getConfig("payPort", 8490);
		String head;
		if (loginPort == 443){
			head = "https://";
		}else{
			head = "http://";
		}
		String mid = (loginPort == 80 || loginPort == 443) ? "" : (":" + loginPort);
		String url = head + loginHost + mid + "/app";
		if (path.startsWith("/")){
			url += path;
		}else{
			url += "/" + path;
		}
		return url;
	}
	public static String getBackServerUrl(String path){
		String loginHost = getConfig("BackHost","127.0.0.1");
		int loginPort    = getConfig("BackPort", 6186);
		String head;
		if (loginPort == 443){
			head = "https://";
		}else{
			head = "http://";
		}
		String mid = (loginPort == 80 || loginPort == 443) ? "" : (":" + loginPort);
		String url = head + loginHost + mid + "/app";
		if (path.startsWith("/")){
			url += path;
		}else{
			url += "/" + path;
		}
		return url;
	}
}
