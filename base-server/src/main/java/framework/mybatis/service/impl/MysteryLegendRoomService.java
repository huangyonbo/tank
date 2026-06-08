package framework.mybatis.service.impl;

import framework.mybatis.domain.MysteryLegendRoom;
import framework.mybatis.mapper.MysteryLegendRoomMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MysteryLegendRoomService extends AbstractServiceImpl<MysteryLegendRoomMapper, MysteryLegendRoom> {

    public List<MysteryLegendRoom> listAll() {
        return list();
    }
}
