package com.xiantu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiantu.entity.Monster;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MonsterMapper extends BaseMapper<Monster> {

    /** 按 code 查怪物；null 表示不存在。 */
    default Monster selectByCode(String code) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Monster>()
                .eq("code", code));
    }
}