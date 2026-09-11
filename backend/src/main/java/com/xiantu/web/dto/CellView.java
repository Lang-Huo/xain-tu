package com.xiantu.web.dto;

import com.xiantu.common.CellType;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 单个格子的视图（前端渲染 + 迷雾状态）。
 *
 * <ul>
 *   <li>{@code type}：格子原始类型（迷雾不可见时仍返回，方便迷雾散去后无延迟显示）</li>
 *   <li>{@code fog}：HIDDEN 未探索 / MEMORY 已探索但当前不在视野 / VISIBLE 当前可见</li>
 *   <li>{@code code}：当 type=RESOURCE/MONSTER 时附加的具体内容 code（前端按 code 显示）</li>
 * </ul>
 */
@Data
@AllArgsConstructor
public class CellView {
    private int x;
    private int y;
    private CellType type;
    private String fog;   // HIDDEN / MEMORY / VISIBLE
    private String code;  // 内容 code，可为空
}