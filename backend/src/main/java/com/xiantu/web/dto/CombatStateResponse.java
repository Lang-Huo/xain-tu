package com.xiantu.web.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 战斗状态响应：玩家 + 怪物双方 HP / 属性 + 回合日志 + 战斗结果。
 */
@Data
public class CombatStateResponse {

    private Long combatId;
    private Long userId;
    private Long mapInstanceId;

    // 怪物
    private String monsterCode;
    private String monsterName;
    private int monsterMaxHp;
    private int monsterHp;
    private int monsterAttack;

    // 玩家
    private int playerHp;          // 战斗中当前 HP
    private int playerHpSnapshot;  // 进入战斗时的 HP（恢复上限）
    private int playerAttack;
    private int playerDefense;

    // 回合
    private int round;
    private String currentTurn;    // PLAYER / MONSTER
    private List<String> log = new ArrayList<>();

    // 结束（FINISHED 后才有）
    private String status;         // ACTIVE / FINISHED
    private String result;         // VICTORY / DEFEAT / FLED

    // 结算奖励（FINISHED 后才有）
    private int expGained;
    private String droppedItemCode;
    private int droppedQuantity;

    /**
     * 背包内灵草数量（仅战斗中可见）。
     * 用于前端禁用"服用灵草"按钮、避免 0 数量时反复点击造成的 UI 闪烁/异常。
     */
    private int lingCaoCount;
}