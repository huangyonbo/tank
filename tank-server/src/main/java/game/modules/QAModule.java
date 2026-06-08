package game.modules;

import framework.pub.IPubData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import framework.game.IGameObject;
import framework.game.IKernel;
import framework.game.ILogicModule;
import framework.game.KernelEvent;
import game.custommsg.C2SMsgDef;
import game.custommsg.CustomMsg;
import game.custommsg.S2CMsgDef;

public class QAModule implements ILogicModule {

	private static Logger logger = LoggerFactory.getLogger(QAModule.class);

	@Override
	public void onDestroy() {

	}

	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_LINE, "Player", this, "OnPlayerLogin");
		kernel.regClientMessage(C2SMsgDef.C2S_QA_COMMIT.ordinal(), this, "OnQACommit");
		return true;
	}

	public void OnPlayerLogin(IKernel kernel, IGameObject player) {
		IPubData pubData = kernel.getPubData("PubFeedbackData");
		if (null != pubData) {
			String serviceInfo = pubData.getString("FeedBackService");
			CustomMsg.FeedBackService.Builder feedBackService = CustomMsg.FeedBackService.newBuilder();
			feedBackService.setServiceInfo(serviceInfo);
			kernel.sendMessage(player, S2CMsgDef.S2C_FEED_BACK_SERVICE.ordinal(),
					feedBackService.build().toByteArray());
		}

	}

	public void OnQACommit(IKernel kernel, IGameObject player, int msgid, byte[] msg)
			throws InvalidProtocolBufferException {
		logger.info("recive OnQACommit msgid:" + msgid);
		CustomMsg.QACommit qaCommit = CustomMsg.QACommit.parseFrom(msg);
		kernel.advice(player, qaCommit.getContent(), qaCommit.getType());
	}

}
