package framework;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Spring Context 工具类
 *
 */
@Component
@SuppressWarnings("all")
public class SpringContextUtil implements ApplicationContextAware {

	private static ApplicationContext applicationContext = null;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		SpringContextUtil.applicationContext = applicationContext;
	}

	public static <T> T getBean(String name) {
		assertContextInjected();
		return (T) applicationContext.getBean(name);
	}

	public static <T> T getBean(Class<T> requiredType) {
		assertContextInjected();
		return applicationContext.getBean(requiredType);
	}

	public static boolean containsBean(String name) {
		assertContextInjected();
		return applicationContext.containsBean(name);
	}

	public static boolean isSingleton(String name) {
		assertContextInjected();
		return applicationContext.isSingleton(name);
	}

	public static Class<? extends Object> getType(String name) {
		assertContextInjected();
		return applicationContext.getType(name);
	}

	private static void assertContextInjected() {
		if (applicationContext == null) {
			throw new IllegalStateException("applicationContext属性未注入");
		}
	}

	/**
	 * 获取当前的环境配置，无配置返回null
	 *
	 * @return 当前的环境配置
	 */
	public static String[] getActiveProfiles() {
		assertContextInjected();
		return applicationContext.getEnvironment().getActiveProfiles();
	}

	/**
	 * 获取当前的环境配置，当有多个环境配置时，只获取第一个
	 *
	 * @return 当前的环境配置
	 */
	public static String getActiveProfile() {
		final String[] activeProfiles = getActiveProfiles();
		return CollectionUtils.isEmpty(Arrays.asList(activeProfiles)) ? activeProfiles[0] : null;
	}

	public static Boolean isDev() {
		return ("dev").equals(getActiveProfile());
	}

	public static Boolean isTest() {
		return ("test").equals(getActiveProfile());
	}

	public static Boolean isProd() {
		return ("prod").equals(getActiveProfile());
	}

	public static String getIp() {
		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			throw new RuntimeException("获取主机ip失败");
		}
	}
}