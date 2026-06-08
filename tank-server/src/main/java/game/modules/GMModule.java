/**   
*    
* 描述：   GM模块
* 文件：GMModule.java
* 创建人：胡中伟
* 创建时间：2018年4月9日 下午2:07:09 
*    
*/
package game.modules;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.*;
import framework.game.*;
import game.custommsg.C2SMsgDef;
import game.custommsg.CommandDef;
import game.custommsg.CustomMsg;
import game.custommsg.S2CMsgDef;

import game.modules.fishgame.FishModule;

import game.modules.items.ItemModule;
import game.modules.skills.SkillModule;
import game.modules.statemachine.StateMachine;
import game.modules.utils.UtilFunc;
import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 
 * 描述：
 * 
 */
public class GMModule implements ILogicModule {
	public enum PublicOpt {
		GET_PRO,
		SET_PRO,
		GET_TABLE,
		SET_TABLE,
		DEL_ROW,
		CLEAR_REC,
		ADD_ROW,
		CREATE_PUB,
		DEL_PUB
	}

	private Map<String, MethodCallBackData> m_cmdCallbacks = new HashMap<>();
	static Logger logger = LoggerFactory.getLogger(GMModule.class);
	
	private boolean m_switch = true;
	Set<Integer> m_setGms = new HashSet<>();
	// 角色表
	Map<Integer, Set<String>> roles = new HashMap<>();

	// 用户id 角色
	Map<Integer, Integer> userRole = new HashMap<>();

	public static final String CMD_LOG_NAME = "TmpGmCmd";

	/**
	 * @param kernel
	 * @return
	 */
	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
		kernel.regClientMessage(C2SMsgDef.C2S_GM_CMD.ordinal(), this, "OnGmCmd");
		kernel.regClientMessage(C2SMsgDef.C2S_TEST_AUTO_ADD.ordinal(), this, "OnTestAddValue");

		kernel.regServerMsg(ServerMsgDef.MASTER_GM_SWITCH.ordinal(), this, "OnSwitch");

		kernel.regEvent(KernelEvent.KEVENT_ON_LOAD, "Player", this, "OnPlayerOnLoad");
		kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");
		RefreshCfg(kernel, "res/Config/Gm.xml");
		RefreshCfg(kernel, "res/Config/GmRole.xml");
		// 根据uid选择目标，默认选择self
		AddCmd("select", this, "OnSelectCmd");
		// 查看当前目标uid
		AddCmd("target", this, "OnTargetCmd");
		// 查看属性
		AddCmd("gp", this, "OnGpCmd");
		// 设置属性
		AddCmd("sp", this, "OnSpCmd");

		// 查看表数据
		AddCmd("gr", this, "OnGrCmd");
		// 修改表数据
		AddCmd("sr", this, "OnSrCmd");
		// 添加一行数据
		AddCmd("addRow", this, "OnAddRowCmd");
		// 删除一行数据
		AddCmd("delRow", this, "OnDelRowCmd");
		// 清空表数据
		AddCmd("clearRec", this, "OnClearRecCmd");

		// 添加道具
		AddCmd("addItem", this, "OnAddItemCmd");
		// 删除道具
		AddCmd("delItem", this, "OnDelItemCmd");

		AddCmd("sendMail", this, "OnSendMailCmd");
		AddCmd("outFmt", this, "OnOutFmtCmd");
		AddCmd("robotUseSkill", this, "OnRobotUseSkill");
		AddCmd("clearList", this, "OnClearList");

		/** 公共区数据操作命令 **/
		// 查看公共区属性
		AddCmd("pgp", this, "OnPGpCmd");
		// 设置公共区属性
		AddCmd("psp", this, "OnPSpCmd");

		// 查看公共区表数据
		AddCmd("pgr", this, "OnPGrCmd");
		// 修改公共区表数据
		AddCmd("psr", this, "OnPSrCmd");

		// 删除一行公共区数据
		AddCmd("pDelRow", this, "OnPDelRowCmd");
		// 清空公共区表数据
		AddCmd("pClearRec", this, "OnPClearRecCmd");

