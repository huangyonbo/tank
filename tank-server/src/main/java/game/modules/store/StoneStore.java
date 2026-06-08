/**   
*    
* 描述：   灵石商店
* 文件：StoneStore.java
* 创建人：胡中伟
* 创建时间：2018年9月17日 上午10:33:27 
*    
*/
package game.modules.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import framework.pub.IPubData;
import framework.pub.IPubRecord;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import common.ServerMsg;
import common.ServerMsgDef;
import framework.game.ICfgReader;
import framework.game.IGameObject;
import framework.game.IKernel;
import framework.game.ILogicModule;
import framework.game.KernelEvent;
import framework.game.ValueType;
import game.constant.OfflineDataType;
import game.custommsg.C2SMsgDef;
import game.custommsg.CommandDef;
import game.custommsg.CustomMsg;
import game.custommsg.RequestMsgDef;
import game.custommsg.S2CMsgDef;
import game.custommsg.ServerCodeDef;
import game.modules.OfflineDataModule;
import game.modules.items.ItemModule;
import game.modules.items.PropertyItem;
import game.modules.player.VipModule;
import game.modules.utils.ItemTipType;
import game.modules.utils.UtilFunc;
import game.util.TimeUtils;

/**
 * 
 * 描述：
 * 
 */
public class StoneStore implements ILogicModule {

	enum ITEM_STONE_COL {
		COL_ITEM_ID, COL_PRICE, COL_STOCK, COL_VERSION,

		COL_END
	}

	class PkgData {
		String item;
		int count;
		int weight;
	}

	class RandPkg {
		int totalWeight = 0;
		List<PkgData> list = new ArrayList<>();

		public PkgData Random() {
			int val = (int) Math.floor(Math.random() * totalWeight);
			int weight = 0;
			for (int i = 0; i < list.size(); ++i) {
				PkgData data = list.get(i);
				weight += data.weight;
				if (weight > val) {
					return data;
				}
			}
			return null;
		}
	}

	class RealData {
		int min;
		int max;
		RandPkg item;
	}

	Map<String, List<RealData>> m_mapRealDatas = new HashMap<>();

	VipModule m_VipModule = null;
	ItemModule m_ItemModule = null;
	OfflineDataModule m_OfflineDataModule = null;
	private List<String> m_bombId = Arrays.asList("item_skill_bind_bomb", "item_skill_normal_bomb",
			"item_skill_missile", "item_skill_nbomb", "item_skill_hbomb");
	private static final String STONE_2_BRONZE_EXCHANGE_COUNT = PLAYER_PROPERTY_STONE2BRONZEEXCHANGECOUNT;
	private static final String STONE_2_SILVER_EXCHANGE_COUNT = PLAYER_PROPERTY_STONE2SILVEREXCHANGECOUNT;
	private static final String STONE_2_GOLD_EXCHANGE_COUNT = PLAYER_PROPERTY_STONE2GOLDEXCHANGECOUNT;
	private static final String STONE_2_PLATINUM_EXCHANGE_COUNT = PLAYER_PROPERTY_STONE2PLATINUMEXCHANGECOUNT;
	private static final String STONE_2_DIAMOND_EXCHANGE_COUNT = PLAYER_PROPERTY_STONE2DIAMONDEXCHANGECOUNT;
	private Map<String, String> m_mapitemProperty = new HashMap<>();
	private static final String ITEM_EXCHANGE_STONE = "item_exchangestone";
	private List<String> m_discountsBombId = Arrays.asList("item_skill_missile", "item_skill_nbomb",
			"item_skill_hbomb");

	private static Logger logger = LoggerFactory.getLogger(StoneStore.class);

	/**
	 * @param kernel
	 * @return
	 */
	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
		kernel.regEvent(KernelEvent.KEVENT_ON_LINE, "Player", this, "OnPlayerOnLine");

		kernel.listenPropertyChange(PLAYER_PROPERTY_VIPLEVEL, "Player", this, "OnVipLevelChanged");

		kernel.regRequestMessage(RequestMsgDef.REQ_STONE_EXCHANGE.ordinal(), this, "OnReqExStone");
		kernel.regRequestMessage(RequestMsgDef.REQ_TRANSFORM_TO_STONE_TIMES.ordinal(), this,
				"OnReqTransformToStoneTimes");// 兑换石转灵石

