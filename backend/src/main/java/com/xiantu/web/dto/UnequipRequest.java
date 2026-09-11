package com.xiantu.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 卸下装备请求：传入槽位 code（WEAPON 法器 / ARMOR 护身 / STORAGE 储物 / TECHNIQUE 功法）。
 * 后端按 slot 找到当前装备并放回。
 */
@Data
public class UnequipRequest {

    @NotBlank(message = "槽位 code 不能为空")
    private String slot;
}