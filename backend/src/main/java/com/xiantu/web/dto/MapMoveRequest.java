package com.xiantu.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 移动请求：实例 ID + 方向。
 */
@Data
public class MapMoveRequest {

    @NotNull(message = "实例 ID 不能为空")
    private Long instanceId;

    /** UP / DOWN / LEFT / RIGHT */
    @NotBlank(message = "方向不能为空")
    private String direction;
}