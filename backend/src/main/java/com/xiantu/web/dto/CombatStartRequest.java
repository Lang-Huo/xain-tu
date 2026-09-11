package com.xiantu.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 触发战斗请求：移动到 MONSTER 格后由前端发起。
 * 一期 monsterCode 后端写死为 HOU_SHAN_YAO_SHU，但保留传参便于后续扩展。
 */
@Data
public class CombatStartRequest {

    @NotNull(message = "mapInstanceId 不能为空")
    private Long mapInstanceId;

    @NotBlank(message = "monsterCode 不能为空")
    private String monsterCode;
}