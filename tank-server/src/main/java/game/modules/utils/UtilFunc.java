/**
*
* 描述：   工具类
* 文件：UtilFunc.java
* 创建人：胡中伟
* 创建时间：2018年4月10日 上午10:28:17
*
*/
package game.modules.utils;

import com.google.gson.JsonObject;
import framework.MathUtils;
import framework.PropertyKey;
import framework.game.IGameObject;
import framework.game.IKernel;
import framework.game.IRecord;
import framework.game.ValueType;
import game.custommsg.CustomMsg;
import game.custommsg.S2CMsgDef;
import game.custommsg.ServerCodeDef;
import game.modules.RoomModule;
import game.modules.activities.code.JSONResult;
import game.modules.fishgame.FishModule;
import game.modules.fishgame.FishModule.FishData;
import io.netty.util.internal.StringUtil;
import lombok.Getter;
import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 描述：
 */
public class UtilFunc implements PropertyKey{
	private static Logger logger = LoggerFactory.getLogger(UtilFunc.class);
	@Getter
	public enum System {
		BAG("背包系统"),
		BAG_COMBINE("背包合成"),
		CARD_ITEM("兑换卡"),
		CHRISTMAS("圣诞礼包"),
		DAILY_DRAW("每日抽奖"),
		HIT_EGG("砸金蛋"),
		NATION_DAY("国庆节"),
		TICKET_TASK("彩券任务"),
		NEWP_GIFT("新手礼包"),
		RECHARGE_REBATE("充值返利"),
		RELIEF_GOLD("救济金"),
		SEVEN_DAY("七日礼包"),
		SIGN_IN("签到"),
		ARENA("竞技场"),
		BOSS_BATTLE("BOSS战"),
		BULLET_VALUE("炮值"),
		BUY_FUNC_ITEM("购买功能道具"),
		COLOR_DRAW("彩金抽奖"),
		KILL_FISH("击杀掉落"),
		MYSTERY_GIFT("神秘礼包"),
		ONLINE_GIFT("在线领奖"),
		ROBOT("机器人"),
		SCORE_ITEM("积分道具"),
		STORE_STONE("灵石商店"),
		STORE("商店"),
		VIP_BUY_STORE_GOODS("贵族积分商店"),
		BUY_STORE_GOODS("兑换商城"),
		BUY_FAILED_BACK("兑换失败返回"),
		DICE_TASK("骰子任务"),
		NEWP_TASK("新手任务"),
		ACHIEVEMENT("成就"),
		ROLE_DUMP("老角色导入"),
		GM_CMD("GM命令"),
		LEVEL_REWARD("等级奖励"),
		MAIL("系统邮件"),
		MONTH_CARD("月卡"),
		BACK_SERVER("后台反馈赠送"),
		BACK_DEDUCT_ITEM("后台扣除"),
		REAL_NAME("实名认证"),
		REDEEM_CODE("兑换码兑换"),
		VIP("VIP"),
		PLAYER("玩家"),
		NewYearRed("新年红包"),
		NewYearWord("新年集字"),
		NewYearCharge("新年充值"),
		CHOOSE_SEAT("至尊选座报名费"),
		SEND_ITEM("赠送道具"),
		BOMB_AMMO_TRANSFORM("三叉戟转魔晶"),
		AMMO_TRANSFORM_TO_BOMB("魔晶转三叉戟"),
		N_BOMB_SMITH_ING("炮台锻造"),
		ITEM_SCORE("道具积分"),
		BOMB_MONSTER("炸怪兽"),
		MONOPOLY("大富翁"),
		EXCHANGE_STONE_TIMES("兑换石兑换"),
		CHANGE_DAY("跨天"),
		LUCKY_CARD("幸运翻牌"),
		RANK("排行榜奖励"),
		ACTIVITY("活动邮件"),
		BIND("绑定"),
		MASTER("master命令"),
		VIP_INTEGRAL("贵族积分任务"),
		DAILY_TASK("每日任务"),
		WEEKLY_TASK("每周任务"),
		PIGGY_BANK("存金罐"),
		NEWER_WELFARE("新手福利"),
		PAY_DISCOUNTS("充值优惠"),
		TIMER_AWARD("定时奖励"),
		BOX_COMPOUND("宝箱合成"),
		BOX_OPEN("宝箱开启"),
		BAG_OPEN("福袋开启"),
		TREASURE_BOWL_UP("聚宝盆一键升级"),
		TREASURE_BOWL_REFRESH("聚宝盆刷新"),
		TREASURE("聚宝"),
		STORE_CONTROL("商店话费券控制"),
		FUNC_FISH_REWARD("玩法鱼奖励"),
		FUNC_FISH_SKILL("技能鱼奖励"),
		FORCE_CHANGE("命令行强制修改"),
		AUTO_USE_FAILED("自动使用失败"),
		RECYCLE_ITEM("回收道具"),
		STORE_GUILD_REPOSITORY("存入仓库"),
		FETCH_GUILD_REPOSITORY("取出仓库"),
		ACCUM_RECHARGE("累计充值活动"),
		DOUBLE_RECHARGE("双节充值大返利"),
		OVERVALUE_FUND("超值基金"),
		LUCKY_PUZZLE("幸运拼图"),
		NAVIGATION_LEVEL_AWARD("航海指南等级奖励"),
		ROOKIE_ACT_AWARD("新手留存活动奖励"),
		ACCUM_RECHARGE_GOING_MERRY("累计充值送暗金梅丽奖励"),
		DOUBLE11_EXCHANGE_MALL("双十一兑换商城"),
		RECOVER_FISHPOND_FISH("养鱼池收获鱼奖励"),
		CATCH_FISHPOND_FISH("养鱼池捕获鱼奖励"),
		FISH_POND_REFRESH("养鱼池刷新"),
		MERMAID_TREASURE("美人鱼秘宝"),
		TREASURE_IN_SEA("贵族轮盘活动"),
		NEW_PLAYER_LUCKY_CARD("新手七日福卡活动"),
		NEW_PLAYER_LUCKY_CARD_MALL("新手七日福卡活动商城"),
		DEBRIS_PKG("三叉戟红包活动"),
		RANDOM_ITEM("随机道具"),
		DAILY_VALUE_SKILL_PKG(" 每天超值技能大礼包"),
		NEW_PLAYER_SEVEN_DAY_PKG("新手七天乐"),
		ENTER_GOLD_ROOM_COST("进入金币房间消耗"),
		ENTER_SEA_ROOM_COST("进入海神殿房间消耗"),
		GET_PANGU_GIFT("盘古三日惊喜礼包"),
		GET_BAREBONE_AWARD("打开宝骨奖励"),
		EXCHANGE_EXPLOITDS_AWARD("战功章兑换奖励"),
		GET_SHANHAI_ANIMAL("山海异兽礼包"),
		OPEN_OLDE_GOD("打开古神匣"),
		ADD_SHANGGU_LEVEL("升级上古血脉"),
		TPR_COST("龟相来贺消耗"),
		LDK_COST("龙王遗赠消耗"),
		GET_FISHROMMTASK("渔场任务奖励"),
		GET_GOODS_COUPONS("商城点券购买商品"),
		GET_GOODS_ACTIVITY_COUPONS("活动点券购买商品"),
		GET_STORE_FREE_GOLD("商城免费获取金币"),
		ADD_TAIGU_LEVEL("升级太古血脉"),
		ADD_GOD_COUNT("添加开天神像部件投放"),
		VIP_LEVEL_GIFTS("VIP成长礼包"),
		SPIRIT_STONE_FIGHT("灵石大作战"),
		NEW_YEAR_SEVEN_SIGN("新春七天乐"),
		NEW_YEAR_SEVEN_SIGN_PKG("万事如意礼包"),
		NEW_YEAR_YAN_DI_TASK_REWARD("炎帝迎新春任务奖励"),
		NEW_YEAR_TREASURY_YANDI("炎帝宝库"),
		TIME_LIMIT_BURNOUT("限时累充"),
		DIRECT_PURCHASE_GIFT_PACK("72小时直购礼包"),
		THREE_SELECT_ONE_REWARD("三选一奖励"),
		BMBC_REWARD("BMBC"),
		FQZS_REWARD("FQZS"),
		BRNN_REWARD("百人牛牛"),
		NULL("未知类型");
		String label;

