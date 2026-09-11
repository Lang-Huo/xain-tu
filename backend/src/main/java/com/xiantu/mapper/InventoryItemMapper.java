package com.xiantu.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiantu.entity.InventoryItem;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface InventoryItemMapper extends BaseMapper<InventoryItem> {

    default List<InventoryItem> selectByInventoryId(Long inventoryId) {
        return selectList(new LambdaQueryWrapper<InventoryItem>()
                .eq(InventoryItem::getInventoryId, inventoryId));
    }

    default InventoryItem selectByInventoryAndItem(Long inventoryId, Long itemId) {
        return selectOne(new LambdaQueryWrapper<InventoryItem>()
                .eq(InventoryItem::getInventoryId, inventoryId)
                .eq(InventoryItem::getItemId, itemId));
    }

    default long countByInventoryId(Long inventoryId) {
        return selectCount(new LambdaQueryWrapper<InventoryItem>()
                .eq(InventoryItem::getInventoryId, inventoryId));
    }
}