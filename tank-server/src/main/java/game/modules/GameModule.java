package game.modules;

import com.google.protobuf.InvalidProtocolBufferException;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.back.GameObjectData;
import framework.game.*;
import framework.game.IKernel.PlayerState;
import framework.net.InnerMsgDef;
import framework.net.message.InnerMsg;
import game.custommsg.C2SMsgDef;
import game.custommsg.CommandDef;
import game.custommsg.CustomMsg;
import game.custommsg.RequestMsgDef;
import game.modules.utils.UtilFunc;
import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
/**
 *
 * 描述： 游戏模块 创建人：胡中伟 创建时间：2018年3月12日 下午6:17:24
 *
 */
public class GameModule implements ILogicModule {
	private static Logger logger = LoggerFactory.getLogger(GameModule.class);

	@Override
	public boolean onInit(IKernel kernel) {
		kernel.addDisconnectEvent(this, "OnPlayerDisconnect");
		kernel.regRequestMessage(RequestMsgDef.REQ_SERVER_TIME.ordinal(), this, "OnReqSerTime");
		kernel.regClientMessage(C2SMsgDef.C2S_READY.ordinal(), this, "OnClientReady");
		kernel.regClientMessage(C2SMsgDef.C2S_CHANGE_SCENE.ordinal(), this, "OnClientChangeScene");
		kernel.regClientMessage(C2SMsgDef.C2S_QUIT_GAME.ordinal(), this, "OnClientQuit");

		kernel.regServerRequest(ServerMsgDef.M2G_UPDATE_CFG.ordinal(), this, "OnRecvUpdateCfg");
		//kernel.regServerRequest(ServerMsgDef.M2G_UPDATE_JAR.ordinal(), this, "OnRecvUpdateJar");
		kernel.regServerRequest(ServerMsgDef.M2G_RUN_JAR.ordinal(), this, "OnRecvRunJar");
		kernel.regServerRequest(ServerMsgDef.M2G_LOOK_DATAORRECORD.ordinal(), this, "OnLookSomeData");
		kernel.regServerRequest(ServerMsgDef.M2G_UPDATE_PROPERTY.ordinal(), this, "OnRecvUpdateData");
		kernel.regServerRequest(ServerMsgDef.M2G_UPDATE_RECORD.ordinal(), this, "OnRecvUpdateRecord");

		kernel.regEvent(KernelEvent.KEVENT_ON_LINE, "Player", this, "OnPlayerOnLine");
		kernel.regEvent(KernelEvent.KEVENT_OFF_LINE, "Player", this, "OnPlayerOffLine");

		return true;
	}

	@Override
	public void onDestroy() {

	}

	public void OnPlayerOnLine(IKernel kernel, IGameObject player) {
		logger.info("{} OnLine", player.getInt(PLAYER_PROPERTY_UID));
	}

	public void OnPlayerOffLine(IKernel kernel, IGameObject player) {
		logger.info("{} OffLine", player.getInt(PLAYER_PROPERTY_UID));
	}

	public int OnPlayerDisconnect(IKernel kernel, IGameObject player) {
		return player.getInt(PLAYER_PROPERTY_OFFPROTECT);
	}

	public void OnReqSerTime(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg) {
//		logger.info("OnReqSerTime客户端请求服务器时间戳: {}", kernel.getServerTime());
		CustomMsg.ServerTime.Builder build = CustomMsg.ServerTime.newBuilder();
		build.setTime(kernel.getServerTime());
		kernel.response(player, reqid, build.build().toByteArray());
	}

	public void OnClientChangeScene(IKernel kernel, IGameObject player, int msgid, byte[] msg) {
		kernel.setState(player, PlayerState.STATE_CHANGESCENE);
	}

	void OnClientQuit(IKernel kernel, IGameObject player, int msgid, byte[] msg) {
		kernel.kickPlayerNoTip(player);
	}

	public void OnClientReady(IKernel kernel, IGameObject player, int msgid, byte[] msg) {
		kernel.setState(player, PlayerState.STATE_NORMAL);
		kernel.command(player, CommandDef.CMD_CLIENT_READY.ordinal(), player);
	}

