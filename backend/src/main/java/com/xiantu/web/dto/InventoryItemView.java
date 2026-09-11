package com.xiantu.web.dto;

import lombok.Data;

/**
 * 背包内单条物品视图。
 */
@Data
public class InventoryItemView {

    private Long itemId;
    private String code;
    private String name;
    private String type;            // MATERIAL/EQUIPMENT/PILL/TECHNIQUE
    private String rarity;          // COMMON/RARE/...
    private String description;
    private String attrsJson;
    private boolean consumable;     // 是否可直接「使用」
    private int hpRestore;          // 使用时恢复气血（0 表示无）
    private int capacityBonus;      // 装备带来的储物格数加成（祖布囊 +5 等，0 表示无）
    private int quantity;
}