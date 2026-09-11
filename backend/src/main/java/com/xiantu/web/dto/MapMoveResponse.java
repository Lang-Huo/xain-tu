package com.xiantu.web.dto;

import lombok.Data;

/**
 * 移动结果：移动事件 + 最新状态。
 *
 * <p>{@code event} 取值：
 * <ul>
 *   <li>BLOCKED：被障碍阻挡（位置不变）</li>
 *   <li>MOVED：空地 / 移动成功</li>
 *   <li>RESOURCE_COLLECTED：踩到资源并自动采集</li>
 *   <li>MONSTER_ENCOUNTER：踩到野怪（M3 仅占位事件；M4 实装战斗）</li>
 *   <li>EXIT_REACHED：抵达出口，结算完成</li>
 *   <li>STEPS_EXHAUSTED：步数耗尽</li>
 *   <li>OUT_OF_BOUNDS：越界（按理不该出现，前端校验过）</li>
 * </ul>
 */
@Data
public class MapMoveResponse {
    private String event;
    private String message;
    private String resourceName;   // RESOURCE_COLLECTED 时附带：物品名
    private String monsterName;    // MONSTER_ENCOUNTER 时附带：野怪名（M4 战斗时再用）
    private int monsterLevel;
    private MapStateResponse state;
}