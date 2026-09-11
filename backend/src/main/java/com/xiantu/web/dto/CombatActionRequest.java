package com.xiantu.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 战斗中玩家行动请求：{@code action} ∈ {ATTACK, USE_ITEM, FLEE}；USE_ITEM 时 {@code itemCode} 必填。
 */
@Data
public class CombatActionRequest {

    @NotNull(message = "combatId 不能为空")
    private Long combatId;

    @NotBlank(message = "action 不能为空")
    private String action;

    /** USE_ITEM 时必填 */
    private String itemCode;
}