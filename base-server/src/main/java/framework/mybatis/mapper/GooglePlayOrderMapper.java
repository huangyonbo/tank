package framework.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import framework.mybatis.domain.GooglePlayOrder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface GooglePlayOrderMapper extends BaseMapper<GooglePlayOrder> {

    @Select("SELECT CAST(UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000 AS SIGNED)")
    Long currentTimeMillis();

    @Insert("INSERT INTO google_play_order "
            + "(purchase_token_hash, uid, package_name, product_id, google_order_id, obfuscated_account_id, "
            + "purchase_time_millis, quantity, price_yuan, reward_version, gold_amount, head_id, head_hours, "
            + "fulfillment_hash, status, processing_owner, create_time, update_time) "
            + "VALUES (#{purchaseTokenHash}, #{uid}, #{packageName}, #{productId}, #{googleOrderId}, #{obfuscatedAccountId}, "
            + "#{purchaseTimeMillis}, #{quantity}, #{priceYuan}, #{rewardVersion}, #{goldAmount}, #{headId}, #{headHours}, "
            + "#{fulfillmentHash}, #{status}, #{processingOwner}, #{createTime}, #{updateTime}) "
            + "ON DUPLICATE KEY UPDATE id = id")
    int insertIfAbsent(GooglePlayOrder order);

    @Update("UPDATE google_play_order SET "
            + "google_order_id = COALESCE(google_order_id, #{googleOrderId}), "
            + "purchase_time_millis = CASE WHEN purchase_time_millis = 0 "
            + "THEN #{purchaseTimeMillis} ELSE purchase_time_millis END "
            + "WHERE purchase_token_hash = #{tokenHash} AND uid = #{uid} "
            + "AND package_name = #{packageName} AND product_id = #{productId} "
            + "AND obfuscated_account_id = #{obfuscatedAccountId} AND quantity = #{quantity} "
            + "AND (google_order_id IS NULL OR #{googleOrderId} IS NULL "
            + "OR google_order_id = #{googleOrderId}) "
            + "AND (purchase_time_millis = 0 OR #{purchaseTimeMillis} = 0 "
            + "OR purchase_time_millis = #{purchaseTimeMillis})")
    int backfillPurchaseMetadata(@Param("tokenHash") String tokenHash,
                                 @Param("uid") Integer uid,
                                 @Param("packageName") String packageName,
                                 @Param("productId") String productId,
                                 @Param("googleOrderId") String googleOrderId,
                                 @Param("obfuscatedAccountId") String obfuscatedAccountId,
                                 @Param("purchaseTimeMillis") Long purchaseTimeMillis,
                                 @Param("quantity") Integer quantity);

    @Update("UPDATE google_play_order SET status = 2, processing_owner = #{owner}, update_time = #{now} "
            + "WHERE purchase_token_hash = #{tokenHash} AND uid = #{uid} "
            + "AND (status = 1 OR (status = 2 AND update_time < #{staleBefore}))")
    int acquire(@Param("tokenHash") String tokenHash,
                @Param("uid") Integer uid,
                @Param("owner") String owner,
                @Param("now") Long now,
                @Param("staleBefore") Long staleBefore);

    @Update("UPDATE google_play_order SET status = 1, processing_owner = NULL, update_time = #{now} "
            + "WHERE purchase_token_hash = #{tokenHash} AND uid = #{uid} "
            + "AND status = 2 AND processing_owner = #{owner}")
    int release(@Param("tokenHash") String tokenHash,
                @Param("uid") Integer uid,
                @Param("owner") String owner,
                @Param("now") Long now);

    @Update("UPDATE google_play_order SET update_time = #{now} "
            + "WHERE purchase_token_hash = #{tokenHash} AND uid = #{uid} "
            + "AND status = 2 AND processing_owner = #{owner} "
            + "AND update_time >= #{staleBefore}")
    int renewLease(@Param("tokenHash") String tokenHash,
                   @Param("uid") Integer uid,
                   @Param("owner") String owner,
                   @Param("now") Long now,
                   @Param("staleBefore") Long staleBefore);

    @Update("UPDATE google_play_order SET status = 3, processing_owner = NULL, update_time = #{now} "
            + "WHERE purchase_token_hash = #{tokenHash} AND uid = #{uid} "
            + "AND status = 2 AND processing_owner = #{owner} "
            + "AND update_time >= #{staleBefore}")
    int markShipped(@Param("tokenHash") String tokenHash,
                    @Param("uid") Integer uid,
                    @Param("owner") String owner,
                    @Param("now") Long now,
                    @Param("staleBefore") Long staleBefore);

    @Update("UPDATE google_play_order SET status = 5, processing_owner = NULL, update_time = #{now} "
            + "WHERE purchase_token_hash = #{tokenHash} AND uid = #{uid} "
            + "AND status IN (1, 2, 5)")
    int markManualReview(@Param("tokenHash") String tokenHash,
                         @Param("uid") Integer uid,
                         @Param("now") Long now);

    @Update("UPDATE google_play_order SET status = 4, processing_owner = NULL, update_time = #{now} "
            + "WHERE purchase_token_hash = #{tokenHash} AND uid = #{uid} AND status IN (3, 4)")
    int markConsumed(@Param("tokenHash") String tokenHash,
                     @Param("uid") Integer uid,
                     @Param("now") Long now);

    @Update("UPDATE google_play_order SET status = GREATEST(status, #{status}), processing_owner = NULL, "
            + "google_order_id = COALESCE(google_order_id, #{googleOrderId}), update_time = #{now} "
            + "WHERE purchase_token_hash = #{tokenHash} AND uid = #{uid} "
            + "AND package_name = #{packageName} AND product_id = #{productId} "
            + "AND (google_order_id IS NULL OR #{googleOrderId} IS NULL "
            + "OR google_order_id = #{googleOrderId}) "
            + "AND obfuscated_account_id = #{obfuscatedAccountId} AND quantity = #{quantity} "
            + "AND (purchase_time_millis = 0 OR #{purchaseTimeMillis} = 0 "
            + "OR purchase_time_millis = #{purchaseTimeMillis}) "
            + "AND price_yuan = #{priceYuan} AND reward_version = #{rewardVersion} "
            + "AND gold_amount = #{goldAmount} AND head_id = #{headId} AND head_hours = #{headHours} "
            + "AND fulfillment_hash = #{fulfillmentHash}")
    int promoteCompleted(@Param("tokenHash") String tokenHash,
                         @Param("uid") Integer uid,
                         @Param("packageName") String packageName,
                         @Param("productId") String productId,
                         @Param("googleOrderId") String googleOrderId,
                         @Param("obfuscatedAccountId") String obfuscatedAccountId,
                         @Param("purchaseTimeMillis") Long purchaseTimeMillis,
                         @Param("quantity") Integer quantity,
                         @Param("priceYuan") Integer priceYuan,
                         @Param("rewardVersion") Integer rewardVersion,
                         @Param("goldAmount") Long goldAmount,
                         @Param("headId") Integer headId,
                         @Param("headHours") Integer headHours,
                         @Param("fulfillmentHash") String fulfillmentHash,
                         @Param("status") Integer status,
                         @Param("now") Long now);
}
