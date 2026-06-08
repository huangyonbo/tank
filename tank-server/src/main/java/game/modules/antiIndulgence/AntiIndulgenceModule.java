package game.modules.antiIndulgence;

import com.google.protobuf.InvalidProtocolBufferException;
import framework.game.*;
import game.custommsg.C2SMsgDef;
import game.custommsg.CustomMsg;
import game.custommsg.S2CMsgDef;
import game.modules.store.StoreModule;
import game.modules.utils.UtilFunc;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;

//import java.text.ParseException;

/**
 *
 * 描述： 防沉迷模块 1.游客只限累计游玩1小时 2.未成年玩家节假日限玩3小时，非节假日1.5小时 3.未成年玩家限制单次充值额度和月累计充值
 *
 */
public class AntiIndulgenceModule implements ILogicModule {
	private static Logger logger = LoggerFactory.getLogger(AntiIndulgenceModule.class);
	private ICfgReader m_restDaysConfig;
	private StoreModule m_storeModule;

	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
		kernel.regEvent(KernelEvent.KEVENT_ON_LINE, "Player", this, "OnPlayerOnLine");
		kernel.regEvent(KernelEvent.KEVENT_OFF_LINE, "Player", this, "OnPlayerOffLine");
		kernel.regClientMessage(C2SMsgDef.C2S_REPORT_AGE.ordinal(), this, "OnReportAge");
		kernel.listenPropertyChange(PLAYER_PROPERTY_TOTALRECHARGEAMOUNT, "Player", this, "OnTotalRechargeAmountChanged");
		kernel.listenPropertyChange(PLAYER_PROPERTY_CERTIFICATION, "Player", this, "OnPlayerCertificationChanged");
		kernel.declareHeartBeat("HB_OnCheckPlayTime", this, "OnCheckPlayTime");
		m_restDaysConfig = kernel.loadXmlConfig("res/AntiIndulgence/RestDays.xml");
		m_storeModule = (StoreModule)kernel.getModule("StoreModule");
		return true;
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub

	}

	public void OnPlayerClassCreate(IKernel kernel, String script) {
		kernel.declareProperty(script, PLAYER_PROPERTY_ONLINETIMESTAMP, ValueType.LONG, false, false, true); // 玩家上线时间戳
		kernel.declareProperty(script, PLAYER_PROPERTY_ALREADYPLAYTIME, ValueType.LONG, false, false, true); // 已经游玩时间
		kernel.declareProperty(script, PLAYER_PROPERTY_ALREADYPLAYTIMETICKET, ValueType.LONG, false, false, true); // 记录已经游玩时间的时间戳
		kernel.declareProperty(script, PLAYER_PROPERTY_MONTHRECHARGEAMOUNT, ValueType.INT, false, true, true); // 月累计充值额度
		kernel.declareProperty(script, PLAYER_PROPERTY_MONTHRECHARGEAMOUNTDATE, ValueType.LONG, false, false, true); // 记录月累计充值
	}

	public void OnPlayerOnLine(IKernel kernel, IGameObject player) {
		int age = player.getInt(PLAYER_PROPERTY_AGE);
		String cardId = player.getString(PLAYER_PROPERTY_IDENTITYCARD);
		// 更新Age
		if (!StringUtil.isNullOrEmpty(cardId)) {
			age = UtilFunc.getAgeByIdCard(cardId);
			player.setProperty(PLAYER_PROPERTY_AGE, age);
		}
		if (!m_storeModule.getChannelCertification(player.getInt(PLAYER_PROPERTY_CHANNEL)) ||
				player.getLong(PLAYER_PROPERTY_SYS_REAL_NAME_AUTH_END_TIME) != 0L){
			//logger.info(player.GetInt(PLAYER_PROPERTY_UID) + "渠道包自己实名认证");
			return;
		}
		boolean certification = player.getBool(PLAYER_PROPERTY_CERTIFICATION);
		long curTime = kernel.getServerTime();
		// 设置上线时间
		player.setProperty(PLAYER_PROPERTY_ONLINETIMESTAMP, curTime);
		long serverTime = UtilFunc.getZeroTime(curTime);
		// 22点到次日8点 限制未成年玩家登录
		if (certification && age < 18) {
			if (curTime >= serverTime + 22 * 3600 * 1000 || curTime <= serverTime + 8 * 3600 * 1000) {
				CustomMsg.AntiIndulgenceMsg.Builder antiIndulgenceMsg = CustomMsg.AntiIndulgenceMsg.newBuilder();
				antiIndulgenceMsg.setType(0);
				logger.info("-----22 to 8-----");
				kernel.sendMessage(player, S2CMsgDef.S2C_TRIGGER_ANTIINDULGENCE.ordinal(),
						antiIndulgenceMsg.build().toByteArray());
				return;
			}
		}
		// 实名认证的未成年玩家 非当日登录 将已玩时长清零
		if (certification && age < 18 && serverTime != player.getLong(PLAYER_PROPERTY_ALREADYPLAYTIMETICKET)) {
			player.setProperty(PLAYER_PROPERTY_ALREADYPLAYTIMETICKET, serverTime);
			player.setProperty(PLAYER_PROPERTY_ALREADYPLAYTIME, 0L);
		}
		// 未实名认证 和 实名认证未成年玩家 加心跳
		if (!certification || age < 18) {
			kernel.addHeartBeat("HB_OnCheckPlayTime", player, 1000, -1);
		}
		long monthtime = UtilFunc.getMonthZeroTime(curTime);
		// 月初 清除月累计充值额
		if (monthtime != player.getLong(PLAYER_PROPERTY_MONTHRECHARGEAMOUNTDATE)) {
			player.setProperty(PLAYER_PROPERTY_MONTHRECHARGEAMOUNTDATE, monthtime);
			player.setProperty(PLAYER_PROPERTY_MONTHRECHARGEAMOUNT, 0);
		}
	}

	// 获取当日总共游玩时间
	public long GetTotayPlayTime(IKernel kernel, IGameObject player) {
		long AlreadyPlayTime = player.getLong(PLAYER_PROPERTY_ALREADYPLAYTIME);
		long OnlineTimeStamp = player.getLong(PLAYER_PROPERTY_ONLINETIMESTAMP);
		long serverTime = kernel.getServerTime();
		return AlreadyPlayTime + serverTime - OnlineTimeStamp;
	}

	// 玩家离线后 设置已经游玩时间
	public void OnPlayerOffLine(IKernel kernel, IGameObject player) {
		player.setProperty(PLAYER_PROPERTY_ALREADYPLAYTIME, GetTotayPlayTime(kernel, player));
	}

	// 定时检查玩家是否触发防沉迷机制
	public void OnCheckPlayTime(IKernel kernel, IGameObject player) throws ParseException {
		int playTimeLimit = 5400000; // 非节假日1.5小时
		CustomMsg.AntiIndulgenceMsg.Builder antiIndulgenceMsg = CustomMsg.AntiIndulgenceMsg.newBuilder();
		boolean certification = player.getBool(PLAYER_PROPERTY_REALNAME) || player.getBool(PLAYER_PROPERTY_CERTIFICATION);
		long now = kernel.getServerTime();
		long serverTime = UtilFunc.getZeroTime(now);
		long restDayTime = 0L;
		int len = m_restDaysConfig.getItemCount(); // 获取节假日有多少天
		DateFormat format = kernel.getServer().getTimeFormat();
		for (int i = 0; i < len; i++) {
			restDayTime = format.parse(m_restDaysConfig.getString(i, "restDate")).getTime();
			if (serverTime == restDayTime) {
				playTimeLimit = 10800000; // 节假日3小时
				break;
			}
		}
		if (!certification) {
			playTimeLimit = 3600000; // 游客1小时
		}
		// logger.info("-----playTimeLimit：{} GetTotayPlayTime: {}",
		// playTimeLimit, GetTotayPlayTime(kernel, player));
		// 游客在半小时时推送一条消息
//		if (!certification && (GetTotayPlayTime(kernel, player) / 1000 == (playTimeLimit / 2) / 1000)) {
//			antiIndulgenceMsg.setType(2);
//			logger.info("-----half hour-----");
//			kernel.SendMessage(player, S2CMsgDef.S2C_TRIGGER_ANTIINDULGENCE.ordinal(),
//					antiIndulgenceMsg.build().toByteArray());
//		}
		// 未成年玩家在22点到次日8点间在线且总游玩时长未达到限制，踢下线
		if (certification && (now >= serverTime + 22 * 3600 * 1000 || now <= serverTime + 8 * 3600 * 1000)
				&& GetTotayPlayTime(kernel, player) < playTimeLimit) {
			antiIndulgenceMsg.setType(0);
		}
		if (GetTotayPlayTime(kernel, player) >= playTimeLimit) {
			if (certification) {
//				logger.info("----- under age -----");
				antiIndulgenceMsg.setType(1);
			} else {
//				logger.info("----- touristor -----");
				antiIndulgenceMsg.setType(3);
			}
			kernel.sendMessage(player, S2CMsgDef.S2C_TRIGGER_ANTIINDULGENCE.ordinal(),
					antiIndulgenceMsg.build().toByteArray());
			kernel.removeHeartBeat(player, "HB_OnCheckPlayTime");
		}
	}

	// 更新玩家月累计充值金额
	public void OnTotalRechargeAmountChanged(IKernel kernel, IGameObject player, String name, Object oldAmount) {
		player.setProperty(PLAYER_PROPERTY_MONTHRECHARGEAMOUNT,
				player.getInt(PLAYER_PROPERTY_MONTHRECHARGEAMOUNT) + player.getInt(name) - (int) oldAmount);
	}

	//游戏内部玩家进行了实名认证
	public void OnPlayerCertificationChanged(IKernel kernel, IGameObject player, String name, Object oldAmount) {
		if (player.getBool(PLAYER_PROPERTY_CERTIFICATION)){
			int age = player.getInt(PLAYER_PROPERTY_AGE);
			String cardId = player.getString(PLAYER_PROPERTY_IDENTITYCARD);
			if (!StringUtil.isNullOrEmpty(cardId)) {
				age = UtilFunc.getAgeByIdCard(cardId);
				player.setProperty(PLAYER_PROPERTY_AGE, age);
			}
			if (age < 18) {
				kernel.addHeartBeat("HB_OnCheckPlayTime", player, 1000, -1);
			} else {
				kernel.removeHeartBeat(player, "HB_OnCheckPlayTime");
			}
			kernel.removeHeartBeat(player, "HB_OnRealNameAuthTime");
		}
	}

	void OnReportAge(IKernel kernel, IGameObject player, int msgid, byte[] msg) throws InvalidProtocolBufferException {
		CustomMsg.Int32 data = CustomMsg.Int32.parseFrom(msg);
		int age = data.getValue();
		if (age > 0) {
			player.setProperty(PLAYER_PROPERTY_CERTIFICATION, true);
			player.setProperty(PLAYER_PROPERTY_AGE, age);
		}
	}
}
