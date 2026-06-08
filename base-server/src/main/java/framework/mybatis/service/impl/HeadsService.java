package framework.mybatis.service.impl;

import framework.mybatis.domain.Heads;
import framework.mybatis.mapper.HeadsMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HeadsService extends AbstractServiceImpl<HeadsMapper, Heads> {

    public Heads queryById(Integer id) {
        return getById(id);
    }

    public String queryUrlById(Integer id) {
        Heads head = getById(id);
        if (head == null) {
            return "";
        } else {
            return head.getUrl();
        }
    }

    public Heads queryIdByUid(Integer uid) {
        List<Heads> res = lambdaQuery().eq(Heads::getUid,uid).list();
        if (res.size() <= 0) {
            return null;
        }
        return res.get(0);
    }

    public List<Heads> loadAll() {
        return list();
    }
}
