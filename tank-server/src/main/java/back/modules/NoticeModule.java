package back.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import back.modules.data.Write;
import back.modules.data.noticemanage.PubNotice;
import back.modules.data.noticemanage.RepNotice;
import back.modules.dataenum.Code;
import back.modules.dataenum.NoticeType;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.back.BacKernel;
import framework.back.IBackModule;
import framework.back.net.IDataCallBack;

/**
 * 
 * 描述：公告
 * 
 */
public class NoticeModule implements IBackModule {

	private static Logger logger = LoggerFactory.getLogger(NoticeModule.class);

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

	public void pubNotice(BacKernel kernel, PubNotice pn, IDataCallBack cb) {

		try {
			ServerMsg.BackPubNotice.Builder build = ServerMsg.BackPubNotice.newBuilder();
			build.setId(pn.getId());
			build.setType(pn.getType());
			int[] placeIds = pn.getPlaceId();
			int count = placeIds.length;
			for (int i = 0; i < count; i++) {
				build.addPlaceId(placeIds[i]);
			}

			build.setTag(pn.getTag());
			build.setTitle(pn.getTitle());
			build.setContent(pn.getContent());
			build.setPicture(pn.getPicture());
			build.setMail(pn.isMail());

			if (pn.getType() == NoticeType.LOGIN.ordinal()) {
				// 存储到pub服
				logger.info("send to pub to save login notice ...");
				kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.B2P_PUBNOTICE.ordinal(), build.build().toByteArray());
			} else {// 游戏公告
				if (pn.isMail()) {
					// 发送公告邮件（邮件有效期30天）
					for (int placeid : placeIds) {
						logger.info("isMail true, send game notice mail for channel: {}", placeid);
						kernel.sendSystemMail(-1, placeid, pn.getTitle(), pn.getContent(), 30 * 24 * 60 * 60 * 1000L, "");
					}
				} else {
					logger.info("isMail false!");
				}
				// 驱动游戏服按渠道run跑马灯（3次，间隔1分钟）
				logger.info("send to game to scroll notice ...");
				Object[] gameServers = kernel.getServersByType("game");
				kernel.sendServerMsg((String)gameServers[0], ServerMsgDef.B2G_GAMENOTICE.ordinal(), build.build().toByteArray());
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// 后台页面要一个返回来终止连接
			cb.push(new Write());
		}
	}

	public void repNotice(BacKernel kernel, RepNotice rp, IDataCallBack cb) {

		try {
			ServerMsg.BackPubNotice.Builder build = ServerMsg.BackPubNotice.newBuilder();
			build.setId(rp.getId());
			build.setType(rp.getType());

			// 通知到pub服去删除
			logger.info("send to pub to del notice, id: {}", rp.getId());
			kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.B2P_REPNOTICE.ordinal(), build.build().toByteArray());
			cb.push(new Write());
			return;
		} catch (Exception e) {
			e.printStackTrace();
		}
		cb.push(new Write(Code.FAIL));
	}
}
