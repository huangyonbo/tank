package framework.mybatis.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import framework.mybatis.domain.FullGameItems;
import framework.mybatis.domain.GuildRepoRecord;
import framework.mybatis.mapper.FullGameItemsMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FullGameItemsService extends AbstractServiceImpl<FullGameItemsMapper, FullGameItems> {

    public FullGameItems getByItem(String itemNames){
        return lambdaQuery().eq(FullGameItems::getItem, itemNames).one();
    }
}
