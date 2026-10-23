package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 全服唯一的 Google Play 消耗型商品订单。purchase token 只保存 SHA-256。 */
@Data
@TableName("google_play_order")
public class GooglePlayOrder {
    public static final int STATUS_VERIFIED = 1;
    public static final int STATUS_PROCESSING = 2;
    public static final int STATUS_SHIPPED = 3;
    public static final int STATUS_CONSUMED = 4;
    public static final int STATUS_MANUAL_REVIEW = 5;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String purchaseTokenHash;
    private Integer uid;
    private String packageName;
    private String productId;
    private String googleOrderId;
    private String obfuscatedAccountId;
    private Long purchaseTimeMillis;
    private Integer quantity;
    private Integer priceYuan;
    private Integer rewardVersion;
    private Long goldAmount;
    private Integer headId;
    private Integer headHours;
    private String fulfillmentHash;
    private Integer status;
    private String processingOwner;
    private Long createTime;
    private Long updateTime;
}
