package com.xiantu.common;

/**
 * 地图格子类型。
 * - EMPTY    空地
 * - OBSTACLE 障碍物（不可通过）
 * - RESOURCE 资源（采集后变空地）
 * - MONSTER  野怪（M3 阶段触发遭遇占位事件，M4 接入回合制战斗）
 * - EXIT     秘境出口（走到即结算离开）
 */
public enum CellType {
    EMPTY,
    OBSTACLE,
    RESOURCE,
    MONSTER,
    EXIT;

    /** 是否阻挡移动。OBSTACLE 阻挡；其余均允许站立。 */
    public boolean isBlocking() {
        return this == OBSTACLE;
    }
}