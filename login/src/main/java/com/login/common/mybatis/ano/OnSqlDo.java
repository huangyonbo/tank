package com.login.common.mybatis.ano;

import com.login.common.mybatis.DataSourceType;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
@Inherited
public @interface OnSqlDo {
    DataSourceType value() default DataSourceType.LOGIN;
}
