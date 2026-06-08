package game.util;

import framework.game.IKernel;
import hirondelle.date4j.DateTime;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class TimeUtils {

    private static final Logger logger = LoggerFactory.getLogger(TimeUtils.class);
    private static final TimeZone tz = TimeZone.getTimeZone("GMT+8");// 设置时区为北京时间
    private static final DateFormat H_M_FORMAT = new SimpleDateFormat("HH:mm");
    public static String YYYY = "yyyy";

    public static String YYYY_MM = "yyyy-MM";

    public static String YYYY_MM_DD = "yyyy-MM-dd";

    public static String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";

    public static String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    /**
     * 判断是否是同一天
     *
     * @param time1
     * @param time2
     * @return
     */
    public static boolean isSameDay(long time1, long time2) {
        DateTime startTime = getTime(time1);
        DateTime endTime = getTime(time2);
        return startTime.isSameDayAs(endTime);
    }

    public static boolean isSameDay(long time) {
        DateTime startTime = now();
        DateTime endTime = getTime(time);
        return startTime.isSameDayAs(endTime);
    }

    /**
     * 判断是否同一周
     *
     * @param time1
     * @param time2
     * @return
     */
    public static boolean isSameWeek(long time1, long time2) {
        if (time1 == 0 || time2 == 0) {
            return false;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time1);

        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        long begin = calendar.getTimeInMillis();

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
        long end = calendar.getTimeInMillis();

        if (time2 >= begin && time2 <= end + 24 * 60 * 60 * 1000) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 获得时间
     *
     * @param time
     * @return
     */
    public static DateTime getTime(long time) {
        DateTime dt = DateTime.forInstant(time, tz);
        return dt;
    }

    public static DateTime getTime(String str) {
        return new DateTime(str);
    }

    /**
     * 获得当前时间
     *
     * @return
     */
    public static DateTime now() {
        return DateTime.now(tz);// 获得当前时间
    }

    /**
     * 返回两个日期之间的全部天数
     *
     * @param start
     * @param end
     * @return
     */
    public static int getDays(String start, String end) {
        DateTime startDate = getTime(start);
        DateTime endDate = getTime(end);
        // 返回两个日期之间的时间差,�?012-11-1 2012-11-20 差�?=19
        int days = startDate.numDaysFrom(endDate);
        return days;
    }

    /**
     * 将字符串时间转化为long
     *
     * @param time
     * @return
     */
    public static long getTimes(String time) {
        if (null == time || "".equals(time)) {
            return 0;
        }
        return getTime(time).getMilliseconds(tz);
    }

    /**
     * 获取当前时间，格式：HH:mm 时间24小时制
     *
     * @return
     */
    public static String getHHmm(long time) {
        return H_M_FORMAT.format(time);
    }

    /**
     * 返回两个时间之间天数间隔
     *
     * @param start
     * @param end
     * @return
     */
    public static int getDays(long start, long end) {
        DateTime startDate = getTime(start);
        DateTime endDate = getTime(end);
        return startDate.numDaysFrom(endDate);
    }

    public static int getDays(long start) {
        DateTime startDate = getTime(start);
        DateTime endDate = now();
        return startDate.numDaysFrom(endDate);
    }


    public static String GetCurrentDay(IKernel kernel, long currentTime) {
        return kernel.getServer().getDayFormat().format(currentTime);
    }

    public static long GetDayStartTime(IKernel kernel, String strDay) {
        String format = strDay + " 00:00:00";
        try {
            return kernel.getServer().getTimeFormat().parse(format).getTime();
        } catch (Exception e) {
            logger.error("", e);
        }
        return 0;
    }

    /**
     * 是否周末
     *
     * @param time
     * @return
     */
    public static boolean IsWeekend(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        return day == Calendar.SATURDAY || day == Calendar.SUNDAY;
    }

    public static String yesterDay(DateFormat format) {
        long now = System.currentTimeMillis();
        now -= 24 * 3600 * 1000;
        return format.format(now);
    }

    public static String formatDate(Date date, String format) {
        /**
         * 日期路径 即年/月/日 如20180808
         */

        Date now = new Date();
        return DateFormatUtils.format(date == null ? now : date, StringUtils.isEmpty(format)?YYYY_MM_DD_HH_MM_SS:format);
    }
    public static long getWeekZeroTime(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(time));
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime().getTime();
    }
    public static String GetCurrentWeekMonDay(IKernel kernel, long serverTime) {
        return kernel.getServer().getDayFormat().format(getWeekZeroTime(serverTime));
    }
}
