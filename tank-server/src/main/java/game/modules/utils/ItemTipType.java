/**   
*    
F* 描述：   
* 文件：ItemTipType.java
* 创建人：胡中伟
* 创建时间：2018年6月2日 上午10:40:07 
*    
*/
package game.modules.utils;

/**   
*    
* 描述：   
*
*/
public enum ItemTipType {
	TIP_UNKNOW,
	TIP_UNLOCK_BV,     // 解锁炮值  1
	TIP_LEVEL_UP,      // 升级     2
	TIP_BUY,		   // 商城购买非人民币  3
	TIP_PAY,           // 充值购买     4
	TIP_LUCK_TURN,     // 游戏内抽奖   5
	TIP_GET_SUCCESS,   // 领取成功     6
	TIP_GET_RELIEF_GOLD, //领取救济金   7
	TIP_FUNCFISH_REWARD,//领取功能鱼金币  8
	TIP_TRANSFORM_SUCCESS,//转换成功     9
	TIP_BUY_SUCCESS,    //仅展示购买成功文本 10
	TIP_PANGU_GIFT,//盘古3日豪礼奖励         11
	TIP_CHIY_BONE,//获取任务奖励宝骨          12
	TIP_CHIYOU_REWARD,//蚩尤的召唤任务奖励     13
	TIP_OPEN_OLDGOD_BOX,//打开古神匣   14
	TIP_GOD_STATE_ACTIVATE,//开天神像全部激活 15
	TIP_BUY_EFFECT_SUCCESS,//商城数据购买加层
	NEW_YEAR_SEVEN_PKG_OPEN,
	TIP_END
}
