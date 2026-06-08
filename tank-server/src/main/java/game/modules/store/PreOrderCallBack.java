package game.modules.store;
import com.dtflys.forest.callback.OnError;
import com.dtflys.forest.callback.OnSuccess;
import com.dtflys.forest.exceptions.ForestRuntimeException;
import com.dtflys.forest.http.ForestRequest;
import com.dtflys.forest.http.ForestResponse;
import com.google.gson.JsonObject;
import framework.game.IGameObject;
import framework.game.IKernel;
import framework.game.IRecord;
import game.custommsg.CustomMsg;
import game.custommsg.S2CMsgDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreOrderCallBack implements OnSuccess<String>, OnError{

	private static Logger logger = LoggerFactory.getLogger(PreOrderCallBack.class);
	
	private IKernel kernel;
	
	private IGameObject player;
	
	public PreOrderCallBack(IKernel kernel, IGameObject player) {
		this.kernel = kernel;
		this.player = player;
	}

	@Override
	public void onError(ForestRuntimeException ex, ForestRequest request, ForestResponse response) {
		logger.error("OnBuyStorePay preoder error:", ex);
		CustomMsg.PreOrderResp.Builder preOrderResp = CustomMsg.PreOrderResp.newBuilder();
		preOrderResp.setResp("{\"code\":5,\"ID\":-1,\"extra\":{}}");
		kernel.sendMessage(player, S2CMsgDef.S2C_PREORDER_RESP.ordinal(), preOrderResp.build().toByteArray());
	}

	@Override
	public void onSuccess(String data, ForestRequest request, ForestResponse response) {
		logger.info("order success resp ： {} ",data);
		JsonObject json = framework.JsonUtil.decodeToObj(data, JsonObject.class);
		int code = json.get("code").getAsInt();
		if (code == 0){
			int orderId = json.get("id").getAsInt();
			String goodsId = json.get("goodsId").getAsString();
			IRecord buyGoodsRec = player.getRecord("BuyGoodsRec");
			long curTime = kernel.getServerTime();
			for (int i = 0 ; i < buyGoodsRec.getRows() ; ) {
				if (curTime - buyGoodsRec.getLong(i, 1) >= 1800000) {
					buyGoodsRec.removeRow(i);
				}else{
					i++;
				}
			}
			buyGoodsRec.addRow(goodsId, curTime,orderId,goodsId);
		}
		CustomMsg.PreOrderResp.Builder preOrderResp = CustomMsg.PreOrderResp.newBuilder();
		preOrderResp.setResp(data);
		kernel.sendMessage(player, S2CMsgDef.S2C_PREORDER_RESP.ordinal(),preOrderResp.build().toByteArray());
	}
}
