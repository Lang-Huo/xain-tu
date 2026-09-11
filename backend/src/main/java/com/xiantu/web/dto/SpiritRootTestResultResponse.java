package com.xiantu.web.dto;

import lombok.Data;

import java.util.List;

/** 灵根测试结果响应：用户觉醒的灵根列表（无主副、无属性加成）。 */
@Data
public class SpiritRootTestResultResponse {
    /** 用户觉醒的全部灵根（数组顺序即展示顺序；个数为 1=单灵根，2=双灵根）。 */
    private List<SpiritRootInfo> roots;
    /** 是否双灵根（roots.size() >= 2） */
    private boolean dual;
}