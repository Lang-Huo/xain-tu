package com.xiantu.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiantu.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    default Inventory selectByUserId(Long userId) {
        return selectOne(new LambdaQueryWrapper<Inventory>().eq(Inventory::getUserId, userId));
    }
}