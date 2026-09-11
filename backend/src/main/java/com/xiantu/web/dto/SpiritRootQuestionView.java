package com.xiantu.web.dto;

import lombok.Data;

import java.util.List;

/** 灵根测试题（前端展示用，不含权重）。 */
@Data
public class SpiritRootQuestionView {
    private int index;
    private String text;
    private List<SpiritRootOptionView> options;
}