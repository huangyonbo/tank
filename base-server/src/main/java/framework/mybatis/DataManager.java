package framework.mybatis;

import framework.mybatis.service.AbstractService;

public interface DataManager {
    <T extends AbstractService<?>> T getService(String name);
    <T extends AbstractService<?>> T getService(Class<T> type);
}
