package com.xiantu.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 入图请求：传入模板 code 创建新实例。
 */
@Data
public class MapEnterRequest {

    @NotBlank(message = "地图 code 不能为空")
    private String templateCode;
}