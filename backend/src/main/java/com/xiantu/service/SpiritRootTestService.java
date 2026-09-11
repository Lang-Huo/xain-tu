package com.xiantu.service;

import com.xiantu.common.BizException;
import com.xiantu.common.SpiritRoots;
import com.xiantu.entity.SpiritRootOption;
import com.xiantu.entity.SpiritRootQuestion;
import com.xiantu.entity.User;
import com.xiantu.mapper.UserMapper;
import com.xiantu.web.dto.SpiritRootInfo;
import com.xiantu.web.dto.SpiritRootOptionView;
import com.xiantu.web.dto.SpiritRootQuestionView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 灵根测试服务：
 * 1. 返回题目（不含权重）
 * 2. 计算 5 题权重得分
 * 3. 判定首个灵根（空灵根 / 变异升级 / 普通 argmax）
 * 4. 判定可选第二灵根（双灵根阈值；只是计数，无主副之分）
 * 5. 提交：写 User.spiritRoots（JSON 字符串数组 ["GOLD","WOOD"]）
 *
 * 灵根不再有可配置属性（无加成/稀有度等），仅作为 code 集合影响玩家可修炼的功法。
 */
@Service
public class SpiritRootTestService {

    private static final List<String> REGULAR = List.of("GOLD", "WOOD", "WATER", "FIRE", "EARTH");
    private static final List<String> REGULAR_PRIORITY = List.of("FIRE", "WATER", "GOLD", "WOOD", "EARTH");
    private static final int MUTATED_THRESHOLD = 8;
    private static final int VOID_THRESHOLD = 8;
    private static final double DUAL_RATIO = 0.5;

    private final UserMapper userMapper;

    public SpiritRootTestService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /** 获取题目（不含权重），供前端展示。 */
    public List<SpiritRootQuestionView> getQuestions() {
        List<SpiritRootQuestionView> views = new ArrayList<>();
        for (SpiritRootQuestion q : SpiritRootQuestionBank.QUESTIONS) {
            SpiritRootQuestionView qv = new SpiritRootQuestionView();
            qv.setIndex(q.getIndex());
            qv.setText(q.getText());
            List<SpiritRootOptionView> ovs = new ArrayList<>();
            for (SpiritRootOption o : q.getOptions()) {
                SpiritRootOptionView ov = new SpiritRootOptionView();
                ov.setIndex(o.getIndex());
                ov.setText(o.getText());
                ovs.add(ov);
            }
            qv.setOptions(ovs);
            views.add(qv);
        }
        return views;
    }

    /** 计算 5 题各灵根累计得分。answers 长度必须为 5，每项 0-3。 */
    public Map<String, Integer> computeScores(List<Integer> answers) {
        if (answers == null || answers.size() != 5) {
            throw new BizException("需提供 5 道题的答案");
        }
        Map<String, Integer> score = new HashMap<>();
        for (String c : SpiritRoots.CODES) score.put(c, 0);
        List<SpiritRootQuestion> qs = SpiritRootQuestionBank.QUESTIONS;
        for (int i = 0; i < 5; i++) {
            Integer optIdx = answers.get(i);
            if (optIdx == null || optIdx < 0 || optIdx > 3) {
                throw new BizException("第 " + (i + 1) + " 题答案越界");
            }
            SpiritRootOption opt = qs.get(i).getOptions().get(optIdx);
            for (Map.Entry<String, Integer> e : opt.getWeights().entrySet()) {
                score.merge(e.getKey(), e.getValue(), Integer::sum);
            }
        }
        return score;
    }

    /** 判定首个灵根 code（含变异/空灵根升级），可选第二灵根 code（双灵根阈值）。
     *  primary/secondary 仅是位置概念（第一/第二），不代表层级。 */
    public Verdict determine(Map<String, Integer> score) {
        int voidS = score.getOrDefault("VOID", 0);
        int top = score.values().stream().max(Integer::compareTo).orElse(0);

        String primary;
        if (voidS >= VOID_THRESHOLD && voidS >= top) {
            primary = "VOID";
        } else {
            int regMax = REGULAR.stream().mapToInt(c -> score.getOrDefault(c, 0)).max().orElse(0);
            int thu = score.getOrDefault("THUNDER", 0);
            int ice = score.getOrDefault("ICE", 0);
            if (thu >= MUTATED_THRESHOLD && thu >= regMax) {
                primary = "THUNDER";
            } else if (ice >= MUTATED_THRESHOLD && ice >= regMax) {
                primary = "ICE";
            } else {
                primary = argmaxRegular(score);
            }
        }

        int primaryScore = score.getOrDefault(primary, 0);
        String secondary = null;
        if (!"VOID".equals(primary)) {
            int thresh = (int) Math.ceil(primaryScore * DUAL_RATIO);
            int best = -1;
            for (String c : SpiritRoots.CODES) {
                if (c.equals(primary) || "VOID".equals(c)) continue;
                int s = score.getOrDefault(c, 0);
                if (s >= thresh && s > best) {
                    best = s;
                    secondary = c;
                }
            }
        }
        return new Verdict(primary, secondary, score);
    }

    private String argmaxRegular(Map<String, Integer> s) {
        int max = REGULAR.stream().mapToInt(c -> s.getOrDefault(c, 0)).max().orElse(0);
        for (String c : REGULAR_PRIORITY) {
            if (s.getOrDefault(c, 0) == max) return c;
        }
        return "GOLD";
    }

    /** 提交测试：算分 + 判定 + 落库。灵根无属性加成，不再调整神识。 */
    @Transactional
    public CommitResult commit(String username, List<Integer> answers) {
        User user = userMapper.selectByUsername(username);
        if (user == null) throw new BizException("用户不存在");
        // 防止重复测试：spiritRoots 非空则已测过
        List<String> existing = user.getSpiritRoots();
        if (existing != null && !existing.isEmpty()) {
            throw new BizException("已测过灵根，无法重复测试");
        }

        Verdict verdict = determine(computeScores(answers));

        // 组装灵根 JSON 字符串数组（无主副之分，按算法顺序入数组）
        List<String> codes = new ArrayList<>();
        codes.add(verdict.primary);
        if (verdict.secondary != null) codes.add(verdict.secondary);

        // 组装响应灵根列表（仅 code + 中文名，无其他属性）
        List<SpiritRootInfo> roots = new ArrayList<>();
        for (String code : codes) {
            if (SpiritRoots.isValid(code)) {
                roots.add(SpiritRootInfo.of(code));
            }
        }

        // 更新用户：灵根 JSON。神识不受灵根影响（灵根只决定可修炼的功法）。
        user.setSpiritRoots(codes);
        userMapper.updateById(user);

        CommitResult out = new CommitResult();
        out.roots = roots;
        out.dual = roots.size() >= 2;
        return out;
    }

    /** 判定结果（仅 code）。primary/secondary 仅表示顺序（第一/第二），不代表层级。 */
    public static class Verdict {
        public final String primary;
        public final String secondary;
        public final Map<String, Integer> scores;
        public Verdict(String primary, String secondary, Map<String, Integer> scores) {
            this.primary = primary;
            this.secondary = secondary;
            this.scores = scores;
        }
    }

    /** 提交结果（仅灵根列表与是否双灵根，无神识调整项）。 */
    public static class CommitResult {
        public List<SpiritRootInfo> roots;
        public boolean dual;
    }
}