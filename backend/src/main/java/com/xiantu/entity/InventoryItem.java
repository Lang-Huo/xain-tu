package com.xiantu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 背包物品行。同一背包 + 同一 item 唯一，数量累加。
 */
@Getter
@Setter
@TableName("t_inventory_item")
public class InventoryItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("inventory_id")
    private Long inventoryId;

    @TableField("item_id")
    private Long itemId;

    /** 持有数量 */
    private int quantity;
}