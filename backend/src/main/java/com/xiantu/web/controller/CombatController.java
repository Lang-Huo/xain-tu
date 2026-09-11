package com.xiantu.web.controller;

import com.xiantu.common.Result;
import com.xiantu.service.CombatService;
import com.xiantu.service.InventoryService;
import com.xiantu.web.dto.CombatActionRequest;
import com.xiantu.web.dto.CombatStartRequest;
import com.xiantu.web.dto.CombatStateResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 战斗 REST。
 * <ul>
 *   <li>POST /api/combat/start         —— 触发战斗（移动到 MONSTER 格后调用）</li>
 *   <li>POST /api/combat/action        —— 玩家行动（ATTACK / USE_ITEM / FLEE）</li>
 *   <li>GET  /api/combat/{id}          —— 查询战斗状态（前端刷新用）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/combat")
public class CombatController {

    private final CombatService combatService;
    private final InventoryService inventoryService;

    public CombatController(CombatService combatService, InventoryService inventoryService) {
        this.combatService = combatService;
        this.inventoryService = inventoryService;
    }

    @PostMapping("/start")
    public Result<CombatStateResponse> start(@Valid @RequestBody CombatStartRequest req,
                                              Authentication auth) {
        return Result.ok(combatService.startCombat(auth.getName(), req.getMapInstanceId(), req.getMonsterCode()));
    }

    @PostMapping("/action")
    public Result<CombatStateResponse> action(@Valid @RequestBody CombatActionRequest req,
                                              Authentication auth) {
        return Result.ok(combatService.action(auth.getName(), req.getCombatId(), req.getAction(), req.getItemCode()));
    }

    @GetMapping("/{id}")
    public Result<CombatStateResponse> get(@PathVariable Long id, Authentication auth) {
        return Result.ok(combatService.getState(auth.getName(), id));
    }

    private Long resolveUserId(String username) {
        return inventoryService.resolveUserId(username);
    }
}