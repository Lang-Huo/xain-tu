package com.xiantu.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 境界配置（静态、硬编码）：境界 code → 中文名 + 9 层制（初期/中期/后期）。
 * 9 大境界：练气 → 筑基 → 金丹 → 元婴 → 化神 → 炼虚 → 合体 → 大乘 → 渡劫
 * 每个大境界分 1-9 层：
 *   1-3 = 初期，4-6 = 中期，7-9 = 后期
 */
public final class Realms {

    /** code → 中文名（LinkedHashMap 保持插入顺序，便于遍历展示） */
    public static final Map<String, String> NAMES = new LinkedHashMap<>();
    static {
        NAMES.put("FAN_REN",   "凡人");
        NAMES.put("LIAN_QI",   "练气");
        NAMES.put("ZHU_JI",  "筑基");
        NAMES.put("JIN_DAN",   "金丹");
        NAMES.put("YUAN_YING", "元婴");
        NAMES.put("HUA_SHEN",  "化神");
        NAMES.put("LIAN_XU",   "炼虚");
        NAMES.put("HE_TI",     "合体");
        NAMES.put("DA_CHENG",  "大乘");
        NAMES.put("DU_JIE",    "渡劫");
    }

    public static final List<String> CODES = List.copyOf(NAMES.keySet());

    /** 初始境界：注册时默认 */
    public static final String DEFAULT_CODE = "FAN_REN";
    public static final int DEFAULT_LEVEL = 1;

    /** 层数合法范围 */
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 9;

    private Realms() {}

    /** code → 中文名；未知 code 原样返回 */
    public static String nameOf(String code) {
        return NAMES.getOrDefault(code, code);
    }

    /** code 是否合法 */
    public static boolean isValidCode(String code) {
        return NAMES.containsKey(code);
    }

    /** level（1-9）→ 阶段名：1-3 初期 / 4-6 中期 / 7-9 后期；越界自动夹紧 */
    public static String phaseOf(int level) {
        if (level <= 3) return "初期";
        if (level <= 6) return "中期";
        return "后期";
    }

    /**
     * 完整展示名：例如 "练气初期"、"金丹中期"、"元婴后期"。
     * level 越界时夹紧到 [1,9]。
     */
    public static String displayName(String code, int level) {
        int lv = Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
        return nameOf(code) + phaseOf(lv);
    }
}