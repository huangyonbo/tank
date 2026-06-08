package framework.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServerProperties {

    private final List<Map<String, String>> configList = new ArrayList<>();

    public List<Map<String, String>> getConfigList() {
        return configList;
    }
}
