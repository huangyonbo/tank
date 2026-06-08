package game.modules.weapon;

import framework.game.*;
import game.custommsg.CustomMsg;
import game.custommsg.RequestMsgDef;
import game.custommsg.ServerCodeDef;
import game.modules.activities.code.JSONResult;
import game.modules.activities.code.ResponseCode;

import java.util.Calendar;

/**
 * 礼包模块：看完广告后领取金币礼包（次数与奖励见 {@code res/Gift/AdGiftBag.xml}）
 */
public class GiftBagModule implements ILogicModule {

	private static final String CONFIG_PATH = "res/Gift/AdGiftBag.xml";

	/** 用于按自然日重置领取次数，存 yyyyMMdd */
	private static final String PROPERTY_AD_GIFT_BAG_DAY = "AdGiftBagDay";
	/** 当日已领取次数 */
	private static final String PROPERTY_AD_GIFT_BAG_COUNT = "AdGiftBagClaimCount";

	private volatile int configGold = 500;
	private volatile int configDailyLimit = 10;

	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
		kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");
		kernel.regRequestMessage(RequestMsgDef.REQ_AD_GIFT_BAG_CLAIM.getId(), this, "OnReqAdGiftBagClaim");
		RefreshCfg(kernel, CONFIG_PATH);
		return true;
	}

	@Override
	public void onDestroy() {
	}

	public void OnPlayerClassCreate(IKernel kernel, String script) {
		kernel.declareProperty(script, PROPERTY_AD_GIFT_BAG_DAY, ValueType.INT, false, false, true);
		kernel.declareProperty(script, PROPERTY_AD_GIFT_BAG_COUNT, ValueType.INT, false, false, true);
	}

	void RefreshCfg(IKernel kernel, String path) {
		if (!CONFIG_PATH.equals(path)) {
			return;
		}
		LoadConfig(kernel);
	}

	private void LoadConfig(IKernel kernel) {
		ICfgReader cfg = kernel.loadXmlConfig(CONFIG_PATH);
		if (cfg == null || cfg.getItemCount() <= 0) {
			return;
		}
		configGold = Math.max(0, cfg.getInt(0, "gold"));
		configDailyLimit = Math.max(1, cfg.getInt(0, "dailyLimit"));
	}

	/**
	 * 客户端在激励视频关闭成功后调用；请求体可空（与 {@link CustomMsg.String} 兼容）。
	 */
	void OnReqAdGiftBagClaim(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg) {
		if (configGold <= 0) {
			game.modules.utils.UtilFunc.responseSerCodeStr(kernel, player, reqid, ServerCodeDef.CODE_FAILED);
			return;
		}

		long now = kernel.getServerTime();
		int today = yyyymmdd(now);
		int storedDay = player.getInt(PROPERTY_AD_GIFT_BAG_DAY);
		int count = player.getInt(PROPERTY_AD_GIFT_BAG_COUNT);
		if (storedDay != today) {
			count = 0;
			player.setProperty(PROPERTY_AD_GIFT_BAG_DAY, today);
			player.setProperty(PROPERTY_AD_GIFT_BAG_COUNT, 0);
		}

		if (count >= configDailyLimit) {
			game.modules.utils.UtilFunc.responseSerCodeStr(kernel, player, reqid, ServerCodeDef.CODE_TIMES_LIMIT);
			return;
		}

		long add = configGold;
		long curGold = player.getLong(PLAYER_MONEY);
		player.setProperty(PLAYER_MONEY, curGold + add, IKernel.PlayerLogType.PROP_CHANGE.ordinal(),
				"ad gift bag claim +" + add);
		player.setProperty(PROPERTY_AD_GIFT_BAG_COUNT, count + 1);

		JSONResult result = ResponseCode.Success.toJSONResult();
		result.setDesc(ServerCodeDef.CODE_SUCCESS);
		result.put("awardGold", add);
		result.put("totalGold", player.getLong(PLAYER_MONEY));
		result.put("todayClaimed", count + 1);
		result.put("dailyLimit", configDailyLimit);
		kernel.response(player, reqid,
				CustomMsg.String.newBuilder().setValue(result.toJSONString()).build().toByteArray());
	}

	private static int yyyymmdd(long serverTimeMs) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(serverTimeMs);
		return cal.get(Calendar.YEAR) * 10000 + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH);
	}
}