		kernel.regClientMessage(C2SMsgDef.C2S_REQ_STONE_STOCK.ordinal(), this, "OnReqStoneStock");
		kernel.regCommand(CommandDef.CMD_CHANGE_DAY.ordinal(), "Player", this, "OnChangeDay");
		m_VipModule = (VipModule) kernel.getModule("VipModule");
		m_ItemModule = (ItemModule) kernel.getModule("ItemModule");
		m_OfflineDataModule = (OfflineDataModule) kernel.getModule("OfflineDataModule");
		LoadConfig(kernel);
		// initPropertyItem();
		return true;
	}

	/**
	 * 
	 */
	@Override
	public void onDestroy() {
	}

	void initPropertyItem() {
		if (m_bombId.size() > 0) {
			m_mapitemProperty.put(m_bombId.get(0), STONE_2_BRONZE_EXCHANGE_COUNT);
		}
		if (m_bombId.size() > 1) {
			m_mapitemProperty.put(m_bombId.get(1), STONE_2_SILVER_EXCHANGE_COUNT);
		}
		if (m_bombId.size() > 2) {
			m_mapitemProperty.put(m_bombId.get(2), STONE_2_GOLD_EXCHANGE_COUNT);
		}
		if (m_bombId.size() > 3) {
			m_mapitemProperty.put(m_bombId.get(3), STONE_2_PLATINUM_EXCHANGE_COUNT);
		}
		if (m_bombId.size() > 4) {
			m_mapitemProperty.put(m_bombId.get(4), STONE_2_DIAMOND_EXCHANGE_COUNT);
		}
	}

	RandPkg ParsePkg(String strPkg) {
		RandPkg pkg = new RandPkg();
		String[] rands = strPkg.split(";");
		for (int j = 0; j < rands.length; ++j) {
			String[] wd = rands[j].split(":");
			if (wd.length != 2) {
				logger.error("ParsePkg parse pkg failed: {}", strPkg);
				continue;
			}

			String[] its = wd[0].split("\\*");
			if (its.length != 2) {
				logger.error("ParsePkg parse pkg failed: {}", strPkg);
				continue;
			}

			PkgData data = new PkgData();
			data.item = its[0];

			try {
				data.count = Integer.parseInt(its[1]);
				data.weight = Integer.parseInt(wd[1]);
			} catch (NumberFormatException e) {
				continue;
			}

			pkg.totalWeight += data.weight;
			pkg.list.add(data);
		}
		return pkg;
	}

	void LoadConfig(IKernel kernel) {
		ICfgReader cfg = kernel.loadXmlConfig("res/StoreItems/StoreStone.xml");
		int count = cfg.getItemCount();
		for (int i = 0; i < count; ++i) {
			String id = cfg.getString(i, "Id");
			String real = cfg.getString(i, "ReadID");
			String[] reals = real.split(",");
			for (String str : reals) {
				String[] str1 = str.split("=");
				if (str1.length != 2) {
					continue;
				}

				String[] str2 = str1[0].split("-");
				if (str2.length != 2) {
					continue;
				}

				RealData data = new RealData();
				data.min = Integer.parseInt(str2[0]);
				data.max = Integer.parseInt(str2[1]);
				data.item = ParsePkg(str1[1]);

				if (m_mapRealDatas.containsKey(id)) {
					m_mapRealDatas.get(id).add(data);
				} else {
					List<RealData> list = new ArrayList<>();
					list.add(data);
					m_mapRealDatas.put(id, list);
				}
			}
		}
	}

	public void OnChangeDay(IKernel kernel, IGameObject player) {
		CheckExcCount(kernel, player);
		clearSpiritStone(kernel, player);
	}

	public void OnPlayerClassCreate(IKernel kernel, String script) {
		kernel.declareProperty(script, PLAYER_PROPERTY_SPIRITSTONESCORE, ValueType.INT, false, true, true); // 灵石积分
		kernel.declareProperty(script, PLAYER_PROPERTY_LASTSTONE2EXC, ValueType.LONG, false, false, true);
		kernel.declareProperty(script, PLAYER_PROPERTY_STONEEXCHANGECOUNT, ValueType.INT, false, true, true);
		kernel.declareProperty(script, STONE_2_BRONZE_EXCHANGE_COUNT, ValueType.INT, false, true, true);// 剩余兑换青铜次数
		kernel.declareProperty(script, STONE_2_SILVER_EXCHANGE_COUNT, ValueType.INT, false, true, true);// 剩余兑换白银次数
		kernel.declareProperty(script, STONE_2_GOLD_EXCHANGE_COUNT, ValueType.INT, false, true, true);// 剩余兑换黄金次数
		kernel.declareProperty(script, STONE_2_PLATINUM_EXCHANGE_COUNT, ValueType.INT, false, true, true);// 剩余兑换铂金次数
		kernel.declareProperty(script, STONE_2_DIAMOND_EXCHANGE_COUNT, ValueType.INT, false, true, true);// 剩余兑换钻石次数
		kernel.declareProperty(script, PLAYER_PROPERTY_STONE2NBOMBAMOUNT, ValueType.INT, false, false, true);// 总玩总得清零前累计获得铂金弹头数量
		kernel.declareProperty(script, PLAYER_PROPERTY_STONE2HBOMBAMOUNT, ValueType.INT, false, false, true);// 总玩总得清零前累计获得钻石弹头数量
		kernel.declareProperty(script, PLAYER_PROPERTY_TODAYSTONE2NBOMBAMOUNT, ValueType.INT, false, false, true);// 今天累计获得铂金弹头数量
		kernel.declareProperty(script, PLAYER_PROPERTY_TODAYSTONE2HBOMBAMOUNT, ValueType.INT, false, false, true);// 今天累计获得钻石弹头数量
		kernel.declareProperty(script, PLAYER_PROPERTY_LASTSTONE2TIME, ValueType.LONG, false, false, true);// 最近弹头兑换时间
		kernel.declareProperty(script, PLAYER_PROPERTY_STONECLEARTIME, ValueType.LONG, false, false, true);// 灵石清零时间
	}

	void OnVipLevelChanged(IKernel kernel, IGameObject player, String name, Object oldBulletValue) {
		long today = UtilFunc.getZeroTime(kernel.getServerTime());
		if (today != player.getLong(PLAYER_PROPERTY_LASTSTONE2EXC)) {
			// 各种弹头兑换次数分开计 alter by 赵俊@20190517
			// int[] counts = new int[m_bombId.size()];
			// int vipLevel = player.GetInt(PLAYER_PROPERTY_VIPLEVEL);
			// int[] vipCounts = m_VipModule.GetStoneExchangeCount(vipLevel);
			// if (vipCounts != null){
			// counts = vipCounts;
			// }
			// player.SetProperty(PLAYER_PROPERTY_LASTSTONE2EXC, today);
			// setStone2ExchangeCount(player, counts);
			// 各种弹头兑换次数合并计算 alter by 赵俊@20190624
			int count = 3;
			int vipLevel = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
			if (vipLevel > 0) {
				count = m_VipModule.GetStoneExchangeCount(vipLevel - 1);
			}
			player.setProperty(PLAYER_PROPERTY_LASTSTONE2EXC, today);
			player.setProperty(PLAYER_PROPERTY_STONEEXCHANGECOUNT, count);
		} else {
			// int oldCount = 0;
			// int[] oldCounts = new int[m_bombId.size()];
			// int oldLevel = (int)oldBulletValue;
			// int[] vipOldCount = m_VipModule.GetStoneExchangeCount(oldLevel);
			// if (vipOldCount != null){
			// oldCounts = vipOldCount;
			// }
			// for (int o : oldCounts){
			// oldCount += o;
			// }
			//
			// int nowcount = 0;
			// int[] nowcounts = new int[m_bombId.size()];
			// int vipLevel = player.GetInt(PLAYER_PROPERTY_VIPLEVEL);
			// int[] vipNowcount = m_VipModule.GetStoneExchangeCount(vipLevel);
			// if (vipNowcount != null){
			// nowcounts = vipNowcount;
			// }
			// for (int o : nowcounts){
			// nowcount += o;
			// }
			// int plCount = player.GetInt(PLAYER_PROPERTY_STONEEXCHANGECOUNT) + nowcount -
			// oldCount;
			// player.SetProperty(PLAYER_PROPERTY_STONEEXCHANGECOUNT, plCount < 0 ? 0 :
			// plCount);
			//
			// if (oldCounts.length > 0 && nowcounts.length > 0) {
			// int addCount = nowcounts[0] - oldCounts[0];
			// int pCount = player.GetInt(STONE_2_BRONZE_EXCHANGE_COUNT) +
			// addCount;
			// player.SetProperty(STONE_2_BRONZE_EXCHANGE_COUNT, pCount < 0 ? 0
			// : pCount);
			// }
			// if (oldCounts.length > 1 && nowcounts.length > 1) {
			// int addCount = nowcounts[1] - oldCounts[1];
			// int pCount = player.GetInt(STONE_2_SILVER_EXCHANGE_COUNT) +
			// addCount;
			// player.SetProperty(STONE_2_SILVER_EXCHANGE_COUNT, pCount < 0 ? 0
			// : pCount);
			// }
			// if (oldCounts.length > 2 && nowcounts.length > 2) {
			// int addCount = nowcounts[2] - oldCounts[2];
			// int pCount = player.GetInt(STONE_2_GOLD_EXCHANGE_COUNT) +
			// addCount;
			// player.SetProperty(STONE_2_GOLD_EXCHANGE_COUNT, pCount < 0 ? 0 :
			// pCount);
			// }
			// if (oldCounts.length > 3 && nowcounts.length > 3) {
			// int addCount = nowcounts[3] - oldCounts[3];
			// int pCount = player.GetInt(STONE_2_PLATINUM_EXCHANGE_COUNT) +
			// addCount;
			// player.SetProperty(STONE_2_PLATINUM_EXCHANGE_COUNT, pCount < 0 ?
			// 0 : pCount);
			// }
			// if (oldCounts.length > 4 && nowcounts.length > 4) {
			// int addCount = nowcounts[4] - oldCounts[4];
			// int pCount = player.GetInt(STONE_2_DIAMOND_EXCHANGE_COUNT) +
			// addCount;
			// player.SetProperty(STONE_2_DIAMOND_EXCHANGE_COUNT, pCount < 0 ? 0
			// : pCount);
			// }
			// 各种弹头兑换次数合并计算 alter by 赵俊@20190624
			int oldCount = 3;
			int oldLevel = (int) oldBulletValue;
			if (oldLevel > 0) {
				oldCount = m_VipModule.GetStoneExchangeCount(oldLevel - 1);
			}

			int nowcount = 3;
			int vipLevel = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
			if (vipLevel > 0) {
				nowcount = m_VipModule.GetStoneExchangeCount(vipLevel - 1);
			}

			int addCount = nowcount - oldCount;
			player.setProperty(PLAYER_PROPERTY_STONEEXCHANGECOUNT, player.getInt(PLAYER_PROPERTY_STONEEXCHANGECOUNT) + addCount);
		}
	}

	void setStone2ExchangeCount(IGameObject player, int[] counts) {
		int count = 0;
		for (int c : counts) {
			count += c;
		}
		player.setProperty(PLAYER_PROPERTY_STONEEXCHANGECOUNT, count);

		if (counts.length > 0) {
			player.setProperty(STONE_2_BRONZE_EXCHANGE_COUNT, counts[0]);
		}
		if (counts.length > 1) {
			player.setProperty(STONE_2_SILVER_EXCHANGE_COUNT, counts[1]);
		}
		if (counts.length > 2) {
			player.setProperty(STONE_2_GOLD_EXCHANGE_COUNT, counts[2]);
		}
		if (counts.length > 3) {
			player.setProperty(STONE_2_PLATINUM_EXCHANGE_COUNT, counts[3]);
		}
		if (counts.length > 4) {
			player.setProperty(STONE_2_DIAMOND_EXCHANGE_COUNT, counts[4]);
		}
	}

	public void OnPlayerOnLine(IKernel kernel, IGameObject player) {
		CheckExcCount(kernel, player);
		clearSpiritStone(kernel, player);
	}

	private void clearSpiritStone(IKernel kernel, IGameObject player) {
		// 灵石清零 add by 赵俊@20190627
		if (player.getLong(PLAYER_PROPERTY_STONECLEARTIME) == 0L) {
			player.setProperty(PLAYER_PROPERTY_STONECLEARTIME, kernel.getServerTime());
		}
		if (!TimeUtils.isSameDay(player.getLong(PLAYER_PROPERTY_STONECLEARTIME), kernel.getServerTime())) {
			int count = m_ItemModule.GetItemCount(kernel, player, "item_spirit_stones");
			m_ItemModule.SubItem(kernel, player, "item_spirit_stones", count, UtilFunc.System.CHANGE_DAY.ordinal(),
					"Change Day Clear");
			player.setProperty(PLAYER_PROPERTY_STONECLEARTIME, kernel.getServerTime());
		}
	}

	void CheckExcCount(IKernel kernel, IGameObject player) {
		long today = UtilFunc.getZeroTime(kernel.getServerTime());
		if (today != player.getLong(PLAYER_PROPERTY_LASTSTONE2EXC)) {
			// int[] counts = new int[m_bombId.size()];
			// int vipLevel = player.GetInt(PLAYER_PROPERTY_VIPLEVEL);
			// int[] vipCount = m_VipModule.GetStoneExchangeCount(vipLevel);
			// if (vipCount != null){
			// counts = vipCount;
			// }
			// player.SetProperty(PLAYER_PROPERTY_LASTSTONE2EXC, today);
			// setStone2ExchangeCount(player, counts);
			// 各种弹头兑换次数合并计算 alter by 赵俊@20190624
			int count = 3;
			int vipLevel = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
			if (vipLevel > 0) {
				count = m_VipModule.GetStoneExchangeCount(vipLevel - 1);
			}
			player.setProperty(PLAYER_PROPERTY_LASTSTONE2EXC, today);
			player.setProperty(PLAYER_PROPERTY_STONEEXCHANGECOUNT, count);
		}
	}

	void RefreshStock(IKernel kernel, IGameObject player) {
		IPubData pubData = kernel.getPubData( "StoreStone");
		if (pubData == null) {
			return;
		}

		IPubRecord record = pubData.getRecord("Record");
		if (record == null) {
			return;
		}

		CustomMsg.RefreshStock.Builder build = CustomMsg.RefreshStock.newBuilder();

		int rows = record.getRows();
		for (int i = 0; i < rows; ++i) {
			String item = record.getString(i, ITEM_STONE_COL.COL_ITEM_ID.ordinal());
			int stock = record.getInt(i, ITEM_STONE_COL.COL_STOCK.ordinal());

			build.addItem(item);
			build.addCount(stock);
		}

		kernel.sendMessage(player, S2CMsgDef.S2C_RES_STONE_STOCK.ordinal(), build.build().toByteArray());
	}

	void OnReqStoneStock(IKernel kernel, IGameObject player, int msgid, byte[] data) {
		RefreshStock(kernel, player);
	}

	void OnReqExStone(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] data)
			throws InvalidProtocolBufferException {
	}

	void OnReqTransformToStoneTimes(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] data)
			throws InvalidProtocolBufferException {
		CustomMsg.TransformToStone msg = CustomMsg.TransformToStone.parseFrom(data);
		if (msg.getCount() <= 0) {
			UtilFunc.responseSerCode(kernel, player, reqid, ServerCodeDef.CODE_PARAM_ERR);
			return;
		}
		if (msg.getCount() > m_ItemModule.GetItemCount(kernel, player, ITEM_EXCHANGE_STONE)) {
			UtilFunc.responseSerCode(kernel, player, reqid, ServerCodeDef.CODE_NEED_ITEM);
			return;
		}
		m_ItemModule.SubItem(kernel, player, ITEM_EXCHANGE_STONE, msg.getCount(),
				UtilFunc.System.EXCHANGE_STONE_TIMES.ordinal(), "exchangestone to spiritstone times");
		player.setProperty(PLAYER_PROPERTY_STONEEXCHANGECOUNT, player.getInt(PLAYER_PROPERTY_STONEEXCHANGECOUNT) + msg.getCount());
		UtilFunc.responseSerCode(kernel, player, reqid, ServerCodeDef.CODE_SUCCESS);
	}

	// /gm testStore item_skill_hbomb
	void OnTestCmd(IKernel kernel, IGameObject self, IGameObject target, String[] args) {
		Test(kernel, self, args[2]);
	}

	void Test(IKernel kernel, IGameObject player, String item) {
		int vipLevel = player.getInt(PLAYER_PROPERTY_VIPLEVEL);

	}

	private void addItemScore(IKernel kernel, IGameObject player, String itemId, int count) {
		int itemScore = NumberUtils.toInt(kernel.getCfgProperty(itemId, PLAYER_PROPERTY_ITEMSCORE), 0) * count;
		if (itemScore > 0) {
			player.setProperty(PLAYER_PROPERTY_ITEMSCORE, player.getInt(PLAYER_PROPERTY_ITEMSCORE) + itemScore);
		}
	}
}
