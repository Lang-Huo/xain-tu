package com.xiantu.service;

import com.xiantu.common.CellType;
import com.xiantu.entity.MapTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;

/**
 * 地图生成器：按模板参数 + 种子，**确定性**生成一张 N×N 网格。
 *
 * <p>确定性：同一 seed + 同一模板每次生成的网格完全一致，便于「实例表只存 seed」就能还原布局。
 *
 * <p>流程：
 * <ol>
 *   <li>全部格子初始化为 EMPTY</li>
 *   <li>起点 (0,0) 留空（玩家站立）</li>
 *   <li>出口放在 (size-1, size-1)</li>
 *   <li>其余格子按 obstacleRate / monsterRate / resourceRate 比例随机分配</li>
 *   <li>BFS 校验起点到出口可达；不可达则返回 null，调用方换 seed 重试</li>
 * </ol>
 */
@Component
public class MapGenerator {

    /** 最大重试次数（不可达 / 出口被围死） */
    private static final int MAX_RETRY = 8;

    /**
     * 尝试生成一张可达的网格。返回 null 表示该 seed 不可用，调用方应换 seed 重试。
     */
    public CellType[][] tryGenerate(MapTemplate tpl, long seed) {
        final int size = tpl.getSize();
        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            long s = seed + attempt * 9973L;
            CellType[][] grid = regenerate(tpl, s);
            if (isReachable(grid, size)) {
                return grid;
            }
        }
        return null;
    }

    /**
     * 确定性重生成：同一 seed + 同一模板一定得到同一张图。
     * 用于「实例表只存 seed，每次读时还原」的场景；不做 BFS 重试（生成时已校验过可达）。
     */
    public CellType[][] regenerate(MapTemplate tpl, long seed) {
        final int size = tpl.getSize();
        CellType[][] grid = new CellType[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                grid[i][j] = CellType.EMPTY;
            }
        }
        // 起点 (0,0) 留空（玩家）
        grid[0][0] = CellType.EMPTY;
        // 出口 (size-1, size-1)
        grid[size - 1][size - 1] = CellType.EXIT;

        Random rnd = new Random(seed);
        // 把三种概率归一化
        int obstacleRate = Math.max(0, Math.min(100, tpl.getObstacleRate()));
        int monsterRate = Math.max(0, Math.min(100, tpl.getMonsterRate()));
        int resourceRate = Math.max(0, Math.min(100, tpl.getResourceRate()));
        // 起点 / 出口不参与概率
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if ((i == 0 && j == 0) || (i == size - 1 && j == size - 1)) continue;
                int roll = rnd.nextInt(100);
                if (roll < obstacleRate) {
                    grid[i][j] = CellType.OBSTACLE;
                } else if (roll < obstacleRate + monsterRate) {
                    grid[i][j] = CellType.MONSTER;
                } else if (roll < obstacleRate + monsterRate + resourceRate) {
                    grid[i][j] = CellType.RESOURCE;
                } else {
                    grid[i][j] = CellType.EMPTY;
                }
            }
        }
        return grid;
    }

    /** BFS：起点 (0,0) 是否能走到 (size-1, size-1)，OBSTACLE 不可通过。 */
    private boolean isReachable(CellType[][] grid, int size) {
        boolean[][] visited = new boolean[size][size];
        Queue<int[]> q = new ArrayDeque<>();
        q.offer(new int[]{0, 0});
        visited[0][0] = true;
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        while (!q.isEmpty()) {
            int[] p = q.poll();
            if (p[0] == size - 1 && p[1] == size - 1) return true;
            for (int[] d : dirs) {
                int nx = p[0] + d[0];
                int ny = p[1] + d[1];
                if (nx < 0 || nx >= size || ny < 0 || ny >= size) continue;
                if (visited[nx][ny]) continue;
                if (grid[nx][ny].isBlocking()) continue;
                visited[nx][ny] = true;
                q.offer(new int[]{nx, ny});
            }
        }
        return false;
    }

    /**
     * 给定 seed 返回一个新的 seed：常用于「不可达时换 seed 重试」。
     */
    public long nextSeed(long base) {
        return base ^ 0x9E3779B97F4A7C15L;
    }
}