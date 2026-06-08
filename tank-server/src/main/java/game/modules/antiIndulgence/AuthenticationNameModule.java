package game.modules.antiIndulgence;

import framework.game.*;
import game.custommsg.CustomMsg;
import game.custommsg.S2CMsgDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import game.modules.store.StoreModule;

import java.text.ParseException;

/**
 * 未实名认证玩家的校验模块
 * @author tanyong
 *
 */
public class AuthenticationNameModule implements ILogicModule{
	private static String AUTH_TIMESTAMP_STR  = "AuthNTimeStamp";//每次上线的时间戳
	private static String AUTH_TIMESTART_STR  = "AuthNTimeStart";//试玩开始的时间戳
	private static String AUTH_TIMECOUNT_STR  = "AuthNTimeCount";//试玩时长
	private static String AUTH_HALF_SNED_STR  = "AuthNTimeHalfSend";
	private static String AUTH_NAME_TIMER_STR = "HB_OnAuthNamePlayTime";
	private static long MAX_PLAY_TIME_LIMIE   = 3600000L;//最大试玩时间
	private static long TIME_CLEAR_DAY        = 15 * 24 * 3600000L;//15天重置试玩时间
	//private static long MAX_PLAY_TIME_LIMIE   = 240000L;//最大试玩时间
	//private static long TIME_CLEAR_DAY        = 300000L;//15天重置试玩时间

	private static Logger logger              = LoggerFactory.getLogger(AuthenticationNameModule.class);
	private StoreModule m_storeModule		  = null;

	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
		kernel.regEvent(KernelEvent.KEVENT_ON_LINE, "Player", this, "OnPlayerOnLine");
		kernel.regEvent(KernelEvent.KEVENT_OFF_LINE, "Player", this, "OnPlayerOffLine");
		kernel.listenPropertyChange(PLAYER_PROPERTY_CERTIFICATION, "Player", this, "OnPlayerCertificationChanged");
		kernel.declareHeartBeat(AUTH_NAME_TIMER_STR, this, "OnAuthNamePlayTime");

		m_storeModule = (StoreModule)kernel.getModule("StoreModule");

		return m_storeModule != null;
	}

	@Override
	public void onDestroy() {

	}

	public void OnPlayerClassCreate(IKernel kernel, String script) {
		kernel.declareProperty(script, AUTH_TIMESTART_STR,ValueType.LONG, false, false, true);
		kernel.declareProperty(script, AUTH_TIMECOUNT_STR,ValueType.LONG, false, false, true);
		kernel.declareProperty(script, AUTH_TIMESTAMP_STR,ValueType.LONG, false, false, false);
		kernel.declareProperty(script, AUTH_HALF_SNED_STR,ValueType.BOOL, false, false, false);
	}

	public void OnPlayerOnLine(IKernel kernel, IGameObject player){
		if (!m_storeModule.getChannelCertification(player.getInt(PLAYER_PROPERTY_CHANNEL))){
			//logger.info(player.GetInt(PLAYER_PROPERTY_UID) + "渠道包自己实名认证");
			return;
		}
		if (player.getBool(PLAYER_PROPERTY_CERTIFICATION) ||
				player.getLong(PLAYER_PROPERTY_SYS_REAL_NAME_AUTH_END_TIME) != 0L){
			//群包已实名认证
			//logger.info(player.GetInt(PLAYER_PROPERTY_UID) + "已实名认证");
			return;
		}
		long curTime = kernel.getServerTime();
		long preTime = player.getLong(AUTH_TIMESTART_STR);

		if (preTime > 0){
			if (curTime - preTime >= TIME_CLEAR_DAY){
				player.setProperty(AUTH_TIMECOUNT_STR,0L);
				player.setProperty(AUTH_TIMESTART_STR,curTime);
			}
		}else{
			player.setProperty(AUTH_TIMESTART_STR,curTime);
		}
		player.setProperty(AUTH_TIMESTAMP_STR,curTime);
		long count = player.getLong(AUTH_TIMECOUNT_STR);
		if (count >= MAX_PLAY_TIME_LIMIE){//已经达到试玩时长
			notifyClientOpenUI(kernel,player,1);
			return;
		}else{
			notifyClientOpenUI(kernel,player,0);
		}
		logger.info("开始监听" + player.getInt(PLAYER_PROPERTY_UID) + "试玩时间");
		kernel.addHeartBeat(AUTH_NAME_TIMER_STR,player,1000,-1);
	}

	private long GetTotayPlayTime(IKernel kernel,IGameObject player){
		long start = player.getLong(AUTH_TIMESTAMP_STR);
		long count = player.getLong(AUTH_TIMECOUNT_STR) + kernel.getServerTime() - start;
		return count;
	}

	public void OnPlayerOffLine(IKernel kernel, IGameObject player){
		if (!m_storeModule.getChannelCertification(player.getInt(PLAYER_PROPERTY_CHANNEL))){
			//渠道包不保存
			return;
		}
		if (player.getBool(PLAYER_PROPERTY_CERTIFICATION)){
			return;
		}
		long count = GetTotayPlayTime(kernel,player);
		player.setProperty(AUTH_TIMECOUNT_STR,count);
	}

	/**
	 * 通知客户端打开实名认证的UI界面
	 * @param kernel
	 * @param player
	 * @param backLogin
	 */
	private void notifyClientOpenUI(IKernel kernel,IGameObject player,int backLogin){
		CustomMsg.MustOpenAuthenticationUIMsg.Builder builder = CustomMsg.MustOpenAuthenticationUIMsg.newBuilder();
		builder.setBackLogin(backLogin);
		kernel.sendMessage(player, S2CMsgDef.S2C_OPEN_AUTHEN_UI.ordinal(), builder.build().toByteArray());
	}

	public void OnAuthNamePlayTime(IKernel kernel, IGameObject player) throws ParseException{
		long count = GetTotayPlayTime(kernel,player);
		int backLogin = 0;
		if (count >= MAX_PLAY_TIME_LIMIE){//1个小时推送
			kernel.removeHeartBeat(player,AUTH_NAME_TIMER_STR);
			backLogin = 1;
			logger.info("notifyClientOpenUI2  >>>> " + player.getInt(PLAYER_PROPERTY_UID));
		}else if (count >= MAX_PLAY_TIME_LIMIE / 2 && !player.getBool(AUTH_HALF_SNED_STR)){//半个小时推送
			player.setProperty(AUTH_HALF_SNED_STR,true);
			logger.info("notifyClientOpenUI1  >>>>  " + player.getInt(PLAYER_PROPERTY_UID));
		}else{
			return;
		}
		notifyClientOpenUI(kernel, player, backLogin);
	}

	public void OnPlayerCertificationChanged(IKernel kernel, IGameObject player,String name, Object oldAmount){
		if (player.getBool(PLAYER_PROPERTY_CERTIFICATION)){
			kernel.removeHeartBeat(player,AUTH_NAME_TIMER_STR);
		}
	}

}
