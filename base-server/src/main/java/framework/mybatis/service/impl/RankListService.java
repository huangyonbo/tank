package framework.mybatis.service.impl;

import framework.mybatis.domain.RankList;
import framework.mybatis.mapper.RankListMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RankListService extends AbstractServiceImpl<RankListMapper, RankList> {

    public List<RankList> listAll() {
        return list();
    }
}
