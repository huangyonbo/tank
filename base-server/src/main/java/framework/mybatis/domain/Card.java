package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * @Table 实物卡
 *
 * @Column itemId item_card_jd_100 100元京东购物卡 item_card_jd_500 500元京东购物卡
 *         item_card_jd_1000 1000元京东购物卡 item_card_recharge_50 50元话费充值卡
 *         item_card_recharge_100 100元话费充值卡 item_card_recharge_200 200元话费充值卡
 *
 * @Column status 0 未兑换 1 已兑换
 *
 * @Column createTime yyyy-MM-dd HH:mm:ss
 *
 * @Column exchangeTime yyyy-MM-dd HH:mm:ss
 *
 * @Column type 0 京东卡 1 移动卡 2 联通卡 3 电信卡
 */
@Data
public class Card {

	@TableId(type= IdType.AUTO)
	private Integer id = 0; // 主键Id
	private String itemId; // 道具Id
	@TableField("ckey")
	private String key; // 卡号
	private String pwd; // 卡密
	private Integer status = 0; // 状态
	private String createTime; // 上传时间
	private String exchangeTime; // 兑换时间
	private Integer uid = 0; // 玩家ID
	private Integer type = 0; // 类型
	private String expiry; // 有效期
}
