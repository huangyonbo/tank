package game.modules;

import com.google.protobuf.InvalidProtocolBufferException;
import framework.game.*;
import game.custommsg.CustomMsg;
import game.custommsg.RequestMsgDef;
import game.modules.items.ItemModule;
import game.modules.utils.ItemTipType;
import game.modules.utils.UtilFunc;

/**
 * 兑换码模块 用以接收兑换请求，服务器判断兑换码有效性以及兑换物品发放
 *
 * @author fanalong
 * @date 2018.10.16
 *
 */
public class RedeemCode implements ILogicModule {

	private ItemModule m_ItemModule;
	private OfflineDataModule m_OfflineDataModule;

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onInit(IKernel kernel) {
		// 注册对象创建事件
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
		// 注册客户端消息
		kernel.regRequestMessage(RequestMsgDef.REQ_REDEEMCODE.ordinal(), this, "OnReqRedeem");
		// 获取其他模块对象
		m_ItemModule = (ItemModule) kernel.getModule("ItemModule");// 道具模块
		m_OfflineDataModule = (OfflineDataModule) kernel.getModule("OfflineDataModule");// 掉线处理模块
		// 获取失败则返回false，服务器启动失败
		if (m_ItemModule == null || m_OfflineDataModule == null) {
			return false;
		}
		return true;
	}

	void OnPlayerClassCreate(IKernel kernel, String script) {
		// 增加一张数据记录表，记录该玩家已使用过的兑换码
		IRecord rec = kernel.declareRecord(script, "RecRedeemCode", 1, 100, false, false, true);
		rec.setColType(0, ValueType.STRING);
	}

	// 请求兑换处理回调函数
	void OnReqRedeem(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
			throws InvalidProtocolBufferException {
		CustomMsg.String.Builder data = CustomMsg.String.newBuilder();
		data.setValue("");
		CustomMsg.String redeem = CustomMsg.String.parseFrom(msg);
		String redeemCode = redeem.getValue();
		IRecord rec = player.getRecord("RecRedeemCode");
		int pos = rec.findRow(0,0,redeemCode);
		if (pos != -1) {
			// 该玩家已兑换过该码
			kernel.response(player, reqid, data.build().toByteArray());
			return;
		}
		int uid = player.getInt(PLAYER_PROPERTY_UID);
		int channel = player.getInt(PLAYER_PROPERTY_CHANNEL);
		String devid = player.getString(PLAYER_PROPERTY_DEVICEID);
		kernel.useRedeemCode(redeemCode, uid, channel, devid, (items) -> {
			if (items != null && items.length() > 0){
				String result = handleRedeemCode(kernel,player,items,redeemCode);
				data.setValue(result);
				rec.addRow(redeemCode);//将该兑换码加入到玩家已使用过的兑换码列表中
			}
			kernel.response(player, reqid, data.build().toByteArray());
		});
	}

	public String handleRedeemCode(IKernel kernel, IGameObject player, String items, String redeemCode) {
		IRecord rec = player.getRecord("RecRedeemCode");
		String result = "";
		if (null == items || items.equals("")) {// 使用兑换码兑换失败
			// 是否认为玩家已使用了该兑换码
			int row = rec.findRow(0, 0, redeemCode);
			if (row != -1){
				rec.removeRow(row);
			}
		} else {// 使用兑换码兑换成功
			result = ("兑换道具成功！");
			// 处理道具字符串
			String[] arr = items.split(",");
			String[] temp;
			Object[] objects = new Object[arr.length * 2];
			for (int i = 0; i < arr.length; ++i) {
				temp = arr[i].split(":");
				objects[2 * i] = temp[0];
				objects[2 * i + 1] = Integer.valueOf(temp[1]);
				m_ItemModule.AddItem(kernel, player, temp[0], Integer.parseInt(temp[1]), UtilFunc.System.REDEEM_CODE.ordinal(), "RedeemCode " + redeemCode);
//				ItemLogModule.AddItemLog(kernel,player,temp[0],Integer.parseInt(temp[1]), ItemLogEnum.CDK_EXCHANGE_GET.ordinal());
			}
			// 发送提示框
			UtilFunc.sendItemTips(kernel, player, ItemTipType.TIP_GET_SUCCESS, objects);
		}
		return result;
	}
}
