package game.modules.weapon;

import framework.PropertyKey;
import framework.game.IGameObject;
import framework.game.IKernel;
import framework.game.IRecord;
import framework.game.ValueType;
import framework.mybatis.domain.GooglePlayOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Permanent refund receipts live in the same player archive as the money mutation. */
public final class GooglePlayRefunds implements PropertyKey {
    static final String RECORD = "GooglePlayRefundRec";
    private static final Logger log = LoggerFactory.getLogger(GooglePlayRefunds.class);

    static boolean knownRefund(IGameObject player, GooglePlayOrder order) {
        IRecord rec=player.getRecord(RECORD);
        int row=rec==null?-1:rec.findRow(0,0,order.getGoogleOrderId());
        return row>=0 && order.getPurchaseTokenHash().equals(rec.getString(row,1))
                && order.getGoldAmount()!=null && order.getGoldAmount()==rec.getLong(row,2)
                && (rec.getInt(row,5)==0 || rec.getInt(row,5)==1);
    }

    static int pendingCount(IGameObject player) {
        IRecord rec=player.getRecord(RECORD);int count=0;
        if(rec!=null)for(int row=0;row<rec.getRows();row++)if(rec.getInt(row,5)!=1)count++;
        return count;
    }

    /** Server-side diagnostic output deliberately excludes token/account hashes. */
    public static com.alibaba.fastjson.JSONArray pending(IGameObject player) {
        com.alibaba.fastjson.JSONArray result=new com.alibaba.fastjson.JSONArray();
        IRecord rec=player.getRecord(RECORD);
        if(rec!=null)for(int row=0;row<rec.getRows();row++)if(rec.getInt(row,5)!=1) {
            com.alibaba.fastjson.JSONObject item=new com.alibaba.fastjson.JSONObject();
            item.put("orderId",rec.getString(row,0));item.put("orderAmount",rec.getLong(row,2));
            item.put("before",rec.getLong(row,3));item.put("expectedAfter",rec.getLong(row,4));item.put("status",rec.getInt(row,5));
            result.add(item);
        }
        return result;
    }

    /** A reviewed hold may be closed only by acknowledging its exact immutable snapshot; never debit again. */
    public static boolean acknowledgeReview(IGameObject player,String orderId,long before,long after,String evidence) {
        if(evidence==null || evidence.trim().length()<8 || evidence.length()>200) return false;
        IRecord rec=player.getRecord(RECORD);int row=rec==null?-1:rec.findRow(0,0,orderId);
        if(row<0 || rec.getLong(row,3)!=before || rec.getLong(row,4)!=after)return false;
        if(rec.getInt(row,5)==1)return true;
        if(rec.getInt(row,5)!=0)return false;
        rec.setValue(row,5,1);
        log.warn("Google refund manual review acknowledged uid={} orderId={} before={} expectedAfter={} action=close_without_debit evidence={}",
                player.getInt(PLAYER_PROPERTY_UID),orderId,before,after,evidence.replaceAll("[\\r\\n]"," "));
        return rec.getInt(row,5)==1;
    }

    static void declare(IKernel kernel, String script) {
        // Archive row count is a signed short. Never evict a receipt to make room.
        IRecord rec = kernel.declareRecord(script, RECORD, 6, 30000, false, false, true);
        ValueType[] types = {ValueType.STRING, ValueType.STRING, ValueType.LONG,
                ValueType.LONG, ValueType.LONG, ValueType.INT};
        for (int col = 0; col < types.length; col++) rec.setColType(col, types[col]);
    }

    /** Returns true when a receipt needs persistence, including retry after a failed save. */
    static boolean recover(IGameObject player, GooglePlayOrder order) {
        IRecord rec = player.getRecord(RECORD);
        // Version 1 credited the historical Gold wallet, not PlayerMoney.
        if (!Integer.valueOf(2).equals(order.getRewardVersion())) {
            log.warn("Google refund recovery deferred uid={} orderId={} reason=historical_wallet_requires_review",
                    order.getUid(), order.getGoogleOrderId());
            return false;
        }
        if (rec == null || order.getGoldAmount() == null || order.getGoldAmount() <= 0
                || !Integer.valueOf(1).equals(order.getQuantity())) return false;
        int row = rec.findRow(0, 0, order.getGoogleOrderId());
        if (row >= 0) {
            if(!knownRefund(player,order)) {
                log.error("Google refund requires review uid={} orderId={} reason=receipt_identity_mismatch",order.getUid(),order.getGoogleOrderId());
            } else if(rec.getInt(row,5)!=1) {
                log.error("Google refund requires review uid={} orderId={} status={} recovered=UNCONFIRMED forgiven=UNCONFIRMED",
                        order.getUid(),order.getGoogleOrderId(),rec.getInt(row,5));
            }
            // Even an uncertain mutation is never retried automatically. saved=true is not a completed refund.
            return true;
        }
        long before = player.getLong(PLAYER_MONEY), amount = order.getGoldAmount();
        if (before < 0) {
            log.warn("Google refund recovery deferred uid={} orderId={} reason=invalid_balance amount={} balance={}",
                    order.getUid(), order.getGoogleOrderId(), amount, before);
            return false;
        }
        // Insufficient funds: recover the remaining balance and forgive the rest permanently.
        long after = before >= amount ? before - amount : 0L;
        // Status 0 is a durable hold if a property listener leaves an unexpected balance.
        if (!rec.addRow(order.getGoogleOrderId(), order.getPurchaseTokenHash(), amount, before, after, 0)) {
            log.error("Google refund recovery deferred uid={} orderId={} reason=receipt_capacity", order.getUid(), order.getGoogleOrderId());
            return false;
        }
        row = rec.findRow(0, 0, order.getGoogleOrderId());
        try {
            player.setProperty(PLAYER_MONEY, after, IKernel.PlayerLogType.PROP_CHANGE.ordinal(),
                    "google iap refund order=" + order.getGoogleOrderId() + " amount=" + amount);
        } catch (RuntimeException failure) {
            log.warn("Google refund property listener failed uid={} orderId={} exceptionType={}",
                    order.getUid(), order.getGoogleOrderId(), failure.getClass().getSimpleName());
        }
        long actual = player.getLong(PLAYER_MONEY);
        if (actual == after) rec.setValue(row, 5, 1);
        else if (actual == before) { rec.removeRow(row); return false; }
        if(actual==after) {
            log.info("Google refund recovery uid={} orderId={} orderAmount={} recovered={} forgiven={} before={} after={} complete=true",
                    order.getUid(),order.getGoogleOrderId(),amount,before-after,amount-(before-after),before,actual);
        } else {
            log.error("Google refund requires review uid={} orderId={} orderAmount={} before={} expectedAfter={} actualAfter={} recovered=UNCONFIRMED forgiven=UNCONFIRMED complete=false",
                    order.getUid(),order.getGoogleOrderId(),amount,before,after,actual);
        }
        return true;
    }
}
