package com.xiantu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 物品定义。一期背包只挂少数物品（M5 不含炼丹，所以暂不涉及丹方与产出表）。
 *
 * <p>扩展字段说明：
 * <ul>
 *   <li>{@code type}：MATERIAL 材料 / EQUIPMENT 装备 / PILL 丹药 / TECHNIQUE 功法</li>
 *   <li>{@code rarity}：COMMON / RARE / EPIC / LEGENDARY（仅展示）</li>
 *   <li>{@code attrsJson}：物品属性 JSON，如 {@code {"consumable":true,"hpRestore":30}}</li>
 * </ul>
 */
@Getter
@Setter
@TableName("t_item")
public class Item {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private String name;

    /** 类型常量 */
    public static final String TYPE_MATERIAL = "MATERIAL";
    public static final String TYPE_EQUIPMENT = "EQUIPMENT";
    public static final String TYPE_PILL = "PILL";
    public static final String TYPE_TECHNIQUE = "TECHNIQUE";

    private String type;

    /** 子类型：WEAPON 法器 / ARMOR 护身 / STORAGE 储物 / TECHNIQUE 功法（仅 EQUIPMENT/TECHNIQUE 用） */
    public static final String SUBTYPE_WEAPON = "WEAPON";
    public static final String SUBTYPE_ARMOR = "ARMOR";
    public static final String SUBTYPE_STORAGE = "STORAGE";
    public static final String SUBTYPE_TECHNIQUE = "TECHNIQUE";

    private String subtype;

    public static final String RARITY_COMMON = "COMMON";
    public static final String RARITY_RARE = "RARE";
    public static final String RARITY_EPIC = "EPIC";
    public static final String RARITY_LEGENDARY = "LEGENDARY";

    private String rarity;

    private String description;

    @TableField("attrs_json")
    private String attrsJson;

    @TableField("created_at")
    private LocalDateTime createdAt;
}