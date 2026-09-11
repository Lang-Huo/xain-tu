package com.xiantu.web.controller;

import com.xiantu.common.Result;
import com.xiantu.entity.MapTemplate;
import com.xiantu.service.MapService;
import com.xiantu.web.dto.MapEnterRequest;
import com.xiantu.web.dto.MapMoveRequest;
import com.xiantu.web.dto.MapMoveResponse;
import com.xiantu.web.dto.MapStateResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 地图 REST 接口。
 *
 * <ul>
 *   <li>GET  /api/map/templates         —— 可用模板</li>
 *   <li>POST /api/map/enter            —— 入图（创建新实例）</li>
 *   <li>GET  /api/map/state?instanceId —— 取当前实例完整状态（刷新用）</li>
 *   <li>POST /api/map/move             —— 移动一步</li>
 *   <li>POST /api/map/abandon          —— 主动结束本次探索</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/map")
public class MapController {

    private final MapService mapService;

    public MapController(MapService mapService) {
        this.mapService = mapService;
    }

    @GetMapping("/templates")
    public Result<List<MapTemplate>> templates() {
        return Result.ok(mapService.listTemplates());
    }

    @PostMapping("/enter")
    public Result<MapStateResponse> enter(@Valid @RequestBody MapEnterRequest req,
                                          Authentication auth) {
        return Result.ok(mapService.enter(auth.getName(), req.getTemplateCode()));
    }

    @GetMapping("/state")
    public Result<MapStateResponse> state(@RequestParam Long instanceId,
                                          Authentication auth) {
        return Result.ok(mapService.getState(auth.getName(), instanceId));
    }

    /**
     * 取当前 ACTIVE 实例的状态（前端用于「继续上次探索」按钮）。
     * 没有 ACTIVE 时返回 code=0, data=null。
     */
    @GetMapping("/active")
    public Result<MapStateResponse> active(Authentication auth) {
        return Result.ok(mapService.getActiveState(auth.getName()));
    }

    @PostMapping("/move")
    public Result<MapMoveResponse> move(@Valid @RequestBody MapMoveRequest req,
                                        Authentication auth) {
        return Result.ok(mapService.move(auth.getName(), req.getInstanceId(), req.getDirection()));
    }

    @PostMapping("/abandon")
    public Result<Void> abandon(@RequestParam Long instanceId, Authentication auth) {
        mapService.abandon(auth.getName(), instanceId);
        return Result.ok();
    }
}