package back.modules;

import back.modules.data.Write;
import back.modules.data.player.DeductItemGameDTO;
import back.modules.data.player.PlayerPlayWinGameDTO;
import back.modules.data.playermanage.ShutUpPlayer;
import com.google.protobuf.InvalidProtocolBufferException;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.ByteUtils;
import framework.ServerSet;
import framework.SpringContextUtil;
import framework.back.BacKernel;
import framework.back.GameObjectData;
import framework.back.IBackModule;
import framework.back.net.IDataCallBack;
import framework.game.Record;
import framework.game.ValueType;
import framework.mybatis.domain.Roles;
import framework.mybatis.service.impl.RolesService;
import framework.net.InnerMsgDef;
import framework.net.message.InnerMsg;
import game.constant.OfflineDataType;
import game.modules.items.XmlPropertyItem;
import game.modules.utils.UtilFunc;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.mina.core.buffer.IoBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 描述：玩家管理
 */
@Slf4j
public class PlayerModule implements IBackModule {

    @Override
    public boolean onInit(BacKernel kernel) {
        kernel.regServerMsg(ServerMsgDef.GM2Back_SYNC_ALL_PLAYER_DATA.ordinal(), this, "SyncAllPlayerState");
        return IBackModule.super.onInit(kernel);
    }

    public void listOnlinePlayer(BacKernel kernel, List<Integer> uidList, IDataCallBack cb) {
        if (CollectionUtils.isEmpty(uidList)) {
            IoBuffer buffer = IoBuffer.allocate(4);
            buffer.putInt(0);
            buffer.flip();
            cb.push(buffer.array());
            return;
        }
        // 在线玩家ID
        uidList = uidList.stream().filter(uid -> kernel.getPlayerServer(uid).length() != 0).collect(Collectors.toList());
        ServerMsg.IntList.Builder list = ServerMsg.IntList.newBuilder();
        list.addAllElement(uidList);
        // 通知游戏查询
        kernel.requestServer(ServerSet.SERVER_LOGIC_NAME_GAME, ServerMsgDef.B2G_GET_ONLINE_PLAYER_LIST.ordinal(),
                list.build().toByteArray(), cb::push);

    }

    public void listOnLinePlayerBombCoin(BacKernel kernel, List<Integer> uidList, IDataCallBack cb) {
        if (CollectionUtils.isEmpty(uidList)) {
            IoBuffer buffer = IoBuffer.allocate(16);
            buffer.putLong(0);
            buffer.putLong(0);
            buffer.flip();
            cb.push(buffer.array());
            return;
        }
        uidList = uidList.stream().filter(uid -> kernel.getPlayerServer(uid).length() != 0).collect(Collectors.toList());
        ServerMsg.IntList.Builder list = ServerMsg.IntList.newBuilder();
        list.addAllElement(uidList);
        kernel.requestServer(ServerSet.SERVER_LOGIC_NAME_GAME, ServerMsgDef.B2G_GET_ONLINE_PLAYER_BombCount.ordinal(),
                list.build().toByteArray(), cb::push);
    }

