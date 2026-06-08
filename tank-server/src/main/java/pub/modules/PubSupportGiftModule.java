/**   
*    
* 描述：   
* 文件：ProListModule.java
* 创建人：胡中伟
* 创建时间：2018年4月14日 上午9:19:49 
*    
*/
package pub.modules;

import com.google.protobuf.InvalidProtocolBufferException;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.game.ICfgReader;
import framework.game.ValueType;
import framework.pub.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * 描述：
 * 
 */
public class PubSupportGiftModule implements IPubModule {

	enum SupportGiftRecordCol {
		COL_ID, COL_BUY_COUNT, COL_END
	}

	enum MysteryGiftRecordCol {
		COL_ID, COL_BUY_COUNT, COL_END
	}

	enum LimitGiftRecordCol {
		COL_ID, COL_BUY_COUNT, COL_END
	}

	public static String SUPPORT_GIFT_DATA = "SupportGiftData";
	public static String MYSTERY_GIFT_DATA = "MysteryGiftData";
	public static String LIMIT_GIFT_DATA = "LimitGiftData";
	private static Logger logger = LoggerFactory.getLogger(PubSupportGiftModule.class);

	/**
	 * @param kernel
	 * @return
	 */
	@Override
	public boolean onInit(IPubKernel kernel) {
		kernel.regOnLoadEvent("pubdata", this, "OnPubdataLoad");

		kernel.regServerMsg(ServerMsgDef.PUBMSG_ADD_SUPPORT.ordinal(), this, "OnAddSupport");
		kernel.regServerMsg(ServerMsgDef.PUBMSG_START_SUPPORT.ordinal(), this, "OnStartSupport");

		kernel.regServerMsg(ServerMsgDef.PUBMSG_ADD_MYSTERY.ordinal(), this, "OnAddMystery");
		kernel.regServerMsg(ServerMsgDef.PUBMSG_START_MYSTERY.ordinal(), this, "OnStartMystery");

		kernel.regServerMsg(ServerMsgDef.PUBMSG_ADD_LIMIT.ordinal(), this, "OnAddLimit");
		kernel.regServerMsg(ServerMsgDef.PUBMSG_START_LIMIT.ordinal(), this, "OnStartLimit");

		return true;
	}

	/**
	 * 
	 */
	@Override
	public void onDestroy() {
	}

	public void OnPubdataLoad(IPubKernel pubKernel, String none) {
		this.OnPubdataSupportGiftLoad(pubKernel);
		this.OnPubdataMysteryGiftLoad(pubKernel);
		this.OnPubdataLimitGiftLoad(pubKernel);
	}

	public void OnPubdataSupportGiftLoad(IPubKernel pubKernel) {
		//logger.info("begin loadPubData PubSupportGiftModule");
		ICfgReader supportGiftReader = pubKernel.loadXmlConfig("res/Gift/SupportGift.xml");
		int supportGiftCount = supportGiftReader.getItemCount();
		IPubData pubData = pubKernel.getPubData(SUPPORT_GIFT_DATA,false);
		if (null == pubData) {
			pubData = new PubData(SUPPORT_GIFT_DATA);
			Object version = pubData.getValue(PLAYER_PROPERTY_VERSION);
			if (null == version) {
				pubData.addProperty(PLAYER_PROPERTY_VERSION, ValueType.STRING, "", true);
			}
			IPubRecord record = pubData.getRecord("SupportGiftRecord");
			if (null == record) {
				record = pubData.addRecord("SupportGiftRecord", SupportGiftRecordCol.COL_END.ordinal(), 50, true);
				record.setColType(SupportGiftRecordCol.COL_ID.ordinal(), ValueType.STRING);
				record.setColType(SupportGiftRecordCol.COL_BUY_COUNT.ordinal(), ValueType.INT);
			}
			for (int i = 0; i < supportGiftCount; i++) {
				String id = supportGiftReader.getString(i, "Id");
				record.addRow(id, 0);// Id，购买数量
			}
		} else {
			diffNewRecord(pubData, "SupportGiftRecord", supportGiftReader, supportGiftCount);
		}
		pubKernel.storePubData(pubData);
		//logger.info("finish loadPubData PubSupportGiftModule");
	}

