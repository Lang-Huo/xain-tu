package com.xiantu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户装备槽：一期固定 4 个槽位（WEAPON 法器 / ARMOR 护身 / STORAGE 储物 / TECHNIQUE 功法）。
 * 每个 user + 每个 slot 最多 1 件装备；卸下后回到背包。
 */
@Getter
@Setter
@TableName("t_user_loadout")
public class UserLoadout {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    /** 槽位 code：WEAPON / ARMOR / STORAGE / TECHNIQUE */
    private String slot;

    @TableField("item_id")
    private Long itemId;

    public static final String SLOT_WEAPON = "WEAPON";
    public static final String SLOT_ARMOR = "ARMOR";
    public static final String SLOT_STORAGE = "STORAGE";
    public static final String SLOT_TECHNIQUE = "TECHNIQUE";

    /** 槽位 code → 中文显示名 */
    public static String slotName(String slot) {
        switch (slot) {
            case SLOT_WEAPON:    return "法器";
            case SLOT_ARMOR:     return "护身";
            case SLOT_STORAGE:   return "储物";
            case SLOT_TECHNIQUE: return "功法";
            default: return slot;
        }
    }
}