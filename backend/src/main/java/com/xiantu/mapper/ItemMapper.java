package com.xiantu.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiantu.entity.Item;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ItemMapper extends BaseMapper<Item> {

    default Item selectByCode(String code) {
        return selectOne(new LambdaQueryWrapper<Item>().eq(Item::getCode, code));
    }
}