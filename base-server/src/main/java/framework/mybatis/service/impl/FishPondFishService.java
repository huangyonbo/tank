package framework.mybatis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import framework.mybatis.domain.FishPondFish;
import framework.mybatis.mapper.FishPondFishMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FishPondFishService extends AbstractServiceImpl<FishPondFishMapper, FishPondFish> {

    public FishPondFish queryById(Integer id) {
        return getById(id);
    }

    public List<FishPondFish> loadAll() {
        return list();
    }

    public int getMaxIndex(){
        Integer count = lambdaQuery().count();
        if (count == 0) {
            return 0;
        }
        QueryWrapper<FishPondFish> queryWrapper = new QueryWrapper<FishPondFish>().select("MAX(id) as maxId");
        return getObj(queryWrapper, obj -> Integer.parseInt(obj.toString()));
    }

    public boolean addFish(FishPondFish fishPond){
        return save(fishPond);
    }

    public boolean updateFish(FishPondFish fishPond){
        return updateById(fishPond);
    }

    public boolean deleteFish(Integer index){
        return removeById(index);
    }

    public void deleteAll() {
        lambdaUpdate().remove();
    }
}
