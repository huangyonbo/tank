/**   
*    
* 描述：   
* 文件：MasterModule.java
* 创建人：胡中伟
* 创建时间：2018年5月15日 上午11:45:07 
*    
*/
package master.modules;

import common.ServerMsg;
import common.ServerMsgDef;
import framework.ServerConfig;
import framework.ServerSet;
import framework.SystemConfigData;
import framework.game.ValueType;
import framework.master.IMasterModule;
import framework.master.MasterKernel;
import framework.net.InnerMsgDef;
import framework.net.SendMessage;
import framework.pub.PubData;
import framework.pub.PubProperty;
import framework.pub.PubRecord;
import framework.pub.PubUtils;
import game.modules.GMModule;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * 描述：
 * 
 */
public class MasterModule implements IMasterModule {

	/**
	 * @param kernel
	 * @return
	 */
	@Override
	public boolean onInit(MasterKernel kernel) {
		kernel.regCmd("time", this, "OnQueryServerTime");
		kernel.regCmd("leftTime", this, "OnQueryLeftTime");
		kernel.regCmd("list", this, "OnQueryServerList");
		kernel.regCmd("updateCfg", this, "OnUpdateCfg");
		kernel.regCmd("updateJar", this, "OnUpdateJar");
		kernel.regCmd("syncPlayer", this, "doSyncAllPlayer");

		//kernel.RegCmd("gmSwitch", this, "OnGmSwitch");
		kernel.regCmd("runJar", this, "OnRunJar");
		//kernel.RegCmd("activePush", this, "OnActivePush");
		kernel.regCmd("sendMail", this, "OnSendMail");
		kernel.regCmd("shutdown", this, "OnShutDown");
		kernel.regCmd("ls", this, "OnLookData");
		kernel.regCmd("ud", this, "OnUpdateData");
		kernel.regCmd("ur", this, "OnUpdateRecord");
		kernel.regCmd("rc", this, "OnReloadConf");
		kernel.regCmd("us", this, "OnUpdateStoreRecord");
		kernel.regCmd("lp", this, "OnLookPubData");
		kernel.regCmd("up", this, "OnUpdatePubData");
		kernel.regCmd("help", this, "OnReqHelp");
		return true;
	}

	/**
	 * 
	 */
	@Override
	public void onDestroy() {
		
	}
	
	void OnShutDown(MasterKernel kernel, int cmdid, String[] cmd) {
		kernel.getServer().broadToAllServer(null, InnerMsgDef.INNER_MSG_CLOSE.ordinal(),null);
	}
	
	void OnBeforeShutdown(MasterKernel kernel) {
		kernel.logicShutdownWait();
		// 通知公共区停止所有竞技场比赛
		kernel.requestServer(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.M2P_CLOSE_ALL_ARENA.ordinal(), null, (byte[] res) -> {
			kernel.logicShutdownReady();
		});
	}

	public void OnQueryServerTime(MasterKernel kernel, int cmdid, String[] cmd) {
		String fmt = "yyyy-MM-dd HH:mm:ss";
		if (cmd.length == 2) {
			fmt = cmd[1];
		} else if (cmd.length >= 3) {
			fmt = cmd[1] + " " + cmd[2];
		}
		long now = kernel.getServerTime();
		String time = new SimpleDateFormat(fmt).format(now) + "\n" + now;
		kernel.responseCmd(cmdid, time);
	}

	public void OnQueryLeftTime(MasterKernel kernel, int cmdid, String[] cmd) {
		if (cmd.length < 2) {
			kernel.responseCmd(cmdid, "");
			return;
		}
		long endTime = Long.parseLong(cmd[1]);
		long now = kernel.getServerTime();
		long leftMs = endTime - now;
		int leftDay = (int) (leftMs / 1000 / 3600 / 24);
		int leftHour = (int) (leftMs / 1000 / 3600) % 24;
		int leftMin = (int) (leftMs / 1000 / 60) % 60;
		int leftSec = (int) (leftMs / 1000) % 60;
		kernel.responseCmd(cmdid, leftDay + ":" + leftHour + ":" + leftMin + ":" + leftSec);
	}