		System(String label) {
			this.label = label;
		}

		public static String getLabel(int system) {
			return Arrays.stream(values()).filter(s -> s.ordinal() == system).findAny().orElse(NULL).label;
		}
	}

	// 跑马灯类型
	public enum BroadCastType {
		NORMAL,    // 0-普通
		SYSTEM,        // 1-系统
		NOTICE,        // 2-公告
		WORLD,        // 3-世界
		MOJIN,        // 4-魔晶场
		FISNPOND,        // 5-养鱼池
		MERMAID_TREASURE, // 6-美人鱼秘宝
		PANGU_SKY//3日
	}

	// 关服的时候需要执行的方法模块
	public enum StopListenerOrder {
		NONE,
		ROOM_MODULE,
		GUILD_MODULE,
		LIST_MODULE,
		ARENA_MODULE,
		BOSS_BATTLE,
		FISH_POND_ACT,
		NEW_PLAYER_LUCKY_CARD,
		SPIRIT_STONE_FIGHT,
	}

	public enum JsonType{
		S2C_BIG_KUN_DISC,//巨鲲轮盘奖励
	}
	public static void sendSerCode(IKernel kernel, IGameObject player, int msgid, ServerCodeDef code) {
		CustomMsg.ServerCode.Builder build = CustomMsg.ServerCode.newBuilder();
		build.setCode(code.ordinal());

		kernel.sendMessage(player, msgid, build.build().toByteArray());
	}

