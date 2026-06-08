package game.modules.weapon;

import common.ServerMsg;
import common.ServerMsgDef;
import framework.game.ICfgReader;
import framework.game.IGameObject;
import framework.game.IKernel;
import framework.game.ILogicModule;
import framework.game.IRecord;
import framework.game.KernelEvent;
import framework.game.ValueType;
import game.constant.OfflineDataType;
import game.custommsg.CommandDef;
import game.custommsg.CustomMsg;
import game.custommsg.RequestMsgDef;
import game.modules.OfflineDataModule;
import game.modules.items.ItemModule;
import game.modules.player.PlayerModule;
import game.modules.utils.ItemTipType;
import game.modules.utils.UtilFunc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 商城（外部支付购买金币/钻石等）
 */
public class MallsModule implements ILogicModule {
	private static final Logger logger = LoggerFactory.getLogger(MallsModule.class);

	private static final String STORE_PKG_PATH = "res/StoreItems/StorePkg.xml";

	// 支付回调幂等/状态机记录
	private static final String PAY_ORDER_REC = "MallsPayOrderRec";
	private static final int PAY_STATUS_PAID = 1;    // 已支付（校验通过，等待发货）
	private static final int PAY_STATUS_SHIPPED = 2; // 已发货（发放完成，忽略重复回调）
	private static final long PAID_RETRY_LOCK_MS = 5L * 60 * 1000;

	private ItemModule m_itemModule;
	private PlayerModule m_playerModule;
	private OfflineDataModule m_offlineDataModule;

	private final List<String> m_noDispatchItems = new ArrayList<>();

	@Override
	public boolean onInit(IKernel kernel) {
		// 支付回调
		kernel.regServerRequest(ServerMsgDef.H2G_PAY_BACK.ordinal(), this, "OnRecPayCallBack");
		// 兼容旧前端：订单结果请求
		kernel.regRequestMessage(RequestMsgDef.REQ_ORDER_RES.ordinal(), this, "OnReqOrderInfo");
		// 充值相关属性 + 幂等记录
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");

		m_itemModule = (ItemModule) kernel.getModule("ItemModule");
		m_playerModule = (PlayerModule) kernel.getModule("PlayerModule");
		m_offlineDataModule = (OfflineDataModule) kernel.getModule("OfflineDataModule");

		loadNoDispatchItems(kernel);
		return true;
	}

	private void loadNoDispatchItems(IKernel kernel) {
		try {
			ICfgReader cfg = kernel.loadXmlConfig(STORE_PKG_PATH);
			if (cfg == null) {
				return;
			}
			int count = cfg.getItemCount();
			for (int i = 0; i < count; i++) {
				String id = cfg.getString(i, "Id");
				boolean noDispatch = false;
				try {
					noDispatch = cfg.getBool(i, "NoDispatch");
				} catch (Exception ignored) {
					// 缺省 false
				}
				if (noDispatch && id != null && !id.isEmpty()) {
					m_noDispatchItems.add(id);
				}
			}
		} catch (Exception e) {
			logger.warn("loadNoDispatchItems failed", e);
		}
	}

	@Override
	public void onDestroy() {
	}

	void OnPlayerClassCreate(IKernel kernel, String script) {
		// 被多个充值活动监听
		kernel.declareProperty(script, PLAYER_PROPERTY_TOTALRECHARGEAMOUNT, ValueType.INT, false, true, true);
		kernel.declareProperty(script, PLAYER_PROPERTY_ALREADYRECHARGED, ValueType.BOOL, false, true, true);

		IRecord payOrderRec = kernel.declareRecord(script, PAY_ORDER_REC, 4, 200, false, false, true);
		payOrderRec.setColType(0, ValueType.INT);   // orderId
		payOrderRec.setColType(1, ValueType.INT);   // status
		payOrderRec.setColType(2, ValueType.STRING);// goodsId
		payOrderRec.setColType(3, ValueType.LONG);  // updateTime
	}

	/**
	 * 支付回调（H2G_PAY_BACK）
	 */
	void OnRecPayCallBack(IKernel kernel, int reqid, byte[] data) {
		byte[] result = new byte[1];
		try {
			ServerMsg.PayBack payBack = ServerMsg.PayBack.parseFrom(data);
			int playerId = payBack.getUid();
			int orderId = payBack.getOrderId();
			long orderSuccessTime = kernel.getServerTime();

			IGameObject player = kernel.getPlayer(playerId);
			if (player != null) {
				doPayLogic(kernel, player, payBack, orderSuccessTime, false, orderId);
			} else if (m_offlineDataModule != null) {
				int payMoney = 0;
				try {
					payMoney = Integer.parseInt(payBack.getPayMoney());
				} catch (Exception ignored) {
				}
				String cashTicket = payBack.hasCashTicket() ? payBack.getCashTicket() : "";
				StringBuilder sb = new StringBuilder();
				sb.append(payBack.getGoodId()).append("-")
						.append(payMoney).append("-")
						.append(payBack.getInfo() == null ? "" : payBack.getInfo()).append("-")
						.append(orderSuccessTime).append("-")
						.append(orderId).append("-")
						.append(cashTicket == null ? "" : cashTicket);

				m_offlineDataModule.AddOfflineData(kernel, playerId, OfflineDataType.PAY_CALL_BACK,
						sb.toString(), "PayCallBack");
			}
			result[0] = 0;
		} catch (Exception e) {
			result[0] = 1;
			logger.error("OnRecPayCallBack error", e);
		}
		kernel.responseServer(reqid, result);
	}

