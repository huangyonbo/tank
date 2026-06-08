package framework.logic;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import framework.*;
import framework.Store.ParamData;
import framework.mybatis.domain.ArenaRecord;
import framework.mybatis.domain.ListHistory;
import framework.mybatis.domain.OrderLog;
import framework.mybatis.domain.RunRecord;
import framework.mybatis.service.AbstractService;
import framework.mybatis.service.impl.ArenaRecordService;
import framework.mybatis.service.impl.ListHistoryService;
import framework.mybatis.service.impl.OrderLogService;
import framework.mybatis.service.impl.RunRecordService;
import framework.net.InnerMsgDef;
import framework.net.message.InnerMsg;
import framework.pub.PubData;
import framework.pub.PubKernel;
import framework.pub.PubUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class PublicLogic implements ILogic {
	private Logger logger = LoggerFactory.getLogger(PublicLogic.class);
	private BaseServer m_baseServer;
	private PubKernel m_kernel;
	private ActorTimer m_checkActor;

	@Override
	public boolean onInit(BaseServer ser) {
		m_baseServer = ser;
		m_kernel = new PubKernel();
		if (!m_kernel.onInit(this)) {
			return false;
		}
		m_baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_CUSTOM_MSG.ordinal(), "onCustomMsg");
		m_baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_CUSTOM_REQUEST.ordinal(), "onCustomRequest");
		m_baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_CUSTOM_RESPONSE.ordinal(), "onCustomResponse");
		//m_baseServer.AddRequestListener(this, InnerMsgDef.INNER_MSG_PS_LIST.ordinal(), "onReqPsList");
		//m_baseServer.AddRequestListener(this, InnerMsgDef.INNER_MSG_PD_LIST.ordinal(), "onReqPdList");
		//m_baseServer.AddRequestListener(this, InnerMsgDef.INNER_MSG_PD.ordinal(), "onReqPd");
		m_checkActor = m_baseServer.setTimer(this, 60000, -1, "onCheckPubData", null);
		return true;
	}

	@Override
	public void execute() {
		m_kernel.execute();
	}

	void onCheckPubData(Object obj, int leftCount){
		PubUtils.tick();
	}

	@Override
	public void onStop() {
		m_checkActor.stop();
	}

	@Override
	public void initStop() {
		Map<String, Boolean> map = m_baseServer.getMustCloseBeforeMe();
		map.clear();
		ServerSet ss = m_baseServer.getServerSet();
		Object[] sers = ss.getServersByType("game");
		for (int i = 0 ; i < sers.length; i++){
			map.put(sers[i].toString(),false);
		}
	}

	@Override
	public void onReady() {
		m_kernel.onLoadPubSpaceComp("pubdata");
		m_baseServer.onLogicReady();
	}

	@Override
	public void onDestroy() {
		m_kernel.onDestroy();
	}

	@Override
	public BaseServer getServer() {
		return m_baseServer;
	}

	void onCustomMsg(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		InnerMsg.CustomMsg custom = InnerMsg.CustomMsg.parseFrom(bytes);
		byte[] msg = null;
		if (custom.getData() != null) {
			msg = custom.getData().toByteArray();
		}
		m_kernel.onRecServerMsg((int)session.getAttribute("SerID"), custom.getMsgid(), msg);
	}

	void onCustomRequest(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		m_kernel.onServerRequest((int) session.getAttribute("SerID"), bytes);
	}

	void onCustomResponse(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		m_kernel.onServerResponse((int) session.getAttribute("SerID"), bytes);
	}


	public void storePubData(String space, String name, byte[] data) {
		/*
		String time = m_baseServer.getTimeFormat().format(m_baseServer.GetServerTime());
		List<Object> params = new ArrayList<>();
		PubDataSave save = new PubDataSave();
		save.setName(name);
		save.setTime(time);
		save.setData(data);
		params.add(save);
		ExecuteSomeToStore("pubdataDao",framework.ServerSet.SERVER_LOGIC_NAME_STORE,params,null);
		 */
	}

	public void deletePubData(String space, String name) {
		/*
		List<Object> params = new ArrayList<>();
		params.add(name);
		ExecuteSomeToStore("pubdataDao", "deleteById",params,null);
		 */
	}

	public void onReqPsList(int reqid, byte[] bytes) {
		m_baseServer.response(reqid, m_kernel.getPsList().toByteArray());
	}

	public void onReqPdList(int reqid, byte[] bytes) throws InvalidProtocolBufferException {
		/*
		InnerMsg.PubDataList pd = InnerMsg.PubDataList.parseFrom(bytes);
		String spaceName = pd.getName();
		PubSpace pubSpace = m_kernel.GetPubSpace(spaceName);
		if (pubSpace == null) {
			return;
		}
		m_baseServer.Response(reqid, pubSpace.GetPdList().toByteArray());
		*/
	}

	public void onReqPd(int reqid, byte[] bytes) throws InvalidProtocolBufferException {
		InnerMsg.PubData pd = InnerMsg.PubData.parseFrom(bytes);
		//String spaceName = pd.getSpacename();
		String name = pd.getName();
		PubData pubdata = m_kernel.getPubData(name,false);
		if (pubdata == null) {
			return;
		}
		InnerMsg.PubDataRes.Builder build = InnerMsg.PubDataRes.newBuilder();
		build.setData(ByteString.copyFrom(pubdata.getDebugData()));
		m_baseServer.response(reqid, build.build().toByteArray());
	}

	public void addOrder(int uid, int channel, String name, String item, String material, boolean isComp,
						 String fullName, String qq, String wechat, String cellphone, String address,int vip) {
		OrderLog order = new OrderLog();
		order.setUid(uid);
		order.setChannel(channel);
		order.setName(name);
		order.setItem(item);
		order.setMaterial(material);
		order.setCreateTime(new Date());
		if (isComp) {
			order.setStatus(1);
			order.setCompTime(m_baseServer.getTimeFormat().format(m_baseServer.getServerTime()));
			order.setRemarks("");
		} else {
			order.setStatus(0);
			order.setType(1);
			String remarks = String.format("姓名：%s； QQ：%s； 微信号：%s； 手机号：%s； 收货地址：%s", fullName, qq, wechat, cellphone,address);
			order.setRemarks(remarks);
		}
		order.setVip(vip);
		List<Object> params = new ArrayList<>();
		params.add(order);
		executeSomeToStore(OrderLogService.class, "addOne",params,null);
	}

	public void addRunRecord(String ser, int roomType, long date, long tp, long tw) {
		RunRecord rec = new RunRecord();
		rec.setSerName(ser);
		rec.setRoomType(roomType);
		rec.setTotalPlay(tp);
		rec.setTotalWin(tw);
		rec.setCreateDate(m_baseServer.getDayFormat().format(date));
		List<Object> params = new ArrayList<>();
		params.add(rec);
		executeSomeToStore(RunRecordService.class, "addOne",params,null);
	}

	public void addArenaRecord(int gameid, int turnid, int signPop, int signCount, int joinPop, int joinCount,
							   long start, long end, String signs, String rewards, IoBuffer ranks) {
		ArenaRecord record = new ArenaRecord();
		record.setGameId(gameid);
		record.setTurnId(turnid);
		record.setSignPop(signPop);
		record.setSignCount(signCount);
		record.setJoinPop(joinPop);
		record.setJoinCount(joinCount);
		record.setRobotCount(0);
		record.setStartTime(m_baseServer.getTimeFormat().format(start));
		record.setEndTime(m_baseServer.getTimeFormat().format(end));
		record.setSigns(signs);
		record.setRewards(rewards);
		record.setRanks(ranks.array());
		List<Object> params = new ArrayList<>();
		params.add(record);
		executeSomeToStore(ArenaRecordService.class, "addOne",params,null);
	}

	public void addListHistory(String type, String name, byte[] data, String createTime) {
		ListHistory listHistory = new ListHistory();
		listHistory.setType(type);
		listHistory.setName(name);
		listHistory.setData(data);
		listHistory.setCreateTime(createTime);
		List<Object> params = new ArrayList<>();
		params.add(listHistory);
		executeSomeToStore(ListHistoryService.class,"addOne",params,null);
	}

	public void executeSomeToStore(Class<? extends AbstractService<?>> requireType, String method, List<Object> objects, Consumer<String> cb){
		if (ObjectUtils.isEmpty(requireType)) {
			logger.error("error params <requireType> when call LoadDataFromDB");
			return;
		}
		if (StringUtils.isEmpty(method)) {
			logger.error("error params <method> when call LoadDataFromDB");
			return;
		}
		InnerMsg.LoadDataFromDb.Builder builder = InnerMsg.LoadDataFromDb.newBuilder();
		String serviceName = StringUtils.uncapitalize(requireType.getSimpleName());
		builder.setDao(serviceName);
		builder.setMethod(method);
		if (objects != null) {
			for (int i = 0; i < objects.size() ; i++) {
				Object obj = objects.get(i);
				Class<?> clazz = obj.getClass();
				if (List.class.isAssignableFrom(clazz)){
					clazz = List.class;
				}
				if (Map.class.isAssignableFrom(clazz)){
					clazz = Map.class;
				}
				builder.addTypes(clazz.getTypeName());
				if (obj instanceof String){
					builder.addValues(obj.toString());
				}else{
					String valueStr = new ParamData().encode(obj);
					if (valueStr == null){
						cb.accept(null);
						return;
					}
					builder.addValues(valueStr);
				}
			}
		}
		byte[] msg = builder.build().toByteArray();
		m_baseServer.request(framework.ServerSet.SERVER_LOGIC_NAME_STORE,InnerMsgDef.INNER_MSG_EXECUTE_SQL_METHOD.ordinal(),msg, (bytes)->{
			try {
				InnerMsg.ComeFromDbData datas = InnerMsg.ComeFromDbData.parseFrom(bytes);
				int code = datas.getCode();
				if (code == 0){
					if (cb != null){
						String _data = datas.getDatas();
						cb.accept(_data.length() == 0 ? null : _data);
					}
				}else{
					if (cb != null){
						cb.accept(null);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				if (cb != null){
					cb.accept(null);
				}
			}
		});
	}

	@Override
	public boolean tryToStop() {
		return m_kernel.runStopByOrder();
	}

	@Override
	public Jedis getJedis() {
		return m_baseServer.getJedis();
	}
}
