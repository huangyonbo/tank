package back.modules.data.runstats;

import back.modules.data.Pager;

/**
 * 今日游戏运行统计数据
 * Created by Administrator on 2018/8/16.
 */
public class RunStats extends Pager<RunStatsData> {
    public RunStats() {
    	
    }

    @Override
    public String toString() {
        return "RunStats{}" +
                " " + super.toString();
    }
}
