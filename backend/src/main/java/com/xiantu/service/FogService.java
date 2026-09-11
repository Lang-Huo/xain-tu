package com.xiantu.service;

import org.springframework.stereotype.Component;

/**
 * 战争迷雾服务：基于玩家坐标 + 神识，计算可见区域。
 *
 * <p>可见半径公式：{@code R = 基础值(2) + 神识 / 3}。
 * 例：神识 5 → R=3；神识 8 → R=4；神识 12 → R=6。
 *
 * <p>可见形状：Chebyshev 距离 ≤ R 的方格（即以玩家为中心的方形区域，
 * 探索类游戏最常见的「圆形感」近似，比曼哈顿距离更自然）。
 */
@Component
public class FogService {

    private static final int BASE_RADIUS = 2;

    /** 由神识计算可见半径。 */
    public int radiusFromSpiritualSense(int spiritualSense) {
        return BASE_RADIUS + Math.max(0, spiritualSense) / 3;
    }

    /**
     * 返回 size×size 的可见掩码：true = 当前可见；false = 不可见（含记忆）。
     * 玩家所在格一定可见。
     */
    public boolean[][] visibleMask(int size, int px, int py, int radius) {
        boolean[][] vis = new boolean[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int dx = Math.abs(i - px);
                int dy = Math.abs(j - py);
                vis[i][j] = Math.max(dx, dy) <= radius;
            }
        }
        return vis;
    }
}