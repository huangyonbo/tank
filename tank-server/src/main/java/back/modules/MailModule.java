package back.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import back.modules.data.ItemData;
import back.modules.data.Write;
import back.modules.data.mailmanage.CheckItem;
import back.modules.data.mailmanage.ItemValue;
import back.modules.data.mailmanage.SendMail;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.back.BacKernel;
import framework.back.IBackModule;
import framework.back.net.IDataCallBack;

/**
 * 
 * 描述：邮件管理
 * 
 */
public class MailModule implements IBackModule {

	private static Logger logger = LoggerFactory.getLogger(MailModule.class);

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

	public void sendMail(BacKernel kernel, SendMail sendMail, IDataCallBack cb) {
		try {
			ServerMsg.SysMail.Builder build = ServerMsg.SysMail.newBuilder();
			int id = sendMail.getId();
			Integer[] receivers = sendMail.getReceiver();
			String title = sendMail.getTitle();
			int[] channelIds = sendMail.getChannelId();
			String content = sendMail.getContent();
			ItemData[] itemDatas = sendMail.getAttachment();
			int expiry = sendMail.getExpiry();
			build.setId(id);
			for (int receiver : receivers) {
				build.addReceivers(receiver);
			}
			build.setTitle(title);
			for (int channel : channelIds) {
				build.addChannelIds(channel);
			}
			build.setContent(content);
			for (ItemData data : itemDatas) {
				ServerMsg.ItemData.Builder info = ServerMsg.ItemData.newBuilder();
				info.setItemId(data.getId());
				info.setCount(data.getCount());
				build.addItemDatas(info.build());
			}
			build.setExpiry(expiry);
			//驱动游戏服按邮件地址、渠道发邮件
			logger.info("send to game to send sys mail ...");
			//Object[] gameServers = kernel.GetServersByType("game");
			kernel.broadToServer("game",ServerMsgDef.B2G_SYSMAIL.ordinal(),build.build().toByteArray());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			cb.push(new Write());
		}
	}

	public void checkAttachment(BacKernel kernel, CheckItem checkItem, IDataCallBack cb) {
		List<ItemValue> list = new ArrayList<>();
		try {
			ServerMsg.StringArray.Builder build = ServerMsg.StringArray.newBuilder();
			String[] items = checkItem.getItem();
			for(String item : items) {
				build.addSomewords(item);
			}
			// 通知到game服去校验有效性
			logger.info("send to game to verify: {}", Arrays.toString(items));
			Object[] gameServers = kernel.getServersByType("game");
			kernel.requestServer((String)gameServers[0], ServerMsgDef.B2G_CHECK_ATTACHMENT.ordinal(), build.build().toByteArray(),
			(byte[] data) -> {
				ServerMsg.StringSingle res;
				try {
					res = ServerMsg.StringSingle.parseFrom(data);
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
				String itemValues = res.getWords();
				String[] itemValArray = itemValues.split(" ");
				for (String prop : itemValArray) {
					ItemValue iv = new ItemValue();
					String[] detail = prop.split(":");
					String itemId = detail[0];
					float val = Float.valueOf(detail[1]);
					iv.setId(itemId);
					if(val < 0) {
						iv.setExist(false);
						iv.setValue(-1);
					} else {
						iv.setExist(true);
						iv.setValue(val);
					}
					list.add(iv);
				}
				checkItem.setRoot(list);
				cb.push(checkItem);
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
