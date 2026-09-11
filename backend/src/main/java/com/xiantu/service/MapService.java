package com.xiantu.service;

import com.xiantu.common.BizException;
import com.xiantu.common.CellType;
import com.xiantu.common.Realms;
import com.xiantu.entity.MapInstance;
import com.xiantu.entity.MapTemplate;
import com.xiantu.entity.User;
import com.xiantu.mapper.MapInstanceMapper;
import com.xiantu.mapper.MapTemplateMapper;
import com.xiantu.mapper.UserMapper;
import com.xiantu.web.dto.CellView;
import com.xiantu.web.dto.MapMoveResponse;
import com.xiantu.web.dto.MapStateResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 地图服务：enter / move / getState / abandon。
 *
 * <p>数据组织：实例表只存 seed，格子布局按 seed 确定性重生成；consumed 列表（已采集的资源）覆盖在底图之上。
 * 每次读取时拼装完整 {@link MapStateResponse} 一次性返回（含迷雾三态）。
 */
@Service
public class MapService {

    private static final String RESOURCE_NAME = "灵草";   // 一期只一种资源
    private static final String RESOURCE_CODE = "LING_CAO"; // 与 t_item.code 对齐
    private static final String MONSTER_NAME = "后山妖鼠"; // 一期只一种野怪
    private static final int MONSTER_LEVEL = 3;

    private final MapTemplateMapper templateMapper;
    private final MapInstanceMapper instanceMapper;
    private final UserMapper userMapper;
    private final MapGenerator generator;
    private final FogService fogService;
    private final InventoryCoreService inventoryCoreService;

    public MapService(MapTemplateMapper templateMapper,
                       MapInstanceMapper instanceMapper,
                       UserMapper userMapper,
                       MapGenerator generator,
                       FogService fogService,
                       InventoryCoreService inventoryCoreService) {
        this.templateMapper = templateMapper;
        this.instanceMapper = instanceMapper;
        this.userMapper = userMapper;
        this.generator = generator;
        this.fogService = fogService;
        this.inventoryCoreService = inventoryCoreService;
    }

    /** 列出可用的地图模板（前端展示）。 */
    public List<MapTemplate> listTemplates() {
        List<MapTemplate> list = templateMapper.selectList(null);
        // 把 code 翻译为中文名（非持久化字段，序列化时随 entity 一起返回）
        for (MapTemplate t : list) {
            t.setRecommendedRealmName(Realms.nameOf(t.getRecommendedRealm()));
        }
        return list;
    }

    /**
     * 进入秘境：创建一条 ACTIVE 实例，返回完整状态。
     * 若用户已有 ACTIVE 实例则报错（避免并发；如需切换地图请先 abandon）。
     */
    @Transactional
    public MapStateResponse enter(String username, String templateCode) {
        User user = requireUser(username);
        MapInstance active = instanceMapper.selectActiveByUserId(user.getId());
        if (active != null) {
            throw new BizException("你已在秘境中，请先离开当前秘境再进入新的");
        }
        MapTemplate tpl = templateMapper.selectByCode(templateCode);
        if (tpl == null) {
            throw new BizException("未找到地图模板: " + templateCode);
        }

        long seed = newSeed(user.getId());
        CellType[][] grid = generator.tryGenerate(tpl, seed);
        if (grid == null) {
            throw new BizException("地图生成失败，请重试");
        }

        MapInstance inst = new MapInstance();
        inst.setUserId(user.getId());
        inst.setTemplateId(tpl.getId());
        inst.setTemplateCode(tpl.getCode());
        inst.setSeed(seed);
        inst.setPosX(0);
        inst.setPosY(0);
        inst.setStepCount(0);
        inst.setMaxSteps(tpl.getMaxSteps());
        inst.setStatus(MapInstance.STATUS_ACTIVE);
        List<String> explored = new ArrayList<>();
        explored.add("0,0");
        inst.setExplored(explored);
        inst.setConsumed(new ArrayList<>());
        LocalDateTime now = LocalDateTime.now();
        inst.setCreatedAt(now);
        inst.setUpdatedAt(now);
        instanceMapper.insert(inst);

        return buildState(inst, tpl, grid);
    }

