package com.xiantu.service;

import com.xiantu.common.BizException;
import com.xiantu.entity.Combat;
import com.xiantu.entity.CombatLog;
import com.xiantu.entity.Item;
import com.xiantu.entity.MapInstance;
import com.xiantu.entity.Monster;
import com.xiantu.entity.User;
import com.xiantu.mapper.CombatLogMapper;
import com.xiantu.mapper.CombatMapper;
import com.xiantu.mapper.ItemMapper;
import com.xiantu.mapper.MapInstanceMapper;
import com.xiantu.mapper.MonsterMapper;
import com.xiantu.mapper.UserMapper;
import com.xiantu.web.dto.CombatStateResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 一期 M4 战斗系统。
 *
 * <p>规则：
 * <ul>
 *   <li>触发：玩家在秘境里移动到 MONSTER 格 → 由 MapService.move 调 {@link #startCombat}</li>
 *   <li>属性：进入战斗时**快照**玩家当前的攻击/防御/速度/HP（不受战斗外变化影响）</li>
 *   <li>回合：按双方速度决定先手（playerSpeed ≥ monsterSpeed → PLAYER 先手），回合内 PLAYER 先行动 → MONSTER 反击</li>
 *   <li>伤害：{@code max(1, attack × 1.0 − defense × 0.5)}</li>
 *   <li>逃跑：{@code clamp((playerSpeed − monsterSpeed) × 0.10 + 0.30, 0, 0.90)}；monster.fleeBlocked=1 时禁止</li>
 *   <li>行动选项：PLAYER 可选 ATTACK / USE_ITEM（仅 LING_CAO 一期）/ FLEE；MONSTER 固定 ATTACK</li>
 *   <li>结束：任一方 HP ≤ 0 → FINISHED；FLEE 成功也 FINISHED</li>
 *   <li>结算（finishCombat）：
 *     <ul>
 *       <li>VICTORY：User.exp += monster.expReward；按 drop_item_code 调 inventoryCoreService.addItem</li>
 *       <li>DEFEAT：把 playerHp（剩余的）写回 User.hp（不致死，最低 1）</li>
 *       <li>FLED：玩家当前 HP 写回 User.hp</li>
 *       <li>任何结束：写 t_combat_log 一行</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>简化（一期不做）：
 * <ul>
 *   <li>技能 / 功法 / 状态 / buff / 灵根克制 / 灼烧冰冻</li>
 *   <li>玩家多种用药（目前只允许 LING_CAO，恢复 30 HP）</li>
 *   <li>AI 行为（野怪固定普攻）</li>
 * </ul>
 */
@Service
public class CombatService {

    /** 一期唯一可在战斗中使用的消耗品：灵草 */
    private static final String USABLE_IN_COMBAT = "LING_CAO";

    private final CombatMapper combatMapper;
    private final CombatLogMapper combatLogMapper;
    private final MonsterMapper monsterMapper;
    private final UserMapper userMapper;
    private final MapInstanceMapper mapInstanceMapper;
    private final ItemMapper itemMapper;
    private final InventoryCoreService inventoryCoreService;

    public CombatService(CombatMapper combatMapper,
                         CombatLogMapper combatLogMapper,
                         MonsterMapper monsterMapper,
                         UserMapper userMapper,
                         MapInstanceMapper mapInstanceMapper,
                         ItemMapper itemMapper,
                         InventoryCoreService inventoryCoreService) {
        this.combatMapper = combatMapper;
        this.combatLogMapper = combatLogMapper;
        this.monsterMapper = monsterMapper;
        this.userMapper = userMapper;
        this.mapInstanceMapper = mapInstanceMapper;
        this.itemMapper = itemMapper;
        this.inventoryCoreService = inventoryCoreService;
    }

    // --------------------------------------------------------------
    // 触发与查询
    // --------------------------------------------------------------

    /** 玩家移动到 MONSTER 格时由 MapService 调用，创建一条 ACTIVE 战斗并返回状态。 */
    @Transactional
    public CombatStateResponse startCombat(String username, Long mapInstanceId, String monsterCode) {
        User user = requireUser(username);
        MapInstance inst = requireOwnedInstance(mapInstanceId, user.getId());
        Monster monster = requireMonster(monsterCode);

        Combat c = new Combat();
        c.setUserId(user.getId());
        c.setMapInstanceId(inst.getId());
        c.setMonsterCode(monster.getCode());
        c.setMonsterName(monster.getName());
        c.setMonsterHp(monster.getHp());
        c.setMonsterMaxHp(monster.getHp());
        c.setMonsterAttack(monster.getAttack());
        c.setMonsterDefense(monster.getDefense());
        c.setMonsterSpeed(monster.getSpeed());
        // 玩家快照：HP 与战斗属性
        c.setPlayerHp(user.getHp());
        c.setPlayerHpSnapshot(user.getHp());
        c.setPlayerAttack(user.getAttack());
        c.setPlayerDefense(user.getDefense());
        c.setPlayerSpeed(user.getSpeed());
        // 先手：玩家速度 ≥ 怪物速度 → PLAYER 先
        c.setRound(1);
        c.setCurrentTurn(c.getPlayerSpeed() >= c.getMonsterSpeed()
                ? Combat.TURN_PLAYER : Combat.TURN_MONSTER);
        c.setLog(new ArrayList<>());
        c.setStatus(Combat.STATUS_ACTIVE);
        LocalDateTime now = LocalDateTime.now();
        c.setCreatedAt(now);
        c.setUpdatedAt(now);
        combatMapper.insert(c);

        appendLog(c, "遭遇「" + monster.getName() + "」！战斗开始。");
        if (c.getCurrentTurn().equals(Combat.TURN_MONSTER)) {
            // 怪物先手：直接反击
            monsterAttack(c);
        }
        combatMapper.updateById(c);
        return toStateResponse(c);
    }

    /** 查战斗状态（玩家可主动刷新）。 */
    public CombatStateResponse getState(String username, Long combatId) {
        User user = requireUser(username);
        Combat c = requireOwnedCombat(combatId, user.getId());
        return toStateResponse(c);
    }

    // --------------------------------------------------------------
    // 玩家行动
    // --------------------------------------------------------------

    @Transactional
    public CombatStateResponse action(String username, Long combatId, String action, String itemCode) {
        User user = requireUser(username);
        Combat c = requireOwnedCombat(combatId, user.getId());
        if (!Combat.STATUS_ACTIVE.equals(c.getStatus())) {
            throw new BizException("本场战斗已结束");
        }
        if (!Combat.TURN_PLAYER.equals(c.getCurrentTurn())) {
            throw new BizException("当前不是你的回合");
        }

        switch (action) {
            case "ATTACK": playerAttack(c); break;
            case "USE_ITEM": useItem(c, itemCode); break;
            case "FLEE": flee(c); break;
            default: throw new BizException("未知行动: " + action);
        }

        // 检查战斗是否结束
        if (c.getMonsterHp() <= 0) {
            return finishCombat(c, Combat.RESULT_VICTORY);
        }
        if (c.getPlayerHp() <= 0) {
            return finishCombat(c, Combat.RESULT_DEFEAT);
        }

        // 玩家行动后轮到怪物反击（仅当还没结束）
        if (Combat.STATUS_ACTIVE.equals(c.getStatus())) {
            c.setCurrentTurn(Combat.TURN_MONSTER);
            combatMapper.updateById(c);
            // 怪物反击一次
            monsterAttack(c);
            if (c.getPlayerHp() <= 0) {
                return finishCombat(c, Combat.RESULT_DEFEAT);
            }
            // 反击后回到玩家回合 + round++
            c.setCurrentTurn(Combat.TURN_PLAYER);
            c.setRound(c.getRound() + 1);
        }
        combatMapper.updateById(c);
        return toStateResponse(c);
    }

    // --------------------------------------------------------------
    // 内部：单次行动
    // --------------------------------------------------------------

    private void playerAttack(Combat c) {
        int dmg = calcDamage(c.getPlayerAttack(), c.getMonsterDefense(), c.getMonsterSpeed());
        int newHp = Math.max(0, c.getMonsterHp() - dmg);
        appendLog(c, "你施放攻击，造成 " + dmg + " 点伤害（" + c.getMonsterName()
                + " HP " + c.getMonsterHp() + " → " + newHp + "）");
        c.setMonsterHp(newHp);
    }

    private void monsterAttack(Combat c) {
        int dmg = calcDamage(c.getMonsterAttack(), c.getPlayerDefense(), c.getPlayerSpeed());
        int newHp = Math.max(0, c.getPlayerHp() - dmg);
        appendLog(c, "「" + c.getMonsterName() + "」反扑，对你造成 " + dmg + " 点伤害"
                + "（HP " + c.getPlayerHp() + " → " + newHp + "）");
        c.setPlayerHp(newHp);
    }

    private void useItem(Combat c, String itemCode) {
        if (itemCode == null || !USABLE_IN_COMBAT.equals(itemCode)) {
            throw new BizException("本期战斗中暂不支持该物品");
        }
        // 防御：背包里没这种物品就直接报错（前端按钮也应按 lingCaoCount 禁用）
        int owned = inventoryCoreService.countItem(c.getUserId(), itemCode);
        if (owned <= 0) {
            throw new BizException("背包中没有可用的「灵草」");
        }
        // 扣背包 1 个（InventoryCoreService.removeItem 内部校验数量与抛错）
        inventoryCoreService.removeItem(c.getUserId(), itemCode, 1);
        Item item = itemMapper.selectByCode(itemCode);
        // 防御：理论上种子数据必有，但万一配置丢失不让 NPE 拖崩事务
        if (item == null) {
            throw new BizException("未知道具: " + itemCode);
        }
        int restore = restoreHpOf(item);
        int before = c.getPlayerHp();
        int after = Math.min(c.getPlayerHpSnapshot(), before + restore);
        c.setPlayerHp(after);
        appendLog(c, "你服用「" + item.getName() + "」+气血 " + (after - before)
                + "（HP " + before + " → " + after + "）");
    }

    private void flee(Combat c) {
        Monster monster = monsterMapper.selectByCode(c.getMonsterCode());
        if (monster != null && monster.getFleeBlocked() == 1) {
            appendLog(c, "「" + c.getMonsterName() + "」紧追不舍，无法脱身！");
            // 逃跑失败，怪物照常反击一次
            monsterAttack(c);
            c.setCurrentTurn(Combat.TURN_PLAYER);
            c.setRound(c.getRound() + 1);
            return;
        }
        double p = fleeChance(c.getPlayerSpeed(), c.getMonsterSpeed());
        boolean ok = ThreadLocalRandom.current().nextDouble() < p;
        appendLog(c, "你尝试脱身…（成功率 " + (int) (p * 100) + "%）");
        if (ok) {
            appendLog(c, "脱身成功！");
            c.setStatus(Combat.STATUS_FINISHED);
            c.setResult(Combat.RESULT_FLED);
            return;
        }
        appendLog(c, "脱身失败！");
        monsterAttack(c);
        if (c.getPlayerHp() <= 0) {
            return;
        }
        c.setCurrentTurn(Combat.TURN_PLAYER);
        c.setRound(c.getRound() + 1);
    }

    // --------------------------------------------------------------
    // 结束 + 结算
    // --------------------------------------------------------------

    private CombatStateResponse finishCombat(Combat c, String result) {
        c.setStatus(Combat.STATUS_FINISHED);
        c.setResult(result);

        Monster monster = monsterMapper.selectByCode(c.getMonsterCode());
        int expGained = 0;
        String droppedCode = null;
        int droppedQty = 0;

        if (Combat.RESULT_VICTORY.equals(result)) {
            assert monster != null;
            expGained = monster.getExpReward();
            // 发经验
            User user = userMapper.selectById(c.getUserId());
            if (user != null) {
                user.setExp(user.getExp() + expGained);
                userMapper.updateById(user);
            }
            appendLog(c, "胜利！获得修为 +" + expGained);

            // 掉落（addItem 容量满会抛 BizException；这里 catch 后改写日志，不阻断战斗结束）
            if (monster.getDropItemCode() != null && monster.getDropQuantity() > 0) {
                try {
                    inventoryCoreService.addItem(c.getUserId(), monster.getDropItemCode(), monster.getDropQuantity());
                    droppedCode = monster.getDropItemCode();
                    droppedQty = monster.getDropQuantity();
                    appendLog(c, "获得「" + resolveItemName(monster.getDropItemCode()) + "」×" + monster.getDropQuantity());
                } catch (BizException e) {
                    appendLog(c, "背包已满，掉落物未能拾取（" + e.getMessage() + "）");
                }
            }

            // 把触发战斗的那一格加入 instance.consumed，下次踩到变空地（避免重复战斗）
            // DEFEAT / FLED 不清：妖怪还在，玩家可以再战
            try {
                MapInstance inst = mapInstanceMapper.selectById(c.getMapInstanceId());
                if (inst != null) {
                    java.util.List<String> consumed = inst.getConsumed();
                    if (consumed == null) consumed = new java.util.ArrayList<>();
                    String key = inst.getPosX() + "," + inst.getPosY();
                    if (!consumed.contains(key)) {
                        consumed.add(key);
                        inst.setConsumed(consumed);
                        inst.setUpdatedAt(LocalDateTime.now());
                        mapInstanceMapper.updateById(inst);
                    }
                }
            } catch (Exception e) {
                // 清理格子失败不影响战斗结算
                appendLog(c, "（标记格子失败：" + e.getMessage() + "）");
            }
        } else if (Combat.RESULT_DEFEAT.equals(result)) {
            // 失败：把剩余 HP（最低 1）写回 User
            int remain = Math.max(1, c.getPlayerHp());
            User user = userMapper.selectById(c.getUserId());
            if (user != null) {
                user.setHp(remain);
                userMapper.updateById(user);
            }
            appendLog(c, "你被「" + c.getMonsterName() + "」击败，踉跄撤退（剩余 HP " + remain + "）");
        } else if (Combat.RESULT_FLED.equals(result)) {
            // 逃跑：把当前 HP 写回 User
            User user = userMapper.selectById(c.getUserId());
            if (user != null) {
                user.setHp(c.getPlayerHp());
                userMapper.updateById(user);
            }
        }

        // 写战斗记录
        CombatLog log = new CombatLog();
        log.setUserId(c.getUserId());
        log.setMapInstanceId(c.getMapInstanceId());
        log.setMonsterCode(c.getMonsterCode());
        log.setMonsterName(c.getMonsterName());
        log.setResult(result);
        log.setRounds(c.getRound());
        log.setExpGained(expGained);
        log.setDroppedItemCode(droppedCode);
        log.setDroppedQuantity(droppedQty);
        log.setCreatedAt(LocalDateTime.now());
        combatLogMapper.insert(log);

        c.setUpdatedAt(LocalDateTime.now());
        combatMapper.updateById(c);

        CombatStateResponse r = toStateResponse(c);
        r.setExpGained(expGained);
        r.setDroppedItemCode(droppedCode);
        r.setDroppedQuantity(droppedQty);
        return r;
    }

    // --------------------------------------------------------------
    // 工具
    // --------------------------------------------------------------

    /** 伤害公式：max(1, attack − defense × 0.5)。 */
    static int calcDamage(int atk, int def, int speedIgnored) {
        int raw = (int) Math.round(atk - def * 0.5);
        return Math.max(1, raw);
    }

    /** 逃跑成功率：clamp((playerSpeed − monsterSpeed) × 0.10 + 0.30, 0, 0.90)。 */
    static double fleeChance(int playerSpeed, int monsterSpeed) {
        double p = (playerSpeed - monsterSpeed) * 0.10 + 0.30;
        if (p < 0) p = 0;
        if (p > 0.90) p = 0.90;
        return p;
    }

    private static void appendLog(Combat c, String msg) {
        if (c.getLog() == null) c.setLog(new ArrayList<>());
        c.getLog().add(msg);
    }

    private User requireUser(String username) {
        User u = userMapper.selectByUsername(username);
        if (u == null) throw new BizException("用户不存在");
        return u;
    }

    private MapInstance requireOwnedInstance(Long instanceId, Long userId) {
        MapInstance inst = mapInstanceMapper.selectById(instanceId);
        if (inst == null) throw new BizException("秘境不存在");
        if (!inst.getUserId().equals(userId)) throw new BizException("无权操作该秘境");
        return inst;
    }

    private Combat requireOwnedCombat(Long combatId, Long userId) {
        Combat c = combatMapper.selectById(combatId);
        if (c == null) throw new BizException("战斗不存在");
        if (!c.getUserId().equals(userId)) throw new BizException("无权操作该战斗");
        return c;
    }

    private Monster requireMonster(String code) {
        Monster m = monsterMapper.selectByCode(code);
        if (m == null) throw new BizException("未知怪物: " + code);
        return m;
    }

    /** 解析物品的 hpRestore（兼容将来的 attr 扩展）。 */
    private int restoreHpOf(Item item) {
        if (item == null || item.getAttrsJson() == null) return 0;
        try {
            com.fasterxml.jackson.databind.JsonNode n = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(item.getAttrsJson());
            return n.path("hpRestore").asInt(0);
        } catch (Exception ignored) {
            return 0;
        }
    }

    /** 解析物品名（用于掉落日志）。 */
    private String resolveItemName(String code) {
        Item i = itemMapper.selectByCode(code);
        return i == null ? code : i.getName();
    }

    // ----------- response -----------

    private CombatStateResponse toStateResponse(Combat c) {
        CombatStateResponse r = new CombatStateResponse();
        r.setCombatId(c.getId());
        r.setUserId(c.getUserId());
        r.setMapInstanceId(c.getMapInstanceId());
        r.setMonsterCode(c.getMonsterCode());
        r.setMonsterName(c.getMonsterName());
        r.setMonsterMaxHp(c.getMonsterMaxHp());
        r.setMonsterHp(c.getMonsterHp());
        r.setMonsterAttack(c.getMonsterAttack());
        r.setPlayerHp(c.getPlayerHp());
        r.setPlayerHpSnapshot(c.getPlayerHpSnapshot());
        r.setPlayerAttack(c.getPlayerAttack());
        r.setPlayerDefense(c.getPlayerDefense());
        r.setRound(c.getRound());
        r.setCurrentTurn(c.getCurrentTurn());
        r.setLog(c.getLog() == null ? new ArrayList<>() : c.getLog());
        r.setStatus(c.getStatus());
        r.setResult(c.getResult());
        // 顺手把背包里灵草数量附上，前端据此禁用"服用灵草"按钮
        r.setLingCaoCount(inventoryCoreService.countItem(c.getUserId(), USABLE_IN_COMBAT));
        return r;
    }
}