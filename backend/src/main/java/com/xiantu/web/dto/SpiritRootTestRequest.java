package com.xiantu.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 灵根测试提交：5 道题的选项下标（每题 0-3）。 */
@Data
public class SpiritRootTestRequest {
    @NotNull
    @Size(min = 5, max = 5, message = "需提供 5 道题的答案")
    private List<Integer> answers;
}