		// 添加一行公共区数据
		AddCmd("pAddRow", this, "OnPAddRowCmd");
		// 删除公共区
		AddCmd("pDelPub", this, "OnPDelPub");
		// 创建公共区
		AddCmd("pCrePub", this, "OnPCrePub");
		// 出鱼
		AddCmd("outfish", this, "OnOutFish");
		AddCmd("findfish", this, "OnFindFish");
		AddCmd("killfish", this, "OnKillFish");
		// 出鱼阵
		AddCmd("outgroup", this, "OnOutFishGroup");
		return true;
	}

	/**
	 * 
	 */
	@Override
	public void onDestroy() {
	}


	void OnPlayerOnLoad(IKernel kernel, IGameObject player) {
		boolean gm = SystemConfigData.getConfig("gm", false);
		int uid = player.getInt(PLAYER_PROPERTY_UID);
		if (gm) {
			userRole.put(uid, 1);
			player.setProperty(PLAYER_PROPERTY_ROLEID, uid);
			player.setProperty(PLAYER_PROPERTY_GMMASTER, 1);
		} else {
			Integer roleId = userRole.get(uid);
			if (roleId != null) {
				player.setProperty(PLAYER_PROPERTY_ROLEID, roleId);
				player.setProperty(PLAYER_PROPERTY_GMMASTER, 1);
			}
		}
	}

	void RefreshCfg(IKernel kernel, String path) {
		if (path.equals("res/Config/Gm.xml")) {
			userRole.clear();
			ICfgReader cfg = kernel.loadXmlConfig(path);
			int count = cfg.getItemCount();
			for (int i = 0; i < count; ++i) {
				userRole.put(cfg.getInt(i, "uid"), cfg.getInt(i, PLAYER_PROPERTY_ROLEID));
			}
		} else if ("res/Config/GmRole.xml".equals(path)) {
			roles.clear();
			ICfgReader rolCfg = kernel.loadXmlConfig(path);
			int roleCount = rolCfg.getItemCount();
			for (int i = 0; i < roleCount; ++i) {
				String[] cmds = rolCfg.getStringArray(i, "Cmds", ",");
				Set<String> cmdSet = new HashSet<>();
				if (cmds != null) {
					for (String cmd : cmds) {
						cmdSet.add(cmd);
					}
				}
				roles.put(rolCfg.getInt(i, "Id"), cmdSet);
			}
		}
	}

	public void OnPlayerClassCreate(IKernel kernel, String script) {
		kernel.declareProperty(script, PLAYER_PROPERTY_GMTARGET, ValueType.LONG, false, false, false);
		kernel.declareProperty(script, PLAYER_PROPERTY_GMMASTER, ValueType.INT, true, true, false); // GM等级
		kernel.declareProperty(script, PLAYER_PROPERTY_ROLEID, ValueType.INT, true, true, false); // GM等级
	}

	public void AddCmd(String cmd, Object listener, String methodName) {
		MethodCallBackData data = new MethodCallBackData();
		data.listener = listener;
		data.access = MethodAccessCache.tryToGet(listener.getClass());
		data.methodIndex = data.access.getIndex(methodName, IKernel.class, IGameObject.class, IGameObject.class,
				String[].class);

		m_cmdCallbacks.put(cmd, data);
	}

	void CmdResult(IKernel kernel, IGameObject player, String res) {
		// S2C_GM_CMD_RES
		CustomMsg.String.Builder msg = CustomMsg.String.newBuilder();
		msg.setValue(res);
		kernel.sendMessage(player, S2CMsgDef.S2C_GM_CMD_RES.ordinal(), msg.build().toByteArray());

		if (!player.haveTempData(CMD_LOG_NAME)) {
			return;
		}

		String cmd = player.getTempString(CMD_LOG_NAME);

		IGameObject target = null;
		if (player.getLong(PLAYER_PROPERTY_GMTARGET) == 0L) {
			target = player;
		} else {
			target = kernel.getGameObject(player.getLong(PLAYER_PROPERTY_GMTARGET));
		}
		if (target == null) {
			return;
		}

		kernel.addGmLog(player, target, cmd, res);
	}

	void OnSwitch(IKernel kernel, int serid, int msgid, byte[] msg) {
		m_switch = msg[0] == 1;
	}
	
	void OnTestAddValue (IKernel kernel, IGameObject player, int msgid, byte[] msg) throws InvalidProtocolBufferException{
		CustomMsg.String str = CustomMsg.String.parseFrom(msg);
		JsonObject json = JsonUtil.decodeToObj(str.getValue(),JsonObject.class);
		int type = json.get("type").getAsInt();
		long num = json.get("num").getAsLong();
		if (type == 0){
			long addNum = num + player.getLong(PLAYER_PROPERTY_GOLD);
			player.setProperty(PLAYER_PROPERTY_GOLD,addNum);
		} else if (type == 1){
			long addNum = num + player.getLong(PLAYER_PROPERTY_DIAMOND);
			player.setProperty(PLAYER_PROPERTY_DIAMOND,addNum);
		} else if (type == 2){
			long addNum = num + player.getLong(PLAYER_PROPERTY_BOMB_COIN);
			player.setProperty(PLAYER_PROPERTY_BOMB_COIN,addNum);
		} else if (type == 3){
			player.setProperty(PLAYER_PROPERTY_VIPLEVEL,(int)num);
		}
	}
	
	// /gm cmd args
	void OnGmCmd(IKernel kernel, IGameObject player, int msgid, byte[] msg)
			throws InvalidProtocolBufferException {
		if (!m_switch) {
			return;
		}
		if (player.getInt(PLAYER_PROPERTY_ROLEID) == 0) {
			CmdResult(kernel, player, "Sorry, you are not a gm");
			return;
		}
		// 默认操作目标为自己 add by 胡中伟, 2019年5月13日 下午3:05:09
		IGameObject target = null;
		if (player.getLong(PLAYER_PROPERTY_GMTARGET) == 0L) {
			target = player;
		} else {
			target = kernel.getGameObject(player.getLong(PLAYER_PROPERTY_GMTARGET));
		}
		CustomMsg.GmCmd gmcmd = CustomMsg.GmCmd.parseFrom(msg);
		String str = gmcmd.getCmd();
		String[] cmds = str.split(" ");
		if (cmds.length < 2 || !cmds[0].equals("/gm")) {
			return;
		}
		player.addTempData(CMD_LOG_NAME, ValueType.STRING, str);
		String cmd = cmds[1];
		int uid = player.getInt(PLAYER_PROPERTY_UID);
		Integer roleId = userRole.get(uid);
		Set<String> allowedCommands = roles.get(roleId);
		if (allowedCommands == null || !allowedCommands.contains(cmd)) {
			CmdResult(kernel, player, "cmd not allowed");
			return;
		}
		if (!cmd.equals("select") && target == null) {
			CmdResult(kernel, player,"target is null, please select a target first.(use '/gm select self' or '/gm select uid')");
			return;
		}
		if (m_cmdCallbacks.containsKey(cmd)) {
			MethodCallBackData cb = m_cmdCallbacks.get(cmd);
			cb.access.invoke(cb.listener, cb.methodIndex, kernel, player, target, cmds);
		}
	}
	
	// gm select uid
	void OnSelectCmd(IKernel kernel, IGameObject self, IGameObject target, String[] args) {
		if (args.length < 3) {
			return;
		}
		String tar = args[2];
		if (tar.equals("self")) {
			self.setProperty(PLAYER_PROPERTY_GMTARGET, 0L);
			CmdResult(kernel, self, "set target = " + self.getString(PLAYER_PROPERTY_NAME));
		} else {
			int uid = -1;
			try {
				uid = Integer.parseInt(tar);
			} catch (Exception e) {
				CmdResult(kernel, self, "set target error: " + e.getMessage());
				return;
			}
			String tarName = kernel.getUserName(uid);
			if (tarName == null) {
				CmdResult(kernel, self, "uid is not exist, please check and retry");
				return;
			}
			long tarid = kernel.getPlayerObjID(uid);
			if (tarid == -1) {
				CmdResult(kernel, self, "uid is not online");
				return;
			}
			self.setProperty(PLAYER_PROPERTY_GMTARGET, tarid);
			CmdResult(kernel, self, "set target = " + tarName);
			kernel.changeServer(self,tarid);
		}
	}

	// /gm target
	void OnTargetCmd(IKernel kernel, IGameObject self, IGameObject target, String[] args) {
		if (args.length < 2) {
			return;
		}

		CmdResult(kernel, self, "target = " + target.getString(PLAYER_PROPERTY_NAME));
	}

	// /gm gp pro
	void OnGpCmd(IKernel kernel, IGameObject self, IGameObject target, String[] args) {
		if (args.length < 3) {
			return;
		}

		String pro = args[2];
		Object res = target.getProperty(pro);
		if (res == null) {
			CmdResult(kernel, self, "property " + pro + " not exist");
		} else {
			CmdResult(kernel, self, pro + " = " + res.toString());
		}
	}

	// /gm sp pro value
	void OnSpCmd(IKernel kernel, IGameObject self, IGameObject target, String[] args) {
		if (args.length < 4) {
			return;
		}
		Object val = null;
		ValueType type = target.getProType(args[2]);
		switch (type) {
		case SHORT:
			val = Short.parseShort(args[3]);
			break;
		case INT:
			val = Integer.parseInt(args[3]);
			break;
		case LONG:
			val = Long.parseLong(args[3]);
			break;
		case BOOL:
			val = StringUtils.equals(args[3].toLowerCase(), "true");
			break;
		case FLOAT:
			val = Float.parseFloat(args[3]);
			break;
		case DOUBLE:
			val = Double.parseDouble(args[3]);
			break;
		case STRING:
			val = args[3];
			break;
		case OBJECT:
			val = args[3];
			break;
		default:
			return;
		}
		if (val != null) {
			target.setProperty(args[2], val, UtilFunc.System.GM_CMD.ordinal(), "Gm cmd");
		}
		CmdResult(kernel, self, args[2] + " = " + val.toString());
	}

	// /gm gr record
	void OnGrCmd(IKernel kernel, IGameObject self, IGameObject target, String[] args) {
		if (args.length < 3) {
			return;
		}

		IRecord rec = target.getRecord(args[2]);
		if (rec == null) {
			CmdResult(kernel, self, "record [" + args[2] + "] not exist");
			return;
		}

		int rows = rec.getRows();
		int cols = rec.getCols();
		String res = "record [" + args[2] + "] = (" + rows + "X" + cols + ")\n";
		for (int i = 0; i < rows; ++i) {
			String rowVal = "r" + i + " = ";
			for (int j = 0; j < cols; ++j) {
				rowVal += rec.getValue(i, j).toString() + ", ";
			}

			res += rowVal + "\n";
		}
		CmdResult(kernel, self, res);
	}

	// /gm sr record row col value
	void OnSrCmd(IKernel kernel, IGameObject self, IGameObject target, String[] args) {
		if (args.length < 6) {
			return;
		}
		IRecord rec = target.getRecord(args[2]);
		int row = Integer.parseInt(args[3]);
		int col = Integer.parseInt(args[4]);

		if (row < 0 || row >= rec.getRows()) {
			CmdResult(kernel, self, "param error. row(" + row + ") < 0 or >= maxrow");
			return;
		}

		Object val = null;
		ValueType type = rec.getColType(col);
		switch (type) {
		case SHORT:
			val = Short.parseShort(args[5]);
			break;
		case INT:
			val = Integer.parseInt(args[5]);
			break;
		case LONG:
			val = Long.parseLong(args[5]);
			break;
		case BOOL:
			val = StringUtils.equals(args[5].toLowerCase(), "true");
			break;
		case FLOAT:
			val = Float.parseFloat(args[5]);
			break;
		case DOUBLE:
			val = Double.parseDouble(args[5]);
			break;
		case STRING:
			val = args[5];
			break;
		case OBJECT:
			val = args[5];
			break;
		default:
			break;
		}

		if (val != null) {
			rec.setValue(row, col, val);
			CmdResult(kernel, self, "set " + args[2] + "(" + row + ", " + col + ") = " + rec.getValue(row, col));
		} else {
			CmdResult(kernel, self, "set value is null");
		}
	}

	void OnAddRowCmd(IKernel kernel, IGameObject self, IGameObject target, String[] args) {
		CmdResult(kernel, self, "not support this cmd");
	}

	// /gm delRow record row
	void OnDelRowCmd(IKernel kernel, IGameObject self, IGameObject target, String[] args) {
		if (args.length < 4) {
			return;
		}
		IRecord rec = target.getRecord(args[2]);
		if (rec == null) {
			return;
		}

		int row = Integer.parseInt(args[3]);
		rec.removeRow(row);
		CmdResult(kernel, self, "del row success. left " + rec.getRows());
	}

	void OnClearRecCmd(IKernel kernel, IGameObject self, IGameObject target, String[] args) {
		if (args.length < 3) {
			return;
		}
		IRecord rec = target.getRecord(args[2]);
		if (rec == null) {
			return;
		}
		rec.clear();
		CmdResult(kernel, self, "clear success. left " + rec.getRows());
	}

	// /gm addItem name count
	void OnAddItemCmd(IKernel kernel, IGameObject self, IGameObject target, String[] args) {
		if (args.length < 4) {
			return;
		}

		int count = Integer.parseInt(args[3]);
		ItemModule module = (ItemModule) kernel.getModule("ItemModule");
		if (module.AddItem(kernel, target, args[2], count, UtilFunc.System.GM_CMD.ordinal(), "Gm cmd")) {
			CmdResult(kernel, self, "add item success. ");
			//kernel.addGameLog(target, LogKind.OTHERS, LogType.GET, args[2] + "*" + count, this.getClass().getName(), "Add item with GM cmd", -1);
		}else {
			CmdResult(kernel, self, "add fail,no such item ");
		}
	}

	// /gm delItem name count
	void OnDelItemCmd(IKernel kernel, IGameObject self, IGameObject target, String[] args) {
		if (args.length < 4) {
			return;
		}

		int count = Integer.parseInt(args[3]);
		ItemModule module = (ItemModule) kernel.getModule("ItemModule");
		int c = module.SubItem(kernel, target, args[2], count, UtilFunc.System.GM_CMD.ordinal(), "Gm cmd");

		CmdResult(kernel, self, "have " + count + ", del " + c);
		//kernel.addGameLog(target, LogKind.OTHERS, LogType.USED, args[2] + "*" + c, this.getClass().getName(), "Del item with GM cmd", -1);
	}

	// /gm sendMail uid title context lifetime appendix
	void OnSendMailCmd(IKernel kernel, IGameObject self, IGameObject target, String[] args) {
		if (args.length < 7) {
			return;
		}
		int uid = Integer.parseInt(args[2]);
		long lieftime = Long.parseLong(args[5]);
		kernel.sendSystemMail(uid, -1, args[3], args[4], lieftime, args[6]);
	}

	void OnOutFmtCmd(IKernel kernel, IGameObject self, IGameObject target, String[] args) {

	}

	void OnRobotUseSkill(IKernel kernel, IGameObject self, IGameObject target, String[] args)
			throws InvalidProtocolBufferException {
		long deskid = self.getLong(PLAYER_PROPERTY_DESKID);
		IGameObject desk = kernel.getGameObject(deskid);
		if (desk == null) {
			return;
		}

		int count = desk.getSeatCount();
		for (int i = 0; i < count; ++i) {
			IGameObject robot = desk.getSeatObject(i);
			if (robot == null) {
				continue;
			}

			if (robot.getBool("IsRobot")) {
				SkillModule module = (SkillModule) kernel.getModule("SkillModule");
				module.UseSkill(kernel, robot, null, "skill_battery");
			}
		}
	}

	// /gm cleatList WeekList Gold
	void OnClearList(IKernel kernel, IGameObject self, IGameObject target, String[] args) {
		String pre = args[2];
		String name = args[3];
		ServerMsg.PubClearList.Builder clear = ServerMsg.PubClearList.newBuilder();
		clear.addType(pre + "_");
		clear.addName(name);

		kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.PUBMSG_CLEAR_LIST.ordinal(), clear.build().toByteArray());
	}

	// /gm checkProList uid name vip proName val
	void OnCheckList(IKernel kernel, IGameObject self, IGameObject target, String[] args) {

	}

	// /gm pgp dataName proName
	void OnPGpCmd(IKernel kernel, IGameObject self, IGameObject target, String[] args) {
		if (args.length < 4) {
			return;
		}

		String dataName = args[2];
		String proName = args[3];
		ServerMsg.UpdatePubData.Builder builder = ServerMsg.UpdatePubData.newBuilder();
		builder.setOpt(PublicOpt.GET_PRO.ordinal());
		builder.setDataName(dataName);
		builder.setProName(proName);

		reqPublicServer(kernel, self, builder.build().toByteArray());
	}

	// /gm psp dataName pro value
	void OnPSpCmd(IKernel kernel, IGameObject self, IGameObject target, String[] args) {
		if (args.length < 5) {
			return;
		}
		String dataName = args[2];
		String proName = args[3];
		String value = args[4];
		ServerMsg.UpdatePubData.Builder builder = ServerMsg.UpdatePubData.newBuilder();
		builder.setOpt(PublicOpt.SET_PRO.ordinal());
		builder.setDataName(dataName);
		builder.setProName(proName);
		builder.setProValue(value);

		reqPublicServer(kernel, self, builder.build().toByteArray());
	}

	// /gm pgr dataName tableName
	void OnPGrCmd(IKernel kernel, IGameObject self, IGameObject target, String[] args) {
		if (args.length < 4) {
			return;
		}
		String dataName = args[2];
		String tableName = args[3];
		ServerMsg.UpdatePubData.Builder builder = ServerMsg.UpdatePubData.newBuilder();
		builder.setOpt(PublicOpt.GET_TABLE.ordinal());
		builder.setDataName(dataName);
		builder.setTableName(tableName);

		reqPublicServer(kernel, self, builder.build().toByteArray());

	}

	// /gm psr dataName tableName row col value
	void OnPSrCmd(IKernel kernel, IGameObject self, IGameObject target, String[] args) {
		if (args.length < 7) {
			return;
		}
		String dataName = args[2];
		String tableName = args[3];
		String rowStr = args[4];
		String colStr = args[5];
		String valStr = args[6];
		ServerMsg.UpdatePubData.Builder builder = ServerMsg.UpdatePubData.newBuilder();
		builder.setOpt(PublicOpt.SET_TABLE.ordinal());
		builder.setDataName(dataName);
		builder.setTableName(tableName);
		builder.setRow(Integer.parseInt(rowStr));
		builder.setCol(Integer.parseInt(colStr));
		builder.setCellValue(valStr);

		reqPublicServer(kernel, self, builder.build().toByteArray());
	}

	// /gm pDelRow dataName tableName row
	void OnPDelRowCmd(IKernel kernel, IGameObject self, IGameObject target, String[] args) {
		if (args.length < 5) {
			return;
		}
		String dataName = args[2];
		String tableName = args[3];
		String rowStr = args[4];

		ServerMsg.UpdatePubData.Builder builder = ServerMsg.UpdatePubData.newBuilder();
		builder.setOpt(PublicOpt.DEL_ROW.ordinal());
		builder.setDataName(dataName);
		builder.setTableName(tableName);
		builder.setRow(Integer.parseInt(rowStr));

		reqPublicServer(kernel, self, builder.build().toByteArray());
	}

	// /gm pClearRec dataName tableName
	void OnPClearRecCmd(IKernel kernel, IGameObject self, IGameObject target, String[] args) {
		if (args.length < 3) {
			return;
		}

		String dataName = args[2];
		String tableName = args[3];

		ServerMsg.UpdatePubData.Builder builder = ServerMsg.UpdatePubData.newBuilder();
		builder.setOpt(PublicOpt.CLEAR_REC.ordinal());
		builder.setDataName(dataName);
		builder.setTableName(tableName);

		reqPublicServer(kernel, self, builder.build().toByteArray());
	}

	// /gm pAddRow dataName tableName rowData
	void OnPAddRowCmd(IKernel kernel, IGameObject self, IGameObject target, String[] args) {
		if (args.length < 5) {
			return;
		}

		String dataName = args[2];
		String tableName = args[3];
		String rowData = args[4];

		ServerMsg.UpdatePubData.Builder builder = ServerMsg.UpdatePubData.newBuilder();
		builder.setOpt(PublicOpt.ADD_ROW.ordinal());
		builder.setDataName(dataName);
		builder.setTableName(tableName);
		builder.setValue(rowData);

		reqPublicServer(kernel, self, builder.build().toByteArray());
	}

	// /gm pDelPub dataName
	void OnPDelPub(IKernel kernel, IGameObject self, IGameObject target, String[] args) {
		if (args.length < 2) {
			return;
		}

		String dataName = args[2];

		ServerMsg.UpdatePubData.Builder builder = ServerMsg.UpdatePubData.newBuilder();
		builder.setOpt(PublicOpt.DEL_PUB.ordinal());
		builder.setDataName(dataName);

		reqPublicServer(kernel, self, builder.build().toByteArray());
	}

	// /gm pCrePub dataName
	void OnPCrePub(IKernel kernel, IGameObject self, IGameObject target, String[] args) {
		if (args.length < 3) {
			return;
		}

		String dataName = args[2];

		ServerMsg.UpdatePubData.Builder builder = ServerMsg.UpdatePubData.newBuilder();
		builder.setOpt(PublicOpt.CREATE_PUB.ordinal());
		builder.setDataName(dataName);

		reqPublicServer(kernel, self, builder.build().toByteArray());
	}

	private void reqPublicServer(IKernel kernel, IGameObject self, byte[] bytes) {
		kernel.requestServer(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.PUBMSG_UPDATE_DATA.ordinal(), bytes, (byte[] data) -> {
			ServerMsg.UpdatePubDataRes res = null;
			try {
				res = ServerMsg.UpdatePubDataRes.parseFrom(data);
			} catch (Exception e) {
				e.printStackTrace();
			}
			CmdResult(kernel, self, res == null ? "error" : res.getRes());
		});
	}
	void OnKillFish(IKernel kernel, IGameObject self, IGameObject target, String[] args) {
		if (args.length < 3) {
			return;
		}
		JSONObject playerLog = kernel.getPlayerLog(self);
		String fishId = args[2];
		long deskId = self.getLong(PLAYER_PROPERTY_DESKID);
		IGameObject desk = kernel.getGameObject(deskId);
		if (desk == null) {
			CmdResult(kernel, self, "player not in desk");
			return;
		}
		IRecord rec = desk.getRecord(DESK_FISH_LIST);
		FishModule.FishData fishData =((FishModule) kernel.getModule("FishModule")).GetFishData(fishId);
		if (fishData == null) {
			CmdResult(kernel, self, "fish :" +fishId+" 不存在");
			return;
		}
		int row = rec.findRow(0, 1, fishId);
		playerLog.put("fish:"+fishId,row==-1?"不存在":"存在");

	}
	void OnFindFish(IKernel kernel, IGameObject self, IGameObject target, String[] args) {
		if (args.length < 3) {
			return;
		}
		JSONObject playerLog = kernel.getPlayerLog(self);
		String fishId = args[2];
		long deskId = self.getLong(PLAYER_PROPERTY_DESKID);
		IGameObject desk = kernel.getGameObject(deskId);
		if (desk == null) {
			CmdResult(kernel, self, "player not in desk");
			return;
		}
		IRecord rec = desk.getRecord(DESK_FISH_LIST);
		IRecord reCount = desk.getRecord(DESK_FISH_COUNT);
		FishModule.FishData fishData =((FishModule) kernel.getModule("FishModule")).GetFishData(fishId);
		if (fishData == null) {
			CmdResult(kernel, self, "fish :" +fishId+" 不存在");
			return;
		}
		int typeCount = reCount.findRow(0, 0, fishData.type);
		int row = rec.findRow(0, 1, fishId);
		playerLog.put("type:"+fishData.type,reCount.getInt(typeCount,1));
		playerLog.put("fish:"+fishId,row==-1?"不存在":"存在");
		playerLog.put("fishindex:",rec.getInt(row,0));
		CmdResult(kernel, self, playerLog.toString());
	}
	void OnOutFish(IKernel kernel, IGameObject self, IGameObject target, String[] args) {
		if (args.length < 3) {
			return;
		}
		String fishId = args[2];
		long deskId = self.getLong(PLAYER_PROPERTY_DESKID);
		IGameObject desk = kernel.getGameObject(deskId);
		if (desk == null) {
			return;
		}
		kernel.command(desk, CommandDef.CMD_GM_OUT_FISH.ordinal(), fishId);
	}

	void OnOutFishGroup(IKernel kernel, IGameObject self, IGameObject target, String[] args) {

	}
}
