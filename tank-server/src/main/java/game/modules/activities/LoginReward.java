package game.modules.activities;

import back.modules.MailModule;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import framework.game.*;
import game.modules.activities.ActivityMgr.ActivityData;

public class LoginReward extends BaseActivity {
	MailModule m_MailModule = null;

	public LoginReward(int type, ActivityMgr mgr) {
		super(type, mgr);
	}

	public boolean ParseCfg(ActivityData cfg) {
		JsonParser parse = new JsonParser();
		JsonObject json = (JsonObject) parse.parse(cfg.param);
		JsonArray array = json.get("reward").getAsJsonArray();

		String appendix = "";
		for (int j = 0; j < array.size(); ++j) {
			JsonObject item = array.get(j).getAsJsonObject();
			if (!appendix.isEmpty()) {
				appendix += ";";
			}
			appendix += item.get("type").getAsString() + "*" + item.get("count").getAsInt();
		}

		cfg.data = appendix;
		return true;
	}

	@Override
	public boolean OnInit(IKernel kernel) {
		m_MailModule = (MailModule) kernel.getModule("MailModule");
		return true;
	}

	public void OnPlayerClassCreate(IKernel kernel, String script) {
		kernel.declareProperty(script, PLAYER_PROPERTY_LOGINDATE, ValueType.LONG, false, false, true);
		kernel.declareProperty(script, PLAYER_PROPERTY_LOGINVERSION, ValueType.LONG, false, false, true);
	}

	@Override
	public void OnWorldCreate(IKernel kernel, IGameObject world) {

	}

	@Override
	public void OnPlayerOnLine(IKernel kernel, IGameObject player) {
		OnCheckVersion(kernel, player, null);
	}

	@Override
	public void OnPlayerOffLine(IKernel kernel, IGameObject player) {

	}

	@Override
	public void RefreshCfg(IKernel kernel, String path) {

	}

	protected void OnCheckVersion(IKernel kernel, IGameObject player, ActivityData cfg) {
		CheckVersion(kernel, player, PLAYER_PROPERTY_LOGINVERSION, PLAYER_PROPERTY_LOGINDATE);
	}

	// 开始显示
	void OnStartShow(IKernel kernel, ActivityData cfg) {

	}

	// 结束显示
	void OnStopShow(IKernel kernel, ActivityData cfg) {

	}

	// 开始
	void OnStart(IKernel kernel, ActivityData cfg) {
	}

	// 结束
	void OnStop(IKernel kernel, ActivityData cfg) {
	}

	// 清理数据
	void OnClearData(IKernel kernel, IGameObject player, ActivityData cfg) {
		player.setProperty(PLAYER_PROPERTY_LOGINDATE, 0);
	}

	// 初始化数据
	void OnInitData(IKernel kernel, IGameObject player, ActivityData cfg) {

	}

	// 每日数据重置
	void OnDailyReset(IKernel kernel, IGameObject player, ActivityData cfg) {
		String appendix = (String) cfg.data;
	}

	// 检测活动奖励
	void OnCheckReward(IKernel kernel, IGameObject player, ActivityData cfg) {

	}
}
