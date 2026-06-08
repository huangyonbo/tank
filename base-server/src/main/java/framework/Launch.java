package framework;

import com.dtflys.forest.springboot.annotation.ForestScan;
import framework.BaseServer.State;
import framework.logic.*;
import framework.net.NetAdapter;
import framework.net.ProtocolCodecFactory;
import framework.net.Verify;
import framework.properties.ServerProperties;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 *
 * 描述： 启动器 创建人：胡中伟 创建时间：2018年3月12日 下午6:14:05
 *
 */
@MapperScan("framework.mybatis.mapper")
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, DataSourceAutoConfiguration.class})
@ForestScan(basePackages = "framework.net.http")
@EnableTransactionManagement
public class Launch {
    private static Logger logger;
    private static boolean isRunning = false;
    private static List<BaseServer> allSer = new ArrayList<>();
    private static ApplicationContext applicationContext;
    private static Map<String, Class<?>> springTypes = new HashMap<>();

    private static void initTypes() {
        springTypes.put("BackLogic", BackLogic.class);
        springTypes.put("DebugLogic", DebugLogic.class);
        springTypes.put("EntryLogic", EntryLogic.class);
        springTypes.put("GameLogic", GameLogic.class);
        springTypes.put("GameLogic2", GameLogic.class);
        springTypes.put("GateLogic", GateLogic.class);
        springTypes.put("GateLogic2", GateLogic.class);
        springTypes.put("GateLogic3", GateLogic.class);
        springTypes.put("GateLogic4", GateLogic.class);
        springTypes.put("HttpLogic", HttpLogic.class);
        springTypes.put("MasterLogic", MasterLogic.class);
        springTypes.put("MatchLogic", MatchLogic.class);
        springTypes.put("PublicLogic", PublicLogic.class);
        springTypes.put("StoreLogic", StoreLogic.class);
        springTypes.put("Launch", Launch.class);
    }

    private static List<String> getLocalHostLANAddress() throws Exception {
        List<String> localAddress = new ArrayList<>();
        //遍历所有的网络接口
        Enumeration<NetworkInterface> faces = NetworkInterface.getNetworkInterfaces();
        while (faces.hasMoreElements()) {
            NetworkInterface face = faces.nextElement();
            // 在所有的接口下再遍历IP
            Enumeration<InetAddress> inetAddress = face.getInetAddresses();
            while (inetAddress.hasMoreElements()) {
                InetAddress inetAddr = inetAddress.nextElement();
                if (!inetAddr.isLoopbackAddress() && inetAddr instanceof Inet4Address && !inetAddr.getHostAddress().contains(":")) {
                    localAddress.add(inetAddr.getHostAddress());
                }
            }
        }
        return localAddress;
    }

    public static void main(String[] args) throws Exception {
        initTypes();
        SpringApplication springApplication = new SpringApplication(springTypes.values().toArray(new Class<?>[0]));
        String basePath = System.getProperty("user.dir");
        logger = LoggerFactory.getLogger(Launch.class);
        logger.info("basePath: {}", basePath);
        String path = basePath + File.separator + "config" + File.separator + "application.yml";
        YamlMapFactoryBean yamlMapFactoryBean = new YamlMapFactoryBean();
        yamlMapFactoryBean.setResources(new FileSystemResource(path));
        if (!SystemConfigData.load(basePath, yamlMapFactoryBean)) {
            logger.error("启动失败");
            return;
        }
        springApplication.setDefaultProperties(yamlMapFactoryBean.getObject());
        applicationContext = springApplication.run(args);
        Launch launch = getBean(Launch.class);
        launch.run(args);
    }

    public static <T> T getBeanBySimpleName(String simpleName) {
        Class<T> type = (Class<T>) springTypes.get(simpleName);
        if (type != null) {
            return getBean(type);
        }
        return null;
    }

    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }

    public void run(String[] args) throws Exception {
        try {
            isRunning = true;
            logger.info("Launch. args length:{}, values:{}", args.length, args);
            Verify.init(logger);
            List<String> localIps = getLocalHostLANAddress();
            String localIp = localIps.get(0);
            NetAdapter netAdapter = new NetAdapter();
            NioSocketAcceptor ioAcceptor = new NioSocketAcceptor();
            ioAcceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ProtocolCodecFactory()));
            ioAcceptor.setHandler(netAdapter);
            IoSessionConfig sessionConfig = ioAcceptor.getSessionConfig();
            sessionConfig.setReadBufferSize(2048);
            sessionConfig.setIdleTime(IdleStatus.BOTH_IDLE, 20);
            sessionConfig.setWriteTimeout(3);
            ioAcceptor.setReuseAddress(true); //设置端口复用，防止重启时端口占用
            Timer _timer = new HashedWheelTimer(10, TimeUnit.MILLISECONDS);
            IoBuffer.setUseDirectBuffer(false);
            IoBuffer.setAllocator(new SimpleBufferAllocator());
            ServerSet baseSet = new ServerSet("");
            ServerProperties serverProperties = applicationContext.getBean(ServerProperties.class);
            List<Map<String, String>> configList = serverProperties.getConfigList();
            for (Map<String, String> configMap : configList) {
                baseSet.addServerConfig(configMap, localIp);
            }
            tryToRun(netAdapter, ioAcceptor, baseSet, _timer, localIps, args);
            while (isRunning) {
                int size = allSer.size();
                if (size > 0) {
                    boolean allShutDown = true;
                    for (int i = 0; i < size; i++) {
                        BaseServer ser = allSer.get(i);
                        if (ser.GetState() != State.END) {
                            allShutDown = false;
                            break;
                        }
                    }
                    if (allShutDown) {
                        break;
                    }
                }
                Thread.sleep(100);
            }
            _timer.stop();
            ioAcceptor.unbind();
            ioAcceptor.dispose();
            logger.info("Server exit.");
            System.exit(0);
        } catch (Throwable e) {
            logger.error("", e);
        }
    }

    private static void tryToRun(NetAdapter netAdapter, IoAcceptor ioAcceptor, ServerSet baseSet, Timer _timer, List<String> localIps, String[] args) throws Exception {
        if (isRunning) {
            List<String> servers = baseSet.getServers();
            for (int i = 0; i < servers.size(); ++i) {
                String ser = servers.get(i);
                ServerConfig cfg = baseSet.getServerConfig(ser);
                if (!localIps.contains(cfg.addr)) {
                    continue;
                }
                if (args.length > 0) {
                    int count = 0;
                    for (String _ser : args) {
                        if (org.apache.commons.lang.StringUtils.equals(_ser, ser)) {
                            count++;
                            break;
                        }
                    }
                    if (count == 0) {
                        continue;
                    }
                }
                Class<?> launchSer = Class.forName("framework." + cfg.ser);
                Constructor<?> cons = launchSer.getConstructor(String.class, ServerSet.class, IoAcceptor.class, NetAdapter.class);
                BaseServer server = (BaseServer) cons.newInstance(cfg.name, new ServerSet(cfg.name), ioAcceptor, netAdapter);
                server.setTimer(_timer, 10);
                allSer.add(server);
                baseSet.addServer(cfg.name, server);
            }
        }
    }

    @ConfigurationProperties("server")
    @Bean
    public ServerProperties serverProperties() {
        return new ServerProperties();
    }
}