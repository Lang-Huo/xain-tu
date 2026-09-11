package com.xiantu.web.dto;

import lombok.Data;

import java.util.List;

/**
 * 背包视图：容量 + 物品列表。
 */
@Data
public class InventoryView {

    private int capacity;
    private int used;            // 已用格子（物品种类数）
    private List<InventoryItemView> items;
    private List<LoadoutSlotView> loadout;   // 4 个固定装备槽（WEAPON / ARMOR / STORAGE / TECHNIQUE）
    private int capacityBonus;   // 当前装备带来的容量加成（祖布囊 +5 等），用于前端展示
}