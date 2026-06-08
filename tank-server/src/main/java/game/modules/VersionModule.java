package game.modules;

import back.modules.MailModule;
import com.google.protobuf.InvalidProtocolBufferException;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.game.*;
import framework.mybatis.domain.VersionReward;
import framework.mybatis.service.impl.VersionRewardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 版本模块
public class VersionModule implements ILogicModule {
	public class VersionData {
		public int id;
		public List<Integer> channles;
		public String version;
		public String appendix;
		public String startTime;
		public String stopTime;
		public long startDate;
		public long stopDate;
	}

	static Logger logger = LoggerFactory.getLogger(VersionModule.class);
	Map<String, Map<Integer, Integer>> m_mapCfgs = new HashMap<>();
	Map<Integer, VersionData> m_mapCfgsByID = new HashMap<>();

	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
		kernel.regEvent(KernelEvent.KEVENT_ON_LINE, "Player", this, "OnPlayerOnLine");

		kernel.regServerMsg(ServerMsgDef.B2G_REFRESH_VERREWARD.ordinal(), this, "OnRefreshVersion");
		kernel.regServerMsg(ServerMsgDef.B2G_CLOSE_VERREWARD.ordinal(), this, "OnCloseVersion");


		return true;
	}

	@Override
	public void onNetReady(IKernel kernel) {
		kernel.executeSomeToStore(VersionRewardService.class, "loadAll", null, (str) -> {
			List<VersionReward> rewards = framework.JsonUtil.decodeToList(str, VersionReward.class);
			for (int i = 0; i < rewards.size(); i++) {
				LoadCfg(kernel,rewards.get(i));
			}
		});
	}

	@Override
	public void onDestroy() {

	}

	public boolean LoadCfg(IKernel kernel,VersionReward cfg) {
		VersionData data = new VersionData();
		data.id = cfg.getId();
		data.version = cfg.getTarVer();
		data.channles = framework.MathUtils.SplitByFlag(cfg.getChannel(), ",", Integer.class);
		data.appendix = cfg.getAppendix().replace(',', ';').replace(':', '*');
		data.startTime = cfg.getStartTime();
		data.stopTime = cfg.getEndTime();
		try {
			DateFormat format = kernel.getServer().getTimeFormat();
			data.startDate = format.parse(data.startTime).getTime();
			data.stopDate  = format.parse(data.stopTime).getTime();
		} catch (ParseException e) {
			e.printStackTrace();
			return false;
		}
		// 先清除之前的配置
		RemoveCfg(data.id);
		m_mapCfgsByID.put(data.id, data);
		for (Integer channel : data.channles) {
			if (!m_mapCfgs.containsKey(data.version)) {
				Map<Integer, Integer> map = new HashMap<>();
				m_mapCfgs.put(data.version, map);
			}
			m_mapCfgs.get(data.version).put(channel, data.id);
		}
		return true;
	}

	void OnRefreshVersion(IKernel kernel, int serid, int msgid, byte[] msg) throws InvalidProtocolBufferException {
		ServerMsg.IntArray data = ServerMsg.IntArray.parseFrom(msg);
		int count = data.getIdCount();
		List<Object> params = new ArrayList<>();
		params.add(data.getIdList());
		kernel.executeSomeToStore(VersionRewardService.class, "query", params, (str) -> {
			List<VersionReward> rewards = framework.JsonUtil.decodeToList(str, VersionReward.class);
			for (int i = 0; i < rewards.size(); i++) {
				LoadCfg(kernel,rewards.get(i));
			}
		});
	}

	void OnCloseVersion(IKernel kernel, int serid, int msgid, byte[] msg) throws InvalidProtocolBufferException {
		ServerMsg.IntArray data = ServerMsg.IntArray.parseFrom(msg);
		int count = data.getIdCount();
		for (int i = 0; i < count; ++i) {
			int id = data.getId(i);
			RemoveCfg(id);
		}
	}

	void RemoveCfg(int id) {
		if (!m_mapCfgsByID.containsKey(id)) {
			return;
		}
		VersionData cfg = m_mapCfgsByID.get(id);
		for (int channel : cfg.channles) {
			if (!m_mapCfgs.containsKey(cfg.version)) {
				continue;
			}

			m_mapCfgs.get(cfg.version).remove(channel, cfg.id);
		}
		m_mapCfgsByID.remove(id);
	}

	public void OnPlayerClassCreate(IKernel kernel, String script) {
		kernel.declareProperty(script, PLAYER_PROPERTY_LASTVERSION, ValueType.STRING, false, false, true);
	}

	void OnPlayerOnLine(IKernel kernel, IGameObject player) {
		String nowVer = player.getString(PLAYER_PROPERTY_VERSION);
		if (player.getBool(PLAYER_PROPERTY_ISNEW)) {
			player.setProperty(PLAYER_PROPERTY_LASTVERSION, nowVer);
			return;
		}

		String lastVer = player.getString(PLAYER_PROPERTY_LASTVERSION);
		int res = CompareVersion(nowVer, lastVer);
		if (res > 0) {
			// level up
			player.setProperty(PLAYER_PROPERTY_LASTVERSION, nowVer);
			VersionUpTo(kernel, player, nowVer);
		}
	}

	// 0: 相等
	// -: left < right
	// +: right < left
	public static int CompareVersion(String left, String right) {
		String[] l = left.split("\\.");
		String[] r = right.split("\\.");
		for (int i = 0; i < 3; ++i) {
			int lv = 0;
			int rv = 0;
			if (l.length >= i + 1 && !l[i].isEmpty()) {
				lv = Integer.parseInt(l[i]);
			}
			if (r.length >= i + 1 && !r[i].isEmpty()) {
				rv = Integer.parseInt(r[i]);
			}
			if (lv != rv) {
				return lv - rv;
			}
		}
		return 0;
	}

	void VersionUpTo(IKernel kernel, IGameObject player, String target) {
		if (!m_mapCfgs.containsKey(target)) {
			return;
		}

		int channel = player.getInt(PLAYER_PROPERTY_CHANNEL);
		int id = -1;
		if (m_mapCfgs.get(target).containsKey(-1)) {
			id = m_mapCfgs.get(target).get(-1);
		} else if (m_mapCfgs.get(target).containsKey(channel)) {
			id = m_mapCfgs.get(target).get(channel);
		} else {
			return;
		}

		if (!m_mapCfgsByID.containsKey(id)) {
			return;
		}

		VersionData data = m_mapCfgsByID.get(id);
		long now = kernel.getServerTime();
		if (now < data.startDate || now > data.stopDate) {
			return;
		}

		String appendix = data.appendix;

	}
}
