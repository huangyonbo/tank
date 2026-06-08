package http.modules;

import common.ServerMsg;
import common.ServerMsgDef;
import framework.JsonUtil;
import framework.http.HttpKernel;
import framework.http.IHttpModule;

import java.nio.charset.StandardCharsets;

public class HttpModule implements IHttpModule {

	class PayBackInfo {
		public int uid;
		public int orderId;
		public String payMoney;
		public String goodsId;
		public String info;
		private String cashTicket = "";
	}

	@Override
	public boolean onInit(HttpKernel kernel) {
		kernel.regHttpMessage("/payBack", this, "OnPayBack");
		kernel.regHttpMessage("/channelChange", this, "OnChannelChange");
		return true;
	}

	@Override
	public void onDestroy() {

	}

	public String OnPayBack(HttpKernel kernel, String json) {
		PayBackInfo payBackInfo = JsonUtil.decodeToObj(json,PayBackInfo.class);
		String lastGame = kernel.getUserSession(payBackInfo.uid);
		ServerMsg.PayBack.Builder builder = ServerMsg.PayBack.newBuilder();
		builder.setUid(payBackInfo.uid);
		builder.setPayMoney(payBackInfo.payMoney);
		builder.setGoodId(payBackInfo.goodsId);
		builder.setInfo(payBackInfo.info);
		builder.setOrderId(payBackInfo.orderId);
		if (payBackInfo.cashTicket != null && !payBackInfo.cashTicket.isEmpty()) {
			builder.setCashTicket(payBackInfo.cashTicket);
		}
		byte[] datas =  builder.build().toByteArray();
		if (lastGame == null || lastGame.isEmpty()) {
			//kernel.SendServerMsg(lastGame, ServerMsgDef.H2G_PAY_BACK.ordinal(), builder.build().toByteArray());
			lastGame = framework.ServerSet.SERVER_LOGIC_NAME_GAME;
		}
		kernel.requestServer(lastGame,ServerMsgDef.H2G_PAY_BACK.ordinal(),datas,(msg)->{
			byte code = -1;//超时逻辑
			if (msg != null){
				code = msg[0];
			}
			if (code != 0){
				kernel.addPayErrorLog(payBackInfo.uid,payBackInfo.goodsId,payBackInfo.orderId,code);
			}
		});
		return "ok";
	}

	public String OnChannelChange(HttpKernel kernel, String json) {
		kernel.requestServerByType("game",ServerMsgDef.H2G_CHANNEL_DATA_CHANGE.ordinal(),json.getBytes(StandardCharsets.UTF_8));
		return "ok";
	}
}
