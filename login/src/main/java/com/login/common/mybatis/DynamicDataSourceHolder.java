package com.login.common.mybatis;

public class DynamicDataSourceHolder {
    private static final ThreadLocal<String> holder = new ThreadLocal<>();

    public static void putDataSource(DataSourceType dataSourceType) {
        holder.set(dataSourceType.getType());
    }

    public static String getDataSouce() {
        return holder.get();
    }

    public static void remove() {
        holder.remove();
    }
}
