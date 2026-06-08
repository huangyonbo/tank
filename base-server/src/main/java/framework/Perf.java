package framework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

class Statistics {
	long count;
	long used;
	long start;
}

public class Perf {
	private static Logger logger = LoggerFactory.getLogger(Perf.class);
	private boolean m_statiSwitch = false;
	private Map<String, Statistics> m_mapStatistics = new HashMap<>();

	static Perf instance = null;
	static long startTime = 0l;

	public static Perf GetInstane() {
		if (instance == null) {
			instance = new Perf();
			startTime = System.currentTimeMillis();
		}
		return instance;
	}

	public void staticSwitch(boolean on) {
		m_statiSwitch = on;
		logger.warn("StaticSwitch {}", on);
	}

	public void startPerf(Object... func) {
		if (!m_statiSwitch) {
			return;
		}
		StringBuilder build = new StringBuilder();
		for (Object o : func) {
			build.append(o.toString());
		}
		String name = build.toString();

		if (!m_mapStatistics.containsKey(name)) {
			m_mapStatistics.put(name, new Statistics());
		}
		m_mapStatistics.get(name).start = System.currentTimeMillis();
	}

	public void overPerf(Object... func) {
		if (!m_statiSwitch) {
			return;
		}
		StringBuilder build = new StringBuilder();
		for (Object o : func) {
			build.append(o.toString());
		}
		String name = build.toString();

		if (!m_mapStatistics.containsKey(name)) {
			return;
		}
		m_mapStatistics.get(name).used += System.currentTimeMillis() - m_mapStatistics.get(name).start;
		++m_mapStatistics.get(name).count;
	}

	public String dumpPerf() {
		if (!m_statiSwitch) {
			return "";
		}

		StringBuilder build = new StringBuilder();
		build.append("Runtime ").append(System.currentTimeMillis() - startTime).append("\n");

		for (Entry<String, Statistics> entry : m_mapStatistics.entrySet()) {
			if (entry.getValue().count <= 0) {
				continue;
			}

			build.append(entry.getKey()).append(" ").append(entry.getValue().used).append(" ")
					.append(entry.getValue().count).append("\n");
		}
		return build.toString();
	}
}
