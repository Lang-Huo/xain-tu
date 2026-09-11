package com.xiantu.service;

import com.xiantu.entity.SpiritRootOption;
import com.xiantu.entity.SpiritRootQuestion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 灵根测试 5 道题题库。每题 4 个选项，每个选项对灵根配置（code）施加权重分（每题合计 4 分）。
 * 阈值设计：变异（雷/冰）总得分 ≥ 8 且 ≥ 普通最高分 ⇒ 升级为变异灵根；
 *          VOID 总得分 ≥ 8 且为全场最高 ⇒ 触发空灵根（极稀有）；
 *          双灵根阈值：副灵根得分 ≥ 主灵根得分 * 0.5。
 */
public final class SpiritRootQuestionBank {

    public static final List<SpiritRootQuestion> QUESTIONS = build();

    private SpiritRootQuestionBank() {}

    private static List<SpiritRootQuestion> build() {
        List<SpiritRootQuestion> qs = new ArrayList<>();
        qs.add(q(0, "山中忽闻异响，你最先作何反应？",
                o(0, "持剑戒备，凝神以对",   w("GOLD", 2, "FIRE", 1, "EARTH", 1)),
                o(1, "屏息探查，以神识感知", w("WATER", 2, "VOID", 2)),
                o(2, "循声寻木，借林隐身形", w("WOOD", 3, "EARTH", 1)),
                o(3, "电光石火间，先发制人", w("THUNDER", 2, "FIRE", 1, "GOLD", 1))));
        qs.add(q(1, "你最想修炼的心境是？",
                o(0, "锐不可当，一往无前",   w("GOLD", 2, "FIRE", 2)),
                o(1, "上善若水，柔而不争",   w("WATER", 2, "WOOD", 1, "VOID", 1)),
                o(2, "生生不息，草木长春",   w("WOOD", 3, "EARTH", 1)),
                o(3, "静如寒潭，心如止水",   w("ICE", 3, "WATER", 1))));
        qs.add(q(2, "面对强敌，你偏好？",
                o(0, "正面硬撼，守如山岳",   w("EARTH", 2, "GOLD", 1, "FIRE", 1)),
                o(1, "以柔克刚，后发先至",   w("WATER", 2, "WOOD", 1, "GOLD", 1)),
                o(2, "雷霆一击，毕其功于一役", w("THUNDER", 3, "FIRE", 1)),
                o(3, "寒霜封锁，困敌于无形", w("ICE", 3, "WATER", 1))));
        qs.add(q(3, "师尊赐法，你最想修哪一道？",
                o(0, "庚金剑诀，锋芒毕露",   w("GOLD", 3, "ICE", 1)),
                o(1, "乙木长青诀，延年益寿", w("WOOD", 2, "WATER", 1, "EARTH", 1)),
                o(2, "离火真诀，焚尽八荒",   w("FIRE", 3, "THUNDER", 1)),
                o(3, "玄冰诀，寒彻天地",     w("ICE", 2, "WATER", 1, "VOID", 1))));
        qs.add(q(4, "灵根觉醒时，你期望见到何种异象？",
                o(0, "金芒贯体，剑意冲霄",   w("GOLD", 2, "FIRE", 1, "EARTH", 1)),
                o(1, "绿木逢春，生机盎然",   w("WOOD", 2, "EARTH", 1, "WATER", 1)),
                o(2, "雷光乍现，紫电青霜",   w("THUNDER", 3, "GOLD", 1)),
                o(3, "万象归虚，返璞归真",   w("VOID", 4))));
        return Collections.unmodifiableList(qs);
    }

    private static SpiritRootQuestion q(int idx, String text, SpiritRootOption... opts) {
        SpiritRootQuestion qq = new SpiritRootQuestion();
        qq.setIndex(idx);
        qq.setText(text);
        qq.setOptions(Arrays.asList(opts));
        return qq;
    }

    private static SpiritRootOption o(int idx, String text, Map<String, Integer> weights) {
        SpiritRootOption oo = new SpiritRootOption();
        oo.setIndex(idx);
        oo.setText(text);
        oo.setWeights(weights);
        return oo;
    }

    /** 构造 {"GOLD":2,"FIRE":1} 这种 map；按插入序，便于调试。 */
    private static Map<String, Integer> w(Object... kv) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], (Integer) kv[i + 1]);
        }
        return m;
    }
}