	void OnRecvUpdateCfg(IKernel kernel, int reqid, byte[] data) throws InvalidProtocolBufferException {
		ServerMsg.StringArray request = ServerMsg.StringArray.parseFrom(data);
		List<String> paths = request.getSomewordsList();
		boolean success = true;
		for (String path : paths) {
			if (kernel.updateConfig(path)) {
				logger.info("Update cfg {} success.", path);
			} else {
				logger.info("Update cfg {} failed.", path);
				success = false;
			}
		}
		if (success) {
			kernel.responseServer(reqid, new byte[] { 1 });
		} else {
			kernel.responseServer(reqid, new byte[] { 0 });
		}
	}

	void OnRecvRunJar(IKernel kernel, int reqid, byte[] data) {
		String res = "";
		try {
			String arg = new String(data);
			String[] args = arg.split("\\|");
			URL url = new URL("file:" + System.getProperty("user.dir") + File.separator + "config" + File.separator + args[0]);
			URLClassLoader loader = new URLClassLoader(new URL[] { url },Thread.currentThread().getContextClassLoader());
			Class<?> c = loader.loadClass("game.modules." + args[1]);
			if (IRunJar.class.isAssignableFrom(c)) {
				IRunJar jar = (IRunJar) c.newInstance();
				res = jar.run(kernel);
			}
			loader.close();
		} catch (Exception exp) {
			res = "failed: " + exp.getMessage();
		}
		kernel.responseServer(reqid, res.getBytes());
	}

	private void respMsg(IKernel kernel,String name,int reqid,List<String> msgs){
		StringBuffer buf = new StringBuffer(name + "\n");
		for (int i = 0 ; i < msgs.size() ; i++){
			buf.append(msgs.get(i)).append("\n");
		}
		kernel.responseServer(reqid,buf.toString().getBytes());
	}

	private void fillData(List<Property> props,List<String> msgs,int start,int end){
		start = start < 0 ? 0 : start;
		end = end <= start ? props.size() : (end > props.size() ? props.size() : end);
		for (int i = start ; i < end ; i++){
			Property prop = props.get(i);
			ValueType _type = prop.getType();
			Object obj = prop.getValue();
			if (obj == null){
				msgs.add(prop.getName() + " [" + _type + "]" + " = null");
			}else if (_type == ValueType.OBJECT){
				msgs.add(prop.getName() + " [" + _type + "]" + " = " + obj.getClass().getSimpleName());
			}else{
				msgs.add(prop.getName() + " [" + _type + "]" + " = " + obj.toString());
			}
		}
	}

	private void fillRecord(List<Record> records, List<String> msgs,int start,int end){
		start = start < 0 ? 0 : start;
		end = end <= start ? records.size() : (end > records.size() ? records.size() : end);
		for (int i = start ; i < end ; i++){
			Record record = records.get(i);
			StringBuilder buffer = new StringBuilder(record.getName() + "{\n");
			int rows = record.getRows();
			int cols = record.getCols();
			for (int j = 0 ; j < rows ; j++){
				for (int k = 0 ; k < cols ; k++){
					Object value = record.getValue(j, k);
					ValueType _type = record.getColType(k);
					buffer.append("   [").append(j).append(",").append(k);
					buffer.append(",").append(_type);
					buffer.append("] = ");
					if (value == null){
						buffer.append("null\n");
					} else if (_type == ValueType.OBJECT){
						buffer.append(value.getClass().getSimpleName()).append("\n");
					}else{
						buffer.append(value.toString()).append("\n");
					}
				}
			}
			buffer.append("}");
			msgs.add(buffer.toString());
		}
	}