	public void OnPubdataMysteryGiftLoad(IPubKernel pubKernel) {
		//logger.info("begin loadPubData OnPubdataMysteryGiftLoad");
		ICfgReader mysteryGiftReader = pubKernel.loadXmlConfig("res/Gift/MysteryGift.xml");
		int mysteryGiftCount = mysteryGiftReader.getItemCount();
		IPubData pubData = pubKernel.getPubData(MYSTERY_GIFT_DATA,false);
		if (null == pubData) {
			pubData = new PubData(MYSTERY_GIFT_DATA);
			Object version = pubData.getValue("BuyTime");
			if (null == version) {
				long currentTime = pubKernel.getServerTime();
				String strCurrentTime = pubKernel.getServer().getDayFormat().format(currentTime);
				pubData.addProperty("BuyTime", ValueType.STRING, strCurrentTime, true);
			}
			IPubRecord record = pubData.getRecord("MysteryGiftRecord");
			if (null == record) {

				record = pubData.addRecord("MysteryGiftRecord", MysteryGiftRecordCol.COL_END.ordinal(), 50, true);
				record.setColType(MysteryGiftRecordCol.COL_ID.ordinal(), ValueType.STRING);
				record.setColType(MysteryGiftRecordCol.COL_BUY_COUNT.ordinal(), ValueType.INT);
			}
			for (int i = 0; i < mysteryGiftCount; i++) {
				String id = mysteryGiftReader.getString(i, "Id");
				record.addRow(id, 0);// Id，购买数量
			}
		} else {
			diffNewRecord(pubData, "MysteryGiftRecord", mysteryGiftReader, mysteryGiftCount);
		}
		pubKernel.storePubData(pubData);
		//logger.info("finish loadPubData OnPubdataMysteryGiftLoad");
	}

	public void OnPubdataLimitGiftLoad(IPubKernel pubKernel) {
		//logger.info("begin loadPubData OnPubdataLimitGiftLoad");
		ICfgReader limitGiftReader = pubKernel.loadXmlConfig("res/Gift/LimitGift.xml");
		int limitGiftCount = limitGiftReader.getItemCount();
		IPubData pubData = pubKernel.getPubData(LIMIT_GIFT_DATA,false);
		if (null == pubData) {
			pubData = new PubData(LIMIT_GIFT_DATA);
			Object version = pubData.getValue(PLAYER_PROPERTY_VERSION);
			if (null == version) {
				pubData.addProperty(PLAYER_PROPERTY_VERSION, ValueType.STRING, "", true);
			}
			IPubRecord record = pubData.getRecord("LimitGiftRecord");
			if (null == record) {
				record = pubData.addRecord("LimitGiftRecord", LimitGiftRecordCol.COL_END.ordinal(), 50, true);
				record.setColType(LimitGiftRecordCol.COL_ID.ordinal(), ValueType.STRING);
				record.setColType(LimitGiftRecordCol.COL_BUY_COUNT.ordinal(), ValueType.INT);
			}
			for (int i = 0; i < limitGiftCount; i++) {
				String id = limitGiftReader.getString(i, "Id");
				record.addRow(id, 0);// Id，购买数量
			}
		} else {
			diffNewRecord(pubData, "LimitGiftRecord", limitGiftReader, limitGiftCount);
		}
		pubKernel.storePubData(pubData);
		//logger.info("finish loadPubData PubLimitGiftModule");
	}

	private void diffNewRecord(IPubData pubData, String recordName, ICfgReader supportGiftReader,
			int supportGiftCount) {
		IPubRecord record = pubData.getRecord(recordName);
		if (record == null) {
			return;
		}
		// 查找是否有删除的礼包
		for (int i = 0; i < record.getRows(); i++) {
			String giftId = record.getString(i, 0);
			boolean has = hasGiftId(supportGiftReader, giftId);
			if (!has) {
				record.removeRow(i);
				i--;
			}
		}
		/*
		 * int recordRechargeItemCount = record.GetRows(); for (int i = 0; i <
		 * supportGiftReader.GetItemCount(); i++) { String giftId =
		 * record.GetString(i, 0); boolean has = StringUtils.equals(giftId,
		 * supportGiftReader.GetString(giftId, "Id")); if (!has) {
		 * record.RemoveRow(i); i--; } }
		 */// alter by madi 2018-05-24
		// 查找是否有新增的礼包
		for (int i = 0; i < supportGiftCount; i++) {
			int row = record.findRow(0, 0, supportGiftReader.getString(i, "Id"));
			if (-1 == row) {
				String id = supportGiftReader.getString(i, "Id");
				record.addRow(id, 0);// Id，购买数量
			}
		}
	}