	public void OnQueryServerList(MasterKernel kernel, int cmdid, String[] cmd) {
		Object[] sers;
		if (cmd.length >= 2) {
			sers = kernel.getServersByType(cmd[1]);
		} else {
			sers = kernel.getServers();
		}
		String list = "";
		for (int i = 0; i < sers.length; ++i) {
			String name = sers[i].toString();
			ServerConfig cfg = kernel.getServerCfg(name);
			long secid = kernel.getServerSecid(name);
			list += String.format("%3d %4d %s(%s) %s:%d [%s:%d]\n", cfg.id, secid, name, cfg.type, cfg.addr,cfg.port, cfg.frontAddr, cfg.frontPort);
		}
		if (sers.length == 0) {
			list = "no server with this type.";
		}
		kernel.responseCmd(cmdid, list);
	}

	void OnGmSwitch(MasterKernel kernel, int cmdid, String[] cmd) {
		boolean on = false;
		if (cmd.length > 1) {
			on = cmd[1].toLowerCase().equals("on");
		}
		byte[] msg = new byte[1];
		msg[0] = (byte) (on ? 1 : 0);
		kernel.broadToServer("game", ServerMsgDef.MASTER_GM_SWITCH.ordinal(), msg);
		kernel.responseCmd(cmdid, "gm cmd " + (on ? "on" : "off"));
	}

	String res = "";
	int recvCount = 0;
	boolean comp = true;

	public void OnUpdateCfg(MasterKernel kernel, int cmdid, String[] cmd) {
		if (!comp) {
			kernel.responseCmd(cmdid, "wait another update request.");
		} else if (cmd.length <= 1) {
			kernel.responseCmd(cmdid, "no file.");
		} else {
			String path = cmd[1];
			ServerMsg.StringArray.Builder build = ServerMsg.StringArray.newBuilder();
			build.addSomewords(path);
			Object[] sers = kernel.getServersByType("game");
			comp = false;
			res = "";
			recvCount = 0;
			for (int i = 0; i < sers.length; ++i) {
				String name = sers[i].toString();
				kernel.requestServer(name, ServerMsgDef.M2G_UPDATE_CFG.ordinal(), build.build().toByteArray(),(resData) -> {
					res += name + " : " + (resData[0] == 1 ? "success" : "failed") + ".\n";
					recvCount++;
					if (recvCount == sers.length) {
						comp = true;
						kernel.responseCmd(cmdid, res);
					}
				});
			}
		}
	}
	void doSyncAllPlayer(MasterKernel kernel, int cmdid, String[] cmd) {
		ServerMsg.StringSingle.Builder clear = ServerMsg.StringSingle.newBuilder();
		clear.setWords("");
		kernel.sendServerMsg(ServerSet.SERVER_LOGIC_NAME_BACK, ServerMsgDef.GM2Back_SYNC_ALL_PLAYER_DATA.ordinal(), clear.build().toByteArray());
		kernel.responseCmd(cmdid, "sendRequest request.");
	}
	void OnUpdateJar(MasterKernel kernel, int cmdid, String[] cmd) {
		if (!comp) {
			kernel.responseCmd(cmdid, "wait another update request.");
		} else if (cmd.length <= 1) {
			kernel.responseCmd(cmdid, "no file.");
		} else {
			String path = cmd[1];
			Object[] sers = kernel.getServersByType("game");
			comp = false;
			res = "";
			recvCount = 0;
			for (int i = 0; i < sers.length; ++i) {
				String name = sers[i].toString();
				kernel.requestServer(name, ServerMsgDef.M2G_UPDATE_JAR.ordinal(), path.getBytes(), (byte[] resData) -> {
					res += name + " : " + (resData[0] == 1 ? "success" : "failed") + ".\n";
					recvCount++;
					if (recvCount == sers.length) {
						comp = true;
						kernel.responseCmd(cmdid, res);
					}
				});
			}
		}
	}

	void OnUpdateStoreRecord(MasterKernel kernel, int cmdid, String[] cmd) {
		if (!comp) {
			kernel.responseCmd(cmdid, "wait another update request.");
		} else if (cmd.length <= 1) {
			kernel.responseCmd(cmdid, "no file.");
		} else {
			String name = cmd[1];
			comp = false;
			res = "";
			recvCount = 0;
			kernel.requestServer(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.B2P_UPDATE_STORE.ordinal(), name.getBytes(), (byte[] resData) -> {
				res += "Public" + " : " + (resData[0] == 1 ? "success" : "failed") + ".\n";
				recvCount++;
				comp = true;
				kernel.responseCmd(cmdid, res);
			});
		}
	}

