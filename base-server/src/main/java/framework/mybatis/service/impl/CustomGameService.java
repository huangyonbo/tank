package framework.mybatis.service.impl;

import framework.mybatis.domain.CustomGame;
import framework.mybatis.mapper.CustomGameMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class CustomGameService extends AbstractServiceImpl<CustomGameMapper, CustomGame> {

    public CustomGame queryById(Integer id) {
        return lambdaQuery().eq(CustomGame::getId,id).one();
    }

    public List<CustomGame> loadAll() {
        return list();
    }

    public boolean updateOnline(){
        return lambdaUpdate().set(CustomGame::getOnline, 0).update();
    }

    public boolean updateOneOnline(Integer deskId, Integer online, Long totalPlay, Long totalWin){
        return lambdaUpdate().
                set(CustomGame::getOnline, online).
                set(CustomGame::getTotalPlay, totalPlay).
                set(CustomGame::getTotalWin, totalWin).
                eq(CustomGame::getId, deskId).
                update();
    }

    public boolean UpdateOrAdd(CustomGame game){
        CustomGame customGame = getById(game.getId());
        return customGame == null ? save(game) : lambdaUpdate().
                set(game.getCreateBy() != null, CustomGame::getCreateBy, game.getCreateBy()).
                set(CustomGame::getEnterLimit, game.getEnterLimit()).
                set(CustomGame::getType, game.getType()).
                set(CustomGame::getRoomType, game.getRoomType()).
                set(CustomGame::getOnline, game.getOnline()).
                set(CustomGame::getTotalPlay, game.getTotalPlay()).
                set(CustomGame::getTotalWin, game.getTotalWin()).
                set(CustomGame::getStatus, game.getStatus()).
                set(CustomGame::getLevel, game.getLevel()).
                set(CustomGame::getMinBv, game.getMinBv()).
                set(CustomGame::getMaxBv, game.getMaxBv()).
                set(CustomGame::getPasswd, game.getPasswd()).
                eq(CustomGame::getId, game.getId()).
                update();
    }

    public List<CustomGame> saveList(List<CustomGame> params){
        params.forEach(a->{
            save((CustomGame)a);
        });
        return list();
    }

}
