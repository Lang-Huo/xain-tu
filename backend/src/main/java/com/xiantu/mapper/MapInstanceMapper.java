package com.xiantu.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiantu.entity.MapInstance;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MapInstanceMapper extends BaseMapper<MapInstance> {

    /**
     * 查某用户当前进行中的实例（ACTIVE 状态，最多一条）。
     */
    default MapInstance selectActiveByUserId(Long userId) {
        return selectOne(new LambdaQueryWrapper<MapInstance>()
                .eq(MapInstance::getUserId, userId)
                .eq(MapInstance::getStatus, MapInstance.STATUS_ACTIVE)
                .last("LIMIT 1"));
    }
}