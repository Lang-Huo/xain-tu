package com.xiantu.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 使用物品请求：传入物品 code；后端按 code 找到物品定义 + 背包行，扣 1 数量并应用效果。
 */
@Data
public class UseItemRequest {

    @NotBlank(message = "物品 code 不能为空")
    private String itemCode;
}