package com.xiantu.web.controller;

import com.xiantu.common.Result;
import com.xiantu.service.InventoryService;
import com.xiantu.service.LoadoutService;
import com.xiantu.web.dto.InventoryView;
import com.xiantu.web.dto.UnequipRequest;
import com.xiantu.web.dto.UseItemRequest;
import com.xiantu.web.dto.UseItemResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 背包 REST。
 * <ul>
 *   <li>GET  /api/inventory       —— 查看背包（含容量、装备槽、物品列表）</li>
 *   <li>POST /api/inventory/use   —— 使用物品（按 itemCode 扣 1 并应用效果）</li>
 *   <li>POST /api/inventory/equip —— 装备背包里某件物品到对应槽位</li>
 *   <li>POST /api/inventory/unequip —— 卸下指定槽位的装备（回到背包）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final LoadoutService loadoutService;

    public InventoryController(InventoryService inventoryService, LoadoutService loadoutService) {
        this.inventoryService = inventoryService;
        this.loadoutService = loadoutService;
    }

    @GetMapping("")
    public Result<InventoryView> view(Authentication auth) {
        return Result.ok(inventoryService.view(resolveUserId(auth.getName())));
    }

    @PostMapping("/use")
    public Result<UseItemResponse> use(@Valid @RequestBody UseItemRequest req,
                                       Authentication auth) {
        return Result.ok(inventoryService.useItem(resolveUserId(auth.getName()), req.getItemCode()));
    }

    @PostMapping("/equip")
    public Result<Void> equip(@Valid @RequestBody UseItemRequest req, Authentication auth) {
        loadoutService.equip(resolveUserId(auth.getName()), req.getItemCode());
        return Result.ok();
    }

    @PostMapping("/unequip")
    public Result<Void> unequip(@RequestBody UnequipRequest req, Authentication auth) {
        loadoutService.unequip(resolveUserId(auth.getName()), req.getSlot());
        return Result.ok();
    }

    private Long resolveUserId(String username) {
        return inventoryService.resolveUserId(username);
    }
}