	public static void responseSerCode(IKernel kernel, IGameObject player, int reqid, ServerCodeDef code) {
		CustomMsg.ServerCode.Builder build = CustomMsg.ServerCode.newBuilder();
		build.setCode(code.ordinal());
		kernel.response(player, reqid, build.build().toByteArray());
	}

    public static void responseSerCodeStr(IKernel kernel, IGameObject player, int reqid, ServerCodeDef code) {
        CustomMsg.String.Builder build = CustomMsg.String.newBuilder();
        JSONResult jsonObject = new JSONResult();
        jsonObject.put("code", code.ordinal());
        build.setValue(jsonObject.toJSONString());
        kernel.response(player, reqid, build.build().toByteArray());
    }


	/**
	 * 获取一周开始的日期[星期一]
	 *
	 * @param timestamp
	 * @return
	 */
	public static long weekBegin(long timestamp) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(timestamp);
		int dayWeek = cal.get(Calendar.DAY_OF_WEEK);
		if (1 == dayWeek) {
			cal.add(Calendar.DAY_OF_MONTH, -1);
		}
		cal.setFirstDayOfWeek(Calendar.MONDAY);
		int day = cal.get(Calendar.DAY_OF_WEEK);
		// 根据日历的规则，给当前日期减去星期几与一个星期第一天的差值
		cal.add(Calendar.DATE, cal.getFirstDayOfWeek() - day);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTimeInMillis();
	}

	// 时:分:秒 =>long
	public static int timeParse(String fmt) throws ParseException {
		String[] str = fmt.split(":");
		if (str.length != 3) {
			return 0;
		}
		int hour = Integer.parseInt(str[0]);
		int min  = Integer.parseInt(str[1]);
		int sec  = Integer.parseInt(str[2]);
		return hour * 3600000 + min * 60000 + sec * 1000;
	}

	//校验正整数
	public static boolean isPosInteger(String str) {
		Pattern pattern = Pattern.compile("[1-9]\\d*");
		if (str != null && !str.equals("")) {
			Matcher matcher = pattern.matcher(str);
			return matcher.matches();
		}
		return false;
	}

	public static long getZeroTime(long time) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date(time));
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime().getTime();
	}
	public static long getWeekZeroTime(long time) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date(time));
		calendar.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime().getTime();
	}

	public static long getMonthZeroTime(long time) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date(time));
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime().getTime();
	}

	public static long getHourTime(long time) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date(time));
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime().getTime();
	}

	// 0-6 ： 日一二三四五六
	public static int getDayOfWeek(long time) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date(time));
		return calendar.get(Calendar.DAY_OF_WEEK) - 1;
	}

	public static boolean isSameDay(long date1, long date2) {
		return getZeroTime(date1) == getZeroTime(date2);
	}

	/**
	 * 向客户端发送提示框
	 *
	 * @param kernel
	 * @param player
	 * @param type
	 * @param objects
	 */
	public static void sendItemTips(IKernel kernel, IGameObject player, ItemTipType type, Object... objects) {
		if (objects.length <= 1 || objects.length % 2 != 0 || objects[0] == null) {
			return;
		}
		CustomMsg.ItemTip.Builder itemTip = CustomMsg.ItemTip.newBuilder();
		itemTip.setType(type.ordinal());
		for (int i = 0; i < objects.length / 2; ++i) {
			String item = (String) objects[i * 2];
			if (item == null) {
				continue;
			}
			Object numObj = objects[i * 2 + 1];
			int count = (int) objects[i * 2 + 1];
			String script = kernel.getCfgProperty(item, "Script");
			if (script == null) {
				continue;
			}
			String autoUse = "0";
			if (kernel.getCfgProperty(item, "AutoUse") != null) {
				autoUse = kernel.getCfgProperty(item, "AutoUse").toLowerCase();
			}
			boolean bAutoUse = !(StringUtils.equals(autoUse, "false") || StringUtils.equals(autoUse, "0"));
			if (script.equals("RandomItem") && bAutoUse) {
				// 自动使用的随机包，由随机包自身提示获得
				continue;
			}
			itemTip.addItemId(item);
			itemTip.addItemCount(count);
		}

		if (itemTip.getItemIdCount() > 0) {
			kernel.sendMessage(player, S2CMsgDef.S2C_ITEM_TIP.ordinal(), itemTip.build().toByteArray());
		}
	}

	// 滚动消息
	public static void sendScrollMsg(IKernel kernel, IGameObject player, String txtid, int count, String... params) {
		CustomMsg.ScrollMsg.Builder scrollMsg = CustomMsg.ScrollMsg.newBuilder();
		scrollMsg.setCount(count);
		scrollMsg.setTxtid(txtid);
		scrollMsg.addParams(kernel.getServer().getTimeFormat().format(kernel.getServerTime()));
		for (String str : params) {
			scrollMsg.addParams(str);
		}
		kernel.sendMessage(player, S2CMsgDef.S2C_SCROLL_MSG.ordinal(), scrollMsg.build().toByteArray());
	}

	// 滚动消息-本服广播
	public static void broadCastScrollMsg(IKernel kernel, String txtid, int count, String... params) {
		CustomMsg.ScrollMsg.Builder scrollMsg = CustomMsg.ScrollMsg.newBuilder();
		scrollMsg.setCount(count);
		scrollMsg.setTxtid(txtid);
		scrollMsg.addParams(kernel.getServer().getTimeFormat().format(kernel.getServerTime()));
		for (String str : params) {
			scrollMsg.addParams(str);
		}
		kernel.broadCastCurServer(S2CMsgDef.S2C_SCROLL_MSG.ordinal(), scrollMsg.build().toByteArray());
	}

	// 滚动消息-本服广播
	public static void broadCastScrollMsg2(IKernel kernel, String txtid, int count, String... params) {
		CustomMsg.ScrollMsg.Builder scrollMsg = CustomMsg.ScrollMsg.newBuilder();
		scrollMsg.setCount(count);
		scrollMsg.setTxtid(txtid);
		scrollMsg.addParams(kernel.getServer().getTimeFormat().format(kernel.getServerTime()));
		for (String str : params) {
			scrollMsg.addParams(str);
		}
		kernel.broadCastCurServer(S2CMsgDef.S2C_SCROLL_MSG.ordinal(), scrollMsg.build().toByteArray());
	}

	// 滚动消息-全服广播
	public static void broadCastScrollMsgAllServer(IKernel kernel, String txtid, int count, String... params) {
		CustomMsg.ScrollMsg.Builder scrollMsg = CustomMsg.ScrollMsg.newBuilder();
		scrollMsg.setCount(count);
		scrollMsg.setTxtid(txtid);
		scrollMsg.addParams(kernel.getServer().getTimeFormat().format(kernel.getServerTime()));
		for (String str : params) {
			scrollMsg.addParams(str);
		}
		logger.info("跑马灯消息  {}  {} {} {} {} {} {} {} {} {} {}", params);
		kernel.broadCastAllServer(S2CMsgDef.S2C_SCROLL_MSG.ordinal(), scrollMsg.build().toByteArray());
	}

	// 滚动消息-桌子广播
	public static void broadCastScrollMsgByDesk(IKernel kernel, IGameObject desk, String txtid, int count,
												String... params) {
		CustomMsg.ScrollMsg.Builder scrollMsg = CustomMsg.ScrollMsg.newBuilder();
		scrollMsg.setCount(count);
		scrollMsg.setTxtid(txtid);
		scrollMsg.addParams(kernel.getServer().getTimeFormat().format(kernel.getServerTime()));
		for (String str : params) {
			scrollMsg.addParams(str);
		}
		kernel.broadCastByDesk(desk, S2CMsgDef.S2C_SCROLL_MSG.ordinal(), scrollMsg.build().toByteArray());
	}

	// 滚动消息-渠道广播
	public static void broadCastScrollMsgByChannel(IKernel kernel, int channel, String txtid, int count,
												   String... params) {
		CustomMsg.ScrollMsg.Builder scrollMsg = CustomMsg.ScrollMsg.newBuilder();
		scrollMsg.setCount(count);
		scrollMsg.setTxtid(txtid);
		scrollMsg.addParams(kernel.getServer().getTimeFormat().format(kernel.getServerTime()));
		for (String str : params) {
			scrollMsg.addParams(str);
		}
		kernel.broadCastByChannel(channel, S2CMsgDef.S2C_SCROLL_MSG.ordinal(), scrollMsg.build().toByteArray());
	}

	// 滚动消息-渠道广播
	public static void broadCastScrollMsg2ByChannel(IKernel kernel, int channel, String txtid, int count,
													String... params) {
		CustomMsg.ScrollMsg.Builder scrollMsg = CustomMsg.ScrollMsg.newBuilder();
		scrollMsg.setCount(count);
		scrollMsg.setTxtid(txtid);
		scrollMsg.addParams(kernel.getServer().getTimeFormat().format(kernel.getServerTime()));
		for (String str : params) {
			scrollMsg.addParams(str);
		}
		kernel.broadCastByChannel(channel, S2CMsgDef.S2C_SCROLL_MSG.ordinal(), scrollMsg.build().toByteArray());
	}

	public static boolean dieOneFish(IRecord fishList, FishData fishData, int pos, FishModule fishModule,
									 IRecord fishCount, long now) {
		return true;
	}

	public static void addOneFish(int fishType, IRecord rec) {
		int pos = rec.findRow(0, 0, fishType);
		if (pos != -1) {
			int num = rec.getInt(pos, 1) + 1;
			rec.setValue(pos, 1, num);
		}
	}

	public static void subOneFish(int fishType, IRecord rec) {
		int pos = rec.findRow(0, 0, fishType);
		if (pos != -1) {
			rec.setValue(pos, 1, rec.getInt(pos, 1) - 1);
		}
	}

	public static void showTip(IKernel kernel, IGameObject player, String textid, String... args) {
		CustomMsg.MessageTip.Builder itemTip = CustomMsg.MessageTip.newBuilder();
		itemTip.setTextid(textid);
		for (int i = 0; i < args.length; ++i) {
			itemTip.addArgs(args[i]);
		}
		kernel.sendMessage(player, S2CMsgDef.S2C_MESSAGE_TIP.ordinal(), itemTip.build().toByteArray());
	}
	public static void showTipByJson(IKernel kernel, IGameObject player,int msgID, String str) {
		CustomMsg.String.Builder builder = CustomMsg.String.newBuilder();
		builder.setValue(str);
		kernel.sendMessage(player, msgID, builder.build().toByteArray());
	}

	public static void parseMapStr(String str, Map<String, Integer> map, String p1, String p2) {
		String[] arr1 = str.split(p1);
		for (int i = 0; i < arr1.length; ++i) {
			String[] arr2 = arr1[i].split(p2);
			if (arr2.length != 2) {
				continue;
			}

			String key = arr2[0];
			int val = Integer.parseInt(arr2[1]);

			if (map.containsKey(key)) {
				map.put(key, map.get(key) + val);
			} else {
				map.put(key, val);
			}
		}
	}

	public static int[] parseIntArray(String str, String sp) {
		if (StringUtil.isNullOrEmpty(str)) {
			return null;
		}

		String[] res = str.split(sp);
		int[] array = new int[res.length];
		try {
			for (int i = 0; i < res.length; ++i) {
				array[i] = Integer.parseInt(res[i]);
			}
		} catch (NumberFormatException exp) {
			return null;
		}

		return array;
	}

	public static boolean isRunBuys(int channel) {// 是否群包
		if (channel == 164 || channel == 0 || channel == 151)
			return true;
		return false;
	}

	public static Object getValueByType(String value, ValueType valueType) {
		Object val = null;
		switch (valueType) {
			case SHORT:
				val = Short.parseShort(value);
				break;
			case INT:
				val = Integer.parseInt(value);
				break;
			case LONG:
				val = Long.parseLong(value);
				break;
			case BOOL:
				val = StringUtils.equals(value.toLowerCase(), "true");
				break;
			case FLOAT:
				val = Float.parseFloat(value);
				break;
			case DOUBLE:
				val = Double.parseDouble(value);
				break;
			case STRING:
				val = value;
				break;
			case OBJECT:
				val = value;
				break;
			default:
				break;
		}
		return val;
	}

	/**
	 * 通过身份证获取年龄
	 *
	 * @param idCard
	 * @return
	 */
	public static int getAgeByIdCard(String idCard) {
		if (idCard == null || idCard.length() < 18) {
			return 1;
		}
		try {
			String year = idCard.substring(6, 10);
			String month = idCard.substring(10, 12);
			String day = idCard.substring(12, 14);
			int _year = Integer.parseInt(year);
			int _month = Integer.parseInt(month);
			int _day = Integer.parseInt(day);
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(new Date());
			int y = calendar.get(Calendar.YEAR);
			int m = calendar.get(Calendar.MONTH) + 1;
			int d = calendar.get(Calendar.DAY_OF_MONTH);
			int age = y - _year - 1; // 过了年月生日后+1岁
			if (m > _month) {
				age++;
			} else if (m == _month && d >= _day) {
				age++;
			}
			return age;
		} catch (Exception e) {

		}
		return 1;
	}

	public static void respRpcStringToClient(IKernel kernel, IGameObject player, int reqId, String json) {
		CustomMsg.String.Builder builder = CustomMsg.String.newBuilder();
		builder.setValue(json);
		kernel.response(player, reqId, builder.build().toByteArray());
	}

	// 发送给客户端返回码
	public static void sendResponseCode(IKernel kernel, IGameObject player, int reqId, int code) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("code", code);
		CustomMsg.String.Builder builder = CustomMsg.String.newBuilder();
		builder.setValue(jsonObject.toString());
		kernel.response(player, reqId, builder.build().toByteArray());
	}

	public static void respNotifyStringClient(IKernel kernel, IGameObject player, int msgId, String json) {
		CustomMsg.String.Builder builder = CustomMsg.String.newBuilder();
		builder.setValue(json);
		kernel.sendMessage(player, msgId, builder.build().toByteArray());
	}

	public static int randomPath(IGameObject desk, FishData fishData, int max) {
		int path = -1;
		max = max == 0 ? 10 : max;
		if (fishData.path != null && fishData.path.length > 0) {
			List<Integer> listPath = desk.getRecord("OutFishPathRec").getValue(fishData.enumType - 1, 0);
			List<Integer> temp = new ArrayList<Integer>();
			for (int i = 0; i < fishData.path.length; i++) {
				if (!listPath.contains(fishData.path[i])) {
					temp.add(fishData.path[i]);
				}
			}
			if (temp.size() == 1) {
				path = temp.get(0);
			} else if (temp.size() > 1) {
				int index = MathUtils.random(temp.size());
				path = temp.get(index);
			}
			if (fishData.path.length > max) {
				listPath.add(path);
				if (listPath.size() > max) {
					listPath.remove(0);
				}
			}
		}
		return path;
	}

	/**
	 * 2020-08-29
	 * 目前只有传说、至尊、传说碎片、至尊碎片有道具积分
	 *
	 * @param itemId 道具id
	 * @return 道具积分
	 */
	public static int bomb2ItemScore(IKernel kernel, String itemId) {
		if (itemId.equals("item_skill_hbomb") || itemId.equals("item_skill_nbomb") || itemId.equals("item_debris_hbomb")
				|| itemId.equals("item_debris_nbomb")) {
			String tmp = kernel.getCfgProperty(itemId, "ItemScore");
			return tmp == null ? 0 : Integer.parseInt(tmp);
		}
		return 0;
	}

	public static long bomb2MoJin(String itemId, long count) {
		if (count < 0) {
			return 0L;
		}
		if (itemId.equals("item_skill_hbomb")) {
			return 5000000L * count;
		} else if (itemId.equals("item_skill_nbomb")) {
			return 2500000L * count;
		} else if (itemId.equals("item_debris_hbomb")) {
			return 500000L * count;
		} else if (itemId.equals("item_debris_nbomb")) {
			return 250000L * count;
		}
		if ("item_bombcoin_1".equals(itemId) || "BombCoin".equals(itemId)) {
			return count;
		}
		return 0L;
	}


	/**
	 * @param player 玩家对象
	 * @return 传说、至尊、传说碎片、至尊碎片
	 */
	public static String getHN(IGameObject player) {
		if (player == null) {
			return "";
		}
		StringBuffer playerHas = new StringBuffer();
		IGameObject itemBag = player.getContainer("ItemBag");
		int nbomb = getItemCountByName(itemBag, ITEM_PROPERTY_SKILL_NBOMB);
		int hbomb = getItemCountByName(itemBag, ITEM_PROPERTY_SKILL_HBOMB);
		int debrisnbomb = getItemCountByName(itemBag, ITEM_PROPERTY_DEBRIS_NBOMB);
		int debrishbomb = getItemCountByName(itemBag, ITEM_PROPERTY_DEBRIS_HBOMB);
		append(playerHas, nbomb, "至尊");
		append(playerHas, hbomb, "传说");
		append(playerHas, debrisnbomb, "至尊碎片");
		append(playerHas, debrishbomb, "传说碎片");
		return "".equals(playerHas.toString()) ? "-" : playerHas.toString();
	}

	private static void append(StringBuffer buffer, long num, String tail) {
		if (num > 0) {
			if (buffer.length() > 0) {
				buffer.append("、");
			}
			buffer.append(num).append("个").append(tail);
		}
	}

	public static int getItemCountByName(IGameObject itemBag, String itemName) {
		if (itemBag == null) {
			return 0;
		}
		int count = 0;
		List<IGameObject> items = itemBag.findChildObjById(itemName);
		for (IGameObject item : items) {
			count += item.getInt("Count");
		}
		return count;
	}

	public static boolean checkChineseChar(String str) {
		Pattern p = Pattern.compile("[\u4e00-\u9fa5]");
		Matcher m = p.matcher(str);
		return m.find();
	}

	public static int getInt(List<Object> objs, int index) {
		Object obj = objs.get(index);
		if (obj != null) {
			return (int) obj;
		}
		return 0;
	}

	public static float getFloat(List<Object> objs, int index) {
		Object obj = objs.get(index);
		if (obj != null) {
			return (float) obj;
		}
		return 0;
	}

	public static String getString(List<Object> objs, int index) {
		Object obj = objs.get(index);
		if (obj != null) {
			return obj.toString();
		}
		return "";
	}

	private static boolean isContainChinese(String str) {
		String regEx = "[\\u4e00-\\u9fa5]+";
		Pattern p = Pattern.compile(regEx);
		Matcher m = p.matcher(str);
		return m.find();
	}

	//通过字节码进行判断
	public static boolean isChinese(char c) {
		return c >= 0x4E00;
	}

	public static IGameObject getDesk(IKernel kernel, IGameObject player) {
		IGameObject desk = kernel.getGameObject(player.getLong(PLAYER_PROPERTY_DESKID));
		if (desk == null) {
			logger.debug("desk is null");
			return null;
		}
		return desk;
	}

	public static boolean recordWin(IGameObject desk, IGameObject player, long win) {
		if (desk == null || player == null || win <= 0) {
			return false;
		}
		IGameObject room = desk.getParent();
		if (room == null) {
			return false;
		}
		int roomType = room.getInt(DESK_TYPE_KEY);
		IRecord totalPw = player.getRecord("TotalPlayWin");

		// 房间总赢
		room.setProperty(PLAYER_PROPERTY_TOTALWIN, room.getLong(PLAYER_PROPERTY_TOTALWIN) + win);
		// 房间当天总赢
		room.setProperty("TodayWin", room.getLong("TodayWin") + win);

		// 玩家总赢 （这个是传入算法的玩家总赢）
		totalPw.setValue(roomType, 1, totalPw.getLong(roomType, 1) + win);
		return true;
	}

	/**
	 * 找不到返回-1
	 * @param kernel
	 * @param player
	 * @return
	 */
	public static int getRoomType(IKernel kernel, IGameObject player) {
		IGameObject desk = getDesk(kernel, player);
		if (desk == null) {
			return -1;
		}
		return desk.getInt(DESK_TYPE_KEY);
	}
}
