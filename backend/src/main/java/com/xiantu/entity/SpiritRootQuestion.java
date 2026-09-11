package com.xiantu.entity;

import lombok.Data;

import java.util.List;

/**
 * 灵根测试题目（服务端含权重，对前端隐藏）。
 */
@Data
public class SpiritRootQuestion {
    private int index;
    private String text;
    private List<SpiritRootOption> options;
}