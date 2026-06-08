package back.modules.dataenum;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 公会职位枚举
 * @author gzc
 */
@Getter
@AllArgsConstructor
public enum GuildPositionEnum {

    NULL(StringUtils.EMPTY),

    PRESIDENT("会长"),

    VICE_PRESIDENT("副会长"),

    THE_ELDER("元老"),

    NORMAL("普通成员");

    private final String label;

    public static Map<Integer, String> getGuildPosition() {
        return Arrays.stream(values()).filter(e -> e != NULL).collect(Collectors.toMap(GuildPositionEnum::ordinal, GuildPositionEnum::getLabel));
    }
}
