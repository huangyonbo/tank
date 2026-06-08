package game.modules;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.InvalidProtocolBufferException;
import common.ServerMsgDef;
import framework.JsonUtil;
import framework.PropertyKey;
import framework.ServerSet;
import framework.SpringContextUtil;
import framework.game.*;
import framework.mybatis.domain.Config;
import framework.mybatis.domain.SendItems;
import framework.mybatis.service.impl.ConfigService;
import framework.mybatis.service.impl.SendItemsService;
import framework.net.InnerMsgDef;
import framework.net.message.InnerMsg;
import game.custommsg.CommandDef;
import game.custommsg.CustomMsg;
import game.custommsg.RequestMsgDef;
import game.custommsg.ServerCodeDef;
import game.modules.items.ItemModule;
import game.modules.player.PlayerModule;
import game.modules.utils.UtilFunc;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class SendItemModule implements ILogicModule {
    public String CheckSendItemHeart = "HB_CheckItemHeart";
    private PlayerModule m_playerModule;
    private ItemModule m_ItemModule;
    public boolean IsCanSend=false;
    @Override
    public boolean onInit(IKernel kernel) {
        kernel.regEvent(KernelEvent.KEVENT_ON_LINE, "Player", this, "OnPlayerOnLine");
        kernel.declareHeartBeat(CheckSendItemHeart, this, "OnCheckRecord");
        kernel.regRequestMessage(RequestMsgDef.REQ_SEND_ITEM_NEW.ordinal(), this, "OnSendItem");
        kernel.regRequestMessage(RequestMsgDef.REQ_SEND_HISTORY.ordinal(), this, "OnGetHistory");
        kernel.regServerMsg(ServerMsgDef.B2C_UPDATE_SEND_ITEM_CONFIG.ordinal(),this, "OnUpdateSendItemConfig");

//        kernel.regRequestMessage(RequestMsgDef.REQ_CHECK_SEND_PLAYER.ordinal(), this, "OnCheckUid");
        kernel.regCommand(CommandDef.CMD_CHECK_SEND_ITEMS.ordinal(), "Player", this, "OnRecvCheckItem");
        m_playerModule = (PlayerModule) kernel.getModule("PlayerModule");
        m_ItemModule = kernel.getModule(ItemModule.class);
        CompletableFuture.supplyAsync(()->{
            ConfigService configService = SpringContextUtil.getBean(ConfigService.class);
            Config canSendItem = configService.getById("CanSendItem");
           return canSendItem;
        }).thenAccept(config -> {
            log.info(config.getValue());
            if (StringUtils.isNotEmpty(config.getValue())) {
                try {
                    int value = Integer.parseInt(config.getValue());
                    if (value == 1) {
                        IsCanSend = true;
                    }
                } catch (Exception e) {
                    log.info("无法解析配置文件 {}", config.getValue());
                }
            }
        });
        return true;
    }
    void OnUpdateSendItemConfig(IKernel kernel, int serid, int msgid, byte[] msg) throws Exception {
        CompletableFuture.supplyAsync(()->{
            ConfigService configService = SpringContextUtil.getBean(ConfigService.class);
            Config canSendItem = configService.getById("CanSendItem");
            return canSendItem;
        }).thenAccept(config -> {
            log.info(config.getValue());

                try {
                    if (StringUtils.isBlank(config.getValue())) {
                        int value = Integer.parseInt(config.getValue());
                        if (value == 1) {
                            IsCanSend = true;
                        }
                    }
                    log.info("更新配置 CanSendItem {}",IsCanSend);
                }catch (Exception e){
                    log.info("无法解析配置文件 {}",config.getValue());
                }
        });
    }
    public void OnPlayerOnLine(IKernel kernel, IGameObject player) {
        if (kernel.isMain()) {
            // TODO 需要和客户端优化下。
            kernel.addHeartBeat(CheckSendItemHeart, player, 60000, -1);
        }
        this.OnCheckRecord(kernel, player);
    }

    public void OnCheckUid(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
            throws InvalidProtocolBufferException {
        CustomMsg.CheckUidInfo check = CustomMsg.CheckUidInfo.parseFrom(msg);
        int uid = check.getUid();
        String name = kernel.getUserName(uid);

        CustomMsg.CheckUidInfoRes.Builder res = CustomMsg.CheckUidInfoRes.newBuilder();
        res.setName(name == null ? "" : m_playerModule.replaceSensitiveWord(name));
        kernel.response(player, reqid, res.build().toByteArray());
    }

    public void OnCheckRecord(IKernel kernel, IGameObject player) {
        List<Object> params = new ArrayList<>();
        params.add(player.getInt(PLAYER_PROPERTY_UID));
        kernel.executeSomeToStore(SendItemsService.class, "getList", params, cb -> {
            List<String> ids = new ArrayList();
            List<SendItems> list = JsonUtil.decodeToList(cb, SendItems.class);
            list.forEach(data -> {
                String id = data.getId();
                ids.add(id);
                String appendix = data.getAppendix();
                String[] split = appendix.split("\\*");
                m_ItemModule.AddItem(kernel, player, split[0], Integer.parseInt(split[1]), UtilFunc.System.BAG.ordinal(), "赠送者" + data.getSenderUid() + " id:" + data.getId());
            });
            if (ids.isEmpty()) {
                return;
            }
            CustomMsg.String.Builder builder = CustomMsg.String.newBuilder();
            builder.setValue(JsonUtil.encodeToStr(ids));
            kernel.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_DEL_SEND_ITEM_RECORD.ordinal(), builder.build().toByteArray());
        });
    }
    public void OnGetHistory(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg) {
        SendItemsService bean = SpringContextUtil.getBean(SendItemsService.class);
        JSONObject jsonObject = new JSONObject();
        java.util.Date date = DateUtils.addDays(new Date(kernel.getServerTime()), -15);
        if (bean != null) {
            Integer playerUid = player.getInt(PropertyKey.PLAYER_PROPERTY_UID);
            String formattedDate = framework.mybatis.utils.DateUtils.format(date);
            List<SendItems> sendList = querySendItems(bean, playerUid, formattedDate);
            List<SendItems> recList = queryReceivedItems(bean, playerUid, formattedDate);
            List<JSONObject> allItems = Stream.concat(sendList.stream(), recList.stream())
                    .sorted(Comparator.comparing(SendItems::getCreateTime).reversed())
                    .map(this::convertToJsonObject)
                    .collect(Collectors.toList());
            jsonObject.put("list", allItems);
        }
        CustomMsg.String.Builder builder = CustomMsg.String.newBuilder();
        builder.setValue(jsonObject.toJSONString());
        kernel.response(player, reqid, builder.build().toByteArray());
    }

    private List<SendItems> querySendItems(SendItemsService bean, Integer playerUid, String date) {
        return bean.lambdaQuery()
                .eq(SendItems::getSenderUid, playerUid)
                .gt(SendItems::getCreateTime, date)
                .list();
    }

    private List<SendItems> queryReceivedItems(SendItemsService bean, Integer playerUid, String date) {
        return bean.lambdaQuery()
                .eq(SendItems::getRecUid, playerUid)
                .gt(SendItems::getCreateTime, date)
                .list();
    }

    private JSONObject convertToJsonObject(SendItems item) {
        return new JSONObject()
                .fluentPut("id", item.getId())
                .fluentPut("sendId", item.getSenderUid())
                .fluentPut("sendName", item.getSenderName())
                .fluentPut("recId", item.getRecUid())
                .fluentPut("recName", item.getRecName())
                .fluentPut("context", item.getAppendix())
                .fluentPut("creatTime", item.getCreateTime())
                .fluentPut("state", item.getState());//0 发送未读取，1读取，2领取
    }

    public void OnSendItem(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
            throws InvalidProtocolBufferException {
        CustomMsg.SendItem sendItem = CustomMsg.SendItem.parseFrom(msg);
        int uid = sendItem.getUid();
        int pos = sendItem.getPos();
        int count = sendItem.getCount();

        CustomMsg.ServerCode.Builder code = CustomMsg.ServerCode.newBuilder();

        do {
            if (!IsCanSend) {
                code.setCode(ServerCodeDef.CODE_CANT_SEND.ordinal());
                break;
            }
            if (uid == player.getInt(PLAYER_PROPERTY_UID) || kernel.getUserName(uid) == null) {
                code.setCode(ServerCodeDef.CODE_UID_NOT_EXIST.ordinal());
                break;
            }
            if (count <= 0) {
                code.setCode(ServerCodeDef.CODE_COUNT_ILLEGAL.ordinal());
                break;
            }
            IGameObject item = m_ItemModule.GetItem(player, pos);
            if (item == null) {
                code.setCode(ServerCodeDef.CODE_NOT_EXIST.ordinal());
                break;
            } else if (!item.getBool("CanSend")) {
                code.setCode(ServerCodeDef.CODE_CANT_SEND.ordinal());
                break;
            } else if (count > item.getInt("Count")) {
                code.setCode(ServerCodeDef.CODE_NEED_ITEM.ordinal());
                break;
            }
            String itemid = item.getString("Id");
            if (!itemid.equals("item_skill_hbomb")) {
                code.setCode(ServerCodeDef.CODE_NOT_EXIST.ordinal());
                break;
            }
            int vipStatus = player.getInt(PropertyKey.PLAYER_INVITER_VIP_STATUS);
            if (vipStatus == 0) {
                int bindId = player.getInt(PropertyKey.PLAYER_INVITER_BIND_ID);
                if (bindId != uid) {
                    code.setCode(ServerCodeDef.CODE_NOT_VIP_NOT_SEND.ordinal());
                    break;
                }
            }
//            int proxyid = player.getInt(PropertyKey.PLAYER_PROPERTY_PROPERTY_PROXY_ID);
//            if (proxyid != kernel.getProxyId(uid)) {
//                code.setCode(ServerCodeDef.CODE_PROXY_DIFFERENT.ordinal());
//                log.info("{} {}  {} {}", player.getInt(PLAYER_PROPERTY_UID), proxyid, uid, kernel.getProxyId(uid));
//                break;
//            }
            count = m_ItemModule.SubItem(kernel, player, pos, count, UtilFunc.System.SEND_ITEM.ordinal(),
                    "Give to " + uid);

            this.SendItemMail(kernel, player, uid, "SendItem", "SendItemContext", -1, "item_skill_hbomb" + "*" + count);

            // 邮寄预警

            code.setCode(ServerCodeDef.CODE_SUCCESS.ordinal());

            // 邮件发送后，立即通知对方自动领取 add by 胡中伟, 2019年3月27日 下午8:06:05
            kernel.commandPlayer(uid, CommandDef.CMD_CHECK_SEND_ITEMS.ordinal());

        } while (false);
        kernel.response(player, reqid, code.build().toByteArray());
    }

    // 增加道具邮件接口（该类型邮件对客户端不可见） add by 胡中伟, 2019年3月27日 下午8:10:32
    public void SendItemMail(IKernel kernel, IGameObject player, int recvuid, String title, String context, long lifeTime,
                             String appendix) {
        if (player == null || player.getType() != GameObjectType.GOTYPE_PLAYER) {
            log.error("SendItemMail player not valid");
            return;
        }

        String recvName = "";
        if (recvuid != -1) {
            recvName = kernel.getUserName(recvuid);
            if (recvName == null) {
                log.error("recvName is null, uid:{}", recvuid);
                return;
            }
        }
        InnerMsg.SendMail.Builder build = InnerMsg.SendMail.newBuilder();
        build.setChannel(-1);
        build.setSenderuid(player.getInt(PLAYER_PROPERTY_UID));
        build.setSendername(player.getString(PLAYER_PROPERTY_NAME));
        build.setRecvuid(recvuid);
        build.setRecvname(recvName);
        build.setLifetime(lifeTime);
        build.setAppendix(appendix);
        kernel.getServer().sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_SEND_ITEM.ordinal(), build.build().toByteArray());
    }

    void OnRecvCheckItem(IKernel kernel, IGameObject player, Object... objects) {
        // 等待1秒后，检测邮件
        kernel.addHeartBeat(CheckSendItemHeart, player, 1000, 1);
    }
}
