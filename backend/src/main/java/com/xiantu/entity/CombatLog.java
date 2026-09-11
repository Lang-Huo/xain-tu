package com.xiantu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 战斗记录（每场战斗一行，仅历史用途）。战斗结束后由 {@code CombatService.finishCombat} 写入。
 */
@Getter
@Setter
@TableName("t_combat_log")
public class CombatLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("map_instance_id")
    private Long mapInstanceId;

    @TableField("monster_code")
    private String monsterCode;

    @TableField("monster_name")
    private String monsterName;

    private String result;          // VICTORY / DEFEAT / FLED
    private int rounds;
    private int expGained;

    @TableField("dropped_item_code")
    private String droppedItemCode;

    @TableField("dropped_quantity")
    private int droppedQuantity;

    @TableField("created_at")
    private LocalDateTime createdAt;
}