    // 后台扣除道具（重构）
    public void deductItem(BacKernel kernel, DeductItemGameDTO gameDTO, IDataCallBack cb) throws Exception {
        String server = kernel.getPlayerServer(gameDTO.getUid());
        if (StringUtils.isEmpty(server)) {
            // 玩家不在线
            kernel.getServer().request(ServerSet.SERVER_LOGIC_NAME_STORE, InnerMsgDef.INNER_MSG_REQ_ROLE_PARAM.ordinal(), String.valueOf(gameDTO.getUid()).getBytes(), r -> {
                InnerMsg.RoleParam roleData;
                try {
                    roleData = InnerMsg.RoleParam.parseFrom(r);
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                    cb.push(new Write("玩家不存在"));
                    return;
                }
                IoBuffer buffer = IoBuffer.wrap(roleData.getParam().toByteArray());
                GameObjectData player = new GameObjectData(true);
                player.loadFromArchive(buffer);
                player.setUid(gameDTO.getUid());
                String itemId = gameDTO.getItemId();
                kernel.requestServer(ServerSet.SERVER_LOGIC_NAME_GAME, ServerMsgDef.B2G_REQ_PROPERTY_ITEM.ordinal(), itemId.getBytes(), res -> {
                    if (res.length != 0) {
                        // 扣除属性道具
                        try {
                            XmlPropertyItem item = ByteUtils.byteToObject(res);
                            String property = item.getProperty();
                            if (player.GetProType(property) == ValueType.LONG) {

                                player.setProperty(property, Math.max(player.getLong(property) - gameDTO.getCount(), 0L));
                            } else {
                                player.setProperty(property, Math.max(player.getInt(property) - gameDTO.getCount(), 0));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            cb.push(new Write("扣除失败"));
                            return;
                        }

                    } else {
                        // 扣除非属性道具
                        GameObjectData itemBag = player.getChildByName("ItemBag");
                        if (itemBag != null) {
                            for (int i = 0; i < itemBag.getChildCount(); ++i) {
                                GameObjectData ownItem = itemBag.getChild(i);
                                if (ownItem == null) {
                                    continue;
                                }
                                if (itemId.equals(ownItem.getConfig())) {
                                    int count = ownItem.getInt("Count");
                                    ownItem.setProperty("Count", Math.max(count - gameDTO.getCount(), 0));
                                    // 日志
                                    int vipLevel = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
                                    int level = player.getInt(PLAYER_PROPERTY_LEVEL);
                                    long gold = player.getLong(PLAYER_PROPERTY_GOLD);
                                    long diamond = player.getLong(PLAYER_PROPERTY_DIAMOND);
                                    long colorTicket = player.getLong(PLAYER_PROPERTY_COLORTICKET);
                                    long bombCoin = player.getLong(PLAYER_PROPERTY_BOMB_COIN);
                                    kernel.addPlayerLog(itemId, gameDTO.getUid(),
                                            vipLevel, level, gold, diamond, bombCoin, colorTicket, 7,
                                            UtilFunc.System.BACK_DEDUCT_ITEM.ordinal(),
                                            count + ",-" + gameDTO.getCount() + "," + ownItem.getInt("Count"),
                                            UtilFunc.System.BACK_DEDUCT_ITEM.getLabel());
                                }
                            }
                        }
                    }
                    kernel.storePlayerData(player);
                    player.clear();
                    cb.push(new Write());
                });
            });
        } else {
            // 玩家在线
            kernel.requestServer(server, ServerMsgDef.B2G_REQ_DEDUCT_ITEM.ordinal(), ByteUtils.objectToByte(gameDTO),
                    res -> cb.push(res.length == 0 ? new Write() : new Write("扣除失败")));
        }
    }
    /**
     * 同步所有玩家的炸弹币和炸弹物品数量
     */
    void SyncAllPlayerState(BacKernel kernel, int serid, int msgid, byte[] data) throws Exception {
        RolesService rolesService = SpringContextUtil.getBean(RolesService.class);
        String itemId = "item_skill_hbomb";
        List<Integer> roleIds = rolesService.loadAllRoles();
        if (CollectionUtils.isEmpty(roleIds)) {
            log.info("没有需要同步的角色数据");
            return;
        }
        
        log.info("开始同步 {} 个角色的炸弹币和物品数据", roleIds.size());
        int successCount = 0;
        int failCount = 0;
        
        for (Integer roleId : roleIds) {
            try {
                Roles roleData = rolesService.getById(roleId);
                if (roleData == null || roleData.getParam() == null) {
                    log.warn("角色 {} 数据不存在或参数为空", roleId);
                    failCount++;
                    continue;
                }
                
                // 解析玩家数据
                IoBuffer buffer = IoBuffer.wrap(roleData.getParam());
                GameObjectData player = new GameObjectData(true);
                player.loadFromArchive(buffer);
                player.setUid(roleId);
                
                // 获取炸弹币数量
                long coinCount = player.getLong(PLAYER_PROPERTY_BOMB_COIN);
                
                // 查找并获取炸弹物品数量
                long itemCount = 0;
                GameObjectData itemBag = player.getChildByName("ItemBag");
                if (itemBag != null) {
                    for (int i = 0; i < itemBag.getChildCount(); ++i) {
                        GameObjectData ownItem = itemBag.getChild(i);
                        if (ownItem == null) {
                            continue;
                        }
                        if (itemId.equals(ownItem.getConfig())) {
                            itemCount = ownItem.getInt("Count");
                            player.setProperty(PLAYER_PROPERTY_BOMB_ITEM, (int) itemCount);
                            break; // 找到后退出循环
                        }
                    }
                }
                
                // 更新数据库
                boolean updated = rolesService.UpdateBomCoinCountAndBomCoin(roleId, coinCount, itemCount);
                if (updated) {
                    successCount++;
                    log.debug("成功同步角色 {} 的数据: 炸弹币={}, 物品数量={}", roleId, coinCount, itemCount);
                } else {
                    failCount++;
                    log.warn("更新角色 {} 的数据失败", roleId);
                }
                
                // 清理资源
                player.clear();
            } catch (Exception e) {
                failCount++;
                log.error("同步角色 {} 数据时发生异常", roleId, e);
            }
        }
        
        log.info("同步完成: 成功 {} 个, 失败 {} 个", successCount, failCount);
    }
    public void freezePlayer(BacKernel kernel, int uid, IDataCallBack cb) {
        log.info("--------- freeze player:{}", uid);
        kernel.frozen(uid);
        cb.push(new Write());
    }

	public void unfreezePlayer(BacKernel kernel, int uid, IDataCallBack cb) {
		log.info("--------- unfreeze player:{}", uid);
		kernel.unFrozen(uid);
		cb.push(new Write());
	}

	public void shutUpPlayer(BacKernel kernel, ShutUpPlayer shutUpPlayer, IDataCallBack cb) {
		int uid = shutUpPlayer.getUserId();
		long time = shutUpPlayer.getType();
		log.info("--------- shutup time:{}", time);
		String server = kernel.getPlayerServer(uid);
		if (server == null || server.length() == 0) {
			// 玩家不在线
			String context = String.valueOf(time);
			kernel.loadPlayerFromDB(uid, true, null, (GameObjectData data, Object arg) -> {
				if (data != null) {
					long shutUp = data.getLong(PLAYER_PROPERTY_SHUTUP);
					data.setProperty(PLAYER_PROPERTY_SHUTUP, shutUp + time);
					kernel.storePlayerData(data);
				}
			});
			// 通知到game服OfflineDataModule去禁言
			kernel.addOfflineData(uid, OfflineDataType.SHUT_UP, context, "offline shutup");
		} else {
			// player is online
			ServerMsg.ChatSet.Builder build = ServerMsg.ChatSet.newBuilder();
			build.setPlayerId(uid);
			build.setTime(time);
			// 通知到game服chatModule去禁言
			log.info("send to chatModule to shutup uid:{}...", uid);
			kernel.sendServerMsg(server, ServerMsgDef.MMSG_CHAT_SET.ordinal(), build.build().toByteArray());
		}
		cb.push(new Write());
	}



	public void listRoomPlayer(BacKernel kernel, int roomId, IDataCallBack cb) {

		ServerMsg.IntSingle.Builder build = ServerMsg.IntSingle.newBuilder();
		build.setIntMember(roomId);

		Object[] sers = kernel.getServersByType("game");
		for (Object ser : sers) {
			kernel.requestServer(ser.toString(), ServerMsgDef.B2G_ROOM_PLAYER_DATA.ordinal(),
					build.build().toByteArray(), (byte[] resData) -> {

						ServerMsg.RoomPlayerData player = null;
						try {
							player = ServerMsg.RoomPlayerData.parseFrom(resData);
						} catch (Exception e) {
							e.printStackTrace();
						}
						if (player != null) {
							cb.push(player.getUidList());
						} else {
							cb.push(Collections.emptyList());
						}
					});
		}
	}

    public void getPlayData(BacKernel kernel, int uid, IDataCallBack cb) {
        String server = kernel.getPlayerServer(uid);
        List<PlayerPlayWinGameDTO> resList = new ArrayList<>();
        if (server == null || server.length() == 0) {
            // 玩家不在线
            kernel.loadPlayerFromDB(uid, false, uid, (GameObjectData obj, Object _uid) -> {
                Record rec = obj.getRecord("TotalPlayWin");
                int rows = rec.getRows();
                for (int i = 0; i < rows; ++i) {
                    PlayerPlayWinGameDTO dto = new PlayerPlayWinGameDTO();
                    dto.setRoomId(i);
                    dto.setPlay(rec.getLong(i, 0));
                    dto.setWin(rec.getLong(i, 1));
                    resList.add(dto);
                }
                cb.push(resList);
            });
        } else {
            ServerMsg.IntSingle.Builder builder = ServerMsg.IntSingle.newBuilder();
            builder.setIntMember(uid);
            kernel.requestServer(server, ServerMsgDef.B2G_GET_TOTAL_PW.ordinal(), builder.build().toByteArray(),
                    (byte[] msgData) -> {
                        ServerMsg.TotalPW msg = null;
                        try {
                            msg = ServerMsg.TotalPW.parseFrom(msgData);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return;
                        }
                        int count = msg.getPlayCount();
                        for (int i = 0; i < count; ++i) {
                            PlayerPlayWinGameDTO dto = new PlayerPlayWinGameDTO();
                            dto.setRoomId(i);
                            dto.setPlay(msg.getPlay(i));
                            dto.setWin(msg.getWin(i));
                            resList.add(dto);
                        }
                        cb.push(resList);
                    });
        }
    }

    public void UpPlayerVip(BacKernel kernel, int uid, int vip, IDataCallBack cb) {
        log.info("back set invite vip  uid:{},  vip:{} ", uid, vip);
        String server = kernel.getPlayerServer(uid);
        if (server == null || server.isEmpty()) {
            cb.push(new Write());
        } else {
            ServerMsg.InviteVip.Builder builder = ServerMsg.InviteVip.newBuilder();
            builder.setUid(uid);
            builder.setVip(vip);
            kernel.requestServer(server, ServerMsgDef.B2G_SET_INVITE_VIP.ordinal(), builder.build().toByteArray(), (byte[] msgData) -> {
                cb.push(new Write());
            });
        }
        RolesService rolesService = SpringContextUtil.getBean(RolesService.class);
        rolesService.UpdateInviteViP(uid, vip);
    }

    public void putPlayData(BacKernel kernel, int uid, List<PlayerPlayWinGameDTO> dtoList, IDataCallBack cb) {
        String server = kernel.getPlayerServer(uid);
        if (server == null || server.length() == 0) {
            // 玩家不在线
            kernel.loadPlayerFromDB(uid, true, uid, (GameObjectData obj, Object _uid) -> {
                Record rec = obj.getRecord("TotalPlayWin");
                for (PlayerPlayWinGameDTO dto : dtoList) {
                    int id = dto.getRoomId();
                    rec.setValue(id, 0, dto.getPlay());
                    rec.setValue(id, 1, dto.getWin());
                }
                kernel.storePlayerData(obj);
                cb.push(new Write());
            });
        } else {
            ServerMsg.TotalPW.Builder builder = ServerMsg.TotalPW.newBuilder();
            builder.setUid(uid);
            for (PlayerPlayWinGameDTO dto : dtoList) {
                int id = dto.getRoomId();
                builder.addId(id);
                builder.addPlay(dto.getPlay());
                builder.addWin(dto.getWin());
            }

			kernel.requestServer(server, ServerMsgDef.B2G_SET_TOTAL_PW.ordinal(), builder.build().toByteArray(),
					(byte[] msgData) -> {
						cb.push(new Write());
					});
		}
	}
}