	private boolean hasGiftId(ICfgReader reader, String giftId) {
		int itemCount = reader.getItemCount();
		for (int i = 0; i < itemCount; i++) {
			if (giftId.equals(reader.getString(i, "Id"))) {
				return true;
			}
		}
		return false;
	}

	public void OnAddSupport(IPubKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		ServerMsg.PubAddBuySupport pubAddBuySupport = ServerMsg.PubAddBuySupport.parseFrom(data);
		String id = pubAddBuySupport.getId();
		addCount(kernel, id, SUPPORT_GIFT_DATA, "SupportGiftRecord", SupportGiftRecordCol.COL_ID.ordinal(), SupportGiftRecordCol.COL_BUY_COUNT.ordinal());
	}

	public void OnAddMystery(IPubKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		ServerMsg.PubAddBuyMystery pubAddBuyMystery = ServerMsg.PubAddBuyMystery.parseFrom(data);
		String id = pubAddBuyMystery.getId();
		addCount(kernel, id, MYSTERY_GIFT_DATA, "MysteryGiftRecord", MysteryGiftRecordCol.COL_ID.ordinal(), MysteryGiftRecordCol.COL_BUY_COUNT.ordinal());
	}

	public void OnAddLimit(IPubKernel kernel, int serid, int msgid, byte[] data) throws InvalidProtocolBufferException {
		ServerMsg.PubAddLimit pubAddLimit = ServerMsg.PubAddLimit.parseFrom(data);
		String id = pubAddLimit.getId();
		addCount(kernel, id, LIMIT_GIFT_DATA, "LimitGiftRecord", LimitGiftRecordCol.COL_ID.ordinal(), LimitGiftRecordCol.COL_BUY_COUNT.ordinal());
	}

	private void addCount(IPubKernel kernel, String id, String pubDataName, String recordName, int colId,
			int colBuyCount) {
		IPubData pubData = kernel.getPubData(pubDataName,false);
		IPubRecord record = pubData.getRecord(recordName);
		int row = record.findRow(0, colId, id);
		if (-1 != row) {
			int count = record.getInt(row, colBuyCount) + 1;
			record.setValue(row, colBuyCount, count);
		}
		kernel.storePubData(pubData);
	}

	public void OnStartSupport(IPubKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		ServerMsg.PubStartSupport pubStartSupport = ServerMsg.PubStartSupport.parseFrom(data);
		String version = pubStartSupport.getVersion();
		onRefreshCount(kernel, SUPPORT_GIFT_DATA, PLAYER_PROPERTY_VERSION, version, "SupportGiftRecord", "res/Gift/SupportGift.xml");
	}

	public void OnStartMystery(IPubKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		ServerMsg.PubStartMystery pubStartMystery = ServerMsg.PubStartMystery.parseFrom(data);
		String buyTime = pubStartMystery.getBuyTime();

		this.onRefreshCount(kernel, MYSTERY_GIFT_DATA, "BuyTime", buyTime, "MysteryGiftRecord", "res/Gift/MysteryGift.xml");
	}

	public void OnStartLimit(IPubKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		ServerMsg.PubStartLimit pubStartLimit = ServerMsg.PubStartLimit.parseFrom(data);
		String version = pubStartLimit.getVersion();

		this.onRefreshCount(kernel, LIMIT_GIFT_DATA, PLAYER_PROPERTY_VERSION, version, "LimitGiftRecord", "res/Gift/LimitGift.xml");
	}

	private void onRefreshCount(IPubKernel kernel, String pubDataName, String propertyName, String targetValue,
			String recoardName, String xmlPath) {
		IPubData pubData = kernel.getPubData(pubDataName,false);
		String currentValue = (String) pubData.getValue(propertyName);
		if (!StringUtils.equals(currentValue, targetValue)) {
			IPubRecord record = pubData.getRecord(recoardName);
			if (record == null) {
				return;
			}
			// 删除重新添加是防止增加或者删除礼包层次
			record.clear();
			ICfgReader supportGiftReader = kernel.loadXmlConfig(xmlPath);
			int supportGiftCount = supportGiftReader.getItemCount();
			for (int i = 0; i < supportGiftCount; i++) {
				String id = supportGiftReader.getString(i, "Id");
				record.addRow(id, 0);// Id，购买数量
			}
			pubData.setValue(propertyName, targetValue);
			kernel.storePubData(pubData);
		}
	}
}