	void OnLookSomeData(IKernel kernel, int reqid, byte[] data) throws InvalidProtocolBufferException {
		ServerMsg.StringArray request = ServerMsg.StringArray.parseFrom(data);
		List<String> datas = request.getSomewordsList();
		String name = "from " + kernel.getSerName();
		String type = datas.get(0);
		int uid     = 0;
		String uidStr = datas.get(1);
		String key    = datas.get(2);
		List<String> msgs = new ArrayList<>();
		try {
			uid = Integer.parseInt(uidStr);
		} catch (NumberFormatException e) {
			msgs.add(uidStr + " >> not number");
		}
		if (uid > 0){
			GamePlayer player = (GamePlayer)kernel.getPlayer(uid);
			if (player != null) {//玩家在线
				int start = 0,end=-1;
				if (key.equals("-1")){
					if (datas.size() > 3){
						try {
							start = Integer.parseInt(datas.get(3));
						} catch (NumberFormatException e) {

						}
					}
					if (datas.size() > 4){
						try {
							end = Integer.parseInt(datas.get(4));
						} catch (NumberFormatException e) {

						}
					}
				}
				if (type.equals("1")){
					List<Property> props = player.findPropByKey(key);
					fillData(props,msgs,start,end);
				}else if (type.equals("2")){
					List<Record> records = player.findRecordByKey(key);
					fillRecord(records,msgs,start,end);
				}
				respMsg(kernel,name,reqid,msgs);
			}else{//玩家不在线
				InnerMsg.RequestRoleData.Builder builder = InnerMsg.RequestRoleData.newBuilder();
				builder.setUid(uid);
				byte[] _data = builder.build().toByteArray();
				kernel.requestToServer(framework.ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_REQ_READ_ROLE.ordinal(),_data,(resmsg) ->{
					InnerMsg.LoadRoleData roleData = null;
					try {
						roleData = InnerMsg.LoadRoleData.parseFrom(resmsg);
					} catch (Exception e) {
						msgs.add("not find");
						respMsg(kernel,name,reqid,msgs);
						return;
					}
					if (roleData.getCode() != 0){
						msgs.add("not find data");
						respMsg(kernel,name,reqid,msgs);
						return;
					}
					IoBuffer buffer = IoBuffer.wrap(roleData.getData().toByteArray());
					GameObjectData tempObject = new GameObjectData(false);
					tempObject.loadFromArchive(buffer);
					int start = 0,end=-1;
					if (key.equals("-1")){
						if (datas.size() > 3){
							try {
								start = Integer.parseInt(datas.get(3));
							} catch (NumberFormatException e) {

							}
						}
						if (datas.size() > 4){
							try {
								end = Integer.parseInt(datas.get(4));
							} catch (NumberFormatException e) {

							}
						}
					}
					if (type.equals("1")){
						List<Property> props = tempObject.findPropByKey(key);
						fillData(props,msgs,start,end);
					}else if (type.equals("2")){
						List<Record> records = tempObject.findRecordByKey(key);
						fillRecord(records,msgs,start,end);
					}
					tempObject.clear();
					respMsg(kernel,name,reqid,msgs);
				});
			}
		}else{
			msgs.add(uidStr + " must > 0");
			respMsg(kernel,name,reqid,msgs);
		}
	}

	void OnRecvUpdateData(IKernel kernel, int reqid, byte[] data) throws InvalidProtocolBufferException {
		ServerMsg.StringArray request = ServerMsg.StringArray.parseFrom(data);
		List<String> datas = request.getSomewordsList();
		InnerMsg.CmdUpdateProp.Builder temp = InnerMsg.CmdUpdateProp.newBuilder();
		String name = "from " + kernel.getSerName();
		List<String> msgs = new ArrayList<>();
		for (int i = 0 ; i < datas.size() ; i++){
			String params = datas.get(i);
			String[] ss = params.split("@");
			int uid = 0;
			try {
				uid = Integer.parseInt(ss[0]);
			} catch (NumberFormatException e) {
				msgs.add(params + " >> uid not number");
				continue;
			}
			if (uid <= 0){
				msgs.add(params + " >> uid <=0");
				continue;
			}
			if (ss.length < 3){
				msgs.add(params + " >> size not 3");
				continue;
			}
			IGameObject player = kernel.getPlayer(uid);
			if (player != null){
				//在线
				ValueType valueTypeype = player.getProType(ss[1]);
				Object value = UtilFunc.getValueByType(ss[2],valueTypeype);
				player.setProperty(ss[1],value,UtilFunc.System.FORCE_CHANGE.ordinal(),"cmd set");
			}else{//离线
				temp.addUids(uid);
				temp.addKeys(ss[1]);
				temp.addValues(ss[2]);
			}
			msgs.add(params + " >> succ");
		}
		InnerMsg.CmdUpdateProp cmdProp = temp.build();
		if (cmdProp.getUidsCount() > 0){
			kernel.sendMsgToServer(framework.ServerSet.SERVER_LOGIC_NAME_STORE,InnerMsgDef.INNER_MSG_CMD_UPDATE_PRO.ordinal(),cmdProp.toByteArray());
		}
		respMsg(kernel,name,reqid,msgs);
	}

