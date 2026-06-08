/**   
*    
* 描述：  时间管理
* 文件：TimerManager.java
* 创建人：胡中伟
* 创建时间：2018年4月25日 下午2:14:07 
*    
*/
package game.modules;

import com.esotericsoftware.reflectasm.MethodAccess;
import framework.MethodAccessCache;
import framework.MethodCallBackData;
import framework.game.*;
import game.custommsg.CommandDef;
import game.modules.utils.UtilFunc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;
import java.util.Map.Entry;

/**
 * 
 * 描述： 一段时间内，每周几的某个时间点触发
 * 
 */
public class TimerManager implements ILogicModule {
	class Timer {
		long start;
		long end;
		int time;
		boolean week[] = new boolean[7];

		long started = 0l;
		long lastCheck = 0l;
		long checkWeek = 0l;

		ILogicModule module;
		MethodAccess access;
		int startMethod;
		int endMethod;
		int checkMethod;
		int checkWeekMethod;
	}
	
	private static Logger logger = LoggerFactory.getLogger(TimerManager.class);
	private long m_lastTime = 0l;
	private Map<String, Timer> m_mapTimers = new HashMap<>();
	private Map<Integer, List<MethodCallBackData>> m_changeMin = new HashMap<>();
	private Map<Integer,Map<Integer, List<MethodCallBackData>>> m_perMin = new HashMap<>();
	private List<MethodCallBackData> m_changeHour = new ArrayList<>();
	private List<MethodCallBackData> m_changeDay = new ArrayList<>();
	private List<MethodCallBackData> m_changeWeek = new ArrayList<>();
	private List<MethodCallBackData> m_changeMonth = new ArrayList<>();
	private List<MethodCallBackData> m_changeYear = new ArrayList<>();

	private boolean m_first = true;