	void OnRunJar(MasterKernel kernel, int cmdid, String[] cmd) {
		if (cmd.length <= 2) {
			kernel.responseCmd(cmdid, "no file.");
			return;
		}
		String path = cmd[1];
		String name = cmd[2];
		String arg = path + "|" + name;
		Object[] sers = null;
		if (cmd.length >= 4) {
			sers = new String[1];
			sers[0] = cmd[3];
		} else {
			sers = kernel.getServersByType("game");
		}
		res = "";
		recvCount = 0;
		int serCount = sers.length;
		for (int i = 0; i < serCount; ++i) {
			String ser = sers[i].toString();
			kernel.requestServer(ser, ServerMsgDef.M2G_RUN_JAR.ordinal(), arg.getBytes(), (resData) -> {
				String a = new String(resData);
				res += ser + " : " + a + "\n";
				recvCount++;
				if (recvCount == serCount) {
					comp = true;
					kernel.responseCmd(cmdid, res);
				}
			});
		}
	}

	void OnActivePush(MasterKernel kernel, int cmdid, String[] cmd) {
		boolean push = false;
		if (cmd.length > 1) {
			push = cmd[1].toLowerCase().equals("true");
		}
		byte[] msg = new byte[1];
		msg[0] = (byte) (push ? 1 : 0);
		kernel.broadToServer("game", ServerMsgDef.MASTER_ACTIVE_PUSH.ordinal(), msg);
		kernel.responseCmd(cmdid, "ActivePush " + (push ? "true" : "false"));
	}

	void OnSendMail(MasterKernel kernel, int cmdid, String[] cmd) {
		if (cmd.length <= 10) {
			kernel.responseCmd(cmdid,"failed: param count must be 12. \n ./master sendMail type channel title context senduid sendname recvuid recvname lifetime appendix");
			return;
		}
		try {
			int index = 1;
			int type = Integer.parseInt(cmd[index++]);
			int channel = Integer.parseInt(cmd[index++]);
			String title = cmd[index++];
			String context = cmd[index++];
			int senduid = Integer.parseInt(cmd[index++]);
			String sendname = cmd[index++];
			int recvuid = Integer.parseInt(cmd[index++]);
			String recvname = cmd[index++];
			int lifetime = Integer.parseInt(cmd[index++]);
			String appendix = cmd[index++];
			kernel.sendMail(type, channel, title, context, senduid, sendname, recvuid, recvname, lifetime, appendix);
		} catch (Exception e) {
			kernel.responseCmd(cmdid, "failed: " + e.getMessage());
		}
		kernel.responseCmd(cmdid, "send mail success");
	}

	void OnLookData(MasterKernel kernel, int cmdid, String[] cmd){
		if (cmd.length < 4) {
			kernel.responseCmd(cmdid,"params must >= 4");
			return;
		}
		if (!cmd[1].equals("1") && !cmd[1].equals("2")){
			kernel.responseCmd(cmdid,"look type must in [1=(property),2=(record)] " + cmd[1]);
			return;
		}
		ServerMsg.StringArray.Builder builder = ServerMsg.StringArray.newBuilder();
		for (int i = 1 ; i < cmd.length ; i++){
			builder.addSomewords(cmd[i]);
		}
		Object[] sers = kernel.getServersByType("game");
		byte[] msg = builder.build().toByteArray();
		List<String> result = new ArrayList<>();
		for (int i = 0; i < sers.length ; i++) {
			kernel.requestServer(sers[i].toString(), ServerMsgDef.M2G_LOOK_DATAORRECORD.ordinal(),msg,(resData)->{
				String res = new String(resData);
				result.add(res);
				if (result.size() == sers.length){
					String buff = "";
					for (int j = 0; j < result.size() ; j++){
						buff += result.get(j);
					}
					kernel.responseCmd(cmdid,buff);
				}
			});
		}
	}
	
	void OnUpdateData(MasterKernel kernel, int cmdid, String[] cmd){
		if (cmd.length < 2) {
			kernel.responseCmd(cmdid,"input like [ud uid1@proName1@proValue1@uid2@proName2@proValue2]");
			return;
		}
		ServerMsg.StringArray.Builder builder = ServerMsg.StringArray.newBuilder();
		for (int i = 1 ; i < cmd.length ; i++){
			builder.addSomewords(cmd[i]);
		}
		Object[] sers = kernel.getServersByType("game");
		byte[] msg = builder.build().toByteArray();
		List<String> result = new ArrayList<>();
		for (int i = 0; i < sers.length ; i++) {
			kernel.requestServer(sers[i].toString(), ServerMsgDef.M2G_UPDATE_PROPERTY.ordinal(),msg,(resData)->{
				String res = new String(resData);
				result.add(res);
				if (result.size() == sers.length){
					String buff = "";
					for (int j = 0; j < result.size() ; j++){
						buff += result.get(j) + "\n";
					}
					kernel.responseCmd(cmdid,buff);
				}
			});
		}
	}

