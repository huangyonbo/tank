package framework.mybatis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import framework.mybatis.domain.CustomMapRecord;
import framework.mybatis.mapper.CustomMapRecordMapper;
import framework.mybatis.utils.PageResult;
import framework.mybatis.utils.PageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class CustomMapRecordService extends AbstractServiceImpl<CustomMapRecordMapper, CustomMapRecord> {
    @Autowired
    private CustomMapRecordMapper customMapRecordMapper;
    public List<CustomMapRecord> listByOwner(Integer ownerUid, Integer offset, Integer limit) {
        int safeOffset = offset == null || offset < 0 ? 0 : offset;
        int safeLimit = limit == null || limit <= 0 ? 20 : limit;
        long current = safeOffset / safeLimit + 1L;
        Page<CustomMapRecord> page = new Page<>(current, safeLimit);

        LambdaQueryWrapper<CustomMapRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CustomMapRecord::getOwnerUid, ownerUid).orderByDesc(CustomMapRecord::getUpdateTime);
        Page<CustomMapRecord> customMapRecordPage = customMapRecordMapper.selectPage(page, queryWrapper);
        return customMapRecordPage.getRecords();
    }

    public Long countByOwner(Integer ownerUid) {
        return (long) lambdaQuery().eq(CustomMapRecord::getOwnerUid, ownerUid).count();
    }

    public List<CustomMapRecord> listNewest(Integer offset, Integer limit) {
        int safeOffset = offset == null || offset < 0 ? 0 : offset;
        int safeLimit = limit == null || limit <= 0 ? 20 : limit;
        long current = safeOffset / safeLimit + 1L;
        Page<CustomMapRecord> page = new Page<>(current, safeLimit);

        LambdaQueryWrapper<CustomMapRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(CustomMapRecord::getCreateTime);
        Page<CustomMapRecord> customMapRecordPage = customMapRecordMapper.selectPage(page, queryWrapper);
        return customMapRecordPage.getRecords();
    }

    public Long countAll() {
        return (long) count();
    }

    public CustomMapRecord getByMapId(Long mapId) {
        return getById(mapId);
    }

    public boolean removeByMapIdAndOwner(Long mapId, Integer ownerUid) {
        return lambdaUpdate()
                .eq(CustomMapRecord::getMapId, mapId)
                .eq(CustomMapRecord::getOwnerUid, ownerUid)
                .remove();
    }

    public CustomMapRecord createMap(CustomMapRecord record) {
        Date now = new Date();
        record.setHeat(record.getHeat() == null ? 0L : record.getHeat());
        record.setDifficulty(record.getDifficulty() == null ? 0 : record.getDifficulty());
        record.setPlayCount(record.getPlayCount() == null ? 0L : record.getPlayCount());
        record.setSuccessCount(record.getSuccessCount() == null ? 0L : record.getSuccessCount());
        record.setFailCount(record.getFailCount() == null ? 0L : record.getFailCount());
        record.setLikeCount(record.getLikeCount() == null ? 0L : record.getLikeCount());
        record.setCreateTime(now);
        record.setUpdateTime(now);
        return save(record) ? record : null;
    }

    public boolean updateMap(CustomMapRecord record) {
        if (record == null || record.getMapId() == null || record.getMapId() <= 0L) {
            return false;
        }
        record.setUpdateTime(new Date());
        return lambdaUpdate()
            .eq(CustomMapRecord::getMapId, record.getMapId())
            .eq(CustomMapRecord::getOwnerUid, record.getOwnerUid())
            .set(CustomMapRecord::getMapName, record.getMapName())
            .set(CustomMapRecord::getMapData, record.getMapData())
            .set(CustomMapRecord::getWidth, record.getWidth())
            .set(CustomMapRecord::getHeight, record.getHeight())
            .set(CustomMapRecord::getUpdateTime, record.getUpdateTime())
            .update();
    }

    public boolean addHeat(Long mapId, Long delta) {
        long add = delta == null ? 0L : delta;
        if (add <= 0) {
            return true;
        }
        CustomMapRecord one = getById(mapId);
        if (one == null) {
            return false;
        }
        long oldHeat = one.getHeat() == null ? 0L : one.getHeat();
        return lambdaUpdate()
                .eq(CustomMapRecord::getMapId, mapId)
                .set(CustomMapRecord::getHeat, oldHeat + add)
                .update();
    }

    /**
     * 开始游玩：play_count + 1
     */
    public CustomMapRecord recordPlayStart(Long mapId) {
        if (mapId == null || mapId <= 0L) {
            return null;
        }
        boolean ok = lambdaUpdate()
                .eq(CustomMapRecord::getMapId, mapId)
                .setSql("play_count = IFNULL(play_count,0) + 1")
                .set(CustomMapRecord::getUpdateTime, new Date())
                .update();
        if (!ok) {
            return null;
        }
        return getById(mapId);
    }

    /**
     * 通关：success_count + 1
     */
    public CustomMapRecord recordPlaySuccess(Long mapId) {
        if (mapId == null || mapId <= 0L) {
            return null;
        }
        boolean ok = lambdaUpdate()
                .eq(CustomMapRecord::getMapId, mapId)
                .setSql("success_count = IFNULL(success_count,0) + 1")
                .set(CustomMapRecord::getUpdateTime, new Date())
                .update();
        if (!ok) {
            return null;
        }
        return getById(mapId);
    }

    /**
     * 未完成：difficulty + 1 且 fail_count + 1
     */
    public CustomMapRecord recordPlayFail(Long mapId) {
        if (mapId == null || mapId <= 0L) {
            return null;
        }
        boolean ok = lambdaUpdate()
                .eq(CustomMapRecord::getMapId, mapId)
                .setSql("difficulty = IFNULL(difficulty,0) + 1, fail_count = IFNULL(fail_count,0) + 1")
                .set(CustomMapRecord::getUpdateTime, new Date())
                .update();
        if (!ok) {
            return null;
        }
        return getById(mapId);
    }

    /**
     * 点赞总数 +1（与每日去重逻辑配合，由上层保证只调用一次）
     */
    public CustomMapRecord recordLikeIncrement(Long mapId) {
        if (mapId == null || mapId <= 0L) {
            return null;
        }
        boolean ok = lambdaUpdate()
                .eq(CustomMapRecord::getMapId, mapId)
                .setSql("like_count = IFNULL(like_count,0) + 1")
                .set(CustomMapRecord::getUpdateTime, new Date())
                .update();
        if (!ok) {
            return null;
        }
        return getById(mapId);
    }
}
