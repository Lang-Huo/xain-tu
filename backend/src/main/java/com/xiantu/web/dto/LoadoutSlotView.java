package com.xiantu.web.dto;

import lombok.Data;

/**
 * 单个装备槽视图：4 个固定槽（WEAPON 法器 / ARMOR 护身 / STORAGE 储物 / TECHNIQUE 功法）。
 * 空槽时 itemId/code/name 等为 null，empty=true。
 */
@Data
public class LoadoutSlotView {
    private String slot;          // WEAPON / ARMOR / STORAGE / TECHNIQUE
    private String slotName;      // 法器 / 护身 / 储物 / 功法
    private boolean empty;
    private Long itemId;          // null if empty
    private String code;
    private String name;
    private String rarity;
    private String description;
}