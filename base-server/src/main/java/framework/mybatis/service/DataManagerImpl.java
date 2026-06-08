package framework.mybatis.service;

import framework.mybatis.DataManager;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@SuppressWarnings("all")
public class DataManagerImpl implements DataManager {

    @Autowired
    private Map<String, AbstractService<?>> serviceMap = new HashMap<>();

    @Override
    public <T extends AbstractService<?>> T getService(String name) {
        return (T) serviceMap.get(name);
    }

    @Override
    public <T extends AbstractService<?>> T getService(Class<T> type) {
        return (T) serviceMap.get(StringUtils.uncapitalize(type.getSimpleName()));
    }
}