	void OnUpdateRecord(MasterKernel kernel, int cmdid, String[] cmd){
		if (cmd.length < 2) {
			kernel.responseCmd(cmdid,"input data please look code]");
			return;
		}
		ServerMsg.StringArray.Builder builder = ServerMsg.StringArray.newBuilder();
		for (int i = 1 ; i < cmd.length ; i++){
			builder.addSomewords(cmd[i]);
		}
		Object[] sers = kernel.getServersByType("game");
		byte[] msg = builder.build().toByteArray();
		List<String> result = new ArrayList<>();
		for (int i = 0; i < sers.length ; i++) {
			kernel.requestServer(sers[i].toString(), ServerMsgDef.M2G_UPDATE_RECORD.ordinal(),msg,(resData)->{
				String res = new String(resData);
				result.add(res);
				if (result.size() == sers.length){
					String buff = "";
					for (int j = 0; j < result.size() ; j++){
						buff += result.get(j) + "\n";
					}
					kernel.responseCmd(cmdid,buff);
				}
			});
		}
	}
	
	void OnReloadConf(MasterKernel kernel, int cmdid, String[] cmd){
		boolean colonyHeart = SystemConfigData.getConfig("colonyHeart",false);
		if (colonyHeart){
			kernel.getServer();
			SendMessage msg = new SendMessage(InnerMsgDef.INNER_MSG_RELOAD_CONFIG.ordinal(),null);
			kernel.getServer().broadToServer("entry",msg);
			kernel.getServer().broadToServer("gate",msg);
		}
		String config = System.getProperty("user.dir");
		if (SystemConfigData.load(config,null)) {
			kernel.responseCmd(cmdid,"succ");
		}else{
			kernel.responseCmd(cmdid,"fail");
		}
	}

