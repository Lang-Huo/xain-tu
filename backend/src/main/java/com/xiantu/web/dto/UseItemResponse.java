package com.xiantu.web.dto;

import lombok.Data;

/**
 * 使用物品结果：是否成功 + 提示 + 气血变化。
 */
@Data
public class UseItemResponse {

    private boolean success;
    private String message;
    private int hpBefore;
    private int hpAfter;
    private int hpChange;
    /** 使用后剩余的物品数量（用于前端刷新该行） */
    private int quantityAfter;
    /** 使用后剩余的种类数（用于刷新容量条） */
    private int usedAfter;
    private int capacity;
}