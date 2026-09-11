package com.xiantu.entity;

import lombok.Data;

import java.util.Map;

/**
 * 灵根测试题目选项（服务端含权重，对前端隐藏）。
 */
@Data
public class SpiritRootOption {
    private int index;
    private String text;
    /** 选项对各灵根的权重分，例如 {"GOLD":2, "FIRE":1} */
    private Map<String, Integer> weights;
}