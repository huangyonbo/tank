package game.modules;

import java.util.List;

import framework.pub.IPubData;
import framework.pub.IPubRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import back.modules.dataenum.NoticeType;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.game.IGameObject;
import framework.game.IKernel;
import framework.game.ILogicModule;
import framework.game.KernelEvent;
import framework.game.ValueType;
import game.custommsg.C2SMsgDef;
import game.custommsg.CustomMsg;
import game.custommsg.S2CMsgDef;
import game.modules.utils.UtilFunc;
import game.modules.utils.UtilFunc.BroadCastType;

public class NoticeModule implements ILogicModule {
	private static Logger logger = LoggerFactory.getLogger(NoticeModule.class);

	private static List<Integer> placeIds = null;
	private static String content = null;

	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
		// 注册server消息
		kernel.regServerMsg(ServerMsgDef.B2G_GAMENOTICE.ordinal(), this, "OnBackGameNotice");

		// 声明心跳
		kernel.declareHeartBeat("HB_CheckGameNoticeScroll", this, "OnCheckGameNoticeScroll");
		kernel.regEvent(KernelEvent.KEVENT_ON_LINE, "Player", this, "OnPlayerLogin");

		kernel.regClientMessage(C2SMsgDef.C2S_SHOW_POP_NOTICE.ordinal(), this, "OnShowPopNotice");
		return true;
	}

	public void OnPlayerClassCreate(IKernel kernel, String script) {
		kernel.declareProperty(script, PLAYER_PROPERTY_SHOWPOPNOTICE, ValueType.BOOL, false, true, false);// 自动展现登录签到
		kernel.declareProperty(script, PLAYER_PROPERTY_LASTLOGINTIME4NOTICE, ValueType.LONG, false, false, true);// 上次登录时间
	}

	public void OnPlayerLogin(IKernel kernel, IGameObject player) {
		/*
		Date lastLoginDate = new Date(player.GetLong(PLAYER_PROPERTY_LASTLOGINTIME4NOTICE));
		//logger.info("lastLoginDate:" + lastLoginDate);
		Date nowDate = new Date(kernel.GetServerTime());
		if (!DateUtils.isSameDay(lastLoginDate, nowDate)) {
			//logger.info("show pop notice is true");
		}*/
		//通过属性同步通知客户端弹出登录公告界面
		player.setProperty(PLAYER_PROPERTY_SHOWPOPNOTICE,true);
		player.setProperty(PLAYER_PROPERTY_LASTLOGINTIME4NOTICE,kernel.getServerTime());
		//logger.info("show pop notice is false");
	}

	@Override
	public void onDestroy() {

	}

	// 接收到后台pubNotice消息，起跑马灯定时器，发邮件
	public void OnBackGameNotice(IKernel kernel, int serid, int msgid, byte[] msg)
			throws InvalidProtocolBufferException {
		logger.info("OnBackGameNotice, serid={}, msgid={}", serid, msgid);
		ServerMsg.BackPubNotice backPubNotice = ServerMsg.BackPubNotice.parseFrom(msg);
		int type = backPubNotice.getType();
		placeIds = backPubNotice.getPlaceIdList();
		content = backPubNotice.getContent();
		content = content.replace("\r", " ").replace("\n", " ");

		logger.info("pubNotice id={}, type={}, placeIds={}, tag={},title ={}, content={}, picture={}, mail={}",
				backPubNotice.getId(), backPubNotice.getType(), backPubNotice.getPlaceIdList(), backPubNotice.getTag(),
				backPubNotice.getTitle(), backPubNotice.getContent(), backPubNotice.getPicture(),
				backPubNotice.getMail());

		IGameObject room = kernel.getPreloadObject("room1");// 每个room上都挂了一个表，但只在room1上维护

		if (type == NoticeType.GAME.ordinal()) {// Game notice

			for (int placeid : placeIds) {
				if (placeid != -1) {
					UtilFunc.broadCastScrollMsgByChannel(kernel, placeid, content, 1, BroadCastType.NOTICE.ordinal()+"");
				} else {
					UtilFunc.broadCastScrollMsgAllServer(kernel, content, 1, BroadCastType.NOTICE.ordinal()+"");
				}
			}
			kernel.addHeartBeat("HB_CheckGameNoticeScroll", room, 60 * 1000, 2);
		}
	}

	// 心跳方法
	public void OnCheckGameNoticeScroll(IKernel kernel, IGameObject room) throws InvalidProtocolBufferException {
		for (int placeid : placeIds) {
			logger.info("HB Scroll game notice in channel:{}", placeid);
			if (placeid != -1) {
				UtilFunc.broadCastScrollMsgByChannel(kernel, placeid, content, 1, BroadCastType.NOTICE.ordinal()+"");
			} else {
				UtilFunc.broadCastScrollMsgAllServer(kernel, content, 1, BroadCastType.NOTICE.ordinal()+"");
			}
		}
	}

	public void OnShowPopNotice(IKernel kernel, IGameObject player, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		//logger.info("OnShowPopNotice msgid:" + msgid);
		CustomMsg.PopNoticeList.Builder popNoticeList = CustomMsg.PopNoticeList.newBuilder();
		IPubData pubData = kernel.getPubData("PubNoticeData");
		if (null != pubData) {
			IPubRecord record = pubData.getRecord("PubNoticeTable");
			if (null != record) {
				int rows = record.getRows();
				for (int i = 0; i < rows; i++) {
					popNoticeList.addTag(record.getString(i, 3));
					popNoticeList.addTitle(record.getString(i, 4));
					popNoticeList.addContent(record.getString(i, 5));
					popNoticeList.addPicture(record.getString(i, 6));
				}
			}
		}
		logger.info("send S2C_POP_NOTICE_LIST:" + popNoticeList.getTagList().toString());
		kernel.sendMessage(player,S2CMsgDef.S2C_POP_NOTICE_LIST.ordinal(), popNoticeList.build().toByteArray());
	}

}
