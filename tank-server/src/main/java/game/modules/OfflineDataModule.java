/**
 *
 * 描述：   离线数据模块
 * 文件：OfflineDataModule.java
 * 创建人：胡中伟
 * 创建时间：2018年5月8日 下午3:57:31
 *
 */
package game.modules;

import common.ServerMsg;
import framework.game.IGameObject;
import framework.game.IKernel;
import framework.game.ILogicModule;
import framework.game.KernelEvent;
import game.constant.OfflineDataType;
import game.modules.items.ItemModule;
import game.modules.player.PlayerModule;
import game.modules.store.StoreModule;
import game.modules.trigger.TriggerModule;
import game.modules.utils.ItemTipType;
import game.modules.utils.UtilFunc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * 描述：
 *
 */
public class OfflineDataModule implements ILogicModule {

    private static Logger logger = LoggerFactory.getLogger(OfflineDataModule.class);
    private ItemModule itemModule;
    private StoreModule storeModule;
    private RedeemCode redeemCode;
    //	private RedeemCode redeemCode;
    private PlayerModule playerModule;
    private TriggerModule triggerModule;
    private String friendInvitation = "item_friendinvitation";

    /**
     * @param kernel
     * @return
     */
    @Override
    public boolean onInit(IKernel kernel) {
        kernel.regEvent(KernelEvent.KEVENT_ON_OFFLINEDATA, "Player", this, "OnOfflineData");
        itemModule = (ItemModule) kernel.getModule("ItemModule");
        storeModule = (StoreModule) kernel.getModule("StoreModule");
        redeemCode = (RedeemCode) kernel.getModule("RedeemCode");
//		redeemCode = (RedeemCode) kernel.getModule("RedeemCode");
        playerModule = (PlayerModule) kernel.getModule("PlayerModule");
        triggerModule = (TriggerModule) kernel.getModule("TriggerModule");
        return true;
    }

    /**
     *
     */
    @Override
    public void onDestroy() {
    }

    public void AddOfflineData(IKernel kernel, int uid, int type, String context, String reason) {
        logger.info("AddOfflineData {} {} {} {}", uid, type, context, reason);
        kernel.addOfflineData(uid, type, context, reason);
    }

