package framework.mybatis.service.impl;

import framework.mybatis.domain.PopupWindows;
import framework.mybatis.mapper.PopupWindowsMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PopupWindowsService extends AbstractServiceImpl<PopupWindowsMapper, PopupWindows> {

    public List<PopupWindows> getList() {
        return list();
    }
}
