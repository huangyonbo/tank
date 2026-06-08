package com.login.common.mybatis;

import com.alibaba.druid.support.http.StatViewServlet;
import com.alibaba.druid.support.http.WebStatFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;


@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.type}")
    private Class<? extends DataSource> dataSourceType;

    @Bean(name = "loginDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.login")
    public DataSource loginDataSource() {
        return DataSourceBuilder.create().type(dataSourceType).build();
    }


    @Bean(name = "bmsDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.bms")
    public DataSource bmsDataSource() {
        return DataSourceBuilder.create().type(dataSourceType).build();
    }

    @Bean(name = "gameDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.game")
    public DataSource gameDataSource() {
        return DataSourceBuilder.create().type(dataSourceType).build();
    }

    //动态数据源
    @Bean(name = "dynamicDataSource")
    //解决互相依赖关系
    @DependsOn({"loginDataSource","bmsDataSource","gameDataSource"})
    @Primary
    public DataSource getDataSource(@Qualifier("loginDataSource")DataSource loginDataSource,@Qualifier("bmsDataSource")DataSource bmsDataSource,@Qualifier("gameDataSource")DataSource gameDataSource) {
        DynamicDataSource dataSource = new DynamicDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceType.LOGIN.getType(),loginDataSource);
        targetDataSources.put(DataSourceType.BMS.getType(),bmsDataSource);
        targetDataSources.put(DataSourceType.GAME.getType(),gameDataSource);
        dataSource.setTargetDataSources(targetDataSources);
        return dataSource;
    }

    /**
     * 配置监控服务器
     * @return 返回监控注册的servlet对象
     */
    @Bean
    public ServletRegistrationBean statViewServlet() {
        ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean(new StatViewServlet(), "/druid/*");
        //添加IP白名单
        //servletRegistrationBean.addInitParameter("allow", "192.168.25.125,127.0.0.1");
        //添加IP黑名单，当白名单和黑名单重复时，黑名单优先级更高
        //servletRegistrationBean.addInitParameter("deny", "192.168.25.123");
        // 添加控制台管理用户
        servletRegistrationBean.addInitParameter("loginUsername", "druid");
        servletRegistrationBean.addInitParameter("loginPassword", "123456");
        // 是否能够重置数据
        servletRegistrationBean.addInitParameter("resetEnable", "false");
        return servletRegistrationBean;
    }

    /**
     * 配置服务过滤器
     * @return 返回过滤器配置对象
     */
    @Bean
    public FilterRegistrationBean statFilter() {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(new WebStatFilter());
        //添加过滤规则
        filterRegistrationBean.addUrlPatterns("/*");
        //忽略过滤格式
        filterRegistrationBean.addInitParameter("exclusions","*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*,");
        return filterRegistrationBean;
    }
}
