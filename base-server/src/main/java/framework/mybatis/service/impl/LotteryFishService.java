package framework.mybatis.service.impl;

import framework.mybatis.domain.LotteryFish;
import framework.mybatis.mapper.LotteryFishMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LotteryFishService extends AbstractServiceImpl<LotteryFishMapper, LotteryFish> {

    public List<LotteryFish> loadAll() {
        return list();
    }

    public boolean updateLotteryValue(Integer roomId,Integer roomType,Integer value) {
        return lambdaUpdate().eq(LotteryFish::getId,roomId).eq(LotteryFish::getRoomType,roomType).set(LotteryFish::getLotteryValue,value).update();
    }

    public boolean updateFishValue(Integer roomId,Integer roomType,Integer value) {
        return lambdaUpdate().eq(LotteryFish::getId,roomId).eq(LotteryFish::getRoomType,roomType).set(LotteryFish::getFishValue,value).update();
    }

    public boolean resetValue(Integer roomId,Integer roomType){
        return lambdaUpdate().eq(LotteryFish::getId,roomId).eq(LotteryFish::getRoomType,roomType).set(LotteryFish::getFishValue,0).set(LotteryFish::getLotteryValue,0).update();
    }
}