	/**
	 * @param kernel
	 * @return
	 */
	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");
		RefreshCfg(kernel, "res/TimerManager/TimerManager.xml");
		kernel.declareHeartBeat("HB_CheckTimeMgr", this, "OnCheckTimer");
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE, "World", this, "OnWorldCreate");
		return true;
	}
	
	@Override
	public void onNetReady(IKernel kernel) {
		OnCheckTimer(kernel,null);
	}

	/**
	 * 
	 */
	@Override
	public void onDestroy() {
		
	}

	void RefreshCfg(IKernel kernel, String path) {
		if (path.equals("res/TimerManager/TimerManager.xml")) {
			m_mapTimers.clear();
			LoadConfig(kernel, path);
		}
	}

	private boolean LoadConfig(IKernel kernel, String path) {
		ICfgReader config = kernel.loadXmlConfig(path);
		if (config == null) {
			return false;
		}
		String curSer = kernel.getSerName();
		int count = config.getItemCount();
		for (int i = 0; i < count; ++i) {
			String ser = config.getString(i, "Server");
			if (ser != null && !ser.isEmpty() && !curSer.equals(ser)) {
				continue;
			}
			try {
				Timer timer = addTimer(kernel, config.getString(i, "StartDate"), config.getString(i, "EndDate"),
						config.getString(i, "Weeks"), config.getString(i, "Time"), config.getString(i, "Module"),
						config.getString(i, "StartMethod"), config.getString(i, "EndMethod"),
						config.getString(i, "CheckMethod"), config.getString(i, "CheckWeek"));
				if (timer != null) {
					m_mapTimers.put(config.getString(i, "Id"), timer);
				}
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	// 一段时间内，每周几的某个时间点触发
	// 开始时间 - 结束时间 星期开关 触发时间点
	// 年-月-日 时:分:秒 - 年-月-日 时:分:秒 日一二三四五六 时:分:秒
	// 2018-01-01 00:00:00 - 2019-01-01 23:59:59 1000001 22:00:00
	private Timer addTimer(IKernel kernel, String start, String end, String weeks, String time, String module,
						   String startMethod, String endMethod, String checkMethod, String checkWeek) throws ParseException {
		long now = kernel.getServerTime();
		DateFormat format = kernel.getServer().getTimeFormat();
		long startTime = format.parse(start).getTime();
		long endTime   = format.parse(end).getTime();
		if (startTime >= endTime || endTime <= now) {
			return null;
		}
		Timer t  = new Timer();
		t.start  = startTime;
		t.end    = endTime;
		t.time   = UtilFunc.timeParse(time);
		t.module = kernel.getModule(module);
		if (t.module == null) {
			logger.error("module {} not found.", module);
			return null;
		}
		t.access = MethodAccessCache.tryToGet(t.module.getClass());
		t.startMethod = startMethod.isEmpty() ? -1 : t.access.getIndex(startMethod, IKernel.class, String.class);
		t.endMethod = endMethod.isEmpty() ? -1 : t.access.getIndex(endMethod, IKernel.class, String.class);
		t.checkMethod = checkMethod.isEmpty() ? -1 : t.access.getIndex(checkMethod, IKernel.class, String.class);
		t.checkWeekMethod = checkWeek.isEmpty() ? -1 : t.access.getIndex(checkWeek, IKernel.class, String.class);
		char[] weekSwitch = weeks.toCharArray();
		for (int i = 0; i < 7; ++i) {
			t.week[i] = weekSwitch.length > i ? weekSwitch[i] == '1' : false;
		}
		logger.info("AddTimer {} - {} {} {} {} {} {} {} {}", start, end, weeks, time, module, startMethod, endMethod,checkMethod, checkWeek);
		return t;
	}

	public void OnWorldCreate(IKernel kernel, IGameObject world) {
		kernel.addHeartBeat("HB_CheckTimeMgr", world, 1000, -1);
	}

	// 添加分钟回调（每个小时的min分钟触发）
	public int addChangeMinCallBack(int min, Object listener, String method) {
		if (min < 0 || min >= 60) {
			return -1;
		}

		MethodCallBackData cb = new MethodCallBackData();
		cb.listener = listener;
		cb.access = MethodAccessCache.tryToGet(listener.getClass());
		cb.methodIndex = cb.access.getIndex(method, IKernel.class, int.class);

		List<MethodCallBackData> cbs = m_changeMin.get(min);
		if (cbs == null) {
			cbs = new LinkedList<>();
			m_changeMin.put(min,cbs);
		}
		cbs.add(cb);
		return cbs.size() -1;
	}
	
	/**
	 * 添加每隔几分钟回调(每min分钟调一次)
	 * @param offset 距下次回调还剩几分钟
	 * @param reset 每几分钟回调一次
	 * @param listener
	 * @param method
	 * @return
	 */
	public int addPerMinCallBack(int offset, int reset, Object listener, String method) {
		if (offset <= 0 || offset >= 60 || reset <= 0 || reset >= 60) {
			return -1;
		}
		MethodCallBackData cb = new MethodCallBackData();
		cb.listener = listener;
		cb.access = MethodAccessCache.tryToGet(listener.getClass());
		cb.methodIndex = cb.access.getIndex(method, IKernel.class, int.class);
		Map<Integer, List<MethodCallBackData>> tmp = m_perMin.get(reset);
		if (tmp == null){
			tmp = new HashMap<>();
			m_perMin.put(reset,tmp);
		}
		List<MethodCallBackData> list = tmp.get(offset);
		if (list == null){
			list = new LinkedList<>();
			tmp.put(offset,list);
		}
		list.add(cb);
		return tmp.size() == 1 ? 0 : 1;
	}

	public void removeChangeMinCallBack(int min, int id) {
		if (min < 0 || min >= 60) {
			return;
		}
		if (m_changeMin.containsKey(min)) {
			if (id < 0 || id >= m_changeMin.get(min).size()) {
				return;
			}

			m_changeMin.get(min).set(id, null);
		}
	}

	// 添加小时回调
	public int addChangeHourCallBack(Object listener, String method) {
		MethodCallBackData cb = new MethodCallBackData();
		cb.listener = listener;
		cb.access = MethodAccessCache.tryToGet(listener.getClass());
		cb.methodIndex = cb.access.getIndex(method, IKernel.class, int.class);
		m_changeHour.add(cb);
		return m_changeHour.size() - 1;
	}

	public void removeChangeHourCallBack(int id) {
		if (id < 0 || id >= m_changeHour.size()) {
			return;
		}
		m_changeHour.set(id, null);
	}

	// 添加跨天回调（每日00:00:00）
	public int addChangeDayCallBack(Object listener, String method) {
		MethodCallBackData cb = new MethodCallBackData();
		cb.listener = listener;
		cb.access = MethodAccessCache.tryToGet(listener.getClass());
		cb.methodIndex = cb.access.getIndex(method, IKernel.class, int.class);

		m_changeDay.add(cb);
		return m_changeDay.size() - 1;
	}

	public void removeChangeDayCallBack(int id) {
		if (id < 0 || id >= m_changeDay.size()) {
			return;
		}
		m_changeDay.set(id, null);
	}

	// 添加跨周回调（周日00:00:00）
	public int addChangeWeekCallBack(Object listener, String method) {
		MethodCallBackData cb = new MethodCallBackData();
		cb.listener = listener;
		cb.access = MethodAccessCache.tryToGet(listener.getClass());
		cb.methodIndex = cb.access.getIndex(method, IKernel.class, int.class);

		m_changeWeek.add(cb);
		return m_changeWeek.size() - 1;
	}

	public void removeChangeWeekCallBack(int id) {
		if (id < 0 || id >= m_changeWeek.size()) {
			return;
		}
		m_changeWeek.set(id, null);
	}

	// 添加跨月回调（周月1日00:00:00）
	public int addChangMonthCallBack(Object listener, String method) {
		MethodCallBackData cb = new MethodCallBackData();
		cb.listener = listener;
		cb.access = MethodAccessCache.tryToGet(listener.getClass());
		cb.methodIndex = cb.access.getIndex(method, IKernel.class, int.class);

		m_changeMonth.add(cb);
		return m_changeMonth.size() - 1;
	}

	public void removeChangeMonthCallBack(int id) {
		if (id < 0 || id >= m_changeMonth.size()) {
			return;
		}
		m_changeMonth.set(id, null);
	}

	// 添加跨年回调（周年1月1日00:00:00）
	public int addChangeYearCallBack(Object listener, String method) {
		MethodCallBackData cb = new MethodCallBackData();
		cb.listener = listener;
		cb.access = MethodAccessCache.tryToGet(listener.getClass());
		cb.methodIndex = cb.access.getIndex(method, IKernel.class, int.class);

		m_changeYear.add(cb);
		return m_changeYear.size() - 1;
	}

	public void removeChangeYearCallBack(int id) {
		if (id < 0 || id >= m_changeYear.size()) {
			return;
		}
		m_changeYear.set(id, null);
	}

	private void changeMin(IKernel kernel, int min) {
		for (Integer reset : m_perMin.keySet()) {
			Map<Integer, List<MethodCallBackData>> newTmp = new HashMap<>();
			Map<Integer, List<MethodCallBackData>> tmp = m_perMin.get(reset);
			for (Integer a : tmp.keySet()){
				List<MethodCallBackData> list = tmp.get(a);
				if (a - 1 == 0) {
					for (MethodCallBackData cb : list) {
						if (cb == null) {
							continue;
						}
						cb.access.invoke(cb.listener, cb.methodIndex, kernel, min);
					}
					newTmp.put(reset, list);
				} else {
					newTmp.put(a-1, list);
				}
			}
			m_perMin.put(reset, newTmp);
		}
		List<MethodCallBackData> cbs = m_changeMin.get(min);
		if (cbs == null) {
			return;
		}
		for (MethodCallBackData cb : cbs) {
			if (cb == null) {
				continue;
			}
			cb.access.invoke(cb.listener, cb.methodIndex, kernel, min);
		}
	}

	private void changeHour(IKernel kernel, int hour) {
		//logger.info("ChangeHour");
		for (MethodCallBackData cb : m_changeHour) {
			if (cb == null) {
				continue;
			}
			cb.access.invoke(cb.listener, cb.methodIndex, kernel, hour);
		}
	}

	private void changeDay(IKernel kernel, int day) {
		//logger.info("ChangeDay");
		for (MethodCallBackData cb : m_changeDay) {
			if (cb == null) {
				continue;
			}
			cb.access.invoke(cb.listener, cb.methodIndex, kernel, day);
		}
		kernel.commandAllPlayer(CommandDef.CMD_CHANGE_DAY.ordinal());
	}

	private void changeWeek(IKernel kernel, int week) {
		//logger.info("ChangeWeek");
		for (MethodCallBackData cb : m_changeWeek) {
			if (cb == null) {
				continue;
			}
			cb.access.invoke(cb.listener, cb.methodIndex, kernel, week);
		}
		kernel.commandAllPlayer(CommandDef.CMD_CHANGE_WEEK.ordinal());
	}

	private void changeMonth(IKernel kernel, int month) {
		//logger.info("ChangeMonth");
		for (MethodCallBackData cb : m_changeMonth) {
			if (cb == null) {
				continue;
			}
			cb.access.invoke(cb.listener, cb.methodIndex, kernel, month);
		}
	}

	private void changeYear(IKernel kernel, int year) {
		//logger.info("ChangeYear");
		for (MethodCallBackData cb : m_changeYear) {
			if (cb == null) {
				continue;
			}
			cb.access.invoke(cb.listener, cb.methodIndex, kernel, year);
		}
	}

	// int count = 0;

	public void OnCheckTimer(IKernel kernel, IGameObject world) {
		long now = kernel.getServerTime();
		if (m_lastTime == 0l) {
			m_lastTime = now;
			return;
		}
		Calendar nowCal = Calendar.getInstance();
		nowCal.setTime(new Date(now));
		Calendar lastCal = Calendar.getInstance();
		lastCal.setTime(new Date(m_lastTime));
		// 将精度控制到分钟（N分0秒）
		if (!m_first && nowCal.get(Calendar.MINUTE) == lastCal.get(Calendar.MINUTE)) {
			return;
		}

		changeMin(kernel, nowCal.get(Calendar.MINUTE));

		m_first = false;

		if (nowCal.get(Calendar.HOUR_OF_DAY) != lastCal.get(Calendar.HOUR_OF_DAY)) {
			changeHour(kernel, nowCal.get(Calendar.HOUR_OF_DAY));
		}

		if (nowCal.get(Calendar.DAY_OF_MONTH) != lastCal.get(Calendar.DAY_OF_MONTH)) {
			changeDay(kernel, nowCal.get(Calendar.DAY_OF_MONTH));
			if (nowCal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) // 周1
			{
				changeWeek(kernel, nowCal.get(Calendar.DAY_OF_WEEK));
				if (nowCal.get(Calendar.DAY_OF_MONTH) == 1) // 1号
				{
					changeMonth(kernel, nowCal.get(Calendar.MONTH));
					if (nowCal.get(Calendar.MONTH) == Calendar.JANUARY) // 1月
					{
						changeYear(kernel, nowCal.get(Calendar.YEAR));
					}
				}
			}
		}
		int week = UtilFunc.getDayOfWeek(now);
		for (Entry<String, Timer> entry : m_mapTimers.entrySet()) {
			String timerId = entry.getKey();
			Timer timer = entry.getValue();
			if (timer.start > now) {
				continue;
			}
			if (timer.end <= now) {
				if (timer.started != 0l) {
					timer.started = 0l;
					logger.info("Timer {} stoped", entry.getKey());
					if (timer.endMethod != -1) {
						timer.access.invoke(timer.module, timer.endMethod, kernel, timerId);
					}
				}
				continue;
			}
			if (timer.started == 0l) {
				timer.started = now;
				logger.info("Timer {} start", entry.getKey());
				if (timer.startMethod != -1) {
					timer.access.invoke(timer.module, timer.startMethod, kernel, timerId);
				}
			}
			if (!timer.week[week]) {
				continue;
			}
			if (!UtilFunc.isSameDay(now, timer.checkWeek)) {
				timer.checkWeek = now;
				logger.warn("Timer {} check week", entry.getKey());
				if (timer.checkWeekMethod != -1) {
					timer.access.invoke(timer.module, timer.checkWeekMethod, kernel, timerId);
				}
			}
			if (UtilFunc.isSameDay(now, timer.lastCheck)) {
				continue;
			}
			long checkPoint = UtilFunc.getZeroTime(now) + timer.time;
			if (timer.started <= checkPoint && now >= checkPoint) {
				timer.lastCheck = now;
				logger.info("Timer {} check", entry.getKey());
				if (timer.checkMethod != -1) {
					timer.access.invoke(timer.module, timer.checkMethod, kernel, timerId);
				}
			}
		}
		m_lastTime = now;
	}
}
