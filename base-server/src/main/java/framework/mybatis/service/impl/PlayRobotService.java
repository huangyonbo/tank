package framework.mybatis.service.impl;

import framework.mybatis.domain.PlayRobot;
import framework.mybatis.mapper.PlayRobotMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlayRobotService extends AbstractServiceImpl<PlayRobotMapper, PlayRobot> {

    public PlayRobot queryById(Integer uid){
        return getById(uid);
    }

    public List<PlayRobot> loadAll() {
        return list();
    }

    public boolean updateRobot(Integer uid,Integer state){
        return lambdaUpdate().eq(PlayRobot::getId,uid).set(PlayRobot::getStatus,state).update();
    }

    public boolean updateRobot(Integer uid,Integer level,Long diamond,Long gold,String bag){
        return lambdaUpdate().eq(PlayRobot::getId,uid).
                set(PlayRobot::getLevel,level).
                set(PlayRobot::getDiamond,diamond).
                set(PlayRobot::getGold,gold).
                set(PlayRobot::getBag,bag).
                update();
    }
}
