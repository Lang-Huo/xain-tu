package com.xiantu.common;

import java.util.List;
import java.util.Map;

/**
 * 灵根配置（静态、硬编码）：灵根不再作为可配置表存储，仅作为 code → 中文名 映射。
 * 灵根无其他属性，仅影响玩家可修炼的功法（见功法系统）。
 * 8 种灵根：金木水火土（普通）+ 雷冰空（变异/极稀有）。
 */
public final class SpiritRoots {

    public static final List<String> CODES = List.of(
            "GOLD", "WOOD", "WATER", "FIRE", "EARTH",
            "THUNDER", "ICE", "VOID");

    public static final Map<String, String> NAMES = Map.of(
            "GOLD", "金", "WOOD", "木", "WATER", "水", "FIRE", "火", "EARTH", "土",
            "THUNDER", "雷", "ICE", "冰", "VOID", "空");

    private SpiritRoots() {}

    /** code → 中文名；未知 code 原样返回。 */
    public static String nameOf(String code) {
        return NAMES.getOrDefault(code, code);
    }

    /** 是否合法 code。 */
    public static boolean isValid(String code) {
        return NAMES.containsKey(code);
    }
}