    /**
     * 移动一步。返回移动事件 + 最新状态。
     */
    @Transactional
    public MapMoveResponse move(String username, Long instanceId, String direction) {
        User user = requireUser(username);
        MapInstance inst = requireOwnedInstance(instanceId, user.getId());

        if (!MapInstance.STATUS_ACTIVE.equals(inst.getStatus())) {
            throw new BizException("本次探索已结束，无法移动");
        }
        if (inst.getStepCount() >= inst.getMaxSteps()) {
            // 自动结算为 ABANDONED
            inst.setStatus(MapInstance.STATUS_ABANDONED);
            inst.setUpdatedAt(LocalDateTime.now());
            instanceMapper.updateById(inst);
            throw new BizException("步数耗尽，已自动结束本次探索");
        }

        int nx = inst.getPosX();
        int ny = inst.getPosY();
        switch (direction == null ? "" : direction.toUpperCase()) {
            case "UP":    ny--; break;
            case "DOWN":  ny++; break;
            case "LEFT":  nx--; break;
            case "RIGHT": nx++; break;
            default: throw new BizException("未知方向: " + direction);
        }

        MapTemplate tpl = templateMapper.selectByCode(inst.getTemplateCode());
        if (tpl == null) throw new BizException("模板不存在");
        CellType[][] grid = generator.regenerate(tpl, inst.getSeed());
        int size = tpl.getSize();

        MapMoveResponse resp = new MapMoveResponse();

        if (nx < 0 || nx >= size || ny < 0 || ny >= size) {
            // 越界（前端通常已拦截，这里兜底）
            MapStateResponse state = buildState(inst, tpl, grid);
            resp.setEvent("OUT_OF_BOUNDS");
            resp.setMessage("无法移动到地图之外");
            resp.setState(state);
            return resp;
        }

        // 计算真实格子类型（已采集的资源视为空地）
        CellType cellType = grid[nx][ny];
        if (inst.getConsumed() != null && inst.getConsumed().contains(nx + "," + ny)) {
            cellType = CellType.EMPTY;
        }

        if (cellType == CellType.OBSTACLE) {
            // 不前进
            MapStateResponse state = buildState(inst, tpl, grid);
            resp.setEvent("BLOCKED");
            resp.setMessage("前方有巨石挡路");
            resp.setState(state);
            return resp;
        }

        // 移动到新格
        inst.setPosX(nx);
        inst.setPosY(ny);
        inst.setStepCount(inst.getStepCount() + 1);
        // 标记新坐标为已探索
        if (inst.getExplored() == null) inst.setExplored(new ArrayList<>());
        String key = nx + "," + ny;
        if (!inst.getExplored().contains(key)) inst.getExplored().add(key);

        switch (cellType) {
            case RESOURCE: {
                // 采集：先尝试入背包；若失败（背包满）也照常前进，但消息里带上失败原因。
                if (!inst.getConsumed().contains(key)) inst.getConsumed().add(key);
                boolean added = false;
                String addErr = null;
                try {
                    inventoryCoreService.addItem(user.getId(), RESOURCE_CODE, 1);
                    added = true;
                } catch (BizException e) {
                    addErr = e.getMessage();
                }
                resp.setEvent("RESOURCE_COLLECTED");
                if (added) {
                    resp.setMessage("采集到「" + RESOURCE_NAME + "」×1");
                } else {
                    String reason = (addErr == null || addErr.isEmpty()) ? "未入背包" : addErr;
                    resp.setMessage("采集到「" + RESOURCE_NAME + "」×1，但 " + reason);
                }
                resp.setResourceName(RESOURCE_NAME);
                break;
            }
            case MONSTER: {
                // 野怪保留（M3 占位，不实装战斗）
                resp.setEvent("MONSTER_ENCOUNTER");
                resp.setMessage("遭遇「" + MONSTER_NAME + "」！战斗系统将于 M4 实装");
                resp.setMonsterName(MONSTER_NAME);
                resp.setMonsterLevel(MONSTER_LEVEL);
                break;
            }
            case EXIT: {
                inst.setStatus(MapInstance.STATUS_COMPLETED);
                resp.setEvent("EXIT_REACHED");
                resp.setMessage("成功走出秘境！本次探索结束");
                break;
            }
            default: {
                resp.setEvent("MOVED");
                resp.setMessage("");
            }
        }

        inst.setUpdatedAt(LocalDateTime.now());
        instanceMapper.updateById(inst);

        resp.setState(buildState(inst, tpl, grid));
        return resp;
    }

