package com.xiantu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiantu.entity.MapTemplate;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MapTemplateMapper extends BaseMapper<MapTemplate> {

    default MapTemplate selectByCode(String code) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MapTemplate>()
                .eq(MapTemplate::getCode, code));
    }
}