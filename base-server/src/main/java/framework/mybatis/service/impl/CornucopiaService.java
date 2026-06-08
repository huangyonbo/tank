package framework.mybatis.service.impl;

import framework.mybatis.domain.Cornucopia;
import framework.mybatis.mapper.CornucopiaMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CornucopiaService extends AbstractServiceImpl<CornucopiaMapper, Cornucopia> {

    public String AddAllHistory(List<Cornucopia> list){
        saveBatch(list);
        return "success";
    }
    public String AddOne(Cornucopia cornucopia){
        save(cornucopia);
        return "success";
    }

    public List<Cornucopia> GetHistory(int uid,int count){
        List<Cornucopia> list = lambdaQuery().eq(Cornucopia::getUid,uid).orderByDesc(Cornucopia::getCreateTime).last("limit "+count).list();
        return list;
    }
}
