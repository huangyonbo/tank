/**
*
* 描述：
* 文件：BackModule.java
* 创建人：胡中伟
* 创建时间：2018年4月4日 下午1:44:40
*
*/
package back.modules;

import back.modules.data.Write;
import back.modules.data.config.AdsConfigGameDTO;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.back.BacKernel;
import framework.back.IBackModule;
import framework.back.net.IDataCallBack;
import framework.net.InnerMsgDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;

/**
 *
 * 描述：
 *
 */
public class ConfigModule implements IBackModule {

	private static final Logger log = LoggerFactory.getLogger(ConfigModule.class);

	/**
	 * @param kernel
	 * @return
	 */
	@Override
	public boolean onInit(BacKernel kernel) {
		return true;
	}

	/**
	 *
	 */
	@Override
	public void onDestroy() {
	}

	public void UpDataConfig(BacKernel kernel, String configId, IDataCallBack cb) {

		ServerMsg.StringSingle.Builder build = ServerMsg.StringSingle.newBuilder();

		build.setWords(configId);

		int msgid = -1;
        switch (configId) {
            case "CanSendItem":
                msgid = ServerMsgDef.B2C_UPDATE_SEND_ITEM_CONFIG.ordinal();
                break;
            case "GameDef":
                msgid = ServerMsgDef.B2C_UPDATE_GAEM_DEF_CONFIG.ordinal();
                break;
            case "RateLimiter":
                msgid = InnerMsgDef.INNER_MSG_B2G_UPDATE_RATE_LIMIT.ordinal();
                break;
			case "PlayerGameDef":
				msgid = ServerMsgDef.B2C_SET_PLAYER_GAME_DEF.ordinal();
				break;
			case "caishen":
                msgid = ServerMsgDef.B2C_SET_STATISTICS_RATIO_CONFIG_CAISHEN.ordinal();
				break;
			case "tuiguang":
                msgid = ServerMsgDef.B2C_SET_STATISTICS_RATIO_CONFIG_TUIGUANG.ordinal();
				break;

        }
		if (msgid==-1){
			log.error("没有对应的通知 {}",configId);
			return;
		}
		kernel.broadToServer("game", msgid, build.build().toByteArray());
		cb.push(new Write());
	}

	public void updateConfig(BacKernel kernel, String key, IDataCallBack cb){
		ServerMsg.StringSingle.Builder build = ServerMsg.StringSingle.newBuilder();
		build.setWords(key);
		kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_MATCH, ServerMsgDef.B2M_UPDATE_CFG.ordinal(), build.build().toByteArray());
		cb.push(new Write());
	}

	public void updateAdsConfig(BacKernel kernel, int op, AdsConfigGameDTO gameDTO, IDataCallBack cb){
		ServerMsg.StringSingle.Builder build = ServerMsg.StringSingle.newBuilder();
		long _start = 0;
		long _end = 0;
		try {
			DateFormat format = kernel.getServer().getDayFormat();
			_start = format.parse(gameDTO.getStartTime()).getTime();
			_end   = format.parse(gameDTO.getEndTime()).getTime();
		} catch (Exception e) {
			e.printStackTrace();
		}
		build.setWords(op + "," + gameDTO.getChannel() + "," + _start + "," + _end);
		kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_GAME, ServerMsgDef.B2G_ADS_CONFIG_CHANGE.ordinal(),build.build().toByteArray());
		cb.push(new Write());
	}
}
