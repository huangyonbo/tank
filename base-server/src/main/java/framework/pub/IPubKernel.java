package framework.pub;

import framework.BaseServer;
import framework.IRequestCallback;
import framework.game.ICfgReader;
import framework.game.MailSystemDef;
import framework.mybatis.service.AbstractService;
import org.apache.mina.core.buffer.IoBuffer;

import java.util.List;
import java.util.function.Consumer;

public interface IPubKernel {
	IPubModule getModule(String name);

	IPubData getPubData(String name, boolean create);

	boolean storePubData(IPubData pubData);

	boolean deletePubData(String pubName);

	void regOnLoadEvent(String spacename, Object listener, String methodName);

	void regServerMsg(int msgid, Object listener, String methodName);

	void regServerRequest(int msgid, Object listener, String methodName);

	void sendServerMsg(int serid, int msgid, byte[] data);

	void sendServerMsg(String sername, int msgid, byte[] data);

	void broadToServer(String type, int msgid, byte[] data);

	void requestServer(int serid, int msgid, byte[] data, IRequestCallback cb);

	void requestServer(String sername, int msgid, byte[] data, IRequestCallback cb);

	void responseServer(int reqid, byte[] data);

	ICfgReader loadXmlConfig(String path);

	void addOrder(int uid, int channel, String name, String item, String material, boolean isComp, String fullName,
				  String qq, String wechat, String cellphone, String address,int vip);

	long getServerTime();

	void addDayPlayWin(String ser, int roomType, long date, long tp, long tw);

	void updateOnlineCount(int channel, int count);

	boolean sendSystemMail(int recvuid, int channel, String title, String context, long lifetime, String appendix);

	boolean sendSystemMail(int recvuid, int channel, String title, String context, long lifetime, String appendix,
						   MailSystemDef system);

	void reqSystemMail(int recvuid, int channel, String title, String context, long lifetime, String appendix,
					   Consumer<byte[]> cb);

	void addArenaRecord(int gameid, int turnid, int signPop, int signCount, int joinPop, int joinCount, long start,
						long end, String signs, String rewards, IoBuffer ranks);

	/**
	 * 兑换卡统计
	 * 
	 * @param statsTime
	 *            统计范围 yyyy-MM-dd > yyyy-MM-dd
	 * @param uid
	 *            玩家id
	 * @param friendAmount
	 *            好友数量
	 * @param friendRecharge
	 *            好友充值
	 * @param fansRecharge
	 *            粉丝充值
	 * @param cardAmount
	 *            获得兑换卡数量
	 * @param cb
	 *            回调
	 */
	void cardStats(String statsTime, int uid, int friendAmount, int friendRecharge, int fansRecharge, int cardAmount,
				   Consumer<Boolean> cb);

	void createListHistory(String type, String name, byte[] data, String createTime);

	void addAreaRoomLog(int id, long startTime, long endTime, String rank);
	
	/**
	 * 去数据中心去数据
	 * @param requireType
	 * @param method
	 * @param objects
	 * @param cb
	 */
	void executeSomeToStore(Class<? extends AbstractService<?>> requireType, String method, List<Object> objects, Consumer<String> cb);
	
	/**
	 * 注册功能模块关闭事件
	 * @param listener
	 * @param order
	 * @param methodName
	 */
	void regStopListener(Object listener, int order, String methodName);

    BaseServer getServer();
}