    /** 取当前实例的完整状态（用于刷新）。 */
    public MapStateResponse getState(String username, Long instanceId) {
        User user = requireUser(username);
        MapInstance inst = requireOwnedInstance(instanceId, user.getId());
        MapTemplate tpl = templateMapper.selectByCode(inst.getTemplateCode());
        if (tpl == null) throw new BizException("模板不存在");
        CellType[][] grid = generator.regenerate(tpl, inst.getSeed());
        return buildState(inst, tpl, grid);
    }

    /**
     * 取当前用户的 ACTIVE 实例状态；没有则返回 null。
     * 用于前端「继续上次探索」按钮的判断。
     */
    public MapStateResponse getActiveState(String username) {
        User user = requireUser(username);
        MapInstance inst = instanceMapper.selectActiveByUserId(user.getId());
        if (inst == null) return null;
        MapTemplate tpl = templateMapper.selectByCode(inst.getTemplateCode());
        if (tpl == null) return null;
        CellType[][] grid = generator.regenerate(tpl, inst.getSeed());
        return buildState(inst, tpl, grid);
    }

    /** 主动放弃当前秘境。 */
    @Transactional
    public void abandon(String username, Long instanceId) {
        User user = requireUser(username);
        MapInstance inst = requireOwnedInstance(instanceId, user.getId());
        inst.setStatus(MapInstance.STATUS_ABANDONED);
        inst.setUpdatedAt(LocalDateTime.now());
        instanceMapper.updateById(inst);
    }

    // ---------- helpers ----------

    private User requireUser(String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) throw new BizException("用户不存在");
        return user;
    }

    private MapInstance requireOwnedInstance(Long instanceId, Long userId) {
        MapInstance inst = instanceMapper.selectById(instanceId);
        if (inst == null) throw new BizException("秘境不存在");
        if (!inst.getUserId().equals(userId)) throw new BizException("无权操作该秘境");
        return inst;
    }

    private long newSeed(long userId) {
        long now = System.currentTimeMillis();
        long rnd = ThreadLocalRandom.current().nextLong();
        return now ^ (userId * 0x9E3779B97F4A7C15L) ^ rnd;
    }

    private MapStateResponse buildState(MapInstance inst, MapTemplate tpl, CellType[][] grid) {
        User user = userMapper.selectById(inst.getUserId());
        int radius = user == null ? 2 : fogService.radiusFromSpiritualSense(user.getSpiritualSense());

        int size = tpl.getSize();
        // 当前可见集合
        boolean[][] visMask = fogService.visibleMask(size, inst.getPosX(), inst.getPosY(), radius);
        // 已探索集合（去重）
        Set<String> explored = new HashSet<>();
        if (inst.getExplored() != null) explored.addAll(inst.getExplored());

        List<CellView> cells = new ArrayList<>(size * size);
        // 按行扫描：x 是行（0 在最上），y 是列
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                CellType baseType = grid[x][y];
                if (inst.getConsumed() != null && inst.getConsumed().contains(x + "," + y)) {
                    baseType = CellType.EMPTY;
                }
                String fog;
                boolean isVis = visMask[x][y];
                boolean isExpl = explored.contains(x + "," + y);
                if (isVis) {
                    fog = "VISIBLE";
                } else if (isExpl) {
                    fog = "MEMORY";
                } else {
                    fog = "HIDDEN";
                }
                String code = null;
                if (baseType == CellType.RESOURCE) code = RESOURCE_NAME;
                else if (baseType == CellType.MONSTER) code = MONSTER_NAME;
                else if (baseType == CellType.EXIT) code = "EXIT";
                cells.add(new CellView(x, y, baseType, fog, code));
            }
        }

        MapStateResponse resp = new MapStateResponse();
        resp.setInstanceId(inst.getId());
        resp.setTemplateCode(inst.getTemplateCode());
        resp.setTemplateName(tpl.getName());
        resp.setSize(size);
        resp.setPosX(inst.getPosX());
        resp.setPosY(inst.getPosY());
        resp.setStepCount(inst.getStepCount());
        resp.setMaxSteps(inst.getMaxSteps());
        resp.setStatus(inst.getStatus());
        resp.setRadius(radius);
        resp.setCells(cells);
        return resp;
    }
}