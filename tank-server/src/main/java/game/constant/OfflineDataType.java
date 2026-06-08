package game.constant;

public interface OfflineDataType {
	
	/**
	 * 禁言
	 */
	int SHUT_UP = 0;
	
	/**
	 * 充值回调
	 */
	int PAY_CALL_BACK = 1;
	
	/**
	 * 购买成功
	 */
	int BUY_SUCCESS = 2;
	
	/**
	 * 购买失败，返还货币
	 */
	int BUY_FAILED = 3;
	
	/**
	 * 购买失败，返还道具
	 */
	int BUY_FAILED_ITEM = 4;
	
	/**
	 * 购买失败，返还次数
	 */
	int BUY_FAILED_COUNT = 5;
	
	/**
	 * 返还兑换限制次数
	 */
	int BACK_LIMIT_COUNT = 6;
	/**
	 * 兑换码兑换失败
	 */
	int REDEEMCODE_FAILED = 7;
	/**
	 * 使用兑换券失败
	 */
	int USE_CARD_FAILED = 8;
	/**
	 * 实名认证成功
	 */
	int REAL_NAME_SUCCESS = 9;
	/**
	 * 绑定渠道
	 */
	int BIND_CHANNEL = 10;
	/**
	 * 修改密码
	 */
	int CHANGE_PASSWORD = 11;
	/**
	 * 绑定手机号
	 */
	int BIND_PHONE = 12;
	/**
	 * 解绑手机号
	 */
	int UN_BIND_PHONE = 13;
	/**
	 * 返还兑换卡兑换限制次数
	 */
	int BACK_CARD_LIMIT_COUNT = 14;
	
	/**
	 * 绑定代理商
	 */
	int BIND_PROXY = 15;

	/**
	 * 同意离线玩家申请入会
	 */
	int ALLOW_OFFLINE_PLAYER_JOIN_GUILD = 16;

	int THREE_SELECT_ONE_OFFLINE_REWARD = 17;
	int THREE_BMBC_REWARD = 18;
	int FQZS_OFFLINE_REWARD = 19;
	int BMBC_OFFLINE_REWARD = 20;
	int FQZS_OFFLINE_DEDUCTION = 21;
	int BMBC_OFFLINE_DEDUCTION = 22;

    int BRNN_OFFLINE_REWARD = 23;
    int BRNN_OFFLINE_DEDUCTION = 24;
}
