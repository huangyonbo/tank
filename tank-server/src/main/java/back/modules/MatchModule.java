package back.modules;

import back.modules.data.Write;
import back.modules.data.matchset.Match;
import back.modules.data.matchset.MatchRunData;
import back.modules.data.room.ArenaSettingGameDTO;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.back.BacKernel;
import framework.back.IBackModule;
import framework.back.net.IDataCallBack;

import java.util.ArrayList;
import java.util.List;

public class MatchModule implements IBackModule {

    @Override
    public boolean onInit(BacKernel kernel) {
        return true;
    }

    @Override
    public void onDestroy() {

    }

    public void updateArenaSetting(BacKernel kernel, ArenaSettingGameDTO gameDTO, IDataCallBack cb) {
        ServerMsg.SetArena.Builder build = ServerMsg.SetArena.newBuilder();
        build.setId(gameDTO.getId());
        build.setIcon(gameDTO.getIcon());
        build.setName(gameDTO.getName());
        build.setChannel(gameDTO.getChannel());
        build.setType(gameDTO.getType());
        build.setTime(gameDTO.getKeepTime());
        build.setSign(gameDTO.getSign());
        build.setStart(gameDTO.getStartTime());
        build.setOver(gameDTO.getEndTime());
        build.setMaxPlayer(gameDTO.getMaxCapacity());
        build.setMinPlayer(gameDTO.getMinCapacity());
        build.setInterval(gameDTO.getIntervalTime());
        build.setProtect(gameDTO.getProtectTime());
        build.setSettlement(gameDTO.getClearingTime());
        build.setNo1Tip(gameDTO.getMarquee());
        build.setReward(gameDTO.getAward());
        build.setCoin(gameDTO.getMoneyCount());
        build.setMaxbv(gameDTO.getMaxBullet());
        build.setMinbv(gameDTO.getMinBullet());
        build.setDefbv(gameDTO.getDefaultBullet());
        build.setOffbv(gameDTO.getBulletChange());
        build.setLimit(gameDTO.getEnterLimit());
        build.setMaxCount(gameDTO.getJoinTimes());
        build.setOccupy(gameDTO.getOccupy());
        build.setMaxTurn(gameDTO.getMaxRound());

        kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.B2P_SET_ARENA.ordinal(), build.build().toByteArray());
        cb.push(new Write());
    }


    public void createMatch(BacKernel kernel, Match match, IDataCallBack cb) {
        ServerMsg.SetArena.Builder build = ServerMsg.SetArena.newBuilder();
        build.setId(match.getId());
        build.setIcon(match.getIcon());
        build.setName(match.getName());
        build.setChannel(match.getChannel());
        build.setType(match.getType());
        build.setTime(match.getTime());
        build.setSign(match.getSign());
        build.setStart(match.getBegin());
        build.setOver(match.getOver());
        build.setMaxPlayer(match.getMaxPlayer());
        build.setMinPlayer(match.getMinPlayer());
        build.setInterval(match.getInterval());
        build.setProtect(match.getProtect());
        build.setSettlement(match.getSettlement());
        build.setNo1Tip(match.isNo1Tip());
        build.setReward(match.getReward());
        build.setCoin(match.getCoin());
        build.setMaxbv(match.getMaxBV());
        build.setMinbv(match.getMinBV());
        build.setDefbv(match.getDefBV());
        build.setOffbv(match.getOffBV());
        build.setLimit(match.getEnterLimit());
        build.setMaxCount(match.getMaxCount());
        build.setOccupy(match.getOccupy());
        build.setMaxTurn(match.getMaxTurn());

        kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.B2P_SET_ARENA.ordinal(), build.build().toByteArray());
        cb.push(new Write());
    }

    public void delMatch(BacKernel kernel, int id, IDataCallBack cb) {
        ServerMsg.IntSingle.Builder build = ServerMsg.IntSingle.newBuilder();
        build.setIntMember(id);

        kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.B2P_DEL_ARENA.ordinal(), build.build().toByteArray());
        cb.push(new Write());
    }

    public void updateMatch(BacKernel kernel, Match match, IDataCallBack cb) {
        ServerMsg.SetArena.Builder build = ServerMsg.SetArena.newBuilder();
        build.setId(match.getId());
        build.setIcon(match.getIcon());
        build.setName(match.getName());
        build.setChannel(match.getChannel());
        build.setType(match.getType());
        build.setTime(match.getTime());
        build.setSign(match.getSign());
        build.setStart(match.getBegin());
        build.setOver(match.getOver());
        build.setMaxPlayer(match.getMaxPlayer());
        build.setMinPlayer(match.getMinPlayer());
        build.setInterval(match.getInterval());
        build.setProtect(match.getProtect());
        build.setSettlement(match.getSettlement());
        build.setNo1Tip(match.isNo1Tip());
        build.setReward(match.getReward());
        build.setCoin(match.getCoin());
        build.setMaxbv(match.getMaxBV());
        build.setMinbv(match.getMinBV());
        build.setDefbv(match.getDefBV());
        build.setOffbv(match.getOffBV());
        build.setLimit(match.getEnterLimit());
        build.setMaxCount(match.getMaxCount());
        build.setOccupy(match.getOccupy());
        build.setMaxTurn(match.getMaxTurn());

        kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.B2P_SET_ARENA.ordinal(), build.build().toByteArray());
        cb.push(new Write());
    }


    public void getMatchRunData(BacKernel kernel, List<Integer> ids, IDataCallBack cb) {
        ServerMsg.IntArray.Builder build = ServerMsg.IntArray.newBuilder();
        if (ids.isEmpty()) {
            cb.push(null);
            return;
        }
        ids.forEach(build::addId);
        kernel.requestServer(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.B2P_GET_ARENA.ordinal(), build.build().toByteArray(), (byte[] res) ->
        {
            ServerMsg.ArenaData arenaData = null;
            try {
                arenaData = ServerMsg.ArenaData.parseFrom(res);
            } catch (Exception e) {
                e.printStackTrace();
                cb.push(null);
                return;
            }

            int count = arenaData.getIdCount();
            List<MatchRunData> list = new ArrayList<>();
            for (int i = 0; i < count; ++i) {
                MatchRunData data = new MatchRunData();
                data.setId(arenaData.getId(i));
                data.setTurn(arenaData.getTurnid(i));
                data.setApply(arenaData.getSignCount(i));
                data.setPlayCount(arenaData.getJoinCount(i));
                data.setPlayPop(arenaData.getJoinPop(i));
                data.setOnline(arenaData.getOnline(i));
                list.add(data);
            }
            cb.push(list)                                                    ;
        });
    }

    public void stopMatch(BacKernel kernel, int id, IDataCallBack cb) {
        ServerMsg.IntSingle.Builder build = ServerMsg.IntSingle.newBuilder();
        build.setIntMember(id);

        kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.B2P_CLOSE_ARENA.ordinal(), build.build().toByteArray());
        cb.push(new Write());
    }

    public void startMatch(BacKernel kernel, int id, IDataCallBack cb) {
        ServerMsg.IntSingle.Builder build = ServerMsg.IntSingle.newBuilder();
        build.setIntMember(id);
        kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.B2P_OPEN_ARENA.ordinal(), build.build().toByteArray());
        cb.push(new Write());
    }
}
