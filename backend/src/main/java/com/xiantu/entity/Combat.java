package com.xiantu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 战斗状态（一场战斗一条）。{@link #logJson} 是 JSON 字符串数组（用 {@link com.xiantu.common.handler.StringListJsonTypeHandler} 映射），
 * 每次行动追加一条回合日志。
 */
@Getter
@Setter
@TableName("t_combat")
public class Combat {

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

    @TableField("monster_hp")
    private int monsterHp;

    @TableField("monster_max_hp")
    private int monsterMaxHp;

    @TableField("monster_attack")
    private int monsterAttack;

    @TableField("monster_defense")
    private int monsterDefense;

    @TableField("monster_speed")
    private int monsterSpeed;

    @TableField("player_hp")
    private int playerHp;

    /** 进入战斗时玩家 HP 快照（结束结算时把剩余 HP 写回 User） */
    @TableField("player_hp_snapshot")
    private int playerHpSnapshot;

    @TableField("player_attack")
    private int playerAttack;

    @TableField("player_defense")
    private int playerDefense;

    @TableField("player_speed")
    private int playerSpeed;

    private int round;

    /** PLAYER / MONSTER */
    @TableField("current_turn")
    private String currentTurn;

    /** 战斗日志 JSON（List<CombatLogEntry>） */
    @TableField(value = "log_json", typeHandler = com.xiantu.common.handler.StringListJsonTypeHandler.class)
    private java.util.List<String> log;

    private String status;            // ACTIVE / FINISHED
    private String result;            // VICTORY / DEFEAT / FLED

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_FINISHED = "FINISHED";

    public static final String TURN_PLAYER = "PLAYER";
    public static final String TURN_MONSTER = "MONSTER";

    public static final String RESULT_VICTORY = "VICTORY";
    public static final String RESULT_DEFEAT = "DEFEAT";
    public static final String RESULT_FLED = "FLED";
}