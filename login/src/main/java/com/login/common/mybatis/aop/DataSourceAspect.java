package com.login.common.mybatis.aop;

import com.login.common.mybatis.DynamicDataSourceHolder;
import com.login.common.mybatis.ano.OnSqlDo;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
@Aspect
@Component
@Order(0)
@Slf4j
public class DataSourceAspect {

    @Pointcut("@annotation(com.login.common.mybatis.ano.OnSqlDo)")
    public void annoPoint(){

    }

    @Before("annoPoint()")
    public void befroeAnnoSetDsType(JoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();//获取参数名
        OnSqlDo onSqlDo = methodSignature.getMethod().getAnnotation(OnSqlDo.class);
        DynamicDataSourceHolder.putDataSource(onSqlDo.value());
        log.debug("使用注解数据源，dataSource 切换到：{}",onSqlDo.value().getName());
    }

    @After("annoPoint()")
    public void afterAnnoSetDsType(JoinPoint point){
        DynamicDataSourceHolder.remove();
    }
}