    void OnOfflineData(IKernel kernel, IGameObject player, int type, String context, String reason) {
        logger.info("OnOfflineData {} {} {} {}", player.getInt(PLAYER_PROPERTY_UID), type, context, reason);
        switch (type) {
            case OfflineDataType.SHUT_UP: {
                player.setProperty(PLAYER_PROPERTY_SHUTUP, Long.parseLong(context));
                break;
            }
            case OfflineDataType.PAY_CALL_BACK: {
                String[] values = context.split("-");
                ServerMsg.PayBack.Builder payBack = ServerMsg.PayBack.newBuilder();
                payBack.setUid(player.getInt(PLAYER_PROPERTY_UID));
                payBack.setGoodId(values[0]);
                payBack.setPayMoney(values[1]);
                payBack.setInfo(values[2]);
                long orderSuccessTime = Long.parseLong(values[3]);
                int orderId = Integer.parseInt(values[4]);
                payBack.setOrderId(orderId);
                if (values.length > 5) {
                    String cashTicket = values[5];
                    if (cashTicket != null && !"".equals(cashTicket)) {
                        payBack.setCashTicket(cashTicket);
                    }
                }
                storeModule.doPayLogic(kernel, player, payBack.build(), orderSuccessTime, true, orderId);
                break;
            }
            case OfflineDataType.BUY_SUCCESS: {
                String[] items = context.split(":");
                if (items.length != 2) {
                    return;
                }
                int count = Integer.parseInt(items[1]);
                String id = items[0];
                if (friendInvitation.equals(id)) {
                    player.setProperty(PLAYER_PROPERTY_RECRUITTOKEN, player.getInt(PLAYER_PROPERTY_RECRUITTOKEN) + count);
                } else {
                    itemModule.AddItem(kernel, player, items[0], count, UtilFunc.System.STORE.ordinal(),
                            "Buy success with offlinedata");
                }
                UtilFunc.sendItemTips(kernel, player, ItemTipType.TIP_BUY, items[0], count);
                break;
            }
            case OfflineDataType.BUY_FAILED: {
                String[] pros = context.split(":");
                if (pros.length < 2) {
                    return;
                }
                int val = Integer.parseInt(pros[1]);
                if (pros.length == 2) {
                    player.setProperty(pros[0], player.getLong(pros[0]) + val, UtilFunc.System.STORE.ordinal(),"Buy failed, back pro");
                } else {
                    itemModule.AddItem(kernel, player, pros[0], val, UtilFunc.System.STORE.ordinal(),"Buy failed, back pro");
                }
                break;
            }
            case OfflineDataType.BUY_FAILED_ITEM: {
                String[] items = context.split(":");
                if (items.length != 2) {
                    return;
                }
                int count = Integer.parseInt(items[1]);
                itemModule.AddItem(kernel, player, items[0], count, UtilFunc.System.STORE.ordinal(),"Buy failed, back with offlinedata");
                break;
            }
            case OfflineDataType.BUY_FAILED_COUNT: {
                String[] pros = context.split(":");
                if (pros.length != 2) {
                    return;
                }
                int val = Integer.parseInt(pros[1]);
                player.setProperty(pros[0], player.getInt(pros[0]) + val, UtilFunc.System.STORE.ordinal(),"Buy failed, back count");
                break;
            }
            case OfflineDataType.BACK_LIMIT_COUNT: {
                String[] items = context.split(":");
                if (items.length != 2) {
                    return;
                }
                int count = Integer.parseInt(items[1]);
                storeModule.BackLimitCount(kernel, player, items[0], count);
                break;
            }
            case OfflineDataType.BACK_CARD_LIMIT_COUNT: {
                String[] items = context.split(":");
                if (items.length != 2) {
                    return;
                }
                int count = Integer.parseInt(items[1]);
                storeModule.BackCardLimitCount(kernel, player, items[0], count);
                break;
            }
            case OfflineDataType.REDEEMCODE_FAILED:{// 兑换码兑换道具时下线，失败
                String[] result = context.split("\\|\\|");
                String items = result.length == 2 ? result[1] : "";
                redeemCode.handleRedeemCode(kernel, player, items, result[0]);
//			redeemCode.handleRedeemCode(kernel, player, items, result[0]);
                break;
            }
            case OfflineDataType.USE_CARD_FAILED: {
                itemModule.AddItem(kernel, player, context, 1, UtilFunc.System.CARD_ITEM.ordinal(),"Use card failed, back with offline data");
                break;
            }
            case OfflineDataType.REAL_NAME_SUCCESS: {
                playerModule.OnRealNameSuccess(kernel, player);
                break;
            }
            case OfflineDataType.BIND_PROXY: {
                playerModule.OnBindProxySucc(kernel, player);
                break;
            }
            case OfflineDataType.BIND_CHANNEL: {
                playerModule.OnBindRes(kernel, player, context);
                break;
            }
            case OfflineDataType.CHANGE_PASSWORD: {
                playerModule.OnChangePWRes(kernel, player, context);
                break;
            }
            case OfflineDataType.BIND_PHONE: {
                playerModule.OnBindPhoneRes(kernel, player, context);
                break;
            }
            case OfflineDataType.UN_BIND_PHONE: {
                playerModule.OnUnBindPhoneRes(kernel, player, context);
                break;
            }
            case OfflineDataType.ALLOW_OFFLINE_PLAYER_JOIN_GUILD: {
                triggerModule.OnTrigger(kernel, player, TriggerModule.TriggerType.TYPE_JOIN_GUILD.ordinal(), "", 1, TriggerModule.ValueType.INC.ordinal());
            }
            case OfflineDataType.FQZS_OFFLINE_REWARD: {
                String[] items = context.split(":");
                if (items.length < 2) {
                    return;
                }
                itemModule.AddItem(kernel, player, items[0], Long.parseLong(items[1]), UtilFunc.System.FQZS_REWARD.ordinal(), "FQZS player offline, back with offlinedata");
            }
            case OfflineDataType.BMBC_OFFLINE_REWARD: {
                String[] items = context.split(":");
                if (items.length < 2) {
                    return;
                }
                itemModule.AddItem(kernel, player, items[0], Long.parseLong(items[1]), UtilFunc.System.BMBC_REWARD.ordinal(), "BMBC player offline, back with offlinedata");
            }
            case OfflineDataType.BMBC_OFFLINE_DEDUCTION: {
                String[] items = context.split(":");
                if (items.length < 2) {
                    return;
                }
                itemModule.SubItem(kernel, player, items[0], Long.parseLong(items[1]), UtilFunc.System.FQZS_REWARD.ordinal(), "FQZS player offline, back with offlinedata");
            }
            case OfflineDataType.FQZS_OFFLINE_DEDUCTION: {
                String[] items = context.split(":");
                if (items.length < 2) {
                    return;
                }
                itemModule.SubItem(kernel, player, items[0], Long.parseLong(items[1]), UtilFunc.System.BMBC_REWARD.ordinal(), "BMBC player offline, back with offlinedata");
            }
            case OfflineDataType.BRNN_OFFLINE_REWARD: {
                String[] items = context.split(":");
                if (items.length < 2) {
                    return;
                }
                itemModule.AddItem(kernel, player, items[0], Long.parseLong(items[1]), UtilFunc.System.BRNN_REWARD.ordinal(), "BRNN player offline, back with offlinedata");
            }
            case OfflineDataType.BRNN_OFFLINE_DEDUCTION: {
                String[] items = context.split(":");
                if (items.length < 2) {
                    return;
                }
                itemModule.SubItem(kernel, player, items[0], Long.parseLong(items[1]), UtilFunc.System.BRNN_REWARD.ordinal(), "BRNN player offline, back with offlinedata");
            }
            default:
                break;
        }
    }
}