	void OnRecvUpdateRecord(IKernel kernel, int reqid, byte[] data) throws InvalidProtocolBufferException {
		ServerMsg.StringArray request = ServerMsg.StringArray.parseFrom(data);
		List<String> datas = request.getSomewordsList();
		InnerMsg.CmdUpdateRecord.Builder temp = InnerMsg.CmdUpdateRecord.newBuilder();
		String name = "from " + kernel.getSerName();
		List<String> msgs = new ArrayList<>();
		for (int i = 0 ; i < datas.size() ; i++){
			String params = datas.get(i);
			String[] ss = params.split("@");
			int uid = 0;
			int op = 0;
			try {
				uid = Integer.parseInt(ss[0]);
			} catch (NumberFormatException e) {
				msgs.add(params + " >> uid not number");
				continue;
			}
			if (uid <= 0){
				msgs.add(params + " >> uid <=0 ");
				continue;
			}
			if (ss.length < 4){
				msgs.add(params + " >> size must >= 4 ");
				continue;
			}
			try {
				op = Integer.parseInt(ss[2]);
			} catch (NumberFormatException e) {
				msgs.add(params + " >> op not number");
				continue;
			}
			if (op == 1){//修改
				if (ss.length < 6){
					msgs.add(params + " >> size must >= 6 when update");
					continue;
				}
			}else if (op == 2){//删除
				if (ss.length < 5){
					msgs.add(params + " >> size must >= 5 when del");
					continue;
				}
			}
			String value = ss[3];
			IGameObject player = kernel.getPlayer(uid);
			int row = -1;
			int col = -1;
			if (op == 1){//修改
				try {
					row = Integer.parseInt(ss[4]);
				} catch (NumberFormatException e) {
					msgs.add(params + " >> row not number");
					continue;
				}
				try {
					col = Integer.parseInt(ss[5]);
				} catch (NumberFormatException e) {
					msgs.add(params + " >> col not number");
					continue;
				}
			}else if (op == 2){//删除
				try {
					row = Integer.parseInt(ss[4]);
				} catch (NumberFormatException e) {
					msgs.add(params + " >> row not number");
					continue;
				}
			}
			if (player != null){
				//在线
				IRecord record = player.getRecord(ss[1]);
				if (record == null){
					msgs.add(params + " >> not find record key");
					continue;
				}
				if (op == 0){//增加
					if (!record.addCmdRow(value)){
						msgs.add(params + " >> not ok ");
					}
				}else if (op == 1){//修改
					record.setCmdValue(row,col,value);
				}else if (op == 2){//删除
					record.removeRow(row);
				}else if (op == -1){//清理全部
					record.clear();
				}
			}else{//离线
				temp.addUids(uid);
				temp.addKeys(ss[1]);
				temp.addOps(op);
				temp.addValues(value);
				temp.addRows(row);
				temp.addCols(col);
			}
			msgs.add(params + " >> succ");
		}
		InnerMsg.CmdUpdateRecord cmdData = temp.build();
		if (cmdData.getUidsCount() > 0){
			kernel.sendMsgToServer(framework.ServerSet.SERVER_LOGIC_NAME_STORE,InnerMsgDef.INNER_MSG_CMD_UPDATE_REC.ordinal(),cmdData.toByteArray());
		}
		respMsg(kernel,name,reqid,msgs);
	}
}