	/**
	 * 兼容旧前端：订单结果请求（本实现发放后不依赖客户端拉取）
	 */
	void OnReqOrderInfo(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] data) {
		CustomMsg.String.Builder build = CustomMsg.String.newBuilder();
		build.setValue("");
		kernel.response(player, reqid, build.build().toByteArray());
	}

	/**
	 * 发货逻辑：金额归一化/封顶 + 幂等/状态机 + 现金券扣除
	 */
	public void doPayLogic(IKernel kernel, IGameObject player, ServerMsg.PayBack payBack,
	                         long orderSuccessTime, boolean offline, int orderId) {
		if (kernel == null || player == null || payBack == null) {
			return;
		}
		if (m_itemModule == null) {
			m_itemModule = (ItemModule) kernel.getModule("ItemModule");
		}
		if (m_playerModule == null) {
			m_playerModule = (PlayerModule) kernel.getModule("PlayerModule");
		}

		long now = kernel.getServerTime();
		int uid = player.getInt(PLAYER_PROPERTY_UID);

		String goodsId = payBack.getGoodId();
		if (goodsId == null || goodsId.isEmpty()) {
			logger.info("doPayLogic empty goodsId uid={} orderId={}", uid, orderId);
			return;
		}

		int rawPayMoney;
		try {
			rawPayMoney = Integer.parseInt(payBack.getPayMoney());
		} catch (Exception e) {
			logger.info("doPayLogic invalid payMoney uid={} orderId={} payMoney={}", uid, orderId, payBack.getPayMoney());
			return;
		}

		// orderId<=0：邮件/补单等内部场景，跳过幂等状态机
		boolean useIdempotency = orderId > 0;

		IRecord payOrderRec = null;
		int payRow = -1;
		int status = 0;
		if (useIdempotency) {
			payOrderRec = player.getRecord(PAY_ORDER_REC);
			payRow = payOrderRec.findRow(0, 0, orderId);
			if (payRow != -1) {
				status = payOrderRec.getInt(payRow, 1);
				// goodsId 以记录为准（防止同一 orderId 篡改 goodsId）
				String savedGoodsId = payOrderRec.getString(payRow, 2);
				if (savedGoodsId != null && !savedGoodsId.isEmpty()) {
					goodsId = savedGoodsId;
				}

				if (status >= PAY_STATUS_SHIPPED) {
					return; // 已发货
				}
				if (status == PAY_STATUS_PAID) {
					long updateTime = payOrderRec.getLong(payRow, 3);
					if (now - updateTime < PAID_RETRY_LOCK_MS) {
						return; // 仍在“已支付等待发货”的锁定窗口
					}
					// 允许重试
					payOrderRec.setValue(payRow, 3, now);
				}
			} else {
				// 新订单：进入“已支付等待发货”
				payOrderRec.addRow(orderId, PAY_STATUS_PAID, goodsId, now);
				payRow = payOrderRec.findRow(0, 0, orderId);
			}
		}

		// 金额归一化/封顶：以 ItemPkg.xml 的 Cost 为准
		int expectedYuan = 0;
		try {
			Object cost = kernel.getCfgProperty(goodsId, "Cost");
			if (cost != null) {
				expectedYuan = Integer.parseInt(String.valueOf(cost));
			}
		} catch (Exception ignored) {
		}

		int chargeYuan = rawPayMoney;
		if (expectedYuan > 0) {
			if (rawPayMoney == expectedYuan * 10) {
				chargeYuan = expectedYuan;
			} else if (rawPayMoney < expectedYuan) {
				// 欠付：拒绝发货，并回滚“已支付”状态（便于正确回调重试）
				if (useIdempotency && payOrderRec != null && payRow != -1) {
					payOrderRec.removeRow(payRow);
				}
				return;
			} else if (rawPayMoney > expectedYuan) {
				// 过付：封顶
				chargeYuan = expectedYuan;
			}
		}
		if (chargeYuan <= 0) {
			if (useIdempotency && payOrderRec != null && payRow != -1) {
				payOrderRec.removeRow(payRow);
			}
			return;
		}

		// 现金券扣除：不足则不发货，并回滚“已支付”状态
		if (payBack.hasCashTicket()) {
			String cashTicket = payBack.getCashTicket();
			if (cashTicket != null && !cashTicket.isEmpty()) {
				int count = m_itemModule.SubItem(kernel, player, cashTicket, 1, UtilFunc.System.STORE.ordinal(),
						"malls pay use cashTicket");
				if (count <= 0) {
					if (useIdempotency && payOrderRec != null && payRow != -1) {
						payOrderRec.removeRow(payRow);
					}
					return;
				}
			}
		}

		// 发放道具：NoDispatch=true 不发放，只触发充值相关奖励
		if (!m_noDispatchItems.contains(goodsId)) {
			m_itemModule.AddItem(kernel, player, goodsId, 1, UtilFunc.System.STORE.ordinal(), "malls pay award");
			UtilFunc.sendItemTips(kernel, player, ItemTipType.TIP_PAY, goodsId, 1);
		}

		// 更新充值进度
		player.setProperty(PLAYER_PROPERTY_TOTALRECHARGEAMOUNT,
				player.getInt(PLAYER_PROPERTY_TOTALRECHARGEAMOUNT) + chargeYuan);
		player.setProperty(PLAYER_PROPERTY_ALREADYRECHARGED, true);

		// 状态切换为“已发货”
		if (useIdempotency && payOrderRec != null && payRow != -1) {
			payOrderRec.setValue(payRow, 1, PAY_STATUS_SHIPPED);
			payOrderRec.setValue(payRow, 3, now);
		}

		// 触发后续活动奖励
		kernel.command(player,
				CommandDef.CMD_PAY_BACK.ordinal(),
				goodsId,
				chargeYuan,
				payBack.getInfo(),
				orderSuccessTime,
				offline,
				String.valueOf(orderId));
	}
}
