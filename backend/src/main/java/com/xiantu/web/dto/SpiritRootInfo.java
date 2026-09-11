package com.xiantu.web.dto;

import com.xiantu.common.SpiritRoots;
import lombok.Data;

/**
 * 灵根信息 DTO：仅含 code 与中文名。
 * 灵根无其他属性（无加成/稀有度/颜色等），仅影响玩家可修炼的功法。
 */
@Data
public class SpiritRootInfo {
    private String code;
    private String name;

    public static SpiritRootInfo of(String code) {
        SpiritRootInfo i = new SpiritRootInfo();
        i.code = code;
        i.name = SpiritRoots.nameOf(code);
        return i;
    }
}