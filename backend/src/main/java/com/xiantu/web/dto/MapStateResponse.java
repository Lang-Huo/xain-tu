package com.xiantu.web.dto;

import lombok.Data;

import java.util.List;

/**
 * 地图完整状态（入图 / 刷新时返回）。
 *
 * <ul>
 *   <li>{@code instanceId / status / posX / posY / stepCount / maxSteps}：实例信息</li>
 *   <li>{@code templateCode / templateName / size}：模板信息</li>
 *   <li>{@code radius}：当前神识下的可见半径</li>
 *   <li>{@code cells}：N×N 格子按行展开（左下角起，x→y↑），含迷雾状态</li>
 * </ul>
 */
@Data
public class MapStateResponse {

    private Long instanceId;
    private String templateCode;
    private String templateName;
    private int size;
    private int posX;
    private int posY;
    private int stepCount;
    private int maxSteps;
    private String status;          // ACTIVE / COMPLETED / ABANDONED
    private int radius;             // 当前神识 → 可见半径
    private List<CellView> cells;   // size*size 个
}