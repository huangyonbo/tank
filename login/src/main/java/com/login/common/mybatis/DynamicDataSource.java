package com.login.common.mybatis;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class DynamicDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        Object object = DynamicDataSourceHolder.getDataSouce();
        if (object != null) {
            return object;
        }
        return DataSourceType.LOGIN.getType();
    }

}
