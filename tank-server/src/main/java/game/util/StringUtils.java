package game.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @Author HYB
 * @Date 2025/11/20
 * @Time 15:58
 * @Desc
 */
public class StringUtils {
    // 只能是 6 位数字，不允许其他符号、字母、空格
    public static boolean isSixDigitNumber(String input) {
        if (input == null) return false;
        return input.matches("^\\d{6}$");
    }
    public static String timestampToString(long timestamp) {
        // 如果时间戳是秒级，需要 *1000 变成毫秒级
        Instant instant = Instant.ofEpochMilli(timestamp);
        ZonedDateTime dateTime = instant.atZone(ZoneId.systemDefault());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return dateTime.format(formatter);
    }
}
