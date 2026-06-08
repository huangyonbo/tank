package framework.mybatis.service.impl;

import framework.mybatis.domain.PlayerDailyPlayData;
import framework.mybatis.mapper.PlayerDailyPlayDataMapper;
import org.springframework.stereotype.Service;

@Service
public class PlayerDailyPlayDataService extends AbstractServiceImpl<PlayerDailyPlayDataMapper, PlayerDailyPlayData> {

    public void addOrUpdatePlayerDailyData(PlayerDailyPlayData newData){
        PlayerDailyPlayData oldData = lambdaQuery().eq(PlayerDailyPlayData::getUid,newData.getUid()).eq(PlayerDailyPlayData::getDate,newData.getDate()).eq(PlayerDailyPlayData::getRoomId,newData.getRoomId()).one();
        if (oldData == null) {
            save(newData);
        } else {
            oldData.setNickName(newData.getNickName());
            oldData.setVipLevel(newData.getVipLevel());
            oldData.setRoomDf(newData.getRoomDf());
            oldData.setItemHas(newData.getItemHas());
            oldData.setMojin(newData.getMojin());
            oldData.setDayCost(newData.getDayCost());
            oldData.setDayWin(newData.getDayWin());
            oldData.setDayPlayTime(newData.getDayPlayTime());
            oldData.setDayEnterTimes(newData.getDayEnterTimes());
            updateById(oldData);
        }
    }
}
