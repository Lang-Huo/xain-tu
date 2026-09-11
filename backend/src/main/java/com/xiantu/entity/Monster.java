package com.xiantu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 怪物定义。一期用一张配置表按 code 引用；通过 {@link #code} 在战斗触发时按图格怪物类型匹配。
 */
@Getter
@Setter
@TableName("t_monster")
public class Monster {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private String name;

    private int level;
    private int hp;
    private int attack;
    private int defense;
    private int speed;

    private int expReward;

    /** 胜利固定掉落物品 code；null 表示不掉 */
    @TableField("drop_item_code")
    private String dropItemCode;

    @TableField("drop_quantity")
    private int dropQuantity;

    /** 1 = 禁止逃跑；0 = 允许 */
    @TableField("flee_blocked")
    private int fleeBlocked;

    private String description;

    @TableField("created_at")
    private LocalDateTime createdAt;
}