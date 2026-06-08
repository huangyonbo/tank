package framework.mybatis.service.impl;

import framework.mybatis.domain.BmbcGame;
import framework.mybatis.domain.ThreeSelectOneGame;
import framework.mybatis.mapper.BmbcGameMapper;
import framework.mybatis.mapper.ThreeSelectOneGameMapper;
import org.springframework.stereotype.Service;

@Service
public class BmbcGameService extends AbstractServiceImpl<BmbcGameMapper, BmbcGame> {
}