	void OnLookPubData(MasterKernel kernel, int cmdid, String[] cmd){
		if (cmd.length < 2) {
			kernel.responseCmd(cmdid,"input like [lp pubName]");
			return;
		}
		String pubName = cmd[1];
		PubData pubData = PubUtils.loadSyncData(kernel.getServer().getJedis(),pubName);
		if (pubData == null){
			kernel.responseCmd(cmdid,"can not find pubData pubName=" + pubName);
		}else{
			StringBuilder builder = new StringBuilder();
			List<PubProperty> props = pubData.getPropertys();
			int size = props.size();
			builder.append("props={\n");
			for (int i = 0 ; i < size ; i++){
				PubProperty prop = props.get(i);
				ValueType _type = prop.getType();
				Object obj = prop.getValue();
				builder.append("   "+ prop.getName());
				builder.append(" [" + _type + "] = ");
				if (obj == null){
					builder.append("null");
				}else if (_type == ValueType.OBJECT){
					builder.append( obj.getClass().getSimpleName());
				}else{
					builder.append(obj.toString());
				}
				builder.append("\n");
			}
			builder.append("}\n");
			builder.append("records={\n");
			List<PubRecord> records = pubData.getRecords();
			size = records.size();
			for (int i = 0 ; i < size ; i++){
				PubRecord record = records.get(i);
				StringBuilder buffer = new StringBuilder("   " + record.getName() + "{\n");
				int rows = record.getRows();
				int cols = record.getCols();
				for (int j = 0 ; j < rows ; j++){
					for (int k = 0 ; k < cols ; k++){
						Object value = record.getValue(j, k);
						ValueType _type = record.getColType(k);
						buffer.append("      [").append(j).append(",").append(k);
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
				buffer.append("   }\n");
				builder.append(buffer.toString());
			}
			builder.append("}");
			kernel.responseCmd(cmdid,builder.toString());
		}
	}

	void OnReqHelp(MasterKernel kernel, int cmdid, String[] cmd){
		StringBuilder builder  = new StringBuilder();
		builder.append("time: get server time\n");
		builder.append("leftTime: compute leftTime when inupt end time number\n");
		builder.append("list: get all server status\n");
		builder.append("updateCfg: update file like xxx.xml when system running\n");
		builder.append("updateJar: update DieFish method when system running\n");
		builder.append("runJar: fix something when system had error and running\n");
		builder.append("sendMail: send email to player\n");
		builder.append("shutdown: close the system\n");
		builder.append("ls: look player propertys or records\n");
		builder.append("ud: update player propertys\n");
		builder.append("ur: update player records\n");
		builder.append("rc: reload conf.propertes\n");
		builder.append("us: refresh StoreRecord\n");
		builder.append("lp: look PubData detail\n");
		builder.append("up: update PubData\n");
		kernel.responseCmd(cmdid,builder.toString());
	}

	void OnUpdatePubData(MasterKernel kernel, int cmdid, String[] cmd){
		if (cmd.length < 3){
			kernel.responseCmd(cmdid,"input like [up pubName op(-1=look detail)]");
			return;
		}
		String pubName  = cmd[1];
		String _op      = cmd[2];
		int op = 0;
		try {
			op = Integer.parseInt(_op);
		} catch (NumberFormatException e) {
			kernel.responseCmd(cmdid,"op not number");
			return;
		}
		if (op == -1){
			kernel.responseCmd(cmdid,"1:setProperty\n3:setRecord\n4:delRecordRow\n5:clearRecord\n6:addRecord\n7:createNewPubData\n8:delPubData");
			return;
		}
		if (op == GMModule.PublicOpt.GET_PRO.ordinal() || op == GMModule.PublicOpt.GET_TABLE.ordinal()){
			kernel.responseCmd(cmdid,"op error\n1:setProperty\n3:setRecord\n4:delRecordRow\n5:clearRecord\n6:addRecord\n7:createNewPubData\n8:delPubData");
			return;
		}
		ServerMsg.UpdatePubData.Builder builder = ServerMsg.UpdatePubData.newBuilder();
		builder.setDataName(pubName);
		builder.setOpt(op);

		if (GMModule.PublicOpt.SET_PRO.ordinal() == op) {
			if (cmd.length < 5){
				kernel.responseCmd(cmdid,"input like [up pubName op key value]");
				return;
			}
			builder.setProName(cmd[3]);
			builder.setProValue(cmd[4]);
		} else if (GMModule.PublicOpt.CLEAR_REC.ordinal() == op) {
			if (cmd.length < 4){
				kernel.responseCmd(cmdid,"input like [up pubName op recName]");
				return;
			}
			builder.setTableName(cmd[3]);
		}else if (GMModule.PublicOpt.DEL_ROW.ordinal() == op) {
			if (cmd.length < 5){
				kernel.responseCmd(cmdid,"input like [up pubName op recName row]");
				return;
			}
			builder.setTableName(cmd[3]);
			int row = 0 ;
			try {
				row = Integer.parseInt(cmd[4]);
			} catch (NumberFormatException e) {
				kernel.responseCmd(cmdid,"row not number");
				return;
			}
			builder.setRow(row);
		}else if (GMModule.PublicOpt.ADD_ROW.ordinal() == op) {
			if (cmd.length < 5){
				kernel.responseCmd(cmdid,"input like [up pubName op recName cols]");
				return;
			}
			builder.setTableName(cmd[3]);
			builder.setValue(cmd[4]);
		}else if (GMModule.PublicOpt.SET_TABLE.ordinal() == op) {
			if (cmd.length < 7){
				kernel.responseCmd(cmdid,"input like [up pubName op recName row col value]");
				return;
			}
			builder.setTableName(cmd[3]);
			int row = 0 ;
			try {
				row = Integer.parseInt(cmd[4]);
			} catch (NumberFormatException e) {
				kernel.responseCmd(cmdid,"row not number");
				return;
			}
			int col = 0 ;
			try {
				col = Integer.parseInt(cmd[5]);
			} catch (NumberFormatException e) {
				kernel.responseCmd(cmdid,"col not number");
				return;
			}
			builder.setRow(row);
			builder.setCol(col);
			builder.setCellValue(cmd[6]);
		}
		byte[] msg = builder.build().toByteArray();
		kernel.requestServer(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.PUBMSG_UPDATE_DATA.ordinal(), msg, (resp) -> {
			ServerMsg.UpdatePubDataRes res = null;
			try {
				res = ServerMsg.UpdatePubDataRes.parseFrom(resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
			kernel.responseCmd(cmdid,res.getRes());
		});
	}
}
