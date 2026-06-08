package framework.mybatis.service.impl;

import framework.mybatis.domain.RedeemCode;
import framework.mybatis.mapper.RedeemCodeMapper;
import org.springframework.stereotype.Service;


@Service
public class RedeemCodeService extends AbstractServiceImpl<RedeemCodeMapper, RedeemCode> {

    public RedeemCode queryById(String id) {
        return getById(id);
    }
}
